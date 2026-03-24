# 🎫 Coupon Redemption System (Enterprise Microservices)

A high-performance, distributed system designed to manage and track the lifecycle of discount coupons. This project demonstrates a modern approach to building scalable microservices using **Java 21**, **Spring Boot 3.4**, and **Spring Cloud**, with a strong emphasis on **Domain-Driven Design (DDD)** and **Clean Architecture**.

-----

## 🚀 Architectural Highlights

* **Microservices Architecture**: The system is decomposed into specialized services (`coupon-service` and `usage-service`) to allow independent scaling, deployment, and database isolation.
* **Domain-Driven Design (DDD)**: Business logic is encapsulated within **Rich Domain Models** and **Aggregates**. This ensures that invariants (like usage limits and country restrictions) are enforced at the core of the application, avoiding "Anemic Domain Models."
* **Concurrency & Data Integrity**: Uses **Optimistic Locking** (`@Version`) to handle high-traffic redemption spikes. This prevents the "over-redeem" problem where multiple threads might decrement a counter simultaneously without database deadlocks.
* **Resilience & Fault Tolerance**: Powered by **Resilience4j**, the system implements **Circuit Breakers** and **Retries** for external GeoIP API calls. This prevents the "cascading failure" effect if the location provider goes down.
* **Event-Driven Evolution**: Successful redemptions are published as events to **Apache Kafka**, allowing for asynchronous processing by audit or analytics services without increasing latency for the end-user.

-----

## 🏗️ System Components & Tech Stack

| Module | Responsibility | Port |
| :--- | :--- | :--- |
| **`api-gateway`** | Central entry point. Handles request routing, load balancing, and cross-cutting concerns. | 8080 |
| **`eureka-server`** | Service Discovery. Allows microservices to find and communicate with each other dynamically. | 8761 |
| **`coupon-service`** | The Source of Truth for coupon definitions, stock management, and country rules. | 8082 |
| **`usage-service`** | Handles the redemption flow, GeoIP location logic, and per-user anti-fraud tracking. | 8081 |

**Technologies:**

* **Java 21**: Leveraging Virtual Threads (Project Loom) compatibility and modern syntax (Records, Pattern Matching).
* **Spring Boot 3.4**: Core framework for microservice development.
* **PostgreSQL**: Relational storage for ACID-compliant transactions.
* **Apache Kafka**: Distributed event streaming.
* **OpenFeign**: Declarative REST client for inter-service communication.
* **SpringDoc OpenAPI**: Automatic Swagger documentation.

-----

## 🏛️ Architectural Decisions & Design Patterns

### 1\. Clean Architecture (Layered)

The project follows a strict separation of concerns:

* **Domain Layer**: Pure business logic (Entities, Value Objects, Domain Exceptions).
* **Application Layer**: Orchestration (Use Cases, Services).
* **Infrastructure Layer**: Adapters for Databases, Kafka, and External APIs.
* **Why?** This ensures the business logic remains testable and independent of external frameworks or UI changes.

### 2\. Strategy Pattern (GeoIP Providers)

The location detection logic is decoupled from the service using the **Strategy Pattern**.

* **Why?** It allows the system to switch between different GeoIP providers (ipapi, MaxMind, or Mock) without changing the core redemption logic. It also facilitates easy local testing via a fallback strategy for `127.0.0.1`.

### 3\. Transactional Integrity

Redemption involves checking a limit in one service and recording usage in another. To handle this in a distributed environment:

* **Optimistic Locking** ensures the coupon counter is never decremented incorrectly.
* **Idempotency** is baked into the redemption flow to prevent double-spending in case of network retries.

### 4\. RFC 7807 (Problem Details)

All business exceptions (e.g., `LIMIT_EXCEEDED`, `INVALID_COUNTRY`) are mapped to the **RFC 7807** standard.

* **Why?** Provides a machine-readable and consistent error format for API consumers.

-----

## 🚦 Getting Started

### Prerequisites

* Docker & Docker Compose
* JDK 21 (if running locally)

### Quick Start (Docker Compose)

The entire infrastructure (DBs, Kafka, Services) can be launched with a single command:

```bash
docker-compose up --build -d
```

*Wait \~40 seconds for Eureka registration to complete.*

-----

## 🧪 API Documentation & Testing

### Interactive Documentation (Swagger)

Each service provides a comprehensive UI for API exploration:

* **Coupon Service**: [http://localhost:8082/swagger-ui.html](https://www.google.com/search?q=http://localhost:8082/swagger-ui.html)
* **Usage Service**: [http://localhost:8081/swagger-ui.html](https://www.google.com/search?q=http://localhost:8081/swagger-ui.html)

### Automated Smoke Test

I have provided a bash script to verify the entire business flow (Creation -\> Successful Redemption -\> Limit Reached -\> Duplicate User Rejection):

```bash
chmod +x smoke-test.sh
./smoke-test.sh
```

### Manual cURL Example (Geo-Fencing)

To test country restrictions, use the `X-Forwarded-For` header:

```bash
# Valid Polish IP
curl -X POST http://localhost:8080/usage-service/api/v1/usages/SUMMER2026/redeem \
  -H "User-Id: user_99" \
  -H "X-Forwarded-For: 89.64.12.150" \
  -H "Content-Type: application/json" -d '{}'
```

-----

## 🛡️ Business Rules Enforcement

1.  **Case-Insensitivity**: Coupon `SUMMER` and `summer` are treated as the same unique entity.
2.  **Geo-Fencing**: Redemptions are strictly limited to the country defined during coupon creation (detected via IP).
3.  **Stock Protection**: "First-come, first-served." Once the usage limit is reached, all subsequent attempts return `409 Conflict`.
4.  **Anti-Fraud**: A specific `User-Id` cannot redeem the same coupon code twice, even if the total limit is not reached.

-----

## 📝 Implementation Notes (For Reviewers)

* **Concurrency**: The system is designed to handle high-volume traffic using non-blocking principles where possible.
* **Testing Philosophy**: Unit tests cover complex domain logic (JUnit 5), while Integration tests (Testcontainers) verify database and Kafka interactions.
* **Observability**: Standard Spring Boot Actuator endpoints are enabled for health monitoring.

-----

This project was built with the mindset of a **production-ready** application, prioritizing code quality, maintainability, and system resilience.

## 📖 Project Documentation & Testing

For detailed technical information, architecture diagrams, and testing procedures, please refer to the following documents:

| Document                                          | Description |
|:--------------------------------------------------| :--- |
| **[🏗️ Architecture Guide](DOC/ARCHITECTURE.md)** | Deep dive into **C4 Model** diagrams, DDD layers, and Design Patterns used (Strategy, Locking, etc.). |
| **[🧪 API Testing Guide](DOC/GATEWAY-CURL.md)**   | A complete list of **cURL** commands to manually verify every business requirement and edge case. |
| **[🚀 Smoke Test Script](./smoke-test.sh)**       | Automated bash script to verify the entire system flow in seconds. |



