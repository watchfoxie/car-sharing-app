FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -q -pl driver-location-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="Driver Location Service" Version="1.0"

VOLUME /tmp

COPY --from=build /workspace/driver-location-service/target/driver-location-service-1.0-SNAPSHOT.jar ./driver-location-service-1.0-SNAPSHOT.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "driver-location-service-1.0-SNAPSHOT.jar"]
