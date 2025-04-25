FROM eclipse-temurin:21-jdk as build
WORKDIR /app

COPY . .
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the executable JAR file from the build stage and rename it to app.jar
# Exclude potential '-plain.jar' if present
COPY --from=build /app/build/libs/workflow-0.0.1-SNAPSHOT.jar /app/app.jar

# Create directory for storing SQLite database
RUN mkdir -p /app/data
VOLUME /app/data

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]