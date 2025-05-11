# Virtual Power Plant (VPP) System

A reactive, event-driven Spring Boot application using WebFlux, R2DBC, Kafka, Redis, PostgreSQL, and Liquibase for real-time power plant data management and analytics.

---

## 🔍 Analysis of the Requirements
### 🧩 Functional Requirements
1. Battery Ingestion Endpoint
   - Accepts name, postcode, watt capacity in bulk.
   - Persists in database.

2. Battery Query Endpoint
    - Filter by postcode range.
    - Return list of names (sorted).
    - Return total and average watt capacity.

3. Extended Query Support
   - Filter by min/max watt capacity.

4. Concurrent Writes Support
   - System must handle many simultaneous battery registrations.

5. Logging & Observability
   - Log significant events (e.g., ingestion, failures).

6. Testing
   - 70%+ test coverage, with unit + integration tests using Testcontainers.

7. Java Streams Usage
   - For querying, transforming, or aggregating battery data.


## 🏛 Architectural Overview

###
### 🗃️ Persistence
- PostgreSQL (via R2DBC): good for async I/O and relational querying.
- Liquibase: for managing schema evolution in a maintainable way.

### ⚡ Concurrency
- Spring WebFlux + R2DBC: event-loop model scales better for I/O-bound systems.
- Kafka: buffering writes for async durability (used via BatteryConsumerService).

### ⚙️ Caching
- Redis: used to cache battery data or query results.

### 🧪 Testing
- JUnit + Testcontainers: realistic DB integration testing.
- Reactor Test: for verifying reactive pipelines.

## 🚀 Tech Stack

- Java 21
- Spring Boot 3.4.5
- WebFlux (Reactive APIs)
- PostgreSQL with R2DBC
- Redis (Reactive)
- Kafka (Messaging)
- Liquibase (Database migrations)
- Prometheus (Metrics)
- Testcontainers (Testing)

## ⚙️ Getting Started
### 🌱 Environment Setup
Clone the repository and create a `.env` file in your project root:

```env
# App Config
APP_PORT=8080

# Main Database
PG_USER=
PG_PASSWORD=
PG_DATABASE=
PG_HOST=
PG_PORT=
```

These values are picked up automatically by the co.uzzu.dotenv.gradle plugin.

### 🐘 Build the Application

```
./gradlew build
```

### 🧪 Run Tests

```
./gradlew test
```


###  🧬 Database Migrations with Liquibase

Run Migrations:
```bash
./gradlew update
```
Changelog path: src/main/resources/db/changelogs

### 🐬 Run Services
Redis and kafka with Docker:
```bash
docker run --name redis -p 6379:6379 -d redis
docker run --name kafka -p 9092:9092 apache/kafka:4.0.0  
```
Create Kafka topic:
```bash
bin/kafka-topics.sh --create --topic quickstart-events --bootstrap-server localhost:9092
bin/kafka-topics.sh --describe --topic quickstart-events --bootstrap-server localhost:9092
```

### 🚀 Run the Application
```bash
./gradlew bootRun
```
Then access it at:
📍 http://localhost:8080/api


###  📊 Observability & Metrics
- Prometheus metrics: http://localhost:8080/api/actuator/prometheus
- Health check: http://localhost:8080/api/actuator/health
- All actuators: http://localhost:8080/api/actuator

### 📦 API Documentation 
```bash
http://localhost:8080/api/swagger-ui/index.html
```

