FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -q -pl authen-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="Authentication Service" Version="1.0"

VOLUME /tmp

COPY --from=build /workspace/authen-service/target/authen-service-1.0-SNAPSHOT.jar ./authen-service-1.0-SNAPSHOT.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "authen-service-1.0-SNAPSHOT.jar"]
