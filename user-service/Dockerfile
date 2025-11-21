FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -q -pl user-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="User Service" Version="0.0.1"

VOLUME /tmp

COPY --from=build /workspace/user-service/target/user-service-0.0.1-SNAPSHOT.jar ./user-service-0.0.1-SNAPSHOT.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "user-service-0.0.1-SNAPSHOT.jar"]
