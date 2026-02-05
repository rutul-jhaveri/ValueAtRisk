# Multi-stage Dockerfile for VaR Calculation Service
# Optimized for production deployment with security best practices

# Stage 1: Build stage
FROM openjdk:21-jdk-slim AS builder

# Set build arguments
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=1.0.0

# Add labels for metadata
LABEL maintainer="VaR Calculation Team" \
      org.opencontainers.image.title="VaR Calculation Service" \
      org.opencontainers.image.description="Financial VaR calculation service using Historical Simulation" \
      org.opencontainers.image.version=${VERSION} \
      org.opencontainers.image.created=${BUILD_DATE} \
      org.opencontainers.image.revision=${VCS_REF} \
      org.opencontainers.image.source="https://github.com/your-org/var-calculation"

# Install build dependencies
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /build

# Copy Maven wrapper and configuration files
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make Maven wrapper executable
RUN chmod +x ./mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src/ src/

# Build application
RUN ./mvnw clean package -DskipTests -B && \
    java -Djarmode=layertools -jar target/var-calculation-*.jar extract

# Stage 2: Runtime stage
FROM openjdk:21-jre-slim AS runtime

# Install runtime dependencies and security updates
RUN apt-get update && apt-get install -y \
    curl \
    dumb-init \
    && apt-get upgrade -y \
    && rm -rf /var/lib/apt/lists/* \
    && apt-get clean

# Create application user and group
RUN groupadd -r -g 1001 appuser && \
    useradd -r -u 1001 -g appuser -m -d /app -s /bin/bash appuser

# Set working directory
WORKDIR /app

# Copy application layers from builder stage
COPY --from=builder --chown=appuser:appuser /build/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /build/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /build/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /build/application/ ./

# Create logs directory
RUN mkdir -p /app/logs && chown -R appuser:appuser /app/logs

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 9001

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9001/actuator/health || exit 1

# Set JVM options for containerized environment
ENV JAVA_OPTS="-Xms512m -Xmx1024m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+OptimizeStringConcat \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker"

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Start application using Spring Boot's layered JAR approach
CMD ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]