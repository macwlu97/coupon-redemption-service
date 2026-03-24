# 🚀 Coupon System: Production Testing Scenarios

This document outlines the test suite for the Coupon Redemption System, executed through the **API Gateway (Port 8080)**.

> **Note on Architecture:** The system uses a microservice approach with an API Gateway, Service Discovery (Eureka), and a Resiliency layer (Resilience4j) to handle high-concurrency coupon redemptions.

---

## 1. Coupon Management (Administrative)

### 1.1 Create a New Coupon (Poland)
**Requirement:** Unique code (case-insensitive), usage limit, and country restriction.
```bash
curl -i -X POST "http://localhost:8080/api/v1/coupons" \
     -H "Content-Type: application/json" \
     -d '{
       "code": "SPRING2026",
       "usageLimit": 2,
       "targetCountry": "PL"
     }'
```
* **Expected:** `201 Created`.

### 1.2 Case-Insensitivity Test
**Requirement:** "SPRING2026" and "spring2026" must be treated as the same unique entity.
```bash
curl -i -X POST "http://localhost:8080/api/v1/coupons" \
     -H "Content-Type: application/json" \
     -d '{
       "code": "spring2026",
       "usageLimit": 10,
       "targetCountry": "PL"
     }'
```
* **Expected:** `409 Conflict` or `400 Bad Request` (Error: Coupon already exists).

---

## 2. Coupon Redemption (Business Logic)

### 2.1 Successful Redemption (Valid Country)
**Requirement:** Geofencing based on IP address. We use a Polish IP (`89.64.12.150`).
```bash
curl -i -X POST "http://localhost:8080/api/v1/usages/SPRING2026/redeem" \
     -H "X-Forwarded-For: 89.64.12.150" \
     -H "Content-Type: application/json" \
     -d '{}'
```
* **Expected:** `200 OK`.
* **Internal Logic:** The `GeoIpService` sanitizes the `X-Forwarded-For` header and validates the country via 3rd party APIs.

### 2.2 Rejection: Invalid Country (Geofencing)
**Requirement:** Usage restricted by country. Attempting from a US IP (`8.8.8.8`).
```bash
curl -i -X POST "http://localhost:8080/api/v1/usages/SPRING2026/redeem" \
     -H "X-Forwarded-For: 8.8.8.8" \
     -H "Content-Type: application/json" \
     -d '{}'
```
* **Expected:** `422 Unprocessable Entity` (Error: Invalid country).

### 2.3 Rejection: Usage Limit Reached (First-Come, First-Served)
Since the limit was set to **2**, the third redemption attempt must fail.
```bash
# 2nd successful use
curl -X POST "http://localhost:8080/api/v1/usages/SPRING2026/redeem" \
     -H "X-Forwarded-For: 89.64.12.150" \
     -H "Content-Type: application/json" \
     -d '{}'

# 3rd attempt (Exceeding limit)
curl -i -X POST "http://localhost:8080/api/v1/usages/SPRING2026/redeem" \
     -H "X-Forwarded-For: 89.64.12.150" \
     -H "Content-Type: application/json" \
     -d '{}'
```
* **Expected:** `400 Bad Request` (Error: Usage limit reached).

---

## 3. Resilience & Fault Tolerance

### 3.1 GeoIP Failover (Infrastructure Stability)
**Goal:** Prove that the system remains operational if the Primary GeoIP provider/proxy is slow or down.
* **Action:** Check `usage-service` logs after executing a redemption.
* **Expected:** If the Primary Provider fails, the system automatically switches to the **Secondary Provider** without failing the user's request.

### 3.2 Service Down (API Gateway Fallback)
```bash
docker stop coupon-service
curl -i -X GET "http://localhost:8080/api/v1/coupons"
```
* **Expected:** `200 OK` with a JSON Fallback Message (handled by the Gateway's Circuit Breaker).

--- 
