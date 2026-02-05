# VaR Calculation Service Design

## System Architecture

The VaR Calculation Service uses a layered architecture with clear separation of concerns:

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Client Applications                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │   Web Browser   │  │  Mobile App     │  │  API Client     │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                         HTTPS/REST API
                                │
┌─────────────────────────────────────────────────────────────────┐
│                  Presentation Layer                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  VarController  │  │ AuthController  │  │AuditController  │ │
│  │  /api/v1/var    │  │ /api/v1/auth    │  │ /api/v1/audit   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                        JWT Authentication
                                │
┌─────────────────────────────────────────────────────────────────┐
│                    Security Layer                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │JwtAuthFilter    │  │JwtTokenProvider │  │  SecurityConfig │ │
│  │(Validation)     │  │(Token Gen)      │  │  (RBAC)         │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                        Business Logic
                                │
┌─────────────────────────────────────────────────────────────────┐
│                    Service Layer                                │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │VarCalculation   │  │UserDetailsImpl  │  │  AuditService   │ │
│  │Service          │  │(Authentication) │  │  (Compliance)   │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                        Algorithm Layer
                                │
┌─────────────────────────────────────────────────────────────────┐
│                   Strategy Layer                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │           HistoricalSimulationStrategy                      │ │
│  │  • calculateTradeVaR()                                      │ │
│  │  • calculatePortfolioVaR()                                  │ │
│  │  • validateInput()                                          │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                        Data Access
                                │
┌─────────────────────────────────────────────────────────────────┐
│                   Data Access Layer                             │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐ │
│  │  UserRepository │  │AuditRecordRepo  │  │   Cache Layer   │ │
│  │  (JPA/Hibernate)│  │  (JPA/Hibernate)│  │   (Caffeine)    │ │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                        Data Storage
                                │
┌─────────────────────────────────────────────────────────────────┐
│                    Data Layer                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                H2 Database (Development)                    │ │
│  │              PostgreSQL (Production)                        │ │
│  │  • Users Table                                              │ │
│  │  • Audit Records Table                                      │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Component Architecture

**Presentation Layer**
- VarController handles VaR calculation requests
- AuthController manages user authentication  
- AuditController provides audit data access

**Service Layer**
- VarCalculationService orchestrates business operations
- UserDetailsServiceImpl handles user authentication
- AuditService manages compliance logging

**Strategy Layer**
- HistoricalSimulationStrategy implements VaR calculations
- Pluggable design allows for future calculation methods

**Data Layer**
- UserRepository manages user data
- AuditRecordRepository handles audit logging
- H2 in-memory database for development

## Security Design

### Security Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Client Request                               │
│  POST /api/v1/var/trade                                         │
│  Authorization: Bearer <JWT-TOKEN>                              │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                JWT Authentication Filter                        │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  1. Extract JWT token from Authorization header            │ │
│  │  2. Validate token signature and expiration                │ │
│  │  3. Extract username and role from token claims            │ │
│  │  4. Set SecurityContext with authenticated user            │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Role-Based Authorization                        │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  • /api/v1/var/** → Requires USER role                     │ │
│  │  • /api/v1/audit/** → Requires ADMIN role                  │ │
│  │  • /api/v1/auth/** → Public access                         │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Controller Layer                             │
│  Process request with authenticated user context                │
└─────────────────────────────────────────────────────────────────┘
```

### Authentication Flow

Users login with username/password to receive JWT tokens. The AuthController validates credentials and generates tokens using JwtTokenProvider. Tokens are valid for 24 hours by default.

### Authorization Flow

All requests include JWT tokens in Authorization header. JwtAuthenticationFilter validates tokens and sets security context. Controllers check user roles before processing requests.

### Security Configuration

JWT tokens use 256-bit secret keys with HS512 signing. Passwords are encrypted with BCrypt strength 12. CORS is configured for development and CSRF is disabled for stateless API design.

## VaR Calculation Design

### VaR Calculation Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    VaR Calculation Request                      │
│  TradeVarRequest or PortfolioVarRequest                         │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   VarCalculationService                         │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  1. Check cache for existing result                        │ │
│  │  2. If cache miss, delegate to strategy                    │ │
│  │  3. Log audit record (start time)                          │ │
│  │  4. Cache result before returning                          │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│              HistoricalSimulationStrategy                       │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │  Trade VaR:                                                 │ │
│  │  1. Validate input (min data points, confidence level)     │ │
│  │  2. Sort historical P&L in ascending order                 │ │
│  │  3. Calculate percentile position                          │ │
│  │  4. Apply linear interpolation if needed                   │ │
│  │  5. Return absolute VaR value                              │ │
│  │                                                             │ │
│  │  Portfolio VaR:                                             │ │
│  │  1. Validate all trades have same periods                  │ │
│  │  2. Aggregate P&L across trades for each period           │ │
│  │  3. Apply Trade VaR algorithm to aggregated data          │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                      VaR Response                               │
│  • Calculated VaR value                                         │
│  • Confidence level                                             │
│  • Calculation method (HISTORICAL_SIMULATION)                   │
│  • Trade count                                                  │
│  • Timestamp                                                    │
└─────────────────────────────────────────────────────────────────┘
```

### Historical Simulation Algorithm

The Historical Simulation method uses actual historical P&L data to estimate potential losses:

1. Validate inputs - check P&L data exists, has minimum data points, and confidence level is between 0 and 1
2. Sort P&L data in ascending order
3. Calculate percentile position as (1 - confidence level) × (n - 1)
4. Handle interpolation between data points if needed
5. Return absolute value of VaR

### Portfolio VaR Calculation

Portfolio VaR aggregates individual trade P&L before applying Historical Simulation:

1. Validate all trades have same number of periods
2. Sum P&L across all trades for each time period
3. Apply Historical Simulation to aggregated P&L
4. Return portfolio VaR value

### Caching Strategy

Cache keys use trade ID or portfolio ID combined with confidence level. Caffeine provides in-memory caching with 1-hour TTL and 1000 entry maximum. LRU eviction removes oldest entries when cache is full.

## Data Model Design

### Entity Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Data Model                               │
│                                                                 │
│  ┌─────────────────┐         ┌─────────────────┐               │
│  │      User       │         │   AuditRecord   │               │
│  ├─────────────────┤         ├─────────────────┤               │
│  │ id (PK)         │    1    │ id (PK)         │               │
│  │ username        │ ────────│ username        │               │
│  │ password        │    *    │ endpoint        │               │
│  │ role (Enum)     │         │ executionTime   │               │
│  │ enabled         │         │ success         │               │
│  │ createdAt       │         │ errorMessage    │               │
│  │ updatedAt       │         │ timestamp       │               │
│  └─────────────────┘         └─────────────────┘               │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                    DTO Structure                            │ │
│  │                                                             │ │
│  │  TradeVarRequest          PortfolioVarRequest               │ │
│  │  ├─ tradeId               ├─ portfolioId                    │ │
│  │  ├─ historicalPnL         ├─ confidenceLevel                │ │
│  │  └─ confidenceLevel       └─ trades[]                       │ │
│  │                                │                            │ │
│  │  Trade                         │                            │ │
│  │  ├─ tradeId                    │                            │ │
│  │  └─ historicalPnL ─────────────┘                            │ │
│  │                                                             │ │
│  │  VarResponse                                                │ │
│  │  ├─ id                                                      │ │
│  │  ├─ var                                                     │ │
│  │  ├─ confidenceLevel                                         │ │
│  │  ├─ calculationMethod                                       │ │
│  │  ├─ tradeCount                                              │ │
│  │  └─ timestamp                                               │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### User Entity
- id (Long primary key)
- username (unique string)
- password (BCrypt encrypted)
- role (USER or ADMIN enum)
- enabled flag
- created and updated timestamps

### AuditRecord Entity
- id (Long primary key)
- username
- endpoint path
- execution time in milliseconds
- success boolean flag
- error message (optional)
- timestamp

### Request/Response DTOs

Java 21 records provide immutable data transfer objects:

TradeVarRequest contains trade ID, historical P&L list, and confidence level.
PortfolioVarRequest contains portfolio ID, confidence level, and list of trades.
VarResponse contains calculated VaR, confidence level, method, trade count, and timestamp.

## Performance Design

### Performance Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Performance Layers                          │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                 Java 21 Virtual Threads                    │ │
│  │  • Lightweight thread creation (millions possible)         │ │
│  │  • Reduced memory footprint per thread                     │ │
│  │  • Better resource utilization for I/O operations         │ │
│  │  • Tomcat: max=200, min-spare=10                          │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                │                                │
│                                ▼                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                   Caching Layer                             │ │
│  │                                                             │ │
│  │  Request → Cache Check → Cache Hit? ──Yes──→ Return Result │ │
│  │                │              │                             │ │
│  │                │             No                             │ │
│  │                │              │                             │ │
│  │                ▼              ▼                             │ │
│  │         Strategy Calculation                                │ │
│  │                │                                            │ │
│  │                ▼                                            │ │
│  │         Cache Result → Return Result                        │ │
│  │                                                             │ │
│  │  Cache Configuration:                                       │ │
│  │  • Provider: Caffeine (in-memory)                          │ │
│  │  • TTL: 1 hour                                             │ │
│  │  • Max Size: 1000 entries                                  │ │
│  │  • Eviction: LRU                                           │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                │                                │
│                                ▼                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                Database Connection Pool                     │ │
│  │  • HikariCP for connection pooling                         │ │
│  │  • Optimized connection lifecycle                          │ │
│  │  • Connection leak detection                               │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Virtual Threads

Java 21 Virtual Threads enable lightweight concurrency. Spring Boot configuration enables virtual threads for web requests. Tomcat is configured for 200 maximum threads with 10 minimum spare threads.

Benefits include lightweight thread creation, reduced memory usage, better resource utilization for I/O operations, and simplified concurrent programming.

### Caching Architecture

Requests check cache before calculation. Cache hits return stored results immediately. Cache misses trigger calculation, store results, then return values. Method-level caching uses Spring annotations.

### Performance Optimizations

Stream API provides efficient data processing. Method-level caching reduces duplicate calculations. JPA entities use optimal loading strategies. HikariCP manages database connection pooling.

## Error Handling Design

### Exception Hierarchy

IllegalArgumentException maps to 400 Bad Request for validation errors. SecurityException maps to 401/403 for authentication issues. CalculationException maps to 422 for business logic failures. Generic exceptions map to 500 Internal Server Error.

### Global Exception Handler

RestControllerAdvice provides centralized error handling. Specific handlers catch different exception types. Generic handler catches unexpected errors. All responses use consistent ErrorResponse format.

## Configuration Design

### Application Properties

Server configuration includes port 9001 and Tomcat thread settings. Spring configuration covers application name, datasource, JPA, and virtual threads. Custom configuration includes JWT settings and VaR calculation parameters.

### Configuration Classes

VarCalculationProperties uses ConfigurationProperties annotation for type-safe configuration. Properties include minimum data points and cache settings with default values.

## Testing Strategy

### Test Categories

Unit tests cover VaR calculation algorithms, input validation, and utility functions. Integration tests verify controller endpoints, database operations, and security configuration. Property-based tests validate mathematical properties and boundary conditions.

### Testing Approach

Tests verify algorithm correctness with known datasets. Input validation tests check boundary conditions. Security tests verify authentication and authorization. Performance tests measure response times under load.

## Monitoring and Observability

### Actuator Endpoints

Health endpoint shows application status. Info endpoint provides application metadata. Metrics endpoint exposes performance data. All endpoints support JSON format.

### Logging Strategy

Structured logging uses correlation IDs for request tracking. Log levels include DEBUG for development and INFO for production. MDC provides request context in log messages.

### Metrics Collection

Key metrics include request/response times, cache hit ratios, error rates by endpoint, active user sessions, and database connection pool status.

## Deployment Architecture

### Deployment Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    Development Environment                      │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                Docker Container                             │ │
│  │  ┌─────────────────────────────────────────────────────────┐ │ │
│  │  │            VaR Application                              │ │ │
│  │  │  • OpenJDK 21 JRE Slim                                 │ │ │
│  │  │  • Non-root user (appuser)                             │ │ │
│  │  │  • Health checks enabled                               │ │ │
│  │  │  • Port 9001 exposed                                   │ │ │
│  │  └─────────────────────────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                │                                │
│                                ▼                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                H2 In-Memory Database                        │ │
│  │  • Development and testing                                  │ │
│  │  • Auto-creates tables on startup                          │ │
│  │  • H2 console available at /h2-console                     │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                   Production Environment                        │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                 Load Balancer                               │ │
│  │  • HTTPS termination                                        │ │
│  │  • SSL certificates                                         │ │
│  │  • Health check routing                                     │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                │                                │
│                                ▼                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │              Container Orchestration                        │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │ │
│  │  │ Container 1 │  │ Container 2 │  │ Container 3 │         │ │
│  │  │ VaR App     │  │ VaR App     │  │ VaR App     │         │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘         │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                │                                │
│                                ▼                                │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                External Services                            │ │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │ │
│  │  │ PostgreSQL  │  │ Redis Cache │  │ Secrets     │         │ │
│  │  │ Database    │  │ (Distributed)│  │ Manager     │         │ │
│  │  └─────────────┘  └─────────────┘  └─────────────┘         │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Container Design

Multi-stage Dockerfile reduces image size. Runtime image uses OpenJDK 21 JRE slim base. Non-root user improves security. Health checks enable container orchestration.

### Environment Configuration

Development uses H2 in-memory database with debug logging. Production uses external database with info-level logging. Environment variables configure database connections and JWT secrets.

## Future Considerations

### Microservices Evolution

Current monolithic design can split into separate services for calculations, user management, audit logging, and API gateway routing.

### Scalability Enhancements

Future improvements include distributed caching with Redis, message queues for async processing, load balancing across instances, and database sharding for audit data.

### Technology Upgrades

Potential upgrades include GraalVM native compilation, reactive streams with WebFlux, GraphQL for flexible queries, event sourcing for audit trails, and CQRS for read/write separation.