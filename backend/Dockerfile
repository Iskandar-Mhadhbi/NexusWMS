# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine

# Non-root user — required for K8s securityContext.runAsNonRoot
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring

EXPOSE 8080

# Matches the docker-compose profile referenced in phase8_progress.md
ENV SPRING_PROFILES_ACTIVE=docker

# Container-level check — belt-and-suspenders alongside K8s's own liveness probe,
# useful for docker-compose / local `docker run` where no orchestrator is watching.
# K8s itself should probe /actuator/health/liveness and /readiness directly, not
# rely on this HEALTHCHECK instruction (K8s ignores it by default anyway).
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1
    
# Container-aware heap sizing — respects a K8s memory limit instead of guessing
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

