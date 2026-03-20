FROM openjdk:17-jdk-slim
ARG JAR_FILE=target/coupon-service-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} coupon-service.jar
ENTRYPOINT ["java", "-jar", "/coupon-service.jar"]
