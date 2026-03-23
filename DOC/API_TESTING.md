### `API_TESTING.md`

# 🧪 API Testing Guide

This guide provides a collection of `cURL` commands to verify the **Coupon Redemption System**. All requests are routed through the **API Gateway (Port 8080)**.

> **Note:** For Geo-location features, we use the `X-Forwarded-For` header to simulate different countries, as local Docker/localhost IPs cannot be resolved by public GeoIP providers.

---

## 1. Coupon Management (`coupon-service`)

### Create a New Coupon
Creates a coupon with a specific usage limit and target country.
```bash
curl -X POST http://localhost:8080/coupon-service/api/v1/coupons \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SUMMER2026",
    "usageLimit": 2,
    "targetCountry": "PL"
  }'
```

### Get Coupon Details
Check the current state, including total limit and remaining uses.
```bash
curl -X GET http://localhost:8080/coupon-service/api/v1/coupons/SUMMER2026
```

---

## 2. Redemption Flow (`usage-service`)

### ✅ Scenario: Successful Redemption
User from **Poland** redeems a valid coupon.
* **IP:** `89.64.12.150` (Poland)
* **User:** `user_001`
```bash
curl -X POST http://localhost:8080/usage-service/api/v1/usages/SUMMER2026/redeem \
  -H "User-Id: user_001" \
  -H "X-Forwarded-For: 89.64.12.150" \
  -H "Content-Type: application/json" -d '{}'
```

### ❌ Scenario: Wrong Country
User from **Germany** tries to use a Polish coupon.
* **IP:** `5.147.160.1` (Germany)
```bash
curl -X POST http://localhost:8080/usage-service/api/v1/usages/SUMMER2026/redeem \
  -H "User-Id: user_002" \
  -H "X-Forwarded-For: 5.147.160.1" \
  -H "Content-Type: application/json" -d '{}'
```
*Expected: `422 Unprocessable Entity` (Invalid Country)*

### ❌ Scenario: Duplicate User (Anti-Fraud)
The **same user** tries to redeem the **same coupon** again.
```bash
curl -X POST http://localhost:8080/usage-service/api/v1/usages/SUMMER2026/redeem \
  -H "User-Id: user_001" \
  -H "X-Forwarded-For: 89.64.12.150" \
  -H "Content-Type: application/json" -d '{}'
```
*Expected: `409 Conflict` (User already used this coupon)*

### ❌ Scenario: Limit Exceeded
Attempting a 3rd redemption on a coupon with `usageLimit: 2`.
```bash
# First, fulfill the 2nd slot with a new user
curl -X POST http://localhost:8080/usage-service/api/v1/usages/SUMMER2026/redeem \
  -H "User-Id: user_002" -H "X-Forwarded-For: 89.64.12.150" -d '{}'

# Now, try the 3rd attempt
curl -X POST http://localhost:8080/usage-service/api/v1/usages/SUMMER2026/redeem \
  -H "User-Id: user_003" -H "X-Forwarded-For: 89.64.12.150" -d '{}'
```
*Expected: `409 Conflict` or `422` (Limit reached)*

---

## 3. History & Audit

### Get User Redemption History
Retrieve all coupons used by a specific user.
```bash
curl -X GET http://localhost:8080/usage-service/api/v1/usages/history/user_001
```

---

## ⚡ Error Code Reference

The system uses standard HTTP status codes and custom business error codes:

| Code | HTTP Status | Meaning |
| :--- | :--- | :--- |
| `COUPON_NOT_FOUND` | 404 | The provided code does not exist. |
| `LIMIT_EXCEEDED` | 409/422 | The coupon has reached its maximum usage. |
| `INVALID_COUNTRY` | 422 | User IP does not match coupon's target country. |
| `ALREADY_REDEEMED` | 409 | This specific user has already used this coupon. |
| `TRY_LATER` | 422 | GeoIP provider is down (Circuit Breaker is OPEN). |

---