# Docker Guidelines for VaR Calculation Service

## Dockerfile Overview

The Dockerfile uses a two-stage build approach for smaller production images. The build stage compiles the application while the runtime stage only contains what's needed to run it.

### Build Stage Benefits

- Smaller final image size (build tools not included)
- Better security (source code not in production image)
- Faster deployments (smaller images transfer faster)
- Consistent builds across environments

### Security Features

The Dockerfile creates a dedicated non-root user for running the application. It uses minimal base images to reduce attack surface and only installs essential packages.

### Performance Features

JVM settings are optimized for container environments with automatic memory detection and G1 garbage collector for low latency.

## Building and Running

### Basic Build

```bash
docker build -t var-calculation:latest .
```

### Build with Version Tag

```bash
docker build -t var-calculation:1.0.0 -t var-calculation:latest .
```

### Running Development Container

```bash
docker run -d \
  --name var-calculation-dev \
  -p 9001:9001 \
  -e SPRING_PROFILES_ACTIVE=dev \
  var-calculation:latest
```

### Running Production Container

```bash
docker run -d \
  --name var-calculation-prod \
  -p 9001:9001 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://db:5432/vardb \
  -e DATABASE_USERNAME=var_user \
  -e DATABASE_PASSWORD=secure_password \
  -e JWT_SECRET=your-secret-key \
  --restart unless-stopped \
  --memory=2g \
  --cpus=1.0 \
  var-calculation:latest
```

## Docker Compose for Development

Create docker-compose.yml for local development:

```yaml
version: '3.8'

services:
  var-calculation:
    build: .
    ports:
      - "9001:9001"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
    volumes:
      - ./logs:/app/logs
    depends_on:
      - postgres

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=vardb
      - POSTGRES_USER=var_user
      - POSTGRES_PASSWORD=dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

Run with: `docker-compose up -d`

## Environment Variables

### Required for Production

```bash
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://host:5432/vardb
DATABASE_USERNAME=var_user
DATABASE_PASSWORD=secure_password
JWT_SECRET=your-secret-key
```

### Optional Configuration

```bash
SERVER_PORT=9001
JWT_EXPIRATION=86400000
MIN_DATA_POINTS=5
JAVA_OPTS="-Xms512m -Xmx1024m"
```

## Resource Configuration

### Memory Limits

```bash
docker run --memory=2g --memory-swap=2g var-calculation:latest
```

### CPU Limits

```bash
docker run --cpus=1.0 var-calculation:latest
```

### Volume Mounts

```bash
# Logs
docker run -v /host/logs:/app/logs var-calculation:latest

# Configuration
docker run -v /host/config:/app/config var-calculation:latest
```

## Health Checks

The Dockerfile includes a built-in health check that calls the actuator health endpoint every 30 seconds.

Custom health check:
```bash
docker run --health-cmd="curl -f http://localhost:9001/actuator/health || exit 1" \
           --health-interval=30s \
           --health-timeout=10s \
           --health-retries=3 \
           var-calculation:latest
```

## Logging

### Container Logging

```bash
# View logs
docker logs var-calculation

# Follow logs
docker logs -f var-calculation

# Limit log output
docker logs --tail=100 var-calculation
```

### Log Configuration

Configure log rotation:
```bash
docker run --log-driver=json-file \
           --log-opt max-size=10m \
           --log-opt max-file=3 \
           var-calculation:latest
```

## Security Best Practices

### Run as Non-Root User

The Dockerfile creates and uses a dedicated appuser account instead of root.

### Read-Only Filesystem

```bash
docker run --read-only \
           --tmpfs /tmp \
           --tmpfs /app/logs \
           var-calculation:latest
```

### Security Options

```bash
docker run --security-opt=no-new-privileges:true \
           --cap-drop=ALL \
           var-calculation:latest
```

### Image Scanning

Scan for vulnerabilities:
```bash
# Using Docker Scout
docker scout cves var-calculation:latest

# Using Trivy
trivy image var-calculation:latest
```

## Performance Tuning

### JVM Settings for Containers

The Dockerfile sets optimized JVM options:
- UseContainerSupport: Automatic memory detection
- UseG1GC: Low latency garbage collection
- Xms512m/Xmx1024m: Memory allocation

### Custom JVM Options

```bash
docker run -e JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseZGC" var-calculation:latest
```

## Troubleshooting

### Container Won't Start

```bash
# Check logs
docker logs var-calculation

# Check health status
docker inspect --format='{{.State.Health.Status}}' var-calculation

# Debug with shell access
docker run -it --entrypoint=/bin/bash var-calculation:latest
```

### Memory Issues

```bash
# Monitor memory usage
docker stats var-calculation

# Check JVM memory inside container
docker exec var-calculation java -XX:+PrintFlagsFinal -version | grep MaxHeapSize
```

### Network Issues

```bash
# Check port binding
docker port var-calculation

# Test connectivity
docker exec var-calculation curl -f http://localhost:9001/actuator/health
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Docker Build

on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    
    - name: Build Docker image
      run: docker build -t var-calculation:latest .
    
    - name: Run tests
      run: docker run --rm var-calculation:latest ./mvnw test
    
    - name: Push to registry
      run: |
        echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
        docker push var-calculation:latest
```

## Registry Operations

### Push to Docker Hub

```bash
# Tag for registry
docker tag var-calculation:latest username/var-calculation:latest

# Push to registry
docker push username/var-calculation:latest
```

### Pull from Registry

```bash
docker pull username/var-calculation:latest
```

## Multi-Platform Builds

Build for multiple architectures:

```bash
# Create builder
docker buildx create --name multiarch --use

# Build for multiple platforms
docker buildx build --platform linux/amd64,linux/arm64 \
  -t var-calculation:latest --push .
```

This covers the essential Docker operations for the VaR Calculation Service with practical examples and best practices.