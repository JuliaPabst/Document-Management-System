FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create directories for batch processing
RUN mkdir -p /data/input /data/archive

# Copy the jar file
COPY target/*.jar app.jar

# Expose actuator port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
