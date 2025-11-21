FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -q -pl payment-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="Payment service" Version="1.0"

VOLUME /tmp

COPY --from=build /workspace/payment-service/target/payment-service-1.0-SNAPSHOT.jar ./payment-service-1.0-SNAPSHOT.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "payment-service-1.0-SNAPSHOT.jar"]
