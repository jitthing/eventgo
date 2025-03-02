# Stage 1: Build the application
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Create runtime image with Java 21
FROM openjdk:21-slim
WORKDIR /app

# Copy the jar file from the build stage
# Here we rename the jar to app.jar for simplicity
COPY --from=build /app/target/booking-service-1.0-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8000

# Run the Spring Boot application
CMD ["java", "-jar", "app.jar"]

