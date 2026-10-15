FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

FROM bellsoft/liberica-openjre-alpine-musl:21

WORKDIR /app
RUN addgroup -S app && adduser -S -G app app
COPY --from=build /app/target/*.jar app.jar

ENV APP_PORT=3001 \
    CSV_PATH=/data/compras.csv \
    APP_ENV=production

USER app
EXPOSE 3001
ENTRYPOINT ["java", "-jar", "app.jar"]
