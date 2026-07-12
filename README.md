# Task Queue

A lightweight, database-backed job queue service built with Spring Boot and PostgreSQL. Designed as a simple, self-hosted alternative for scheduling and tracking async background jobs — no external message brokers required.

---

## Problem It Solves

Applications often need to run background work asynchronously — send an email, generate a report, process an image. This project provides a simple HTTP-based job queue: a producer POSTs a job, the queue persists it, and a worker polls and processes it when it's due. No Kafka, no RabbitMQ — just a database and a polling loop.

---

## Tech Stack

| Technology       | Version | Role                                  |
|------------------|---------|---------------------------------------|
| Java             | 17      | Language                              |
| Spring Boot      | 4.0.6   | Framework                             |
| Spring Data JPA  | —       | ORM / repository layer                |
| Spring Web MVC   | —       | REST API                              |
| PostgreSQL       | —       | Persistence                           |
| Lombok           | —       | Boilerplate reduction (`@Data`, `@Builder`) |
| Maven            | 3.x     | Build tool                            |

---

## Architecture

```
HTTP Client
     │
     ▼
┌─────────────────┐
│  JobController  │   REST layer — /jobs/**
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   JobService    │   Business logic — creation, status transitions, retries
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  JobRepository  │   JPA repository — JpaRepository<Job, UUID>
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   PostgreSQL    │   jobs table
└─────────────────┘
```

Standard 3-layer Spring Boot architecture. No cross-layer coupling — controllers never touch repositories directly.

---

## Domain Model

### `Job` Entity

The `Job` is the core entity. Every background task is a row in the `jobs` table.

| Field                   | Type               | Description |
|-------------------------|--------------------|-------------|
| `id`                    | `UUID`             | Primary key. UUID avoids sequential ID guessing and allows future client-side ID generation. |
| `jobType`               | `JobType` (enum)   | Discriminates what a worker should do. Stored as `STRING` in DB for readability. |
| `consumerUri`           | `String`           | The URI the worker calls when processing this job (webhook/callback model). |
| `payload`               | `String`           | Arbitrary data (typically JSON) the consumer needs. Raw `String` to decouple queue schema from job-specific payloads. |
| `jobStatus`             | `JobStatus` (enum) | Current position in the lifecycle state machine. |
| `retryCount`            | `Integer`          | How many times this job has been attempted. Worker uses this to enforce retry limits. |
| `eligibleToPickAfter`   | `Instant`          | Earliest time a worker may pick up this job. Dual-purpose: initial delay scheduling + retry backoff. |
| `createdAt`             | `Instant`          | Audit — when the job was enqueued. |
| `updatedAt`             | `Instant`          | Audit — last state change. |

> **Known issue:** The field is declared as `Id` (capital I) in `Job.java`. This is a Java naming convention violation and may cause unexpected JPA column mapping. Should be renamed to `id`.

### Job Lifecycle (State Machine)

```
                   ┌──────────┐
   createJob() →   │ PENDING  │   Enqueued, waiting to be picked up
                   └────┬─────┘
                        │  worker polls: eligibleToPickAfter <= now()
                        ▼
                   ┌──────────┐
                   │ RUNNING  │   Worker is actively processing
                   └────┬─────┘
               ┌────────┴─────────┐
               │                  │
               ▼                  ▼
         ┌──────────┐       ┌──────────┐
         │COMPLETED │       │  FAILED  │   retryCount < maxRetries
         └──────────┘       └────┬─────┘
                                 │  retryCount++, eligibleToPickAfter += backoff
                                 │  → status reset to PENDING (re-queued)
                                 │
                                 │  retryCount >= maxRetries
                                 ▼
                            ┌──────────┐
                            │   DEAD   │   Terminal. No more retries. Stays in DB for audit.
                            └──────────┘
```

- `FAILED` jobs are re-queued automatically: `retryCount` increments, `eligibleToPickAfter` pushed forward (exponential backoff), status flips back to `PENDING`.
- `DEAD` jobs are never retried. They remain in the table for manual inspection and debugging.

### `JobType` Enum

Tells the worker which handler to invoke. All values stored as strings in the DB.

| Value              | Intent                                          |
|--------------------|-------------------------------------------------|
| `SEND_EMAIL`       | Trigger an outbound email via `consumerUri`     |
| `GENERATE_REPORT`  | Kick off a report generation pipeline           |
| `PROCESS_IMAGE`    | Image transformation / processing               |
| `TRIGGER_WEBHOOK`  | Fire a generic outbound webhook                 |
| `DATA_CLEANUP`     | Scheduled maintenance / housekeeping tasks      |

### `JobStatus` Enum

| Value       | Meaning                                                           |
|-------------|-------------------------------------------------------------------|
| `PENDING`   | Waiting to be picked up (or re-queued after a failed attempt)     |
| `RUNNING`   | Actively being processed by a worker                              |
| `COMPLETED` | Successfully processed                                            |
| `FAILED`    | Processing failed, eligible for retry                             |
| `DEAD`      | Exhausted all retries — terminal state                            |

---

## API

**Base path:** `/jobs`

| Method | Path    | Status     | Description          |
|--------|---------|------------|----------------------|
| POST   | `/jobs` | Planned    | Enqueue a new job    |

> `JobController` exists but has no endpoint methods yet. The class is wired with constructor injection of `JobService`.

### Planned — Create Job

```http
POST /jobs
Content-Type: application/json

{
  "jobType": "SEND_EMAIL",
  "consumerUri": "https://example.com/webhooks/email",
  "payload": "{\"to\": \"user@example.com\", \"subject\": \"Hello\"}"
}
```

Expected response: the created `Job` object — `id` (UUID), `jobStatus: PENDING`, `retryCount: 0`, `eligibleToPickAfter: now`, `createdAt`.

---

## Database

**Table:** `jobs` — auto-created by Hibernate from the `Job` entity.

No Flyway/Liquibase yet. Schema is currently managed by `spring.jpa.hibernate.ddl-auto`.

**Approximate DDL:**
```sql
CREATE TABLE jobs (
    id                      UUID PRIMARY KEY,
    job_type                VARCHAR(50)              NOT NULL,
    consumer_uri            VARCHAR(255),
    payload                 TEXT,
    job_status              VARCHAR(20)              NOT NULL,
    retry_count             INTEGER,
    eligible_to_pick_after  TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE,
    updated_at              TIMESTAMP WITH TIME ZONE
);
```

> **Performance note:** When the worker is implemented, add a composite index on `(job_status, eligible_to_pick_after)` — that's the exact predicate the polling query will use (`WHERE job_status = 'PENDING' AND eligible_to_pick_after <= now()`).

---

## Running Locally

**Prerequisites:** Java 17, Maven, PostgreSQL (local or Docker).

**1. Start PostgreSQL:**
```bash
docker run -d \
  -p 5432:5432 \
  -e POSTGRES_DB=taskqueue \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15
```

**2. Add DB config to `src/main/resources/application.properties`:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/taskqueue
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

**3. Run:**
```bash
./mvnw spring-boot:run
```

---

## Design Decisions

**Why UUID as primary key?**
Avoids sequential IDs being guessable via HTTP. Also allows client-side ID generation — a producer can assign an ID before calling the API, enabling idempotent job creation.

**Why store `consumerUri` on the job itself?**
Keeps jobs self-contained. The worker needs no external routing config — the destination is encoded in the job row. This makes the queue generic and reusable across different services without any worker-side configuration.

**Why `eligibleToPickAfter` instead of a plain `scheduledAt`?**
Dual-purpose design: initial scheduling (delay a job's start) and retry backoff (bump the timestamp forward after a failure). One field covers both use cases cleanly.

**Why raw `String` for `payload`?**
Avoids coupling the queue schema to the structure of individual job types. Each `JobType` owns its own payload contract; the queue just stores and forwards the bytes. The consumer (webhook receiver) is responsible for parsing.

**Why no message broker (Kafka, RabbitMQ)?**
Intentional simplicity. Database-backed queues trade raw throughput for operational simplicity: no extra infrastructure, transactional consistency with the application DB, and queue state is fully inspectable via plain SQL. Suitable until throughput demands justify the complexity of a broker.

---

## Current State & TODO

- [x] `Job` entity with `JobStatus` and `JobType` enums
- [x] `JobRepository` — JPA CRUD via `JpaRepository<Job, UUID>`
- [x] `JobService.createJob()` — enqueues a `PENDING` job
- [ ] Fix `Job.java` — rename field `Id` → `id` (JPA naming convention bug)
- [ ] `JobController` — implement REST endpoints (at minimum: `POST /jobs`)
- [ ] Auto-populate `createdAt` / `updatedAt` — use `@PrePersist` / `@PreUpdate` or `@EntityListeners(AuditingEntityListener.class)`
- [ ] Worker / polling loop — query `WHERE job_status = 'PENDING' AND eligible_to_pick_after <= now()`, lock row, call `consumerUri`
- [ ] Retry logic — on worker failure: `retryCount++`, `eligibleToPickAfter = now + backoff`, status → `PENDING`; if `retryCount >= max` → `DEAD`
- [ ] Flyway migrations (replace `ddl-auto=update` with versioned SQL scripts)
- [ ] Composite index on `(job_status, eligible_to_pick_after)` for polling query performance
- [ ] DTOs for API request/response (decouple entity from wire format)
