# --- STAGE 1: Build process ---
# Using Maven with JDK 21 on Alpine Linux for a repeatable build environment.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Step 1: Copy the Parent POM
# This is essential for resolving shared dependencies and plugin versions across modules.
COPY pom.xml .

# Step 2: FIX - Create empty directories for all sub-modules defined in the Parent POM.
# Maven's reactor requires these folders to exist during the project structure validation.
RUN mkdir api-gateway config-server coupon-service usage-service eureka-server

# Step 3: Copy POM files for ALL modules.
# This allows 'dependency:go-offline' to work and optimizes Docker layer caching.
COPY api-gateway/pom.xml api-gateway/
COPY config-server/pom.xml config-server/
COPY coupon-service/pom.xml coupon-service/
COPY usage-service/pom.xml usage-service/
COPY eureka-server/pom.xml eureka-server/

# Step 4: Pre-fetch dependencies
# -pl (project list) targets only the required module to optimize caching.
RUN mvn -pl usage-service dependency:go-offline -B

# Step 5: Copy the actual source code for the Usage Service module
COPY usage-service/src usage-service/src

# Step 6: Compile and package
# -am (also make) ensures any required parent/dependency artifacts are built.
RUN mvn clean package -DskipTests -pl usage-service -am

# --- STAGE 2: Runtime environment ---
# Slim JRE image for production-ready, small, and secure containers.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for health checking in Docker Compose (best practice for microservices)
RUN apk add --no-cache curl

# Copy the built JAR from the 'build' stage.
COPY --from=build /app/usage-service/target/*.jar app.jar

# SECURITY: Run the application as a non-root user.
# This prevents potential security vulnerabilities related to root access inside the container.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# DOCUMENTATION: As per your application.yml, the service listens on port 8081.
EXPOSE 8081

# PERFORMANCE & TUNING:
# -XX:+UseContainerSupport: Makes the JVM aware of Docker memory/CPU limits.
# -XX:MaxRAMPercentage: Dynamically allocates heap memory based on container limits.
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]