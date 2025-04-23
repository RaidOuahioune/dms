FROM eclipse-temurin:21-jdk as build
WORKDIR /app

COPY . .
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Create directory for storing SQLite database
RUN mkdir -p /app/data
VOLUME /app/data

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]