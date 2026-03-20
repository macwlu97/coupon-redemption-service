FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/api-gateway-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} api-gateway.jar
ENTRYPOINT ["java", "-jar", "/api-gateway.jar"]
