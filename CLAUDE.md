# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A lightweight, database-backed job queue service (Spring Boot + PostgreSQL). Producers POST a job over HTTP, the queue persists it, and a worker (not yet implemented) polls and processes it when due. No message broker — Postgres is the only infrastructure dependency.

## Commands

```bash
./mvnw compile              # build
./mvnw test                 # run all tests
./mvnw test -Dtest=TaskQueueApplicationTests#contextLoads   # run a single test
./mvnw spring-boot:run       # run the app locally
```

On Windows use `mvnw.cmd` instead of `./mvnw`.

The app needs a running PostgreSQL instance. `src/main/resources/application.properties` currently only sets `spring.application.name` — no datasource is configured yet, so the app will fail to start against a real DB until `spring.datasource.*` and `spring.jpa.hibernate.ddl-auto` are added (see README.md "Running Locally" for the values used in local dev).

## Architecture

Standard 3-layer Spring Boot flow, no cross-layer coupling (controllers never touch repositories directly):

```
JobController (/jobs REST layer)
    -> JobService (business logic: creation, status transitions, retries)
        -> JobRepository (JpaRepository<Job, UUID>)
            -> PostgreSQL (jobs table, auto-created by Hibernate; no Flyway/Liquibase yet)
```

Base package is `com.param.task_queue` (underscore, not hyphen) — the original `com.param.task-queue` package name is invalid Java, per HELP.md.

### Domain model (`entities/`)

- **`Job`** — the core entity, one row per background task: `id` (UUID, client-assignable), `jobType`, `consumerUri` (the webhook the worker calls to process the job), `payload` (raw `String`, typically JSON — kept untyped so the queue schema doesn't couple to individual job types), `jobStatus`, `retryCount`, `eligibleToPickAfter` (dual-purpose: initial delay scheduling AND retry backoff timestamp), `createdAt`/`updatedAt`.
- **`JobType`** — enum of what a worker should do: `SEND_EMAIL`, `GENERATE_REPORT`, `PROCESS_IMAGE`, `TRIGGER_WEBHOOK`, `DATA_CLEANUP`. Stored as `STRING` in the DB.
- **`JobStatus`** — enum driving the lifecycle state machine below. Stored as `STRING`.

### Job lifecycle (state machine)

```
createJob() -> PENDING -> (worker polls: eligibleToPickAfter <= now()) -> RUNNING -> COMPLETED
                                                                              |
                                                                           FAILED (retryCount < max)
                                                                              |  retryCount++, eligibleToPickAfter += backoff
                                                                              v
                                                                           PENDING (re-queued)
                                                                              |  retryCount >= max
                                                                              v
                                                                            DEAD (terminal, kept for audit)
```

`JobService.createJob()` is the only lifecycle transition implemented so far — it builds a `PENDING` job with `retryCount=0` and `eligibleToPickAfter=now()` and saves it. Retry/backoff/dead-lettering logic and the polling worker do not exist yet.

## Current state / gotchas

- `JobController.java` has an incomplete/invalid annotation (`@` with nothing after it) and will not compile as-is. No endpoint methods exist yet — implementing `POST /jobs` (calling `JobService.createJob`) is the natural next step.
- `createdAt`/`updatedAt` on `Job` are not auto-populated (no `@PrePersist`/`@PreUpdate` or auditing listener yet).
- No DTOs — if endpoints are added, decide whether to expose `Job` directly or add request/response DTOs.
- When the polling worker is added, the query shape will be `WHERE job_status = 'PENDING' AND eligible_to_pick_after <= now()` — a composite index on `(job_status, eligible_to_pick_after)` will matter for that.
