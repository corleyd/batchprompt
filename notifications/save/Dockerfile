FROM eclipse-temurin:21-jdk-alpine as builder
WORKDIR /app

COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./
COPY common/ common/
COPY notifications/ notifications/

RUN ./gradlew :notifications:notifications-service:build -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/notifications/notifications-service/build/libs/*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
