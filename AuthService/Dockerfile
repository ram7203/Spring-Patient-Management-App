FROM maven:3.9.9-eclipse-temurin-21 as builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package

FROM amazoncorretto:21 AS runner

WORKDIR /app

COPY --from=builder ./app/target/AuthService-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]