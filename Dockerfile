# syntax=docker/dockerfile:1

# ---- Build stage: compile + package the Spring Boot jar ----
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Warm the dependency cache first (pom + wrapper change rarely).
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Then build the app.
COPY src ./src
RUN ./mvnw -B -q -DskipTests clean package

# ---- Runtime stage: slim JRE ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# PaaS platforms (Render/Heroku/Railway) inject PORT; application.properties binds
# server.port=${PORT:${SERVER_PORT:8082}}, so no CLI port override is needed.
# Cap the heap so it fits small instances (e.g. Render free = 512 MB).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"

COPY --from=build /app/target/*-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
