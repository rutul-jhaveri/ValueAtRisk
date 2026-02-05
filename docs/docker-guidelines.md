# Docker Guidelines for VaR Calculation Service

## 1. Dockerfile Best Practices

### 1.1 Multi-Stage Build Benefits

The provided Dockerfile uses a multi-stage build approach with the following advantages:

- **Smaller Image Size**: Runtime image only contains necessary components
- **Security**: Build tools and source code not included in final image
- **Layer Caching**: Dependencies cached separately from application code
- **Reproducible Builds**: Consistent builds across environments

### 1.2 Security Features

**Non-Root User:**
```dockerfile
# Creates dedicated user with specific UID/GID
RUN groupadd -r -g 1001 appuser && \
    useradd -r -u 1001 -g appuser -m -d /app -s /bin/bash appuser
USER appuser
```

**Minimal Base Image:**
- Uses `openjdk:21-jre-slim` for smaller attack surface
- Only installs essential packages
- Removes package cache to reduce image size

**Signal Handling:**
- Uses `dumb-init` for proper signal forwarding
- Ensures graceful shutdown in container environments

### 1.3 Performance Optimizations

**JVM Container Awareness:**
```bash
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
```

**Garbage Collection:**
```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

**Spring Boot Layered JARs:**
- Dependencies cached in separate layers
- Faster rebuilds when only application code changes
- Better layer reuse across deployments

## 2. Building and Running

### 2.1 Build Commands

**Basic Build:**
```bash
docker build -t var-calculation:latest .
```

**Build with Metadata:**
```bash
docker build \
  --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
  --build-arg VCS_REF=$(git rev-parse --short HEAD) \
  --build-arg VERSION=1.0.0 \
  -t var-calculation:1.0.0 \
  -t var-calculation:latest .
```

**Multi-Platform Build:**
```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t var-calculation:latest \
  --push .
```

### 2.2 Running Containers

**Development Mode:**
```bash
docker run -d \
  --name var-calculation-dev \
  -p 9001:9001 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  var-calculation:latest
```

**Production Mode:**
```bash
docker run -d \
  --name var-calculation-prod \
  -p 9001:9001 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://db:5432/vardb \
  -e DATABASE_USERNAME=var_user \
  -e DATABASE_PASSWORD=secure_password \
  -e JWT_SECRET=your-256-bit-secret \
  --restart unless-stopped \
  --memory=2g \
  --cpus=1.0 \
  var-calculation:latest
```

### 2.3 Docker Compose for Development

**docker-compose.dev.yml:**
```yaml
version: '3.8'

services:
  var-calculation:
    build: .
    ports:
      - "9001:9001"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - JAVA_OPTS=-Xms256m -Xmx512m -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
    ports:
      - "9001:9001"
      - "5005:5005"  # Debug port
    volumes:
      - ./logs:/app/logs
    depends_on:
      - postgres
    restart: unless-stopped

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=vardb
      - POSTGRES_USER=var_user
      - POSTGRES_PASSWORD=dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_dev_data:/var/lib/postgresql/data
    restart: unless-stopped

volumes:
  postgres_dev_data:
```

## 3. Container Configuration

### 3.1 Environment Variables

**Required:**
```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://host:5432/vardb
DATABASE_USERNAME=var_user
DATABASE_PASSWORD=secure_password
JWT_SECRET=your-256-bit-secret-key
```

**Optional:**
```bash
SERVER_PORT=9001
JWT_EXPIRATION=86400000
MIN_DATA_POINTS=5
CACHE_TTL=3600
CACHE_MAX_SIZE=1000
JAVA_OPTS="-Xms512m -Xmx1024m"
```

### 3.2 Volume Mounts

**Logs:**
```bash
-v /host/logs:/app/logs
```

**Configuration:**
```bash
-v /host/config:/app/config
```

**Temporary Files:**
```bash
-v /host/tmp:/tmp
```

### 3.3 Resource Limits

**Memory Limits:**
```bash
--memory=2g
--memory-swap=2g
--oom-kill-disable=false
```

**CPU Limits:**
```bash
--cpus=1.0
--cpu-shares=1024
```

## 4. Health Checks and Monitoring

### 4.1 Health Check Configuration

The Dockerfile includes a built-in health check:
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:9001/actuator/health || exit 1
```

**Custom Health Check:**
```bash
docker run --health-cmd="curl -f http://localhost:9001/actuator/health || exit 1" \
           --health-interval=30s \
           --health-timeout=10s \
           --health-retries=3 \
           var-calculation:latest
```

### 4.2 Logging Configuration

**JSON Logging for Containers:**
```yaml
# application-docker.yml
logging:
  pattern:
    console: '{"timestamp":"%d{ISO8601}","level":"%level","thread":"%thread","class":"%logger{40}","message":"%message"}%n'
  level:
    com.var.calculation: INFO
    org.springframework.security: WARN
```

**Log Aggregation:**
```bash
docker run --log-driver=json-file \
           --log-opt max-size=10m \
           --log-opt max-file=3 \
           var-calculation:latest
```

## 5. Security Considerations

### 5.1 Image Scanning

**Scan for Vulnerabilities:**
```bash
# Using Docker Scout
docker scout cves var-calculation:latest

# Using Trivy
trivy image var-calculation:latest

# Using Snyk
snyk container test var-calculation:latest
```

### 5.2 Runtime Security

**Read-Only Root Filesystem:**
```bash
docker run --read-only \
           --tmpfs /tmp \
           --tmpfs /app/logs \
           var-calculation:latest
```

**Security Options:**
```bash
docker run --security-opt=no-new-privileges:true \
           --cap-drop=ALL \
           --cap-add=NET_BIND_SERVICE \
           var-calculation:latest
```

### 5.3 Secrets Management

**Using Docker Secrets:**
```bash
echo "secure_password" | docker secret create db_password -
docker service create \
  --secret db_password \
  --env DATABASE_PASSWORD_FILE=/run/secrets/db_password \
  var-calculation:latest
```

## 6. Performance Tuning

### 6.1 JVM Tuning for Containers

**Memory Settings:**
```bash
# Automatic memory detection
JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Manual memory settings
JAVA_OPTS="-Xms512m -Xmx1024m"
```

**Garbage Collection:**
```bash
# G1GC for low latency
JAVA_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# ZGC for very low latency (Java 21)
JAVA_OPTS="-XX:+UseZGC -XX:+UnlockExperimentalVMOptions"
```

### 6.2 Container Resource Optimization

**CPU Optimization:**
```bash
# Set CPU affinity
docker run --cpuset-cpus="0,1" var-calculation:latest

# Set CPU quota
docker run --cpu-period=100000 --cpu-quota=50000 var-calculation:latest
```

**Memory Optimization:**
```bash
# Set memory limits with swap
docker run --memory=1g --memory-swap=1g var-calculation:latest

# Disable swap
docker run --memory=1g --memory-swap=1g --memory-swappiness=0 var-calculation:latest
```

## 7. Troubleshooting

### 7.1 Common Issues

**Container Won't Start:**
```bash
# Check logs
docker logs var-calculation

# Check health status
docker inspect --format='{{.State.Health.Status}}' var-calculation

# Debug with shell access
docker run -it --entrypoint=/bin/bash var-calculation:latest
```

**Memory Issues:**
```bash
# Monitor memory usage
docker stats var-calculation

# Check JVM memory
docker exec var-calculation jcmd 1 VM.info
```

**Network Issues:**
```bash
# Check port binding
docker port var-calculation

# Test connectivity
docker exec var-calculation curl -f http://localhost:9001/actuator/health
```

### 7.2 Debugging

**Enable Debug Mode:**
```bash
docker run -e JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005" \
           -p 5005:5005 \
           var-calculation:latest
```

**JVM Debugging:**
```bash
# Generate heap dump
docker exec var-calculation jcmd 1 GC.run_finalization

# Thread dump
docker exec var-calculation jcmd 1 Thread.print
```

## 8. CI/CD Integration

### 8.1 GitHub Actions

**.github/workflows/docker.yml:**
```yaml
name: Docker Build and Push

on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up Docker Buildx
      uses: docker/setup-buildx-action@v3
    
    - name: Login to DockerHub
      uses: docker/login-action@v3
      with:
        username: ${{ secrets.DOCKERHUB_USERNAME }}
        password: ${{ secrets.DOCKERHUB_TOKEN }}
    
    - name: Build and push
      uses: docker/build-push-action@v5
      with:
        context: .
        push: true
        tags: |
          your-org/var-calculation:latest
          your-org/var-calculation:${{ github.sha }}
        build-args: |
          BUILD_DATE=${{ steps.date.outputs.date }}
          VCS_REF=${{ github.sha }}
```

### 8.2 Image Registry

**Push to Registry:**
```bash
# Tag for registry
docker tag var-calculation:latest your-registry.com/var-calculation:1.0.0

# Push to registry
docker push your-registry.com/var-calculation:1.0.0
```

This Docker setup provides a production-ready, secure, and optimized container for your VaR Calculation Service with comprehensive guidelines for development and production use.