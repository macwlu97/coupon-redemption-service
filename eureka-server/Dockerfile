# --- STAGE 1: Build ---
# Using Maven with JDK 21 on Alpine for a lightweight build environment
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# 1. Copy the Parent POM (Essential for shared versions/dependencies)
COPY pom.xml .

# 2. Create dummy directories for all sub-modules.
# Maven's reactor requires all modules defined in the Parent POM to exist on the file system,
# even if we are only building one of them.
RUN mkdir api-gateway config-server coupon-service usage-service eureka-server

# 3. Copy only the POM files for all modules.
# This allows us to run 'dependency:go-offline' and cache dependencies effectively.
COPY api-gateway/pom.xml api-gateway/
COPY config-server/pom.xml config-server/
COPY coupon-service/pom.xml coupon-service/
COPY usage-service/pom.xml usage-service/
COPY eureka-server/pom.xml eureka-server/

# 4. Pre-fetch dependencies (Cacheable layer)
# -pl (project list) targets only the required module to save time and space.
RUN mvn -pl eureka-server dependency:go-offline -B

# 5. Copy the actual source code for the target module
COPY eureka-server/src eureka-server/src

# 6. Build the application
# -am (also make) ensures all parent/dependency artifacts are built if needed.
RUN mvn clean package -DskipTests -pl eureka-server -am

# --- STAGE 2: Runtime ---
# Using a slim JRE (Java Runtime Environment) for a secure, production-ready image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for Docker health checks (e.g., in docker-compose)
RUN apk add --no-cache curl

# Copy the JAR from the build stage
COPY --from=build /app/eureka-server/target/*.jar app.jar

# Standard port for Eureka Service Registry
EXPOSE 8761

# Security: Run as a non-root user to minimize attack surface
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# JVM Container Awareness:
# -XX:+UseContainerSupport: JVM correctly identifies memory/CPU limits of the container.
# -XX:MaxRAMPercentage: Dynamically sets heap size (75% of container RAM).
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]