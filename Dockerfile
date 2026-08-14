# Imagen de runtime solamente - ver config-server/Dockerfile para el porque.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/credit-service.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
