# --- Stage 1: Build with Maven ---
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /build

# Copy Maven files first for better layer caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B


# --- Stage 2: Runtime with JRE ---
FROM eclipse-temurin:17-jre-alpine

# Add metadata
LABEL maintainer="opty-socket"
LABEL description="WebSocket relay server for client-supervisor communication"
LABEL version="1.1.0"

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the JAR from build stage
COPY --from=build /build/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose WebSocket port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Default JVM options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Run with JVM options
CMD ["-Djava.security.egd=file:/dev/./urandom"]
