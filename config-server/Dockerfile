# --- STAGE 1: Build ---
# Using Maven with JDK 21 on Alpine for a lightweight and secure build environment
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# 1. Copy the Parent POM - essential for resolving shared versions and plugins
COPY pom.xml .

# 2. FIX: Create empty directories for all modules defined in the Parent POM.
# Maven's reactor requires these folders to exist during the project validation phase.
RUN mkdir api-gateway config-server coupon-service usage-service eureka-server

# 3. Copy POM files for ALL modules.
# This is a clean way to leverage Docker layer caching for dependencies.
# If you don't change your pom.xml files, this layer will be cached.
COPY api-gateway/pom.xml api-gateway/
COPY config-server/pom.xml config-server/
COPY coupon-service/pom.xml coupon-service/
COPY usage-service/pom.xml usage-service/
COPY eureka-server/pom.xml eureka-server/

# 4. Pre-fetch dependencies (Cacheable layer)
# -pl (project list) focuses only on this specific module.
RUN mvn -pl config-server dependency:go-offline -B

# 5. Copy the actual source code for the Config Server module
COPY config-server/src config-server/src

# 6. Build the application package while skipping unit tests.
# -am (also-make) ensures that any required parent or sibling dependencies are handled.
RUN mvn clean package -DskipTests -pl config-server -am

# --- STAGE 2: Runtime ---
# Using a slim JRE (Java Runtime Environment) for the final production-ready image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl to allow Docker Compose to perform health checks
RUN apk add --no-cache curl

# Copy the built JAR from the 'build' stage
# Using a wildcard ensures we grab the JAR even if the version changes in POM
COPY --from=build /app/config-server/target/*.jar app.jar

# Expose the default Spring Cloud Config Server port
EXPOSE 8888

# Best Practice: Security - Create and use a non-privileged user.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# JVM Tuning for Containers:
# -XX:+UseContainerSupport: JVM correctly recognizes Docker memory/CPU limits.
# -XX:MaxRAMPercentage: Sets heap size relative to the container's allocated RAM.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]