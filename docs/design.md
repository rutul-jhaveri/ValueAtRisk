# VaR Calculation System - Technical Design Document

**Project:** VaR Calculation REST API  
**Version:** 1.0  
**Date:** 2026-02-02  
**Status:** Design Phase

---

## 1. System Architecture

### 1.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Client Layer                          │
│                    (REST API Consumers)                      │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway Layer                        │
│              (Spring Security + JWT Filter)                  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                          │
│         VarController  │  AuditController                    │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                     Service Layer                            │
│    VarService  │  AuditService  │  UserService              │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Strategy   │  │  Repository  │  │    Async     │
│   Pattern    │  │    Layer     │  │   Executor   │
│              │  │              │  │              │
│ Historical   │  │ AuditRepo    │  │ Audit Queue  │
│ Simulation   │  │              │  │              │
└──────────────┘  └──────┬───────┘  └──────────────┘
                         │
                         ▼
                  ┌──────────────┐
                  │   Database   │
                  │  PostgreSQL  │
                  └──────────────┘
```

### 1.2 Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
│                                                               │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              REST Controllers                           │ │
│  │  - VarController                                        │ │
│  │  - AuditController                                      │ │
│  └────────────────────────────────────────────────────────┘ │
│                           │                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Business Services                          │ │
│  │  - VarCalculationService                                │ │
│  │  - AuditService (Async)                                 │ │
│  │  - UserDetailsService                                   │ │
│  └────────────────────────────────────────────────────────┘ │
│                           │                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │         VaR Calculation Strategy (Interface)            │ │
│  │                                                          │ │
│  │  Implementations:                                        │ │
│  │  - HistoricalSimulationStrategy                         │ │
│  │  - (Future) VarianceCovarianceStrategy                  │ │
│  │  - (Future) MonteCarloStrategy                          │ │
│  └────────────────────────────────────────────────────────┘ │
│                           │                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              Data Access Layer                          │ │
│  │  - AuditRecordRepository (JPA)                          │ │
│  │  - UserRepository (JPA)                                 │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Package Structure

```
com.var.calculation
├── VarApplication.java
├── config
│   ├── SecurityConfig.java
│   ├── AsyncConfig.java
│   ├── OpenApiConfig.java
│   └── DatabaseConfig.java
├── controller
│   ├── VarController.java
│   └── AuditController.java
├── service
│   ├── VarCalculationService.java
│   ├── AuditService.java
│   └── UserDetailsServiceImpl.java
├── strategy
│   ├── VarCalculationStrategy.java (interface)
│   ├── HistoricalSimulationStrategy.java
│   └── VarCalculationContext.java
├── model
│   ├── entity
│   │   ├── AuditRecord.java
│   │   └── User.java
│   ├── dto
│   │   ├── request
│   │   │   ├── TradeVarRequest.java
│   │   │   ├── PortfolioVarRequest.java
│   │   │   └── Trade.java
│   │   └── response
│   │       ├── VarResponse.java
│   │       ├── AuditHistoryResponse.java
│   │       └── AuditStatsResponse.java
│   └── enums
│       ├── CalculationMethod.java
│       ├── AuditStatus.java
│       └── UserRole.java
├── repository
│   ├── AuditRecordRepository.java
│   └── UserRepository.java
├── security
│   ├── JwtAuthenticationFilter.java
│   ├── JwtTokenProvider.java
│   └── UserPrincipal.java
├── exception
│   ├── GlobalExceptionHandler.java
│   ├── InsufficientDataException.java
│   └── InvalidConfidenceLevelException.java
└── util
    ├── VarCalculationUtil.java
    └── ValidationUtil.java
```

---

## 3. Data Model Design

### 3.1 Database Schema

#### AuditRecord Table
```sql
CREATE TABLE audit_record (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    request_payload TEXT,
    response_payload TEXT,
    execution_time_ms BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user_id ON audit_record(user_id);
CREATE INDEX idx_audit_timestamp ON audit_record(timestamp);
CREATE INDEX idx_audit_status ON audit_record(status);
```

#### User Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_username ON users(username);
```

### 3.2 Entity Relationships

```
User (1) ──────── (N) AuditRecord
     │
     └─ username references audit_record.user_id
```

---

## 4. API Design

### 4.1 REST Endpoints Specification

#### VaR Calculation Endpoints

**POST /api/v1/var/trade**
```yaml
Summary: Calculate VaR for single trade
Security: Bearer JWT
Roles: USER, ADMIN
Request:
  Content-Type: application/json
  Body:
    tradeId: string (required)
    historicalPnL: array of numbers (required, min 30 values)
    confidenceLevel: number (required, 0-1, e.g., 0.95)
Response:
  200 OK:
    tradeId: string
    var: number
    confidenceLevel: number
    calculationMethod: string
    timestamp: string (ISO-8601)
  400 Bad Request: Invalid input
  401 Unauthorized: Missing/invalid token
  500 Internal Server Error: Calculation failure
```

**POST /api/v1/var/portfolio**
```yaml
Summary: Calculate VaR for portfolio
Security: Bearer JWT
Roles: USER, ADMIN
Request:
  Content-Type: application/json
  Body:
    portfolioId: string (required)
    trades: array (required, min 1 trade)
      - tradeId: string
        historicalPnL: array of numbers
    confidenceLevel: number (required, 0-1)
Response:
  200 OK:
    portfolioId: string
    var: number
    confidenceLevel: number
    tradeCount: number
    calculationMethod: string
    timestamp: string
  400 Bad Request: Invalid input
  401 Unauthorized: Missing/invalid token
  500 Internal Server Error: Calculation failure
```

#### Audit Endpoints

**GET /api/v1/audit/history**
```yaml
Summary: Retrieve audit history
Security: Bearer JWT
Roles: ADMIN only
Query Parameters:
  page: integer (default: 0)
  size: integer (default: 20, max: 100)
  userId: string (optional)
  startDate: string (optional, ISO-8601)
  endDate: string (optional, ISO-8601)
Response:
  200 OK:
    content: array of audit records
    totalElements: number
    totalPages: number
    currentPage: number
  401 Unauthorized: Missing/invalid token
  403 Forbidden: Insufficient permissions
```

**GET /api/v1/audit/stats**
```yaml
Summary: Retrieve audit statistics
Security: Bearer JWT
Roles: ADMIN only
Response:
  200 OK:
    totalRequests: number
    successRate: number (0-1)
    averageExecutionTime: number (milliseconds)
    requestsByUser: object
    requestsByEndpoint: object
    errorRate: number (0-1)
  401 Unauthorized: Missing/invalid token
  403 Forbidden: Insufficient permissions
```

### 4.2 Authentication Endpoint

**POST /api/v1/auth/login**
```yaml
Summary: Authenticate user and receive JWT
Security: None
Request:
  username: string
  password: string
Response:
  200 OK:
    token: string (JWT)
    expiresIn: number (seconds)
    role: string
  401 Unauthorized: Invalid credentials
```

---

## 5. Core Algorithm Design

### 5.1 Historical Simulation VaR Algorithm

```java
/**
 * Calculate VaR using Historical Simulation method
 * 
 * @param historicalPnL List of historical P&L values
 * @param confidenceLevel Confidence level (e.g., 0.95 for 95%)
 * @return VaR value (positive number representing potential loss)
 */
public double calculateVaR(List<Double> historicalPnL, double confidenceLevel) {
    // Step 1: Validate inputs
    validateInputs(historicalPnL, confidenceLevel);
    
    // Step 2: Sort P&L in ascending order (worst to best)
    List<Double> sortedPnL = new ArrayList<>(historicalPnL);
    Collections.sort(sortedPnL);
    
    // Step 3: Calculate percentile position
    double percentile = 1 - confidenceLevel;
    double position = percentile * (sortedPnL.size() - 1);
    
    // Step 4: Interpolate if position is not integer
    int lowerIndex = (int) Math.floor(position);
    int upperIndex = (int) Math.ceil(position);
    
    double var;
    if (lowerIndex == upperIndex) {
        var = sortedPnL.get(lowerIndex);
    } else {
        double lowerValue = sortedPnL.get(lowerIndex);
        double upperValue = sortedPnL.get(upperIndex);
        double fraction = position - lowerIndex;
        var = lowerValue + fraction * (upperValue - lowerValue);
    }
    
    // Step 5: Return absolute value (VaR is positive)
    return Math.abs(Math.min(var, 0));
}
```

### 5.2 Portfolio VaR Algorithm

```java
/**
 * Calculate Portfolio VaR by aggregating trade P&Ls
 * 
 * @param trades Map of tradeId to historical P&L list
 * @param confidenceLevel Confidence level
 * @return Portfolio VaR value
 */
public double calculatePortfolioVaR(Map<String, List<Double>> trades, 
                                     double confidenceLevel) {
    // Step 1: Validate all trades have same number of observations
    validateTradeDataConsistency(trades);
    
    // Step 2: Aggregate P&L across all trades for each time period
    int periods = trades.values().iterator().next().size();
    List<Double> portfolioPnL = new ArrayList<>(periods);
    
    for (int i = 0; i < periods; i++) {
        double periodTotal = 0.0;
        for (List<Double> tradePnL : trades.values()) {
            periodTotal += tradePnL.get(i);
        }
        portfolioPnL.add(periodTotal);
    }
    
    // Step 3: Calculate VaR on aggregated portfolio P&L
    return calculateVaR(portfolioPnL, confidenceLevel);
}
```

---

## 6. Strategy Pattern Implementation

### 6.1 Strategy Interface

```java
public interface VarCalculationStrategy {
    /**
     * Calculate VaR for a single trade
     */
    double calculateTradeVaR(List<Double> historicalPnL, double confidenceLevel);
    
    /**
     * Calculate VaR for a portfolio
     */
    double calculatePortfolioVaR(Map<String, List<Double>> trades, 
                                  double confidenceLevel);
    
    /**
     * Get the calculation method name
     */
    CalculationMethod getMethodName();
}
```

### 6.2 Strategy Context

```java
@Service
public class VarCalculationContext {
    private final Map<CalculationMethod, VarCalculationStrategy> strategies;
    
    @Autowired
    public VarCalculationContext(List<VarCalculationStrategy> strategyList) {
        this.strategies = strategyList.stream()
            .collect(Collectors.toMap(
                VarCalculationStrategy::getMethodName,
                Function.identity()
            ));
    }
    
    public VarCalculationStrategy getStrategy(CalculationMethod method) {
        VarCalculationStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new UnsupportedOperationException(
                "Calculation method not supported: " + method
            );
        }
        return strategy;
    }
}
```

---

## 7. Security Design

### 7.1 Authentication Flow

```
1. User sends credentials to /api/v1/auth/login
2. System validates credentials against database
3. If valid, generate JWT token with user role
4. Return JWT to user
5. User includes JWT in Authorization header for subsequent requests
6. JwtAuthenticationFilter validates token and extracts user details
7. Spring Security checks role-based permissions
8. Request proceeds to controller if authorized
```

### 7.2 JWT Token Structure

```json
{
  "sub": "username",
  "role": "USER|ADMIN",
  "iat": 1234567890,
  "exp": 1234571490
}
```

### 7.3 Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/var/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/v1/audit/**").hasRole("ADMIN")
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

---

## 8. Audit System Design

### 8.1 Asynchronous Audit Logging

```java
@Service
public class AuditService {
    
    @Async("auditExecutor")
    public CompletableFuture<Void> logRequest(
        String userId,
        String endpoint,
        String requestPayload,
        String responsePayload,
        long executionTime,
        AuditStatus status,
        String errorMessage
    ) {
        AuditRecord record = AuditRecord.builder()
            .userId(userId)
            .endpoint(endpoint)
            .requestPayload(requestPayload)
            .responsePayload(responsePayload)
            .executionTimeMs(executionTime)
            .status(status)
            .errorMessage(errorMessage)
            .timestamp(LocalDateTime.now())
            .build();
        
        auditRepository.save(record);
        return CompletableFuture.completedFuture(null);
    }
}
```

### 8.2 Audit Interceptor

```java
@Component
@Aspect
public class AuditAspect {
    
    @Around("@annotation(Auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        String endpoint = getEndpoint(joinPoint);
        String requestPayload = serializeRequest(joinPoint.getArgs());
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            auditService.logRequest(
                userId, endpoint, requestPayload,
                serializeResponse(result), executionTime,
                AuditStatus.SUCCESS, null
            );
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            
            auditService.logRequest(
                userId, endpoint, requestPayload,
                null, executionTime,
                AuditStatus.ERROR, e.getMessage()
            );
            
            throw e;
        }
    }
}
```

---

## 9. Error Handling Design

### 9.1 Exception Hierarchy

```
RuntimeException
├── VarCalculationException (base)
│   ├── InsufficientDataException
│   ├── InvalidConfidenceLevelException
│   └── InconsistentDataException
├── SecurityException
│   ├── InvalidTokenException
│   └── InsufficientPermissionsException
└── AuditException
```

### 9.2 Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InsufficientDataException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientData(
        InsufficientDataException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INSUFFICIENT_DATA", ex.getMessage()));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
        AccessDeniedException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("ACCESS_DENIED", "Insufficient permissions"));
    }
    
    // Additional handlers...
}
```

---

## 10. Configuration Management

### 10.1 Application Profiles

**application.yml** (common)
```yaml
spring:
  application:
    name: var-calculation-service
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000  # 24 hours

var:
  calculation:
    min-data-points: 30
    default-confidence-level: 0.95
```

**application-dev.yml**
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:vardb
    driver-class-name: org.h2.Driver
  h2:
    console:
      enabled: true
  jpa:
    show-sql: true

logging:
  level:
    com.var.calculation: DEBUG
```

**application-prod.yml**
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

logging:
  level:
    com.var.calculation: INFO
```

---

## 11. Cloud Deployment Design

### 11.1 Docker Configuration

**Dockerfile**
```dockerfile
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY target/var-calculation-*.jar app.jar
EXPOSE 9001
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml**
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "9001:9001"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:postgresql://db:5432/vardb
      - DATABASE_USERNAME=varuser
      - DATABASE_PASSWORD=varpass
      - JWT_SECRET=your-secret-key
    depends_on:
      - db
  
  db:
    image: postgres:15-alpine
    environment:
      - POSTGRES_DB=vardb
      - POSTGRES_USER=varuser
      - POSTGRES_PASSWORD=varpass
    volumes:
      - postgres-data:/var/lib/postgresql/data

volumes:
  postgres-data:
```

### 11.2 Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: var-calculation-service
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
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: DATABASE_URL
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: url
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 9001
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 9001
          initialDelaySeconds: 20
          periodSeconds: 5
```

---

## 12. Performance Optimization

### 12.1 Caching Strategy
- Cache user details (Spring Security)
- No caching for VaR calculations (always fresh)
- Cache audit statistics (5-minute TTL)

### 12.2 Database Optimization
- Index on audit_record(user_id, timestamp)
- Connection pooling (HikariCP)
- Batch insert for audit records (if needed)

### 12.3 Async Processing
- Audit logging runs asynchronously
- Thread pool configuration: 5-10 threads

---

## 13. Monitoring & Observability

### 13.1 Metrics to Track
- VaR calculation request rate
- Average calculation time
- Error rate by endpoint
- Audit log write rate
- Database connection pool usage
- JVM memory usage

### 13.2 Health Checks
```yaml
/actuator/health:
  - Database connectivity
  - Disk space
  - Application status
```

### 13.3 Logging Strategy
- INFO: Request/response for VaR calculations
- DEBUG: Detailed calculation steps
- ERROR: Exceptions with stack traces
- Structured JSON logging for production

---

## 14. Testing Strategy

### 14.1 Unit Tests
- VaR calculation algorithms (various scenarios)
- Strategy pattern implementations
- Service layer business logic
- Utility methods

### 14.2 Integration Tests
- REST endpoint tests (MockMvc)
- Database operations
- Security configurations
- Audit logging flow

### 14.3 Test Data
```java
// Known VaR calculation test case
List<Double> testPnL = Arrays.asList(
    -10.0, -5.0, -2.0, 0.0, 3.0, 5.0, 8.0, 10.0, 12.0, 15.0
);
double confidenceLevel = 0.95;
double expectedVaR = 5.0; // Approximately
```

---

## 15. Migration & Deployment Strategy

### 15.1 Database Migration (Flyway)
```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_audit_record_table.sql
└── V3__create_indexes.sql
```

### 15.2 Deployment Steps
1. Build application: `mvn clean package`
2. Build Docker image: `docker build -t var-calculation:latest .`
3. Run database migrations
4. Deploy application
5. Verify health endpoint
6. Run smoke tests

---

## 16. Security Considerations

### 16.1 Input Validation
- Confidence level: 0 < value < 1
- Historical P&L: minimum 30 data points
- Trade IDs: alphanumeric only
- Request size limits

### 16.2 Rate Limiting
- 100 requests per minute per user
- 1000 requests per minute globally

### 16.3 Secrets Management
- JWT secret from environment variable
- Database credentials from secrets manager
- No hardcoded secrets in code

---

## 17. Future Enhancements

### 17.1 Phase 2 Features
- Variance-Covariance VaR strategy
- Monte Carlo VaR strategy
- Conditional VaR (CVaR)

### 17.2 Scalability Improvements
- Redis caching layer
- Message queue for audit logging (Kafka/RabbitMQ)
- Read replicas for audit queries

---

**Design Status:** Ready for Implementation  
**Next Steps:** Sprint planning, task breakdown, development kickoff
