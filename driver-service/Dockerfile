FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -q -pl driver-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="Driver Service" Version="1.0"

VOLUME /tmp

COPY --from=build /workspace/driver-service/target/driver-service-1.0-SNAPSHOT.jar ./driver-service-1.0-SNAPSHOT.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "driver-service-1.0-SNAPSHOT.jar"]