FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /workspace/app

# Copy gradle files first for better layer caching
COPY gradle gradle
COPY build.gradle ./
COPY settings.gradle ./
COPY gradlew ./
RUN chmod +x gradlew

# Build dependency layer
RUN ./gradlew dependencies

# Copy source code and build
COPY src src
RUN ./gradlew build -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the JAR file directly
COPY --from=build /workspace/app/build/libs/documents-0.0.1-SNAPSHOT.jar /app/app.jar


# Create data directory with permissive permissions
RUN mkdir -p /app/data && chmod 777 /app/data


ENTRYPOINT ["java", "-jar", "/app/app.jar"]
# ENTRYPOINT [ "ls", "-l", "/app/" ]
