# Build stage
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM amazoncorretto:21-alpine
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/email-ingestion-0.0.1-SNAPSHOT.jar app.jar

# Expose actuator port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
