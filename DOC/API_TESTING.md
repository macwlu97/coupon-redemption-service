### `API_TESTING.md`

# 🧪 API Testing Guide

This guide provides a collection of `cURL` commands to verify the **Coupon Redemption System**. All requests are directed to the specific microservices ports as tested in the development environment.

> **Note:** For Geo-location features, we use the `X-Forwarded-For` header to simulate different countries.

---

## 1. Coupon Management (`coupon-service` - Port 8082)

### Create a New Coupon
```bash
curl -X POST 'http://localhost:8082/api/v1/coupons' \
  -H 'Content-Type: application/json' \
  -d '{
    "code": "SUMMER2026",
    "usageLimit": 2,
    "targetCountry": "PL"
  }'
```

---

## 2. Redemption Flow (`usage-service` - Port 8081)

### ✅ Scenario: Successful Redemption
User from **Poland** redeems a valid coupon.
```bash
curl -X POST 'http://localhost:8081/api/v1/usages/SUMMER2026/redeem' \
  -H 'accept: */*' \
  -H 'X-Forwarded-For: 89.64.12.150' \
  -d ''
```

### ❌ Scenario: Wrong Country
User from **Germany** tries to use a Polish coupon.
```bash
curl -X POST 'http://localhost:8081/api/v1/usages/SUMMER2026/redeem' \
  -H 'X-Forwarded-For: 5.147.160.1' \
  -d ''
```
*Expected Status: `422 Unprocessable Entity`*

### ❌ Scenario: Limit Exceeded
Attempting a 3rd redemption on a coupon with `usageLimit: 2`.
```bash
# First, fulfill the 2nd slot
curl -X POST 'http://localhost:8081/api/v1/usages/SUMMER2026/redeem' -H 'X-Forwarded-For: 89.64.12.150' -d ''

# Now, try the 3rd attempt
curl -X POST 'http://localhost:8081/api/v1/usages/SUMMER2026/redeem' -H 'X-Forwarded-For: 89.64.12.150' -d ''
```
*Expected Status: `409 Conflict` or `422`*

---

## 3. History & Audit

### Get User Redemption History
```bash
curl -X GET 'http://localhost:8081/api/v1/usages/history/user_123'
```

---

## ⚡ Error Code Reference

| Code | HTTP Status | Meaning |
| :--- | :--- | :--- |
| `COUPON_NOT_FOUND` | 404 | The provided code does not exist. |
| `LIMIT_EXCEEDED` | 409/422 | The coupon has reached its maximum usage. |
| `INVALID_COUNTRY` | 422 | User IP does not match coupon's target country. |
| `TRY_LATER` | 422 | GeoIP provider is down (Circuit Breaker is OPEN). |

---
