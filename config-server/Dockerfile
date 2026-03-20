FROM openjdk:17-jdk-slim

# Install git, curl, net-tools and other useful diagnostic tools
RUN apt-get update && apt-get install -y \
    git \
    curl \
    net-tools \
    && rm -rf /var/lib/apt/lists/*


# Set path to jar file
ARG JAR_FILE=target/config-server-0.0.1-SNAPSHOT.jar

# copy jar file to container
COPY ${JAR_FILE} config-server.jar

# run app
ENTRYPOINT ["java", "-jar", "/config-server.jar"]
