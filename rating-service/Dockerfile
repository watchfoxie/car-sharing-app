FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY . .

RUN mvn -B -q -pl rating-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

LABEL Description="Rating Service" Version="1.0"

VOLUME /tmp

COPY --from=build /workspace/rating-service/target/rating-service-1.0-SNAPSHOT.jar ./rating-service-1.0-SNAPSHOT.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "rating-service-1.0-SNAPSHOT.jar"]
