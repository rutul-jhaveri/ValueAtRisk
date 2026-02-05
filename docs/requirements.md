# VaR Calculation Service Requirements

## Overview

The VaR Calculation Service estimates potential financial losses in trading positions using historical simulation. It provides REST APIs for calculating Value at Risk at specified confidence levels for individual trades and portfolios.

## Business Goals

- Calculate accurate VaR using historical profit and loss data
- Support risk assessment for single trades and portfolios
- Provide secure access with user authentication
- Maintain audit trails for compliance
- Deliver fast calculations using modern Java features

## Functional Requirements

### VaR Calculations

The system must calculate VaR for individual trades using historical P&L data. It should accept confidence levels between 0 and 1 (like 0.95 for 95% confidence). The system needs at least 5 data points by default but this should be configurable. It will use Historical Simulation with linear interpolation and return the absolute VaR value.

For portfolios, the system must calculate aggregated VaR for multiple trades. It should combine P&L across all trades for each time period and validate that all trades have the same number of data points. The system must support natural diversification effects and require at least one trade per portfolio.

The calculation method will sort historical P&L data in ascending order, calculate percentile position as (1 - confidence level) × (n - 1), and use linear interpolation for non-integer positions.

### Authentication and Authorization

Users must authenticate with JWT tokens through username and password login. The system will issue tokens with 24-hour expiration by default. All protected endpoints require valid tokens.

The system supports USER and ADMIN roles. VaR calculation endpoints are available to authenticated users. Audit endpoints are restricted to ADMIN role only. Default users are user/user123 and admin/admin123.

### Audit and Compliance

All VaR calculation requests must be logged including username, endpoint, execution time, and success status. Error messages are stored for failed calculations. Audit records are persisted to database with query endpoints for administrators.

### Performance and Caching

Trade VaR results are cached by trade ID and confidence level. Portfolio VaR results are cached by portfolio ID and confidence level. The system uses in-memory caching with configurable TTL and size limits.

The application uses Java 21 Virtual Threads for request handling with configurable thread pool sizing to handle concurrent requests efficiently.

### API and Documentation

REST endpoints are exposed under /api/v1 namespace with JSON request/response format. All request payloads are validated with standardized error responses. OpenAPI/Swagger documentation is provided with interactive UI at /swagger-ui.html.

## Non-Functional Requirements

### Performance
- VaR calculations complete within 500ms for up to 1000 data points
- System supports minimum 100 concurrent users  
- Cache hit ratio exceeds 70% for repeated calculations

### Security
- All API endpoints require authentication except login
- JWT tokens use secure HS512 signing algorithm
- Passwords stored with BCrypt hashing
- System prevents SQL injection and XSS attacks

### Reliability
- Input data validation before processing
- Graceful error handling for calculation failures
- 99.5% uptime during business hours

### Maintainability
- Java 21 best practices with records and virtual threads
- Dependency injection for loose coupling
- Comprehensive unit and integration tests
- Debug logging for troubleshooting

### Scalability
- Stateless design supports horizontal scaling
- Externalized configuration for different environments
- Database supports migration from H2 to production RDBMS

## Data Requirements

Input data includes historical P&L as list of doubles, confidence level between 0.0 and 1.0, and non-blank trade/portfolio identifiers.

Output data includes absolute VaR value, calculation metadata with method and timestamp, confidence level echo, and trade count.

Audit data captures request timestamp, username, endpoint path, execution time, success status, and error messages when applicable.

## Integration Requirements

The system exposes health check endpoints for monitoring and supports H2 console for development. Actuator endpoints provide operational metrics and the system supports Docker containerization.

## Constraints

The system requires Java 21 runtime and Spring Boot 3.2.2 framework. It uses in-memory H2 database for development and supports only Historical Simulation methodology currently.

Historical P&L data is pre-calculated by clients. All values are assumed to be in same currency with equally spaced time periods. Users must have valid provisioned credentials.

## Future Enhancements

Potential additions include support for Monte Carlo and Parametric VaR methods, real-time market data integration, multi-currency support with FX conversion, and advanced portfolio analytics like CVaR and stress testing.

Other possibilities are batch calculation processing, external database integration with PostgreSQL or Oracle, distributed caching with Redis, and GraphQL API support.