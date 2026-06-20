# ============================================================
# Multi-stage Docker build
#
# Stage 1 (builder): Maven compiles the app and produces a fat JAR.
#   - We copy pom.xml first and download dependencies before copying source.
#   - This exploits Docker layer caching: if only source changes, Maven
#     doesn't re-download the internet on every build.
#
# Stage 2 (runtime): Minimal JRE image — no Maven, no source code.
#   - eclipse-temurin:21-jre-jammy is ~200MB vs the full JDK at ~400MB.
#   - Smaller image = faster pull in Azure Container Apps = faster cold starts.
# ============================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom.xml separately to cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B --no-transfer-progress

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Non-root user for security (Azure Container Apps requirement for some policies)
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser

# Copy only the fat JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# -Djava.security.egd speeds up SecureRandom on Linux (relevant for JWT signing)
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
