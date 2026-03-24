# --- STAGE 1: Build ---
# Using Maven with JDK 21 on Alpine Linux for a lightweight build environment
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# 1. Copy the Parent POM first to resolve project hierarchy
COPY pom.xml .

# 2. FIX: Create dummy directories for all sub-modules defined in the Parent POM.
# Maven requires these folders to exist to pass the project structure validation.
RUN mkdir api-gateway config-server coupon-service usage-service eureka-server

# 3. Copy POM files for ALL modules to allow dependency resolution.
# This ensures Docker can cache the 'go-offline' step even in a multi-module setup.
COPY api-gateway/pom.xml api-gateway/
COPY config-server/pom.xml config-server/
COPY coupon-service/pom.xml coupon-service/
COPY usage-service/pom.xml usage-service/
COPY eureka-server/pom.xml eureka-server/

# 4. Pre-fetch dependencies to leverage Docker layer caching.
# This prevents re-downloading all JARs if only source code changes.
RUN mvn -pl api-gateway dependency:go-offline -B

# 5. Copy the actual source code of the module
COPY api-gateway/src api-gateway/src

# 6. Build the application package, skipping tests for speed.
# -pl (project list) and -am (also make) target only the gateway and its dependencies.
RUN mvn clean package -DskipTests -pl api-gateway -am

# --- STAGE 2: Runtime ---
# Using a slim JRE (Java Runtime Environment) for the final production image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for health checking purposes in Docker Compose
RUN apk add --no-cache curl

# Copy the compiled executable JAR from the build stage
COPY --from=build /app/api-gateway/target/*.jar app.jar

# Inform Docker that the container listens on port 8080 at runtime
EXPOSE 8080

# Security: Run as a non-root user to limit potential impact of vulnerabilities
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# JVM Tuning for Container Environments:
# -XX:+UseContainerSupport: Makes JVM aware of container CPU/Memory limits.
# -XX:MaxRAMPercentage: Dynamically allocates heap memory (standard 75%).
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]