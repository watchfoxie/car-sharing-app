FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -q -pl config-server -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="Config Server" Version="1.0"

VOLUME /tmp

COPY --from=build /workspace/config-server/target/config-server-1.0-SNAPSHOT.jar ./config-server-1.0-SNAPSHOT.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "config-server-1.0-SNAPSHOT.jar"]
