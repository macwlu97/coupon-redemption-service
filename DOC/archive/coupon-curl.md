### `API_TESTING.md`

# 🧪 API Testing Guide (Recruitment Task Validation)

This guide contains a series of `curl` commands to verify that all business requirements of the recruitment task have been met.

---

## 1. Coupon Management (`coupon-service` - Port 8082)

### 📋 List All Coupons
Verify the initial state of the database and initial coupons.
```bash
curl -X GET http://localhost:8082/api/v1/coupons \
     -H "Accept: application/json"
```

### ✨ Create a New Coupon (Requirement: Uniqueness & Country)
Create a coupon for the Polish market with a limit of 2 uses.
```bash
curl -X POST http://localhost:8082/api/v1/coupons \
     -H "Content-Type: application/json" \
     -d '{
       "code": "SUMMER2026",
       "usageLimit": 2,
       "targetCountry": "PL"
     }'
```

---

## 2. Redemption Flow (`usage-service` - Port 8081)

### ✅ Success: Valid Redemption (Requirement: Geographic Validation)
Redeem `WIOSNA2026` using a Polish IP address.
```bash
curl -i -X POST "http://localhost:8081/api/v1/usages/SUMMER2026/redeem" \
     -H "X-Forwarded-For: 89.64.12.150" \
     -d ''
```
*Expected: `200 OK` or `201 Created`. Current usage counter should increment.*

### ❌ Failure: Case Insensitivity Test (Requirement: Unique Codes)
Trying to create `summer2026` should fail if `SUMMER2026` exists (business logic validation).
```bash
curl -i -X POST http://localhost:8082/api/v1/coupons \
     -H "Content-Type: application/json" \
     -d '{"code": "summer2026", "usageLimit": 10, "targetCountry": "PL"}'
```
*Expected: `409 Conflict` (Codes are case-insensitive).*

### ❌ Failure: Wrong Country (Requirement: IP-based Restriction)
Attempt to use the Polish coupon from a German IP.
```bash
curl -i -X POST "http://localhost:8081/api/v1/usages/SUMMER2026/redeem" \
     -H "X-Forwarded-For: 5.147.160.1" \
     -d ''
```
*Expected: `422 Unprocessable Entity` (Invalid Country).*

### ❌ Failure: Duplicate User (Requirement: One Use Per User)
The same user tries to redeem the same coupon again.
```bash
curl -i -X POST "http://localhost:8081/api/v1/usages/SUMMER2026/redeem" \
     -H "X-Forwarded-For: 89.64.12.150" \
     -d ''
```
*Expected: `409 Conflict` (User has already used this coupon).*

### ❌ Failure: Limit Exceeded (Requirement: Usage Limit)
Redeem the second slot, then try a third time.
```bash
# Fulfill 2nd slot
curl -X POST "http://localhost:8081/api/v1/usages/SUMMER2026/redeem" \
     -H "X-Forwarded-For: 89.64.12.150" -H "User-Id: user_002" -d ''

# Try 3rd time
curl -i -X POST "http://localhost:8081/api/v1/usages/SUMMER2026/redeem" \
     -H "X-Forwarded-For: 89.64.12.150" -H "User-Id: user_003" -d ''
```
*Expected: `409 Conflict` or `422` (Usage limit reached).*

### ❌ Failure: Non-Existent Coupon (Requirement: Existence Check)
```bash
curl -i -X POST "http://localhost:8081/api/v1/usages/BRAK-KUPONU/redeem" \
     -H "X-Forwarded-For: 89.64.12.150" -d ''
```
*Expected: `404 Not Found`.*


---

## ⚡ Error Summary

| Requirement | Scenario | Expected Status |
| :--- | :--- | :--- |
| **Uniqueness** | Existing code (case-insensitive) | `409 Conflict` |
| **Geo-fencing** | IP from unauthorized country | `422 Unprocessable Entity` |
| **Usage Limit** | Max uses reached | `409 Conflict / 422` |
| **Anti-Fraud** | User redeems same coupon twice | `409 Conflict` |
| **Data Integrity** | Coupon code does not exist | `404 Not Found` |

---
