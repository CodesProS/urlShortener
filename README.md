# URL Shortener API

A production-ready REST API for shortening URLs with click analytics, built with Spring Boot 4 and deployed on Azure Container Apps.

**Live Demo:** https://urlshortener-app.salmontree-c1360b35.canadacentral.azurecontainerapps.io/swagger-ui/index.html

## Features

- **JWT Authentication** — stateless auth with BCrypt password hashing
- **URL Shortening** — 8-char alphanumeric codes with optional custom aliases and expiry
- **Redis Caching** — cache-aside pattern with 10-minute TTL for high-traffic redirects
- **Click Analytics** — total clicks + daily breakdown for the last 30 days
- **Rate Limiting** — Bucket4j token bucket (10 requests/min per IP on shorten endpoint)
- **Swagger UI** — fully documented API with JWT bearer auth support

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 4.0.6, Java 21 |
| Database | PostgreSQL 16 (Azure Flexible Server) |
| Cache | Redis 7 (Azure Managed Redis Enterprise) |
| Auth | JWT (JJWT 0.12), Spring Security 7 |
| Rate Limiting | Bucket4j 8.10.1 |
| Docs | SpringDoc OpenAPI 2.8.6 |
| Deployment | Docker, Azure Container Apps, ACR |

## API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | None | Create account, returns JWT |
| POST | `/api/auth/login` | None | Login, returns JWT |
| POST | `/api/urls/shorten` | JWT | Shorten a URL |
| GET | `/api/urls` | JWT | List your shortened URLs |
| DELETE | `/api/urls/{shortCode}` | JWT | Delete a URL |
| GET | `/{shortCode}` | None | Redirect to original URL |
| GET | `/api/analytics/{shortCode}` | JWT | Click stats for a URL |

## Running Locally

**Prerequisites:** Docker, Java 21, Maven

```bash
# Start PostgreSQL
docker run -d --name urlshortener-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=urlshortener \
  -p 5433:5432 postgres:16-alpine

# Start Redis
docker run -d --name urlshortener-redis \
  -p 6379:6379 redis:7-alpine

# Run the app
./mvnw spring-boot:run
```

Swagger UI: http://localhost:8080/swagger-ui/index.html

## Deployment

Deployed on Azure Container Apps with:
- **ACR** — Docker image registry
- **Azure PostgreSQL Flexible Server** — managed database
- **Azure Managed Redis Enterprise** — managed cache
- **Azure Container Apps** — serverless container hosting (scales to zero)

See [deploy-azure.md](deploy-azure.md) for the full deployment guide.
