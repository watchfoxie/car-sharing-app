FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -q -pl api-gateway -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="API Gateway" Version="1.0"

VOLUME /tmp

COPY --from=build /workspace/api-gateway/target/api-gateway-1.0-SNAPSHOT.jar ./api-gateway-1.0-SNAPSHOT.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "api-gateway-1.0-SNAPSHOT.jar"]
