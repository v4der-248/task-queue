# Task Queue

A lightweight, database-backed job queue service built with Spring Boot and PostgreSQL. Producers POST a job over HTTP, the queue persists it, and a worker (not yet implemented) is intended to poll and process it when due. No message broker — PostgreSQL is the only infrastructure dependency.

## Prerequisites

- Java 17
- Maven (or use the bundled `mvnw` / `mvnw.cmd` wrapper — no local Maven install required)
- A running PostgreSQL instance

## Setup

**1. Start PostgreSQL.** Any local install or container works, as long as it's reachable at the host/port/db name used in step 2. Example with Docker:

```bash
docker run -d \
  -p 5432:5432 \
  -e POSTGRES_DB=taskqueue \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  postgres:15
```

**2. Datasource configuration.** `src/main/resources/application.properties` currently contains:

```properties
spring.application.name=task-queue

spring.datasource.url=jdbc:postgresql://localhost:5432/taskqueue
spring.datasource.username=postgres
spring.datasource.password=shree420
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

`ddl-auto=update` means Hibernate creates/updates the `jobs` table automatically from the `Job` entity — there are no Flyway/Liquibase migrations in this project. Adjust the URL/username/password to match your local PostgreSQL instance if it differs from the defaults above.

> Note: the datasource password is currently committed in plaintext in `application.properties`. Treat this as a local-dev-only value and be careful not to reuse it, or any real credential, in a file that gets committed.

**3. Build:**

```bash
./mvnw compile
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

**4. Run tests:**

```bash
./mvnw test
```

To run a single test:

```bash
./mvnw test -Dtest=TaskQueueApplicationTests#contextLoads
```

**5. Run the app:**

```bash
./mvnw spring-boot:run
```

## Current status / limitations

- **Package name:** the base package is `com.param.task_queue` (underscore). The original `com.param.task-queue` is invalid Java — see `HELP.md`.
- **One functional endpoint:** `POST /jobs/create` creates a job in `PENDING` status. See `API_SPEC.md` for the exact request/response contract. There are no other endpoints (no list, get-by-id, cancel, or status-update routes).
- **No request validation:** `CreateJobRequestDTO` has no `@Valid`/Bean Validation constraints, and the project has no `spring-boot-starter-validation` dependency. A request with a missing `consumerUri` or `payload` will be accepted as-is (null values pass through to persistence).
- **No worker / polling loop implemented.** Jobs are created and persisted as `PENDING` but nothing currently picks them up, runs them, or transitions them to `RUNNING`, `COMPLETED`, `FAILED`, or `DEAD`. `JobService.createJob()` is the only lifecycle transition implemented so far.
- **No retry/backoff/dead-lettering logic.** `retryCount` and `eligibleToPickAfter` exist on the `Job` entity and are initialized on creation, but nothing currently reads or increments them.
- **No API documentation dependency.** The project has no springdoc-openapi (or other OpenAPI) dependency, so `API_SPEC.md` is the hand-maintained source of truth for the REST contract until one is added.
- **Schema managed by Hibernate auto-DDL**, not migrations. See `ARCHITECTURE.md` for the current table shape and indexing considerations for the not-yet-implemented polling worker.

See `ARCHITECTURE.md` for the layered design and domain model, and `API_SPEC.md` for the REST contract.
