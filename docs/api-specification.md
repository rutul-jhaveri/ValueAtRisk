# VaR Calculation Service API

## Overview

The VaR Calculation Service provides REST APIs for calculating Value at Risk using Historical Simulation. All endpoints except authentication require JWT bearer tokens.

Base URL: http://localhost:9001/api/v1
Content-Type: application/json
Authentication: JWT Bearer Token

## Authentication

### User Login

POST /auth/login

Authenticate user and receive JWT token. No authentication required.

Request:
```json
{
  "username": "string",
  "password": "string"
}
```

Response (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "username": "user",
  "role": "USER"
}
```

Error Responses:
- 401 Unauthorized: Invalid credentials
- 400 Bad Request: Missing username/password

Default Users:
- Username: user, Password: user123, Role: USER
- Username: admin, Password: admin123, Role: ADMIN

## VaR Calculations

### Calculate Trade VaR

POST /var/trade

Calculate VaR for a single trade using historical P&L data. Requires USER or ADMIN role.

Headers:
```
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

Request:
```json
{
  "tradeId": "TRADE-001",
  "historicalPnL": [-1500.0, 2300.0, -800.0, 1200.0, -2100.0, 900.0, -600.0],
  "confidenceLevel": 0.95
}
```

Validation:
- tradeId: Required, non-blank string
- historicalPnL: Required, minimum 5 data points
- confidenceLevel: Required, between 0.0 and 1.0

Response (200 OK):
```json
{
  "id": "TRADE-001",
  "var": 1875.0,
  "confidenceLevel": 0.95,
  "calculationMethod": "HISTORICAL_SIMULATION",
  "tradeCount": 1,
  "timestamp": "2026-02-05T10:30:45.123"
}
```

Error Responses:
- 400 Bad Request: Invalid input parameters
- 401 Unauthorized: Missing or invalid JWT token
- 422 Unprocessable Entity: Insufficient data points

### Calculate Portfolio VaR

POST /var/portfolio

Calculate VaR for a portfolio containing multiple trades. Requires USER or ADMIN role.

Headers:
```
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

Request:
```json
{
  "portfolioId": "PORTFOLIO-001",
  "confidenceLevel": 0.99,
  "trades": [
    {
      "tradeId": "TRADE-001",
      "historicalPnL": [-1500.0, 2300.0, -800.0, 1200.0, -2100.0]
    },
    {
      "tradeId": "TRADE-002", 
      "historicalPnL": [800.0, -1200.0, 1500.0, -900.0, 600.0]
    }
  ]
}
```

Validation:
- portfolioId: Required, non-blank string
- confidenceLevel: Required, between 0.0 and 1.0
- trades: Required, minimum 1 trade
- All trades must have same number of data points
- Each trade must meet minimum data points requirement

Response (200 OK):
```json
{
  "id": "PORTFOLIO-001",
  "var": 2156.7,
  "confidenceLevel": 0.99,
  "calculationMethod": "HISTORICAL_SIMULATION",
  "tradeCount": 2,
  "timestamp": "2026-02-05T10:35:22.456"
}
```

Error Responses:
- 400 Bad Request: Invalid input parameters
- 401 Unauthorized: Missing or invalid JWT token
- 422 Unprocessable Entity: Mismatched data points across trades

## Audit

### Get Audit Records

GET /audit

Retrieve audit records for all calculation requests. Requires ADMIN role only.

Headers:
```
Authorization: Bearer <jwt-token>
```

Query Parameters:
- page: Page number (default: 0)
- size: Page size (default: 20)
- username: Filter by username (optional)
- success: Filter by success status (optional)

Response (200 OK):
```json
{
  "content": [
    {
      "id": 1,
      "username": "user",
      "endpoint": "/api/v1/var/trade",
      "executionTime": 45,
      "success": true,
      "errorMessage": null,
      "timestamp": "2026-02-05T10:30:45.123"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 1,
  "totalPages": 1
}
```

Error Responses:
- 401 Unauthorized: Missing or invalid JWT token
- 403 Forbidden: Insufficient privileges (USER role)

## Health and Monitoring

### Health Check

GET /actuator/health

Application health status. No authentication required.

Response (200 OK):
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 91943821312,
        "threshold": 10485760,
        "path": "C:\\workspace\\."
      }
    }
  }
}
```

### Application Info

GET /actuator/info

Application metadata. No authentication required.

Response (200 OK):
```json
{
  "app": {
    "name": "VaR Calculation Service",
    "version": "1.0.0",
    "java": {
      "version": "21.0.1",
      "vendor": "Eclipse Adoptium"
    }
  }
}
```

## Error Response Format

All error responses follow consistent format:

```json
{
  "timestamp": "2026-02-05T10:30:45.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Confidence level must be between 0 and 1",
  "path": "/api/v1/var/trade"
}
```

Common HTTP Status Codes:
- 200 OK: Successful operation
- 400 Bad Request: Invalid request parameters
- 401 Unauthorized: Authentication required
- 403 Forbidden: Insufficient permissions
- 404 Not Found: Resource not found
- 422 Unprocessable Entity: Business logic validation failed
- 500 Internal Server Error: Unexpected server error

## Caching

Cache Strategy:
- Trade VaR results cached by trade ID and confidence level
- Portfolio VaR results cached by portfolio ID and confidence level
- Cache TTL: 1 hour
- Cache invalidation: LRU eviction

Caching is transparent to clients with no explicit cache headers in responses.

## API Documentation

Interactive Documentation: http://localhost:9001/swagger-ui.html
OpenAPI Spec: http://localhost:9001/api-docs

The Swagger UI provides interactive API testing, request/response examples, schema documentation, and authentication testing.

## Sample Requests

### Complete Trade VaR Flow

```bash
# Login
curl -X POST http://localhost:9001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "user123"}'

# Calculate Trade VaR
curl -X POST http://localhost:9001/api/v1/var/trade \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "tradeId": "TRADE-001",
    "historicalPnL": [-1500, 2300, -800, 1200, -2100, 900, -600],
    "confidenceLevel": 0.95
  }'
```

### Complete Portfolio VaR Flow

```bash
# Calculate Portfolio VaR
curl -X POST http://localhost:9001/api/v1/var/portfolio \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "portfolioId": "PORTFOLIO-001",
    "confidenceLevel": 0.99,
    "trades": [
      {
        "tradeId": "TRADE-001",
        "historicalPnL": [-1500, 2300, -800, 1200, -2100]
      },
      {
        "tradeId": "TRADE-002",
        "historicalPnL": [800, -1200, 1500, -900, 600]
      }
    ]
  }'
```

## Testing Data

Sample Excel files are available for testing:
- http://localhost:9001/Sample_Single_Trade_Year.xls
- http://localhost:9001/Sample_Portfolio_Year.xls
- http://localhost:9001/Sample_Vectorized_Portfolio.xls

These files contain sample historical P&L data for testing API endpoints.