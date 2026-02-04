# Value at Risk (VaR) Calculation System - Requirements Document

**Project:** VaR Calculation REST API  
**Type:** Spring Boot Application  
**Created:** 2026-02-02  
**Version:** 1.0

---

## 1. Executive Summary

Build a cloud-ready Spring Boot application that calculates Value at Risk (VaR) using historical simulation method. The system will provide REST APIs for VaR calculation at both trade and portfolio levels, with comprehensive audit logging and role-based access control.

---

## 2. Business Requirements

### 2.1 Core Functionality

#### VaR Definition
Value at Risk (VaR) is a statistical measure representing the maximum potential loss over a given time period at a specified confidence level.

**Example:** Given historical P&L values, if the 95% worst P&L was a £5m loss, there is a 5% (1-0.95) chance that the portfolio could lose £5m on a given day.

#### Calculation Method
- **Method:** Historical Simulation (not variance-covariance or Monte Carlo)
- **Common Confidence Levels:** 95%, 97.5%, 99%
- **Confidence Level:** Must be configurable parameter

### 2.2 Functional Requirements

#### FR-1: Single Trade VaR Calculation
- Calculate VaR for a single trade
- Input: Series of historical P&L values
- Input: Confidence level (configurable)
- Output: Single VaR value

#### FR-2: Portfolio VaR Calculation
- Calculate VaR for multiple trades (portfolio level)
- Input: Historical P&L values for multiple trades
- Output: Single aggregated VaR value at portfolio level
- Note: Portfolio VaR ≠ Sum of individual VaRs (diversification effect)

#### FR-3: Extensible VaR Strategy Design
- Design API to support multiple VaR calculation methods
- Initial implementation: Historical Simulation
- Future support: Variance-Covariance, Monte Carlo (not implemented now)

#### FR-4: Audit History
- Record all VaR calculation requests
- Capture: userId, execution time, success/error status, input parameters, result
- Persist to database

#### FR-5: Audit Statistics Endpoint
- Expose endpoint to retrieve audit history statistics
- Metrics: request count, success rate, average execution time, user activity

#### FR-6: Role-Based Access Control
- **USER Role:** Can access VaR calculation endpoints
- **ADMIN Role:** Can access VaR calculation + audit history endpoints
- Authentication and authorization required

---

## 3. Technical Requirements

### 3.1 Technology Stack

#### Core Framework
- **Spring Boot** 2.7.x (last version supporting Java 11)
- **Java** 11 or higher
- **Maven** or **Gradle** for dependency management

#### Required Spring Modules
- Spring Web (REST APIs)
- Spring Security (authentication/authorization)
- Spring Data JPA (database access)
- Spring Boot Actuator (health checks, metrics)

#### Database
- **Development:** H2 (in-memory) or PostgreSQL
- **Production:** PostgreSQL, MySQL, or cloud-managed database
- **ORM:** JPA/Hibernate

#### Additional Libraries
- Lombok (reduce boilerplate)
- MapStruct or ModelMapper (DTO mapping)
- Springdoc OpenAPI (API documentation)
- JUnit 5 + Mockito (testing)

### 3.2 REST API Endpoints

#### VaR Calculation Endpoints

**POST /api/v1/var/trade**
- Calculate VaR for single trade
- Request Body:
  ```json
  {
    "tradeId": "string",
    "historicalPnL": [number],
    "confidenceLevel": number
  }
  ```
- Response:
  ```json
  {
    "tradeId": "string",
    "var": number,
    "confidenceLevel": number,
    "calculationMethod": "HISTORICAL_SIMULATION",
    "timestamp": "ISO-8601"
  }
  ```
- Access: USER, ADMIN

**POST /api/v1/var/portfolio**
- Calculate VaR for portfolio
- Request Body:
  ```json
  {
    "portfolioId": "string",
    "trades": [
      {
        "tradeId": "string",
        "historicalPnL": [number]
      }
    ],
    "confidenceLevel": number
  }
  ```
- Response:
  ```json
  {
    "portfolioId": "string",
    "var": number,
    "confidenceLevel": number,
    "tradeCount": number,
    "calculationMethod": "HISTORICAL_SIMULATION",
    "timestamp": "ISO-8601"
  }
  ```
- Access: USER, ADMIN

#### Audit History Endpoints

**GET /api/v1/audit/history**
- Retrieve audit records
- Query Parameters: page, size, userId, startDate, endDate
- Response: Paginated list of audit records
- Access: ADMIN only

**GET /api/v1/audit/stats**
- Retrieve audit statistics
- Response:
  ```json
  {
    "totalRequests": number,
    "successRate": number,
    "averageExecutionTime": number,
    "requestsByUser": {},
    "requestsByEndpoint": {},
    "errorRate": number
  }
  ```
- Access: ADMIN only

### 3.3 Data Model

#### Audit Record Entity
```
- id (Long, PK)
- userId (String)
- endpoint (String)
- requestPayload (JSON/Text)
- responsePayload (JSON/Text)
- executionTimeMs (Long)
- status (SUCCESS/ERROR)
- errorMessage (String, nullable)
- timestamp (LocalDateTime)
```

### 3.4 Design Patterns

#### Strategy Pattern for VaR Calculation
```
VarCalculationStrategy (interface)
  - calculateTradeVar()
  - calculatePortfolioVar()

Implementations:
  - HistoricalSimulationStrategy
  - (Future) VarianceCovarianceStrategy
  - (Future) MonteCarloStrategy
```

#### Service Layer Architecture
```
Controller Layer → Service Layer → Repository Layer
                ↓
            Strategy Pattern
                ↓
            Audit Service
```

---

## 4. Non-Functional Requirements

### 4.1 Performance
- VaR calculation response time: < 2 seconds for portfolios up to 100 trades
- Audit logging should not impact calculation performance (async processing)
- Support concurrent requests (thread-safe)

### 4.2 Security
- JWT-based authentication or OAuth2
- HTTPS only in production
- Input validation and sanitization
- SQL injection prevention (parameterized queries)
- Rate limiting on API endpoints

### 4.3 Reliability
- 99.9% uptime target
- Graceful error handling
- Transaction management for audit logging
- Database connection pooling

### 4.4 Scalability
- Stateless application design
- Horizontal scaling capability
- Database indexing on audit queries
- Caching for frequently accessed data (if applicable)

### 4.5 Maintainability
- Clean code principles
- Comprehensive unit tests (>80% coverage)
- Integration tests for REST endpoints
- API documentation (OpenAPI/Swagger)
- Logging (SLF4J + Logback)

---

## 5. Cloud Readiness Requirements

### 5.1 Containerization
- **Docker:** Create Dockerfile for application
- **Docker Compose:** Local development environment with database
- Multi-stage build for optimized image size

### 5.2 Configuration Management
- Externalized configuration (application.yml/properties)
- Environment-specific profiles (dev, test, prod)
- Support for environment variables
- Spring Cloud Config (optional, for centralized config)

### 5.3 Health & Monitoring
- Spring Boot Actuator endpoints:
  - /actuator/health
  - /actuator/metrics
  - /actuator/info
- Structured logging (JSON format)
- Integration with monitoring tools (Prometheus, Grafana)
- Distributed tracing support (optional: Spring Cloud Sleuth)

### 5.4 Database
- Cloud-managed database support (AWS RDS, Azure SQL, GCP Cloud SQL)
- Database migration tool (Flyway or Liquibase)
- Connection pooling (HikariCP)
- Read replicas support for audit queries

### 5.5 Deployment
- CI/CD pipeline ready
- Infrastructure as Code (Terraform, CloudFormation)
- Kubernetes deployment manifests (optional)
- Auto-scaling configuration
- Load balancer compatibility

### 5.6 Cloud Platform Considerations

#### AWS
- Deploy on ECS/EKS or Elastic Beanstalk
- RDS for database
- CloudWatch for logging/monitoring
- Secrets Manager for credentials

#### Azure
- Deploy on Azure App Service or AKS
- Azure Database for PostgreSQL
- Application Insights for monitoring
- Key Vault for secrets

#### GCP
- Deploy on Cloud Run or GKE
- Cloud SQL for database
- Cloud Monitoring and Logging
- Secret Manager for credentials

---

## 6. Testing Requirements

### 6.1 Unit Tests
- Test VaR calculation logic with known datasets
- Test strategy pattern implementations
- Test service layer business logic
- Mock external dependencies
- Target: >80% code coverage

### 6.2 Integration Tests
- Test REST endpoints with MockMvc
- Test database operations
- Test security configurations
- Test audit logging flow

### 6.3 Test Scenarios

#### VaR Calculation Tests
- Single trade with various confidence levels (95%, 97.5%, 99%)
- Portfolio with 2, 10, 50 trades
- Edge cases: empty data, single data point, all positive/negative values
- Diversification effect verification (Portfolio VaR < Sum of individual VaRs)

#### Security Tests
- Unauthorized access attempts
- Role-based access enforcement
- Invalid JWT tokens

#### Audit Tests
- Successful request logging
- Failed request logging
- Audit statistics accuracy

---

## 7. Documentation Requirements

### 7.1 Code Documentation
- Javadoc for public APIs
- Inline comments for complex algorithms
- README.md with setup instructions

### 7.2 API Documentation
- OpenAPI/Swagger specification
- Interactive API documentation UI
- Request/response examples
- Error code documentation

### 7.3 Deployment Documentation
- Docker setup instructions
- Environment variable configuration
- Database setup and migration
- Cloud deployment guide

---

## 8. Deliverables

### 8.1 Source Code
- Complete Spring Boot application
- Unit and integration tests
- Configuration files
- Database migration scripts

### 8.2 Build Artifacts
- Dockerfile
- docker-compose.yml
- Maven/Gradle build files
- CI/CD pipeline configuration (optional)

### 8.3 Documentation
- README.md (setup, run, test instructions)
- API documentation (Swagger UI)
- Architecture diagram
- Deployment guide

### 8.4 Dependencies List
- All required packages and versions
- Dependency management file (pom.xml or build.gradle)

---

## 9. Key Observations & Considerations

### 9.1 Portfolio VaR Diversification Effect
**Observation:** Portfolio VaR is typically less than the sum of individual trade VaRs due to diversification benefits. This occurs because:
- Trades may have negative correlations
- Losses in one trade may be offset by gains in another
- Historical simulation captures these correlation effects naturally

**Implementation Note:** The portfolio VaR calculation should aggregate historical P&L across all trades for each time period, then calculate VaR on the aggregated series.

### 9.2 Historical Simulation Method
- Assumes past patterns repeat in the future
- No distribution assumptions required
- Captures fat tails and skewness
- Requires sufficient historical data (typically 250+ observations)

### 9.3 Confidence Level Interpretation
- 95% confidence: 5% chance of exceeding VaR
- Higher confidence = higher VaR value (more conservative)
- Regulatory standards often require 99% confidence

---

## 10. Success Criteria

### 10.1 Functional Success
- ✓ Single trade VaR calculation works correctly
- ✓ Portfolio VaR calculation works correctly
- ✓ Configurable confidence levels
- ✓ Extensible design for future VaR methods
- ✓ Audit logging captures all requests
- ✓ Role-based access control enforced

### 10.2 Technical Success
- ✓ All unit tests pass (>80% coverage)
- ✓ Integration tests pass
- ✓ API documentation accessible
- ✓ Application runs in Docker container
- ✓ Health checks respond correctly
- ✓ Can deploy to cloud platform

### 10.3 Quality Success
- ✓ Code follows clean code principles
- ✓ No critical security vulnerabilities
- ✓ Performance requirements met
- ✓ Complete documentation provided
- ✓ Easy to set up and run locally

---

## 11. Out of Scope

- Real-time market data integration
- Variance-Covariance VaR calculation (future enhancement)
- Monte Carlo VaR calculation (future enhancement)
- Front-end UI (API only)
- Advanced portfolio optimization
- Stress testing or scenario analysis
- Backtesting framework

---

## 12. Assumptions

- Historical P&L data is provided by the client (not calculated)
- P&L values are in the same currency
- Historical data is clean and validated
- Users are pre-registered in the system
- Authentication mechanism is JWT or similar
- Single currency calculations (no FX conversion)

---

## 13. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Insufficient historical data | Inaccurate VaR | Validate minimum data points (250+) |
| Performance with large portfolios | Slow response | Implement caching, async processing |
| Database bottleneck on audit | System slowdown | Async audit logging, database indexing |
| Security vulnerabilities | Data breach | Security testing, dependency scanning |
| Cloud deployment complexity | Delayed launch | Comprehensive deployment documentation |

---

## 14. Future Enhancements

- Additional VaR calculation methods (Variance-Covariance, Monte Carlo)
- Conditional VaR (CVaR/Expected Shortfall)
- Incremental VaR and Component VaR
- Backtesting framework
- Real-time market data integration
- Multi-currency support
- Stress testing scenarios
- Web-based dashboard UI
- Batch processing for large portfolios
- Machine learning-based VaR predictions

---

## Appendix A: VaR Calculation Algorithm (Historical Simulation)

### Single Trade VaR
1. Sort historical P&L values in ascending order
2. Determine percentile position: (1 - confidence level) * n
3. VaR = absolute value of P&L at that percentile
4. If percentile falls between values, interpolate

### Portfolio VaR
1. For each time period, sum P&L across all trades
2. Create aggregated portfolio P&L series
3. Apply single trade VaR algorithm to aggregated series

### Example Calculation
```
Historical P&L: [-10, -5, -2, 0, 3, 5, 8, 10, 12, 15]
Confidence Level: 95%
n = 10
Percentile position = (1 - 0.95) * 10 = 0.5 (5th percentile)
Sorted: [-10, -5, -2, 0, 3, 5, 8, 10, 12, 15]
VaR (95%) = |-5| = 5 (interpolated between -10 and -5)
```

---

**Document Status:** Final  
**Approval Required:** Technical Lead, Product Owner  
**Next Steps:** Architecture design, sprint planning
