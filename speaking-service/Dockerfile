FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY target/speaking-service-0.1.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
