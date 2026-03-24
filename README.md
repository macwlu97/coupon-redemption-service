# 🎫 Coupon Redemption System (Enterprise Microservices)

A high-performance, distributed system designed to manage and track the lifecycle of discount coupons. This project demonstrates a modern approach to building scalable microservices using **Java 21**, **Spring Boot 3.4**, and **Spring Cloud**, with a strong emphasis on **Domain-Driven Design (DDD)** and **Clean Architecture**.

-----

## 🚀 Architectural Highlights

* **Microservices Architecture**: The system is decomposed into specialized services (`coupon-service` and `usage-service`) to allow independent scaling, deployment, and database isolation.
* **Domain-Driven Design (DDD)**: Business logic is encapsulated within **Rich Domain Models** and **Aggregates**. This ensures that invariants (like usage limits and country restrictions) are enforced at the core of the application.
* **Concurrency & Data Integrity**: Uses **Optimistic Locking** (`@Version`) to handle high-traffic redemption spikes. This prevents the "over-redeem" problem without database deadlocks.
* **Resilience & Fault Tolerance**: Powered by **Resilience4j**, the system implements **Circuit Breakers** and **Retries** for external GeoIP API calls.
* **Centralized Configuration**: All service parameters are managed by a dedicated **Spring Cloud Config Server**, allowing for dynamic updates without service restarts.

-----

## 🏗️ System Components & Tech Stack

| Module | Responsibility | Port |
| :--- | :--- | :--- |
| **`eureka-server`** | Service Discovery. Dynamic service registration and heartbeat monitoring. | 8761 |
| **`config-server`** | Centralized configuration management (Native/Git profiles). | 8888 |
| **`api-gateway`** | Central entry point. Request routing, Load Balancing, and Swagger aggregation. | 8080 |
| **`coupon-service`** | Source of Truth for coupon definitions, stock management, and geo-rules. | 8082 |
| **`usage-service`** | Handles redemption flow, GeoIP detection, and per-user anti-fraud tracking. | 8081 |

**Technologies:**

* **Java 21**: Virtual Threads (Loom) ready, Records, and Pattern Matching.
* **Spring Boot 3.4**: Latest stable framework for robust microservices.
* **PostgreSQL**: ACID-compliant storage for mission-critical data.
* **Apache Kafka**: Distributed event streaming for asynchronous audit logs.
* **OpenFeign**: Declarative HTTP client for inter-service communication.
* **WireMock & Testcontainers**: High-fidelity integration testing.
* **SpringDoc OpenAPI**: Automatic Swagger documentation.
-----

## 🏛️ Architectural Decisions & Design Patterns

### 1. Clean Architecture (Layered) & DDD
The project follows a strict separation of concerns:

* **Domain Layer**: Pure business logic (Entities, Value Objects, Domain Exceptions).
* **Application Layer**: Orchestration (Use Cases, Services).
* **Infrastructure Layer**: Adapters for Databases, Kafka, and External APIs.
* **Why?** This ensures the business logic remains testable and independent of external frameworks or UI changes.

### 2. Strategy Pattern (GeoIP Providers)
Location detection is decoupled. The system can switch between providers (ipapi, MaxMind, or Mock) seamlessly. It includes a fallback strategy for `127.0.0.1` to facilitate local development.

### 3. Distributed Consistency
* **Optimistic Locking**: Ensures atomicity of the "decrement stock" operation.
* **Idempotency**: Built-in protection against network retries to prevent double-redemptions.

### 4. RFC 7807 (Problem Details)
Consistent, machine-readable error responses (e.g., `LIMIT_EXCEEDED`, `INVALID_COUNTRY`) across all microservices.

-----

## 🚦 Getting Started

### Prerequisites
* Docker & Docker Compose
* JDK 21 (for local execution)

### Quick Start (Docker Compose)
Launch the entire ecosystem (Databases, Kafka, Services) with one command:

```bash
docker-compose up --build -d
````

*Note: Wait \~40s for services to synchronize with Eureka and Config Server.*

### Local Development & Testing

To run tests locally without the need for a running Config Server:

```bash
mvn clean test -Dspring.profiles.active=test
```

*The `test` profile is pre-configured to use H2 and mock external dependencies.*

-----

## 🧪 API Documentation & Testing

### Swagger UI (Unified Access)

Access all microservice APIs through the Gateway:

* **API Gateway (Aggregated)**: [http://localhost:8080/swagger-ui.html](https://www.google.com/search?q=http://localhost:8080/swagger-ui.html)

### Interactive Documentation (Swagger)

Each service provides a comprehensive UI for API exploration:

* **Coupon Service**: [http://localhost:8082/swagger-ui.html](https://www.google.com/search?q=http://localhost:8082/swagger-ui.html)
* **Usage Service**: [http://localhost:8081/swagger-ui.html](https://www.google.com/search?q=http://localhost:8081/swagger-ui.html)


### Automated Smoke Test

Verify the full business flow (Create -\> Redeem -\> Limit Check -\> Fraud Prevention):

```bash
chmod +x smoke-test.sh
./smoke-test.sh
```

### Manual cURL Example (Geo-Fencing)

```bash
# Valid Polish IP
curl -X POST http://localhost:8080/usage-service/api/v1/usages/SUMMER2026/redeem \
  -H "User-Id: user_1" \
  -H "X-Forwarded-For: 89.64.12.150" \
  -H "Content-Type: application/json" -d '{}'
```

-----

## 🛡️ Business Rules Enforcement

1.  **Case-Insensitivity**: `SUMMER` == `summer`.
2.  **Geo-Fencing**: Redemptions allowed only from the coupon's target country.
3.  **Stock Protection**: "First-come, first-served" – strict limit enforcement.
4.  **Anti-Fraud**: One redemption per `User-Id` per coupon code.

-----

## 📝 Implementation Notes (For Reviewers)

* **Resilience**: Every external call is wrapped in a Circuit Breaker.
* **Testing**: Unit tests (JUnit 5/AssertJ) for Domain; Integration tests (WireMock/H2) for Infra.
* **Scalability**: Stateless services ready for horizontal scaling behind the Gateway.


## 📖 Project Documentation & Testing

For detailed technical information, architecture diagrams, and testing procedures, please refer to the following documents:

| Document                                          | Description |
|:--------------------------------------------------| :--- |
| **[🏗️ Architecture Guide](DOC/ARCHITECTURE.md)** | Deep dive into **C4 Model** diagrams, DDD layers, and Design Patterns used (Strategy, Locking, etc.). |
| **[🧪 API Testing Guide](DOC/GATEWAY-CURL.md)**   | A complete list of **cURL** commands to manually verify every business requirement and edge case. |
| **[🚀 Smoke Test Script](./smoke-test.sh)**       | Automated bash script to verify the entire system flow in seconds. |


This project was built with the mindset of a **production-ready** application, prioritizing code quality, maintainability, and system resilience.