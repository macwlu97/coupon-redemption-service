# STAGE 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy Parent POM
COPY pom.xml ../pom.xml
# Copy API Gateway POM
COPY api-gateway/pom.xml ./pom.xml

# Cache dependencies
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY api-gateway/src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

# API Gateway standard port
EXPOSE 8080

# Best practice: Run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENTRYPOINT ["java", "-jar", "app.jar"]