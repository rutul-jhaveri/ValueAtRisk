# VaR Calculation Service - Design Document

## 1. System Architecture

### 1.1 High-Level Architecture

The VaR Calculation Service follows a layered architecture pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                    Presentation Layer                       │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │  VarController  │  │ AuthController  │  │AuditController│ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                     Service Layer                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────┐ │
│  │VarCalculationSvc│  │UserDetailsImpl  │  │ AuditService│ │
│  └─────────────────┘  └─────────────────┘  └─────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                   Strategy Layer                           │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │        HistoricalSimulationStrategy                     │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                  Persistence Layer                         │
│  ┌─────────────────┐  ┌─────────────────┐                  │
│  │  UserRepository │  │AuditRecordRepo  │                  │
│  └─────────────────┘  └─────────────────┘                  │
└─────────────────────────────────────────────────────────────┘
                                │
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer                             │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │                H2 In-Memory Database                    │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Component Responsibilities

**Controllers (Presentation Layer)**
- Handle HTTP requests/responses
- Perform input validation
- Extract authentication context
- Delegate business logic to services

**Services (Business Layer)**
- Orchestrate business operations
- Manage transactions and caching
- Handle cross-cutting concerns (audit, logging)
- Coordinate between strategies and repositories

**Strategies (Algorithm Layer)**
- Implement VaR calculation algorithms
- Encapsulate mathematical computations
- Provide pluggable calculation methods

**Repositories (Data Access Layer)**
- Abstract database operations
- Provide CRUD operations for entities
- Handle query optimization

## 2. Security Design

### 2.1 Authentication Flow

```
Client                    AuthController              JwtTokenProvider
  │                            │                            │
  │ POST /auth/login          │                            │
  │ {username, password}      │                            │
  │──────────────────────────▶│                            │
  │                            │ validateCredentials()      │
  │                            │──────────────────────────▶ │
  │                            │                            │
  │                            │ generateToken()            │
  │                            │──────────────────────────▶ │
  │                            │ ◀──────────────────────────│
  │ ◀──────────────────────────│ JWT Token                  │
  │                            │                            │
```

### 2.2 Authorization Flow

```
Client                 JwtAuthFilter              SecurityContext
  │                         │                           │
  │ GET /api/v1/var/trade  │                           │
  │ Authorization: Bearer   │                           │
  │──────────────────────▶ │                           │
  │                         │ validateToken()           │
  │                         │─────────────────────────▶ │
  │                         │ ◀─────────────────────────│
  │                         │ setAuthentication()       │
  │                         │─────────────────────────▶ │
  │                         │                           │
  │ ◀──────────────────────│ Continue to Controller    │
```

### 2.3 Security Configuration

- **JWT Secret**: 256-bit key for token signing
- **Token Expiration**: 24 hours (configurable)
- **Password Encoding**: BCrypt with strength 12
- **CORS**: Configured for development (all origins)
- **CSRF**: Disabled for stateless API

## 3. VaR Calculation Design

### 3.1 Historical Simulation Algorithm

The Historical Simulation method uses actual historical P&L data to estimate potential losses:

```java
Algorithm: Historical Simulation VaR
Input: historicalPnL[], confidenceLevel
Output: VaR value

1. Validate inputs:
   - historicalPnL not null/empty
   - size >= minDataPoints (configurable)
   - 0 < confidenceLevel < 1

2. Sort P&L data in ascending order

3. Calculate percentile position:
   percentile = 1 - confidenceLevel
   position = percentile × (n - 1)

4. Handle interpolation:
   lower = floor(position)
   upper = ceil(position)
   
   if (lower == upper):
       VaR = sortedPnL[lower]
   else:
       fraction = position - lower
       VaR = interpolate(sortedPnL[lower], sortedPnL[upper], fraction)

5. Return absolute value of VaR
```

### 3.2 Portfolio VaR Calculation

Portfolio VaR aggregates individual trade P&L before applying Historical Simulation:

```java
Algorithm: Portfolio VaR
Input: List<List<Double>> tradesPnL, confidenceLevel
Output: Portfolio VaR

1. Validate all trades have same number of periods

2. Aggregate P&L across trades for each period:
   for each period i:
       portfolioPnL[i] = sum(trade[j].pnl[i] for all trades j)

3. Apply Historical Simulation to aggregated P&L:
   return calculateTradeVaR(portfolioPnL, confidenceLevel)
```

### 3.3 Caching Strategy

**Cache Keys:**
- Trade VaR: `{tradeId}_{confidenceLevel}`
- Portfolio VaR: `{portfolioId}_{confidenceLevel}`

**Cache Configuration:**
- Provider: Caffeine (in-memory)
- TTL: 1 hour (configurable)
- Max Size: 1000 entries per cache
- Eviction: LRU (Least Recently Used)

## 4. Data Model Design

### 4.1 Entity Relationship Diagram

```
┌─────────────────┐         ┌─────────────────┐
│      User       │         │   AuditRecord   │
├─────────────────┤         ├─────────────────┤
│ id (Long) PK    │         │ id (Long) PK    │
│ username        │    1    │ username        │
│ password        │ ────────│ endpoint        │
│ role (Enum)     │    *    │ executionTime   │
│ enabled         │         │ success         │
│ createdAt       │         │ errorMessage    │
│ updatedAt       │         │ timestamp       │
└─────────────────┘         └─────────────────┘
```

### 4.2 DTO Design (Java 21 Records)

**Request DTOs:**
```java
// Immutable request objects using records
public record TradeVarRequest(
    String tradeId,
    List<Double> historicalPnL,
    Double confidenceLevel
) {}

public record PortfolioVarRequest(
    String portfolioId,
    Double confidenceLevel,
    List<Trade> trades
) {}
```

**Response DTOs:**
```java
public record VarResponse(
    String id,
    Double var,
    Double confidenceLevel,
    String calculationMethod,
    Integer tradeCount,
    LocalDateTime timestamp
) {}
```

## 5. Performance Design

### 5.1 Virtual Threads (Java 21)

The application leverages Virtual Threads for improved concurrency:

```yaml
# Configuration
spring:
  threads:
    virtual:
      enabled: true

server:
  tomcat:
    threads:
      max: 200  # Virtual threads scale much higher
      min-spare: 10
```

**Benefits:**
- Lightweight thread creation (millions possible)
- Reduced memory footprint per thread
- Better resource utilization for I/O-bound operations
- Simplified concurrent programming model

### 5.2 Caching Architecture

```
Request → Controller → Service → Cache Check
                                     │
                              Cache Hit? ──Yes──→ Return Cached Result
                                     │
                                    No
                                     │
                              Strategy Calculation
                                     │
                              Cache Result → Return Result
```

### 5.3 Performance Optimizations

- **Stream API**: Efficient data processing with parallel streams where applicable
- **Method-level Caching**: `@Cacheable` annotations on service methods
- **Lazy Loading**: JPA entities configured for optimal loading strategies
- **Connection Pooling**: HikariCP for database connections

## 6. Error Handling Design

### 6.1 Exception Hierarchy

```
RuntimeException
├── IllegalArgumentException (400 Bad Request)
├── SecurityException (401/403)
├── CalculationException (422 Unprocessable Entity)
└── SystemException (500 Internal Server Error)
```

### 6.2 Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidation(Exception e) {
        return ResponseEntity.badRequest()
            .body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        // Log error, return generic message
        return ResponseEntity.internalServerError()
            .body(new ErrorResponse("INTERNAL_ERROR", "Calculation failed"));
    }
}
```

## 7. Configuration Design

### 7.1 Application Properties Structure

```yaml
# Server Configuration
server:
  port: 9001
  tomcat.threads: {...}

# Spring Configuration  
spring:
  application.name: var-calculation-service
  datasource: {...}
  jpa: {...}
  threads.virtual.enabled: true

# Custom Configuration
jwt:
  secret: ${JWT_SECRET:default-secret}
  expiration: ${JWT_EXPIRATION:86400000}

var:
  calculation:
    min-data-points: ${MIN_DATA_POINTS:5}
    cache:
      ttl: ${CACHE_TTL:3600}
      max-size: ${CACHE_MAX_SIZE:1000}
```

### 7.2 Configuration Classes

```java
@ConfigurationProperties(prefix = "var.calculation")
@Data
public class VarCalculationProperties {
    private int minDataPoints = 5;
    private Cache cache = new Cache();
    
    @Data
    public static class Cache {
        private long ttl = 3600;
        private int maxSize = 1000;
    }
}
```

## 8. Testing Strategy

### 8.1 Test Pyramid

```
                    ┌─────────────────┐
                    │   E2E Tests     │ ← Few, high-value scenarios
                    │   (Integration) │
                    └─────────────────┘
                  ┌───────────────────────┐
                  │    Service Tests      │ ← Business logic validation
                  │   (Unit + Mock)       │
                  └───────────────────────┘
              ┌─────────────────────────────────┐
              │        Unit Tests               │ ← Algorithm correctness
              │   (Strategy, Validation)        │
              └─────────────────────────────────┘
```

### 8.2 Test Categories

**Unit Tests:**
- VaR calculation algorithms
- Input validation logic
- Utility functions
- DTO serialization

**Integration Tests:**
- Controller endpoints
- Database operations
- Security configuration
- Cache behavior

**Property-Based Tests:**
- VaR calculation properties
- Input boundary conditions
- Mathematical invariants

## 9. Monitoring & Observability

### 9.1 Actuator Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
```

### 9.2 Logging Strategy

```java
// Structured logging with correlation IDs
@Slf4j
public class VarCalculationService {
    
    public VarResponse calculateTradeVaR(TradeVarRequest request, String username) {
        MDC.put("tradeId", request.tradeId());
        MDC.put("username", username);
        
        log.info("Starting VaR calculation for trade: {}", request.tradeId());
        // ... calculation logic
        log.info("VaR calculation completed in {}ms", duration);
    }
}
```

### 9.3 Metrics Collection

- Request/response times
- Cache hit ratios
- Error rates by endpoint
- Active user sessions
- Database connection pool metrics

## 10. Deployment Architecture

### 10.1 Container Design

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/var-calculation-*.jar app.jar
EXPOSE 9001
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 10.2 Environment Configuration

**Development:**
- H2 in-memory database
- Debug logging enabled
- Swagger UI accessible
- CORS permissive

**Production:**
- External database (PostgreSQL/Oracle)
- Info-level logging
- Swagger UI disabled
- CORS restricted
- Health checks configured
- Resource limits applied

## 11. Future Architecture Considerations

### 11.1 Microservices Evolution

Current monolithic design can evolve to:
- **Calculation Service**: Core VaR algorithms
- **User Service**: Authentication/authorization
- **Audit Service**: Compliance logging
- **Gateway Service**: API routing and rate limiting

### 11.2 Scalability Enhancements

- **Distributed Caching**: Redis cluster
- **Message Queues**: Async calculation processing
- **Load Balancing**: Multiple service instances
- **Database Sharding**: Partition audit data by date
- **CDN Integration**: Static content delivery

### 11.3 Technology Upgrades

- **GraalVM**: Native image compilation
- **Reactive Streams**: Non-blocking I/O with WebFlux
- **GraphQL**: Flexible API queries
- **Event Sourcing**: Audit trail as event stream
- **CQRS**: Separate read/write models