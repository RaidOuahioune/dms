FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Install necessary tools
RUN apk add --no-cache gradle

# Copy project files
COPY . .
RUN chmod +x gradlew

# Expose port
EXPOSE 8083

# Create data directory
RUN mkdir -p /app/data && chmod 777 /app/data

# Set environment variables for DevTools
ENV SPRING_DEVTOOLS_RESTART_ENABLED=true
ENV SPRING_DEVTOOLS_LIVERELOAD_ENABLED=true

# Command to run the application in development mode
CMD ["./gradlew", "bootRun"]