FROM maven:3.9.9-amazoncorretto-21-alpine AS build
WORKDIR /usr/app

COPY pom.xml .
RUN mvn verify --fail-never

COPY . .

RUN mvn package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app

COPY --from=build /usr/app/target/*.jar /app/workers.jar
ENTRYPOINT ["java","-jar","/app/workers.jar"]
