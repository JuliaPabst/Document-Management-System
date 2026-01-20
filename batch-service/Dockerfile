# Build stage
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create directories for batch processing
RUN mkdir -p /app/input /app/archive

# Copy the jar file from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose actuator port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
