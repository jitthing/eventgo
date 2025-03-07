# Use a lightweight Java image
FROM openjdk:21-jdk-slim

# Set the working directory
WORKDIR /app

# Copy the built application JAR into the container
COPY target/ticket-inventory-0.0.1-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
