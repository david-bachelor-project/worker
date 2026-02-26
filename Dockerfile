FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
  && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
     tesseract-ocr tesseract-ocr-eng \
  && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /opt/paperless/access-logs/incoming \
    && mkdir -p /opt/paperless/access-logs/archive \
    && mkdir -p /opt/paperless/access-logs/error

WORKDIR /app

# Copy the JAR
COPY target/*.jar app.jar

#COPY *.xml /opt/paperless/access-logs/incoming/

ENTRYPOINT ["java", "-jar", "app.jar"]
