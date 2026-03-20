# Coupon Redemption System (Enterprise Microservices)

A scalable, high-concurrency distributed system for managing and redeeming discount coupons. Built with **Java 21**, **Spring Boot 3.4**, and **Spring Cloud**, following **Domain-Driven Design (DDD)** principles.



## 🚀 Architectural Highlights

* **Microservices Architecture**: Decoupled services for Coupon management and Usage tracking.
* **Domain-Driven Design (DDD)**: Logic encapsulated within Rich Domain Models (Agregates) to avoid anemic models.
* **Java 21 Features**: Utilizes `Sealed Classes`, `Records`, `Pattern Matching`, and `Virtual Threads` compatibility.
* **High Concurrency Handling**: Implements **Optimistic Locking** (`@Version`) to prevent race conditions during peak redemption periods.
* **Resilience**: Integrated **Resilience4j** for Retries and Circuit Breakers when calling external GeoIP providers.
* **Clean Architecture**: Strict separation between API, Application, Domain, and Infrastructure layers.

---

## 🏗️ Project Structure

The system consists of the following modules:

* **`api-gateway`**: Single entry point for all client requests.
* **`eureka-server`**: Service discovery and registration.
* **`coupon-service`**: Source of truth for coupon definitions, limits, and country-based validation.
* **`usage-service`**: Audit log and user-specific redemption tracking (Anti-fraud).

---

## 🛠️ Tech Stack

| Technology | Usage |
| :--- | :--- |
| **Java 21** | Modern language features (LTS) |
| **Spring Boot 3.4** | Core framework |
| **Spring Data JPA** | Persistence with Hibernate |
| **H2 / PostgreSQL** | Database (Relational) |
| **Spring Cloud** | Gateway, Eureka, OpenFeign |
| **RestClient** | Modern, functional HTTP client |
| **Lombok** | Boilerplate reduction |

---

## 🚦 Getting Started

### Prerequisites
* JDK 21
* Maven 3.9+

### Running the System
1.  **Start Eureka Server**:
    ```bash
    cd eureka-server && ./mvnw spring-boot:run
    ```
2.  **Start Coupon Service**:
    ```bash
    cd coupon-service && ./mvnw spring-boot:run
    ```
3.  **Start Usage Service**:
    ```bash
    cd usage-service && ./mvnw spring-boot:run
    ```

---

## 🧪 API Endpoints

### 1. Create a New Coupon
**POST** `/api/v1/coupons`
```json
{
  "code": "SUMMER2026",
  "usageLimit": 100,
  "targetCountry": "PL"
}
```

### 2. Redeem a Coupon
**POST** `/api/v1/coupons/{code}/redeem`
*The system automatically detects user country via GeoIP based on the Request IP.*

---

## 🛡️ Business Rules & Validation

1.  **Unique Codes**: Coupon codes are case-insensitive and unique.
2.  **Geo-Fencing**: Coupons can only be redeemed by users from the designated country (verified via `ipapi.co`).
3.  **Usage Limits**: "First come, first served" logic. Once the limit is reached, a `409 Conflict` is returned.
4.  **Error Handling**: Returns standard **RFC 7807 Problem Details** for all business exceptions.

---

## 📝 Implementation Notes (For Reviewers)
* **Optimistic Locking**: Used to handle many users redeeming the same coupon simultaneously without blocking the database.
* **Idempotency**: Designed to be idempotent in the redemption flow to prevent double-spending.
* **External Integration**: The `GeoIpClient` uses a fallback for `localhost` (127.0.0.1) to return "PL" for easier local testing.

---
