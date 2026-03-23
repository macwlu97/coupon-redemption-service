# STAGE 1: Build process
# Using Maven with JDK 21 on Alpine Linux for a repeatable build environment.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Step 1: Copy Parent POM
# Your project has a parent-child structure. The parent POM is required 
# to resolve shared dependencies and versions.
COPY pom.xml ../pom.xml

# Step 2: Copy Usage Service specific configuration
COPY usage-service/pom.xml ./pom.xml

# Step 3: Cache dependencies
# Pre-downloading dependencies speeds up future builds if the code changes but pom.xml remains the same.
RUN mvn dependency:go-offline -B

# Step 4: Compile and package
# Copy source code and build the executable JAR. Tests are skipped to optimize build time.
COPY usage-service/src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Runtime environment
# Slim JRE image for production-ready, small, and secure containers.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage.
COPY --from=build /app/target/*.jar app.jar

# SECURITY: Run the application as a non-root user.
# This prevents potential security vulnerabilities related to root access inside the container.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# DOCUMENTATION: As per your application.yml, the service listens on port 8081.
EXPOSE 8081

# PERFORMANCE: Use container-aware JVM settings to respect Docker memory limits.
ENTRYPOINT ["java", "-XshowSettings:vm", "-jar", "app.jar"]