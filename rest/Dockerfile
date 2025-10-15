FROM maven:3.9.9-amazoncorretto-21-alpine AS build
WORKDIR /usr/app

COPY pom.xml .
RUN mvn verify --fail-never

COPY . .

RUN mvn package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app

COPY --from=build /usr/app/target/*.jar /app/runner.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/runner.jar","--server.port=8081"]

