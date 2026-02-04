# VaR Calculation Service

This service calculates Value at Risk (VaR) using the Historical Simulation method. It estimates potential financial losses for single trades or portfolios at specific confidence levels (e.g., 95% or 99%).

## Core Features
- **Methodology:** Uses actual historical P&L data (Historical Simulation).
- **Scope:** Supports both single trades and aggregated portfolios with diversification.
- **Security:** JWT-based authentication with User and Admin roles.
- **Performance:** Uses Java 21 Virtual Threads and in-memory caching (Caffeine).
- **Audit:** Logs all calculation requests for compliance.

## Tech Stack
- Java 21
- Spring Boot 3.2.2
- H2 Database (In-memory for dev/test)
- Docker

## How to Run
You need Java 21 and Maven installed.

1. **Build the project:**
   ```powershell
   .\mvnw.cmd clean install

2. **Start the server:**
    ```PowerShell
    mvn spring-boot:run
    
   The application will start on http://localhost:9001.

**API Usage** : All endpoints require a JWT token.

1. Authentication
   Endpoint: POST /api/v1/auth/login

  Default Users: user / admin with pwd appending 123

  Response: Returns a Bearer token.

2. Calculate Trade VaR
   Endpoint: POST /api/v1/var/trade

Input: Trade ID, array of historical P&L numbers, and confidence level (e.g., 0.95).

3. Calculate Portfolio VaR
   Endpoint: POST /api/v1/var/portfolio

Input: List of trades with their individual P&L data.

4. Documentation
   View the full interactive API at: http://localhost:9001/swagger-ui.html

Project Layout
Controller: src/main/java/.../controller (API endpoints)

Logic: src/main/java/.../service (Orchestration)

Math: src/main/java/.../strategy (The actual VaR algorithms)

Tests: Run .\mvnw.cmd test to execute the JUnit test suite.