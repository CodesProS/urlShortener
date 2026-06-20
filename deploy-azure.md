# Azure Deployment Guide

**Architecture:**
```
Internet → Azure Container Apps (auto-scales)
               ↓                    ↓
    Azure Database for       Azure Cache for Redis
      PostgreSQL (Flex)         (Basic C0)
               ↓
    Azure Container Registry (image storage)
               ↓
    Azure Monitor + Application Insights (observability)
```

**Why Container Apps over App Service?**
- Auto-scales to zero replicas when idle → $0 cost on free tier
- Event-driven scaling with KEDA (e.g., scale on Redis queue length)
- Container-native: you deploy an image, not a runtime platform
- Built-in ingress with TLS — no cert management

---

## Prerequisites

```bash
az --version          # Azure CLI 2.50+
docker --version      # Docker 24+
```

---

## Step 1 — Login and Resource Group

```bash
az login

# Create a resource group (logical container for all resources)
az group create \
  --name urlshortener-rg \
  --location eastus
```

---

## Step 2 — Azure Container Registry (ACR)

ACR stores your Docker images. Container Apps pulls from it directly.

```bash
az acr create \
  --resource-group urlshortener-rg \
  --name urlshorteneracr \
  --sku Basic \
  --admin-enabled true

# Build and push the image directly in Azure (no local Docker daemon needed)
az acr build \
  --registry urlshorteneracr \
  --image urlshortener:latest \
  --file Dockerfile \
  .
```

If you prefer building locally:
```bash
docker build -t urlshorteneracr.azurecr.io/urlshortener:latest .
az acr login --name urlshorteneracr
docker push urlshorteneracr.azurecr.io/urlshortener:latest
```

---

## Step 3 — Azure Cache for Redis

```bash
# Basic C0 = cheapest tier, single node, 250MB. Fine for caching short URLs.
az redis create \
  --name urlshortener-redis \
  --resource-group urlshortener-rg \
  --location eastus \
  --sku Basic \
  --vm-size c0

# Get the access key (you'll need this for the Container App env vars)
az redis list-keys \
  --name urlshortener-redis \
  --resource-group urlshortener-rg \
  --query primaryKey \
  --output tsv
```

---

## Step 4 — Azure Database for PostgreSQL (Flexible Server)

```bash
az postgres flexible-server create \
  --resource-group urlshortener-rg \
  --name urlshortener-db \
  --location eastus \
  --admin-user pgadmin \
  --admin-password "YourStrongPassword123!" \
  --tier Burstable \
  --sku-name Standard_B1ms \
  --version 16 \
  --public-access 0.0.0.0

# Create the application database
az postgres flexible-server db create \
  --resource-group urlshortener-rg \
  --server-name urlshortener-db \
  --database-name urlshortener
```

---

## Step 5 — Container Apps Environment

```bash
az containerapp env create \
  --name urlshortener-env \
  --resource-group urlshortener-rg \
  --location eastus
```

---

## Step 6 — Deploy the Container App

Replace `<REDIS_KEY>` with the key from Step 3.

```bash
az containerapp create \
  --name urlshortener-app \
  --resource-group urlshortener-rg \
  --environment urlshortener-env \
  --image urlshorteneracr.azurecr.io/urlshortener:latest \
  --registry-server urlshorteneracr.azurecr.io \
  --registry-username urlshorteneracr \
  --registry-password "$(az acr credential show --name urlshorteneracr --query passwords[0].value -o tsv)" \
  --target-port 8080 \
  --ingress external \
  --min-replicas 1 \
  --max-replicas 3 \
  --cpu 0.5 \
  --memory 1Gi \
  --env-vars \
    "SPRING_DATASOURCE_URL=jdbc:postgresql://urlshortener-db.postgres.database.azure.com:5432/urlshortener" \
    "SPRING_DATASOURCE_USERNAME=pgadmin" \
    "SPRING_DATASOURCE_PASSWORD=YourStrongPassword123!" \
    "SPRING_DATA_REDIS_HOST=urlshortener-redis.redis.cache.windows.net" \
    "SPRING_DATA_REDIS_PORT=6380" \
    "SPRING_DATA_REDIS_SSL_ENABLED=true" \
    "SPRING_DATA_REDIS_PASSWORD=<REDIS_KEY>" \
    "JWT_SECRET=replace-with-a-32plus-char-random-string" \
    "APP_BASE_URL=https://urlshortener-app.azurecontainerapps.io"

# Get the public URL
az containerapp show \
  --name urlshortener-app \
  --resource-group urlshortener-rg \
  --query properties.configuration.ingress.fqdn \
  --output tsv
```

---

## Step 7 — Application Insights (Monitoring)

Application Insights for Java uses a **zero-code agent** — no Maven dependency needed.
It auto-instruments HTTP requests, JPA queries, Redis calls, and exceptions.

```bash
# Create the Application Insights resource
az monitor app-insights component create \
  --app urlshortener-insights \
  --location eastus \
  --resource-group urlshortener-rg \
  --application-type web

# Get the connection string
az monitor app-insights component show \
  --app urlshortener-insights \
  --resource-group urlshortener-rg \
  --query connectionString \
  --output tsv
```

Add `APPLICATIONINSIGHTS_CONNECTION_STRING` to your Container App env vars:
```bash
az containerapp update \
  --name urlshortener-app \
  --resource-group urlshortener-rg \
  --set-env-vars "APPLICATIONINSIGHTS_CONNECTION_STRING=<connection-string-from-above>"
```

The agent is activated automatically when the env var is present and the App Insights
Java agent JAR is on the classpath — for Container Apps, attach it via the Dockerfile:

```dockerfile
# Add to Dockerfile before ENTRYPOINT:
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.5.4/applicationinsights-agent-3.5.4.jar /app/applicationinsights-agent.jar
ENTRYPOINT ["java", \
  "-javaagent:/app/applicationinsights-agent.jar", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

---

## Step 8 — Rolling Updates

Every time you push new code:

```bash
# Rebuild and push
az acr build --registry urlshorteneracr --image urlshortener:latest .

# Force Container Apps to pull the new image
az containerapp update \
  --name urlshortener-app \
  --resource-group urlshortener-rg \
  --image urlshorteneracr.azurecr.io/urlshortener:latest
```

---

## Step 9 — Tear Down (avoid surprise bills)

```bash
az group delete --name urlshortener-rg --yes --no-wait
```

---

## Environment Variables Reference

| Variable | Local Dev Default | Production Example |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://127.0.0.1:5433/urlshortener` | Azure PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | `pgadmin` |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | your password |
| `SPRING_DATA_REDIS_HOST` | `localhost` | `urlshortener-redis.redis.cache.windows.net` |
| `SPRING_DATA_REDIS_PORT` | `6379` | `6380` (Azure uses TLS port) |
| `SPRING_DATA_REDIS_SSL_ENABLED` | `false` | `true` |
| `SPRING_DATA_REDIS_PASSWORD` | *(empty)* | Redis primary key |
| `JWT_SECRET` | dev placeholder | 32+ char random string |
| `APP_BASE_URL` | `http://localhost:8080` | your Container Apps FQDN |
| `APPLICATIONINSIGHTS_CONNECTION_STRING` | *(not set)* | from App Insights |
