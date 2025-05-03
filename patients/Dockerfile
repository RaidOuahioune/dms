# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine as build
WORKDIR /workspace/app

# Copy Maven files and source code
COPY pom.xml .
COPY src src

# Package the application (skip tests if needed)
RUN apk add --no-cache maven && mvn clean package 

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /workspace/app/target/*.jar /app/app.jar

# Optional: create writable directory if needed
RUN mkdir -p /app/data && chmod 777 /app/data

# Start the app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
