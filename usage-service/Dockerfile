FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/usage-service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} usage-service.jar
ENTRYPOINT ["java", "-jar", "/usage-service.jar"]
