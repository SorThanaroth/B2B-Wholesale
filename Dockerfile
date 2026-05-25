# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q -DskipTests clean package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ENV SERVER_PORT=8082

COPY --from=build /app/target/*-SNAPSHOT.jar app.jar
EXPOSE 8082
# ENTRYPOINT ["java", "-jar", "/app/app.jar"]
ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT:-8082}"]\