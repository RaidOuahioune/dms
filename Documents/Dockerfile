FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /workspace/app

# Copy gradle files first for better layer caching
COPY gradle gradle
COPY build.gradle settings.gradle gradlew ./
RUN chmod +x gradlew

# Build dependency layer
RUN ./gradlew dependencies

# Copy source code and build
COPY src src
RUN ./gradlew build -x test

FROM eclipse-temurin:21-jre-alpine
VOLUME /app/data
WORKDIR /app

# Copy the JAR file directly
COPY --from=build /workspace/app/build/libs/*.jar /app/app.jar

# Run as non-root user for better security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]