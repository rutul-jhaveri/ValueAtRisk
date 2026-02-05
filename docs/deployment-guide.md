# VaR Calculation Service - Deployment Guide

## 1. Prerequisites

### 1.1 System Requirements

**Development Environment:**
- Java 21 (OpenJDK or Oracle JDK)
- Maven 3.8+ 
- Git
- IDE with Java 21 support (IntelliJ IDEA, Eclipse, VS Code)

**Production Environment:**
- Java 21 Runtime Environment
- Docker 20.10+ (optional)
- Kubernetes 1.20+ (optional)
- Load Balancer (nginx, HAProxy)
- Monitoring tools (Prometheus, Grafana)

### 1.2 Hardware Requirements

**Minimum (Development):**
- CPU: 2 cores
- RAM: 4 GB
- Disk: 10 GB free space

**Recommended (Production):**
- CPU: 4+ cores
- RAM: 8+ GB
- Disk: 50+ GB SSD
- Network: 1 Gbps

## 2. Local Development Setup

### 2.1 Clone and Build

```bash
# Clone repository
git clone <repository-url>
cd var-calculation

# Build project
./mvnw clean install

# Run tests
./mvnw test

# Start application
./mvnw spring-boot:run
```

### 2.2 Verify Installation

```bash
# Check health endpoint
curl http://localhost:9001/actuator/health

# Access Swagger UI
open http://localhost:9001/swagger-ui.html

# Access H2 Console (development only)
open http://localhost:9001/h2-console
```

**H2 Console Settings:**
- JDBC URL: `jdbc:h2:mem:vardb`
- Username: `sa`
- Password: (empty)

### 2.3 IDE Configuration

**IntelliJ IDEA:**
1. Import as Maven project
2. Set Project SDK to Java 21
3. Enable annotation processing for Lombok
4. Configure code style (Google Java Style recommended)

**VS Code:**
1. Install Extension Pack for Java
2. Configure `java.configuration.runtimes` for Java 21
3. Install Lombok Annotations Support

## 3. Configuration Management

### 3.1 Environment-Specific Configuration

**Development (application-dev.yml):**
```yaml
spring:
  profiles:
    active: dev
  h2:
    console:
      enabled: true
  jpa:
    show-sql: true

logging:
  level:
    com.var.calculation: DEBUG
    org.springframework.security: DEBUG
```

**Production (application-prod.yml):**
```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  h2:
    console:
      enabled: false
  jpa:
    show-sql: false

logging:
  level:
    com.var.calculation: INFO
    org.springframework.security: WARN

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}
```

### 3.2 Environment Variables

**Required for Production:**
```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/vardb"
export DATABASE_USERNAME="var_user"
export DATABASE_PASSWORD="secure_password"
export JWT_SECRET="your-256-bit-secret-key"
export JWT_EXPIRATION="86400000"
```

**Optional Configuration:**
```bash
export SERVER_PORT="9001"
export MIN_DATA_POINTS="5"
export CACHE_TTL="3600"
export CACHE_MAX_SIZE="1000"
```

## 4. Database Setup

### 4.1 Development (H2)

H2 database is automatically configured for development:
- In-memory database
- Auto-creates tables on startup
- Data lost on application restart
- H2 console available for debugging

### 4.2 Production (PostgreSQL)

**Install PostgreSQL:**
```bash
# Ubuntu/Debian
sudo apt-get install postgresql postgresql-contrib

# CentOS/RHEL
sudo yum install postgresql-server postgresql-contrib

# macOS
brew install postgresql
```

**Create Database:**
```sql
-- Connect as postgres user
sudo -u postgres psql

-- Create database and user
CREATE DATABASE vardb;
CREATE USER var_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE vardb TO var_user;

-- Exit
\q
```

**Update application-prod.yml:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/vardb
    username: var_user
    password: secure_password
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Use Flyway for production
```

### 4.3 Database Migration (Flyway)

**Add Flyway dependency:**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

**Migration scripts (src/main/resources/db/migration):**
```sql
-- V1__Create_users_table.sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- V2__Create_audit_records_table.sql
CREATE TABLE audit_records (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    endpoint VARCHAR(200) NOT NULL,
    execution_time BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 5. Docker Deployment

### 5.1 Dockerfile

```dockerfile
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src/ src/

# Build application
RUN ./mvnw clean package -DskipTests

# Create runtime image
FROM openjdk:21-jre-slim

WORKDIR /app

# Copy JAR file
COPY --from=0 /app/target/var-calculation-*.jar app.jar

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 9001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9001/actuator/health || exit 1

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 5.2 Docker Compose

**docker-compose.yml:**
```yaml
version: '3.8'

services:
  var-calculation:
    build: .
    ports:
      - "9001:9001"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:postgresql://postgres:5432/vardb
      - DATABASE_USERNAME=var_user
      - DATABASE_PASSWORD=secure_password
      - JWT_SECRET=your-256-bit-secret-key
    depends_on:
      - postgres
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9001/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  postgres:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=vardb
      - POSTGRES_USER=var_user
      - POSTGRES_PASSWORD=secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"
    restart: unless-stopped

volumes:
  postgres_data:
```

### 5.3 Build and Run

```bash
# Build and start services
docker-compose up -d

# View logs
docker-compose logs -f var-calculation

# Stop services
docker-compose down

# Remove volumes (data loss!)
docker-compose down -v
```

## 6. Kubernetes Deployment

### 6.1 Namespace and ConfigMap

**namespace.yaml:**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: var-calculation
```

**configmap.yaml:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: var-config
  namespace: var-calculation
data:
  application.yml: |
    spring:
      profiles:
        active: prod
    server:
      port: 9001
    var:
      calculation:
        min-data-points: "5"
```

### 6.2 Secrets

**secret.yaml:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: var-secrets
  namespace: var-calculation
type: Opaque
data:
  database-url: <base64-encoded-url>
  database-username: <base64-encoded-username>
  database-password: <base64-encoded-password>
  jwt-secret: <base64-encoded-secret>
```

### 6.3 Deployment

**deployment.yaml:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: var-calculation
  namespace: var-calculation
spec:
  replicas: 3
  selector:
    matchLabels:
      app: var-calculation
  template:
    metadata:
      labels:
        app: var-calculation
    spec:
      containers:
      - name: var-calculation
        image: var-calculation:latest
        ports:
        - containerPort: 9001
        env:
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: var-secrets
              key: database-url
        - name: DATABASE_USERNAME
          valueFrom:
            secretKeyRef:
              name: var-secrets
              key: database-username
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: var-secrets
              key: database-password
        - name: JWT_SECRET
          valueFrom:
            secretKeyRef:
              name: var-secrets
              key: jwt-secret
        volumeMounts:
        - name: config
          mountPath: /app/config
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 9001
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 9001
          initialDelaySeconds: 30
          periodSeconds: 10
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
      volumes:
      - name: config
        configMap:
          name: var-config
```

### 6.4 Service and Ingress

**service.yaml:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: var-calculation-service
  namespace: var-calculation
spec:
  selector:
    app: var-calculation
  ports:
  - port: 80
    targetPort: 9001
  type: ClusterIP
```

**ingress.yaml:**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: var-calculation-ingress
  namespace: var-calculation
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
spec:
  rules:
  - host: var-api.example.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: var-calculation-service
            port:
              number: 80
```

## 7. Monitoring and Logging

### 7.1 Application Metrics

**Prometheus Configuration:**
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'var-calculation'
    static_configs:
      - targets: ['localhost:9001']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
```

**Key Metrics to Monitor:**
- `http_server_requests_seconds`: Request duration
- `jvm_memory_used_bytes`: Memory usage
- `jvm_threads_live_threads`: Thread count
- `cache_gets_total`: Cache hit/miss rates
- `database_connections_active`: DB connections

### 7.2 Logging Configuration

**logback-spring.xml:**
```xml
<configuration>
    <springProfile name="prod">
        <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
                <providers>
                    <timestamp/>
                    <logLevel/>
                    <loggerName/>
                    <message/>
                    <mdc/>
                    <stackTrace/>
                </providers>
            </encoder>
        </appender>
        
        <root level="INFO">
            <appender-ref ref="STDOUT"/>
        </root>
    </springProfile>
</configuration>
```

### 7.3 Health Checks

**Custom Health Indicators:**
```java
@Component
public class VarCalculationHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Check calculation service health
        try {
            // Perform lightweight calculation test
            return Health.up()
                .withDetail("calculation", "operational")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("calculation", "failed")
                .withException(e)
                .build();
        }
    }
}
```

## 8. Security Hardening

### 8.1 Production Security Checklist

- [ ] Change default passwords
- [ ] Use strong JWT secret (256-bit minimum)
- [ ] Enable HTTPS/TLS
- [ ] Configure CORS properly
- [ ] Disable H2 console
- [ ] Remove debug logging
- [ ] Set up firewall rules
- [ ] Enable security headers
- [ ] Regular security updates

### 8.2 TLS Configuration

**application-prod.yml:**
```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: var-calculation
```

### 8.3 Security Headers

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(headers -> headers
                .frameOptions().deny()
                .contentTypeOptions().and()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true))
            )
            // ... other configuration
            .build();
    }
}
```

## 9. Performance Tuning

### 9.1 JVM Tuning

**Production JVM Options:**
```bash
java -Xms2g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+UseStringDeduplication \
     -XX:+OptimizeStringConcat \
     -Djava.security.egd=file:/dev/./urandom \
     -jar app.jar
```

### 9.2 Database Tuning

**PostgreSQL Configuration:**
```sql
-- postgresql.conf
shared_buffers = 256MB
effective_cache_size = 1GB
work_mem = 4MB
maintenance_work_mem = 64MB
max_connections = 100
```

### 9.3 Cache Configuration

```yaml
spring:
  cache:
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=1h
```

## 10. Backup and Recovery

### 10.1 Database Backup

```bash
# PostgreSQL backup
pg_dump -h localhost -U var_user -d vardb > backup_$(date +%Y%m%d_%H%M%S).sql

# Automated backup script
#!/bin/bash
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
pg_dump -h localhost -U var_user -d vardb | gzip > $BACKUP_DIR/vardb_$DATE.sql.gz

# Keep only last 7 days
find $BACKUP_DIR -name "vardb_*.sql.gz" -mtime +7 -delete
```

### 10.2 Application State

Since the application is stateless, no application-specific backup is needed. All persistent data is in the database.

## 11. Troubleshooting

### 11.1 Common Issues

**Application won't start:**
```bash
# Check Java version
java -version

# Check port availability
netstat -an | grep 9001

# Check logs
tail -f logs/application.log
```

**Database connection issues:**
```bash
# Test database connectivity
psql -h localhost -U var_user -d vardb

# Check connection pool
curl http://localhost:9001/actuator/health
```

**Memory issues:**
```bash
# Check memory usage
curl http://localhost:9001/actuator/metrics/jvm.memory.used

# Generate heap dump
jcmd <pid> GC.run_finalization
jcmd <pid> VM.gc
```

### 11.2 Log Analysis

**Key log patterns to monitor:**
- `ERROR` level messages
- `Authentication failed` 
- `VaR calculation failed`
- `Database connection` errors
- High response times (>1000ms)

## 12. Maintenance

### 12.1 Regular Tasks

**Daily:**
- Monitor application health
- Check error logs
- Verify backup completion

**Weekly:**
- Review performance metrics
- Update dependencies (security patches)
- Clean old log files

**Monthly:**
- Full system backup
- Security audit
- Performance review
- Capacity planning

### 12.2 Update Process

```bash
# 1. Backup current version
docker tag var-calculation:latest var-calculation:backup

# 2. Build new version
docker build -t var-calculation:latest .

# 3. Test new version
docker-compose -f docker-compose.test.yml up

# 4. Deploy with rolling update
kubectl set image deployment/var-calculation var-calculation=var-calculation:latest

# 5. Verify deployment
kubectl rollout status deployment/var-calculation
```

This deployment guide provides comprehensive instructions for setting up the VaR Calculation Service across different environments, from local development to production Kubernetes clusters.