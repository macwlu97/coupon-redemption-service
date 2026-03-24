# --- STAGE 1: Build process ---
# Using Maven with JDK 21 on Alpine Linux for a repeatable and lightweight build environment.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Step 1: Copy the Parent POM
# This is essential for resolving shared dependencies and plugin versions across the multi-module project.
COPY pom.xml .

# Step 2: FIX - Create empty directories for all sub-modules defined in the Parent POM.
# Maven's reactor requires these folders to exist to pass the project structure validation.
RUN mkdir api-gateway config-server coupon-service usage-service eureka-server

# Step 3: Copy POM files for ALL modules.
# This allows 'dependency:go-offline' to work and ensures Docker can cache dependencies effectively.
COPY api-gateway/pom.xml api-gateway/
COPY config-server/pom.xml config-server/
COPY coupon-service/pom.xml coupon-service/
COPY usage-service/pom.xml usage-service/
COPY eureka-server/pom.xml eureka-server/

# Step 4: Pre-fetch Maven dependencies (Cacheable layer)
# -pl (project list) targets only the required module to optimize caching.
RUN mvn -pl coupon-service dependency:go-offline -B

# Step 5: Copy the actual source code for the Coupon Service module
COPY coupon-service/src coupon-service/src

# Step 6: Compile and package the application
# -am (also make) ensures any required parent/dependency artifacts are built.
RUN mvn clean package -DskipTests -pl coupon-service -am

# --- STAGE 2: Runtime environment ---
# Slim JRE image for a production-ready, small, and secure container.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for health checking (best practice for service orchestration in Docker)
RUN apk add --no-cache curl

# Copy the built JAR from the 'build' stage.
COPY --from=build /app/coupon-service/target/*.jar app.jar

# SECURITY: Run the application as a non-root user.
# This prevents potential security vulnerabilities related to root access inside the container.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# DOCUMENTATION: As per your application.yml, the service listens on port 8082.
EXPOSE 8082

# PERFORMANCE & TUNING:
# -XX:+UseContainerSupport: Makes the JVM aware of Docker memory/CPU limits.
# -XX:MaxRAMPercentage: Dynamically allocates heap memory based on container limits (set to 75%).
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]