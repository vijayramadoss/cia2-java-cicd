# ---------- Stage 1: build the JAR with Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Download dependencies first so Docker can cache this layer
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests already ran in the pipeline's Maven stage, so skip them here
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:21-jre-alpine

# Version shown on the web page (the pipeline passes the Git commit SHA)
ARG APP_VERSION=docker-local
ENV APP_VERSION=${APP_VERSION}

# Run as a non-root user (numeric UID so Kubernetes runAsNonRoot can verify it)
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=build /app/target/taskmanager.jar app.jar
USER 10001

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
