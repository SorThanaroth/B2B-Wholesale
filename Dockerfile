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
ENV SERVER_PORT=8082
# Spring Boot repackages a single runnable jar (the *.original is the thin jar).
COPY --from=build /app/target/*-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
