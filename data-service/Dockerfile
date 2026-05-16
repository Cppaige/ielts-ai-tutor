FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/data-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
