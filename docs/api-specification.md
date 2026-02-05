# VaR Calculation Service - API Specification

## 1. API Overview

The VaR Calculation Service provides RESTful APIs for calculating Value at Risk using Historical Simulation methodology. All endpoints except authentication require JWT bearer token authorization.

**Base URL:** `http://localhost:9001/api/v1`  
**Content-Type:** `application/json`  
**Authentication:** JWT Bearer Token  

## 2. Authentication Endpoints

### 2.1 User Login

**Endpoint:** `POST /auth/login`  
**Description:** Authenticate user and receive JWT token  
**Authentication:** None required  

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "type": "Bearer",
  "username": "user",
  "role": "USER"
}
```

**Error Responses:**
- `401 Unauthorized`: Invalid credentials
- `400 Bad Request`: Missing username/password

**Default Users:**
- Username: `user`, Password: `user123`, Role: `USER`
- Username: `admin`, Password: `admin123`, Role: `ADMIN`

## 3. VaR Calculation Endpoints

### 3.1 Calculate Trade VaR

**Endpoint:** `POST /var/trade`  
**Description:** Calculate VaR for a single trade using historical P&L data  
**Authentication:** Required (USER or ADMIN)  

**Request Headers:**
```
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "tradeId": "TRADE-001",
  "historicalPnL": [-1500.0, 2300.0, -800.0, 1200.0, -2100.0, 900.0, -600.0],
  "confidenceLevel": 0.95
}
```

**Request Validation:**
- `tradeId`: Required, non-blank string
- `historicalPnL`: Required, minimum 5 data points (configurable)
- `confidenceLevel`: Required, between 0.0 and 1.0 (exclusive)

**Response (200 OK):**
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

**Error Responses:**
- `400 Bad Request`: Invalid input parameters
- `401 Unauthorized`: Missing or invalid JWT token
- `422 Unprocessable Entity`: Insufficient data points

### 3.2 Calculate Portfolio VaR

**Endpoint:** `POST /var/portfolio`  
**Description:** Calculate VaR for a portfolio containing multiple trades  
**Authentication:** Required (USER or ADMIN)  

**Request Headers:**
```
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

**Request Body:**
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

**Request Validation:**
- `portfolioId`: Required, non-blank string
- `confidenceLevel`: Required, between 0.0 and 1.0 (exclusive)
- `trades`: Required, minimum 1 trade
- All trades must have same number of historical data points
- Each trade's `historicalPnL` must meet minimum data points requirement

**Response (200 OK):**
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

**Error Responses:**
- `400 Bad Request`: Invalid input parameters
- `401 Unauthorized`: Missing or invalid JWT token
- `422 Unprocessable Entity`: Mismatched data points across trades

## 4. Audit Endpoints

### 4.1 Get Audit Records

**Endpoint:** `GET /audit`  
**Description:** Retrieve audit records for all calculation requests  
**Authentication:** Required (ADMIN only)  

**Request Headers:**
```
Authorization: Bearer <jwt-token>
```

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `username`: Filter by username (optional)
- `success`: Filter by success status (optional)

**Response (200 OK):**
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

**Error Responses:**
- `401 Unauthorized`: Missing or invalid JWT token
- `403 Forbidden`: Insufficient privileges (USER role)

## 5. Health & Monitoring Endpoints

### 5.1 Health Check

**Endpoint:** `GET /actuator/health`  
**Description:** Application health status  
**Authentication:** None required  

**Response (200 OK):**
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

### 5.2 Application Info

**Endpoint:** `GET /actuator/info`  
**Description:** Application metadata  
**Authentication:** None required  

**Response (200 OK):**
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

## 6. Error Response Format

All error responses follow a consistent format:

```json
{
  "timestamp": "2026-02-05T10:30:45.123",
  "status": 400,
  "error": "Bad Request",
  "message": "Confidence level must be between 0 and 1",
  "path": "/api/v1/var/trade"
}
```

**Common HTTP Status Codes:**
- `200 OK`: Successful operation
- `400 Bad Request`: Invalid request parameters
- `401 Unauthorized`: Authentication required
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `422 Unprocessable Entity`: Business logic validation failed
- `500 Internal Server Error`: Unexpected server error

## 7. Rate Limiting

Currently no rate limiting is implemented. Future versions may include:
- Per-user request limits
- IP-based throttling
- Calculation complexity limits

## 8. Caching Behavior

**Cache Strategy:**
- Trade VaR results cached by `{tradeId}_{confidenceLevel}`
- Portfolio VaR results cached by `{portfolioId}_{confidenceLevel}`
- Cache TTL: 1 hour (configurable)
- Cache invalidation: LRU eviction

**Cache Headers:**
- No explicit cache headers in responses
- Caching is transparent to clients

## 9. API Versioning

**Current Version:** v1  
**Versioning Strategy:** URL path versioning (`/api/v1/`)  
**Backward Compatibility:** Maintained within major versions  

## 10. OpenAPI Documentation

**Interactive Documentation:** `http://localhost:9001/swagger-ui.html`  
**OpenAPI Spec:** `http://localhost:9001/api-docs`  

The Swagger UI provides:
- Interactive API testing
- Request/response examples
- Schema documentation
- Authentication testing

## 11. Sample Requests

### 11.1 Complete Trade VaR Flow

```bash
# 1. Login
curl -X POST http://localhost:9001/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "user", "password": "user123"}'

# Response: {"token": "eyJ...", "type": "Bearer", ...}

# 2. Calculate Trade VaR
curl -X POST http://localhost:9001/api/v1/var/trade \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJ..." \
  -d '{
    "tradeId": "TRADE-001",
    "historicalPnL": [-1500, 2300, -800, 1200, -2100, 900, -600],
    "confidenceLevel": 0.95
  }'
```

### 11.2 Complete Portfolio VaR Flow

```bash
# Calculate Portfolio VaR
curl -X POST http://localhost:9001/api/v1/var/portfolio \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJ..." \
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

## 12. Client SDK Considerations

For client applications, consider:
- **Token Management**: Automatic token refresh before expiration
- **Error Handling**: Retry logic for transient failures
- **Request Validation**: Client-side validation before API calls
- **Caching**: Client-side caching of calculation results
- **Logging**: Request/response logging for debugging

## 13. Testing Endpoints

For development and testing, sample Excel files are available:
- `http://localhost:9001/Sample_Single_Trade_Year.xls`
- `http://localhost:9001/Sample_Portfolio_Year.xls`
- `http://localhost:9001/Sample_Vectorized_Portfolio.xls`

These files contain sample historical P&L data that can be used for testing the API endpoints.