# Multi-stage build pentru car-service (Fleet Management)
# Faza 1: Build cu Maven
FROM maven:3.9-eclipse-temurin-25-jammy AS builder

WORKDIR /build

# Copiaz\u0103 parent POM \u0219i module pentru dependency resolution
COPY pom.xml .
COPY car-service/pom.xml ./car-service/
COPY common-libs/pom.xml ./common-libs/

# Download dependencies (cache layer)
RUN mvn dependency:go-offline -pl car-service -am

# Copiaz\u0103 surse \u0219i build
COPY common-libs/src ./common-libs/src
COPY car-service/src ./car-service/src
RUN mvn clean package -pl car-service -am -DskipTests

# Faza 2: Runtime minimal cu distroless
FROM gcr.io/distroless/java25-debian12:nonroot

# Metadata
LABEL maintainer="car-sharing-dev@example.com"
LABEL service="car-service"
LABEL version="1.0.0"

# Build args pentru configura\u021bie
ARG SPRING_PROFILE=dev
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILE}

# User non-root (distroless deja folosește nonroot:65532)
WORKDIR /app

# Copiaz\u0103 JAR din builder
COPY --from=builder /build/car-service/target/*.jar app.jar

# Port Car Service
EXPOSE 8082

# Health check folosind Actuator (va fi configurat în docker-compose)
# HEALTHCHECK delegat la orchestration layer

# Entrypoint cu JVM tuning
ENTRYPOINT ["java", \
    "-XX:+UseG1GC", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
