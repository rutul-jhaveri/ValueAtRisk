# VaR Calculation Service - Requirements Document

## 1. Overview

The VaR (Value at Risk) Calculation Service is a financial risk management system that estimates potential losses in trading positions using historical simulation methodology. The service provides RESTful APIs for calculating VaR at specified confidence levels for both individual trades and diversified portfolios.

## 2. Business Objectives

- Provide accurate VaR calculations using historical P&L data
- Support risk assessment for single trades and multi-trade portfolios
- Enable secure, role-based access to calculation services
- Maintain audit trails for regulatory compliance
- Deliver high-performance calculations using modern Java capabilities

## 3. Functional Requirements

### 3.1 VaR Calculation

**FR-1: Single Trade VaR Calculation**
- System shall calculate VaR for individual trades using historical P&L data
- System shall accept confidence levels between 0 and 1 (e.g., 0.95 for 95%)
- System shall require minimum configurable data points (default: 5)
- System shall use Historical Simulation methodology with linear interpolation
- System shall return absolute VaR value with calculation metadata

**FR-2: Portfolio VaR Calculation**
- System shall calculate aggregated VaR for portfolios containing multiple trades
- System shall aggregate P&L across all trades for each historical period
- System shall validate that all trades have identical number of data points
- System shall support natural diversification effects in portfolio calculations
- System shall require at least one trade in portfolio

**FR-3: Calculation Methodology**
- System shall implement Historical Simulation strategy
- System shall sort historical P&L data in ascending order
- System shall calculate percentile position: (1 - confidence level) × (n - 1)
- System shall use linear interpolation for non-integer percentile positions
- System shall return VaR as absolute value

### 3.2 Authentication & Authorization

**FR-4: User Authentication**
- System shall authenticate users via JWT tokens
- System shall support username/password login
- System shall issue tokens with configurable expiration (default: 24 hours)
- System shall validate tokens on all protected endpoints

**FR-5: Role-Based Access Control**
- System shall support USER and ADMIN roles
- System shall restrict VaR calculation endpoints to authenticated users
- System shall restrict audit endpoints to ADMIN role only
- System shall initialize default users (user/user123, admin/admin123)

### 3.3 Audit & Compliance

**FR-6: Audit Logging**
- System shall log all VaR calculation requests
- System shall capture: username, endpoint, execution time, success/failure status
- System shall store error messages for failed calculations
- System shall persist audit records to database
- System shall provide audit query endpoints for administrators

### 3.4 Performance & Caching

**FR-7: Caching Strategy**
- System shall cache trade VaR results by tradeId and confidence level
- System shall cache portfolio VaR results by portfolioId and confidence level
- System shall use in-memory caching (Caffeine)
- System shall configure cache TTL and size limits

**FR-8: Concurrent Processing**
- System shall leverage Java 21 Virtual Threads for request handling
- System shall support configurable thread pool sizing
- System shall handle concurrent calculation requests efficiently

### 3.5 API & Documentation

**FR-9: RESTful API**
- System shall expose REST endpoints under /api/v1 namespace
- System shall validate all request payloads
- System shall return standardized error responses
- System shall support JSON request/response format

**FR-10: API Documentation**
- System shall provide OpenAPI/Swagger documentation
- System shall expose interactive API UI at /swagger-ui.html
- System shall document all endpoints, parameters, and response schemas

## 4. Non-Functional Requirements

### 4.1 Performance
- **NFR-1:** VaR calculations shall complete within 500ms for datasets up to 1000 points
- **NFR-2:** System shall support minimum 100 concurrent users
- **NFR-3:** Cache hit ratio shall exceed 70% for repeated calculations

### 4.2 Security
- **NFR-4:** All API endpoints shall require authentication except /auth/login
- **NFR-5:** JWT tokens shall use secure signing algorithm (HS512)
- **NFR-6:** Passwords shall be stored using BCrypt hashing
- **NFR-7:** System shall prevent SQL injection and XSS attacks

### 4.3 Reliability
- **NFR-8:** System shall validate all input data before processing
- **NFR-9:** System shall handle calculation errors gracefully
- **NFR-10:** System shall maintain 99.5% uptime during business hours

### 4.4 Maintainability
- **NFR-11:** Code shall follow Java 21 best practices (records, virtual threads)
- **NFR-12:** System shall use dependency injection for loose coupling
- **NFR-13:** System shall include comprehensive unit and integration tests
- **NFR-14:** System shall log debug information for troubleshooting

### 4.5 Scalability
- **NFR-15:** System shall support horizontal scaling via stateless design
- **NFR-16:** System shall externalize configuration for environment-specific settings
- **NFR-17:** Database shall support migration from H2 to production RDBMS

## 5. Data Requirements

### 5.1 Input Data
- Historical P&L data: List of double values representing profit/loss
- Confidence level: Double between 0.0 and 1.0 (exclusive)
- Trade identifiers: Non-blank strings
- Portfolio identifiers: Non-blank strings

### 5.2 Output Data
- VaR value: Absolute double value
- Calculation metadata: method, timestamp, trade count
- Confidence level: Echo of input parameter

### 5.3 Audit Data
- Request timestamp
- Username
- Endpoint path
- Execution time (milliseconds)
- Success/failure status
- Error messages (if applicable)

## 6. Integration Requirements

**IR-1:** System shall expose health check endpoint for monitoring
**IR-2:** System shall support H2 console for development/testing
**IR-3:** System shall provide actuator endpoints for operational metrics
**IR-4:** System shall support Docker containerization

## 7. Constraints & Assumptions

### 7.1 Constraints
- Java 21 runtime required
- Spring Boot 3.2.2 framework
- In-memory H2 database for development
- Historical Simulation methodology only

### 7.2 Assumptions
- Historical P&L data is pre-calculated and provided by client
- All P&L values are in same currency
- Historical periods are equally spaced (e.g., daily)
- Users have valid credentials provisioned

## 8. Future Enhancements
- Support for additional VaR methodologies
