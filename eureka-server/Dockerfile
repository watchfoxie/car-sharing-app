FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy the full multi-module project (targets are ignored via .dockerignore)
COPY . .

# Build only the Eureka module along with any dependent modules
RUN mvn -B -q -pl eureka-server -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="Eureka Server" Version="1.0"

VOLUME /tmp

COPY --from=build /workspace/eureka-server/target/eureka-server-1.0-SNAPSHOT.jar ./eureka-server-1.0-SNAPSHOT.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "eureka-server-1.0-SNAPSHOT.jar"]