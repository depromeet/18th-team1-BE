FROM gradle:8.13-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home --uid 10001 appuser
COPY --from=build /app/build/libs/app.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
