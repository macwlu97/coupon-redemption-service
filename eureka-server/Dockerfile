# STAGE 1: Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy parent pom and service pom
COPY pom.xml ../pom.xml
COPY eureka-server/pom.xml ./pom.xml

# Cache dependencies
RUN mvn dependency:go-offline -B

# Build
COPY eureka-server/src ./src
RUN mvn clean package -DskipTests

# STAGE 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]