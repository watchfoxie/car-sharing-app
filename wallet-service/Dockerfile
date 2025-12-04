FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -q -pl wallet-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="Wallet service" Version="1.0"

VOLUME /tmp

COPY --from=build /workspace/wallet-service/target/wallet-service-1.0-SNAPSHOT.jar ./wallet-service-1.0-SNAPSHOT.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "wallet-service-1.0-SNAPSHOT.jar"]
