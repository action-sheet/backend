# Multi-stage build for Spring Boot (Render Free Tier - 512MB RAM)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create data directories
RUN mkdir -p /data/files /data/repository /data/projects

EXPOSE 8080

# JVM tuned for 512MB RAM free tier
ENTRYPOINT ["java", "-Xmx384m", "-Xms256m", "-XX:+UseSerialGC", "-jar", "app.jar"]
