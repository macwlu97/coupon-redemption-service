# STAGE 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy Parent POM (required for dependency resolution)
COPY pom.xml ../pom.xml
# Copy Config Server POM
COPY config-server/pom.xml ./pom.xml

# Cache dependencies to speed up subsequent builds
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY config-server/src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# Config Server standard port
EXPOSE 8888

# Best practice: Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENTRYPOINT ["java", "-jar", "app.jar"]