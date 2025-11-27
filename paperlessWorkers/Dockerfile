FROM maven:3.9.9-amazoncorretto-21-alpine AS build
WORKDIR /usr/app

COPY pom.xml .
RUN mvn verify --fail-never

COPY . .

RUN mvn package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app

# Install Tesseract OCR, Ghostscript, language data (English, German) and OSD (Orientation and Script Detection)
RUN apk add --no-cache \
    tesseract-ocr \
    tesseract-ocr-data-eng \
    tesseract-ocr-data-deu \
    tesseract-ocr-data-osd \
    ghostscript

# Set Tesseract data path environment variable (Alpine Linux path)
ENV TESSDATA_PREFIX=/usr/share/tessdata

COPY --from=build /usr/app/target/*.jar /app/workers.jar
ENTRYPOINT ["java","-jar","/app/workers.jar"]