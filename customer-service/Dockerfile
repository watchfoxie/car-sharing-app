FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -q -pl customer-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="Customer service" Version="1.0"

VOLUME /tmp

COPY --from=build /workspace/customer-service/target/customer-service-1.0-SNAPSHOT.jar ./customer-service-1.0-SNAPSHOT.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "customer-service-1.0-SNAPSHOT.jar"]