# Build Stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app



COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage (lightweight)
FROM openjdk:21-jdk-slim
WORKDIR /app
# Install curl
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/ticket-inventory-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
