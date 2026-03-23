# STAGE 1: Build
# We use a Maven image with JDK 21 to compile the code.
# Alpine version is used to keep the build environment lightweight.
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Step 1: Handle Parent POM
# Because your project uses a parent pom in the directory above,
# we need to copy it to satisfy Maven's dependency tree.
COPY pom.xml ../pom.xml

# Step 2: Copy the coupon-service specific pom.xml
COPY coupon-service/pom.xml ./pom.xml

# Step 3: Cache Maven dependencies
# This layer is cached unless pom.xml files change, significantly speeding up builds.
RUN mvn dependency:go-offline -B

# Step 4: Build the application
# We copy the source code and package the JAR.
# Tests are skipped here; they should be part of your CI pipeline, not the container build.
COPY coupon-service/src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Runtime
# Using a slim JRE 21 image for the final production container.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the generated JAR from the build stage.
# Wildcards are used to stay flexible with version numbers.
COPY --from=build /app/target/*.jar app.jar

# SECURITY: Best practice is to run the application as a non-root user.
# This prevents potential container breakout attacks from having root access.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# DOCUMENTATION: Your application.yml specifies port 8082.
EXPOSE 8082

# RUN: Use optimized JVM settings for containers.
# 'XshowSettings:vm' helps debug memory limits in container logs.
ENTRYPOINT ["java", "-XshowSettings:vm", "-jar", "app.jar"]