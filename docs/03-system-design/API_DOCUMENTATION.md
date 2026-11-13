# REST API Documentation — Throttlix

## 1. Overview & Conventions
This document details the RESTful HTTP API contracts exposed by the **Throttlix Platform**.

- **Base URL:** `http://localhost:8080/throttlix-backend/api/v1`
- **Data Format:** JSON (`Content-Type: application/json; charset=UTF-8`)
- **Authentication Header:** `X-API-Key: <plaintext_secret>` (required on all protected gateway endpoints)

---

## 2. Standard HTTP Status Codes

| Code | Status Text | Usage Condition |
| :---: | :--- | :--- |
| `200` | `OK` | Request succeeded; returned with requested data or forwarded backend response. |
| `201` | `Created` | Resource successfully created (Client, Key, Policy). |
| `400` | `Bad Request` | Request validation failure (malformed JSON, invalid query parameters). |
| `401` | `Unauthorized` | Missing or unrecognized `X-API-Key` header. |
| `403` | `Forbidden` | Valid API key provided, but account or key is deactivated/revoked. |
| `404` | `Not Found` | Target resource or route does not exist. |
| `429` | `Too Many Requests` | Request rate limit exceeded; client must wait until quota refills. |
| `500` | `Internal Server Error` | Unexpected server or database exception; generic error message returned. |

---

## 3. Rate-Limit Telemetry Headers
Every response from a Throttlix-managed endpoint includes these headers:

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 84
X-RateLimit-Reset: 1735689600
```

When quota is exhausted (`429 Too Many Requests`):
```http
HTTP/1.1 429 Too Many Requests
Content-Type: application/json
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
Retry-After: 12
X-RateLimit-Reset: 1735689612

{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Please retry after 12 seconds.",
  "retryAfter": 12
}
```

---

## 4. Endpoint Specifications

### 4.1 Client Management Endpoints

#### `POST /clients`
Creates a new client organization.

- **Request Headers:** `Content-Type: application/json`
- **Request Body:**
  ```json
  {
    "name": "Acme Corp"
  }
  ```
- **Response `201 Created`:**
  ```json
  {
    "clientId": 1,
    "name": "Acme Corp",
    "active": true,
    "createdAt": "2026-11-06T12:00:00Z"
  }
  ```

---

#### `POST /clients/{id}/keys`
Generates a new API key for the specified client. Plaintext secret is returned **only once**.

- **Parameters:** `id` (path, required, integer) — Client ID.
- **Request Body:**
  ```json
  {
    "name": "Production Key"
  }
  ```
- **Response `201 Created`:**
  ```json
  {
    "apiKeyId": 1,
    "clientId": 1,
    "name": "Production Key",
    "apiKey": "tk_live_9a8b7c6d5e4f3a2b1c0d9e8f7a6b5c4d",
    "createdAt": "2026-11-06T12:05:00Z",
    "warning": "Store this key securely. It will not be shown again."
  }
  ```

---

#### `PATCH /keys/{id}/deactivate`
Deactivates an active API key immediately.

- **Parameters:** `id` (path, required, integer) — API Key ID.
- **Response `200 OK`:**
  ```json
  {
    "apiKeyId": 1,
    "active": false,
    "revokedAt": "2026-11-06T12:30:00Z",
    "message": "API key successfully revoked"
  }
  ```

---

### 4.2 Policy Management Endpoints

#### `POST /policies`
Creates a rate-limiting policy.

- **Request Body:**
  ```json
  {
    "name": "Standard Tier",
    "algorithm": "TOKEN_BUCKET",
    "requestLimit": 100,
    "windowSeconds": 60,
    "enabled": true
  }
  ```
- **Response `201 Created`:**
  ```json
  {
    "policyId": 1,
    "name": "Standard Tier",
    "algorithm": "TOKEN_BUCKET",
    "requestLimit": 100,
    "windowSeconds": 60,
    "enabled": true,
    "createdAt": "2026-11-06T12:10:00Z"
  }
  ```

---

### 4.3 Protected Gateway Demo Endpoint

#### `GET /demo/orders`
Representative protected resource demonstrating rate limiting and gateway forwarding.

- **Request Headers:**
  - `X-API-Key: tk_live_...` (required)
- **Response `200 OK` (Allowed):**
  ```json
  {
    "message": "Request allowed by Throttlix Gateway",
    "status": "SUCCESS",
    "data": {
      "orders": [
        {"orderId": 101, "item": "Widget A", "quantity": 5}
      ]
    }
  }
  ```
- **Response `429 Too Many Requests` (Quota Exceeded):**
  ```json
  {
    "error": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded. Try again after 1 second.",
    "retryAfter": 1
  }
  ```

---

### 4.4 Observability & Analytics Endpoints

#### `GET /analytics/summary`
Returns aggregate traffic statistics for the dashboard.

- **Response `200 OK`:**
  ```json
  {
    "totalRequests": 14250,
    "allowedRequests": 13800,
    "rejectedRequests": 450,
    "activeClients": 12,
    "averageLatencyMs": 3.8,
    "timestamp": "2026-11-06T14:00:00Z"
  }
  ```
