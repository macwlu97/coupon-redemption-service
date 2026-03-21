# 🧪 Testing Guide (Manual API Tests)

This document contains a set of `curl` commands to verify the functionality of the **Coupon Service**. You can run these commands directly from your terminal (bash/zsh).

## 1. List All Coupons
Verify if the initial data from `import.sql` was loaded correctly into the H2 database.
```bash
curl -X GET http://localhost:8081/api/v1/coupons \
     -H "Accept: application/json" |  jq
```

---

## 2. Create a New Coupon
Add a new coupon specifically for the German market (`DE`).
```bash
curl -X POST http://localhost:8081/api/v1/coupons \
     -H "Content-Type: application/json" \
     -d '{
       "code": "BERLIN2026",
       "usageLimit": 50,
       "targetCountry": "DE"
     }' |  jq
```

---

## 3. Redeem Coupon (Success Case - Poland)
Redeem the `WIOSNA2026` coupon (which is restricted to `PL`).
We simulate a Polish IP address using the `X-Forwarded-For` header.
```bash
curl -i -X POST "http://localhost:8081/api/v1/coupons/WIOSNA2026/redeem" \
     -H "X-Forwarded-For: 5.173.0.1" |  jq
```
*Expected Response: `200 OK` or `204 No Content`.*

---

## 4. Redeem Coupon (Failure - Geographic Restriction)
Attempt to use the same Polish coupon while "located" in Germany.
```bash
curl -i -X POST "http://localhost:8081/api/v1/coupons/WIOSNA2026/redeem" \
     -H "X-Forwarded-For: 3.120.0.1" |  jq
```
*Expected Response: `400 Bad Request` or `422 Unprocessable Entity` with an error message regarding the country mismatch.*

---

## 5. Non-Existent Coupon
Try to redeem a code that doesn't exist in the database.
```bash
curl -i -X POST "http://localhost:8081/api/v1/coupons/INVALID-CODE/redeem" \
     -H "X-Forwarded-For: 5.173.0.1" |  jq
```
*Expected Response: `404 Not Found`.*

---

## 💡 Troubleshooting Note
If you are testing on `localhost` and your service returns `127.0.0.1` regardless of the header, the GeoIP provider might default to a specific country. For a production-grade showcase, ensure your `GeoIpClient` is configured to trust the `X-Forwarded-For` header.

---