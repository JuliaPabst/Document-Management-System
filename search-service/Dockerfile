FROM amazoncorretto:21-alpine

WORKDIR /app

# Install Maven
RUN apk add --no-cache maven

# Copy pom.xml
COPY pom.xml ./

# Download dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# Run application
ENTRYPOINT ["java", "-jar", "target/search-service.jar"]
