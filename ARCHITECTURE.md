# Architecture

## Layered design

Standard 3-layer Spring Boot flow. Controllers never touch repositories directly.

```
JobController (REST layer, /jobs/**)
    -> JobService (business logic: job creation)
        -> JobRepository (JpaRepository<Job, UUID>)
            -> PostgreSQL (jobs table, auto-created by Hibernate)
```

- **`JobController`** (`controllers/`) — `@RestController` mapped at `/jobs`. Currently exposes one endpoint, `POST /jobs/create`, which delegates to `JobService` and maps the result onto a response DTO. See `API_SPEC.md` for the exact contract.
- **`JobService`** (`services/`) — `@Service` holding business logic. Currently implements only `createJob(JobType, String consumerUri, String payload)`, which builds a new `Job` in `PENDING` status and persists it. No status-transition, retry, or polling logic exists yet.
- **`JobRepository`** (`repositories/`) — a plain `JpaRepository<Job, UUID>` with no custom query methods. All persistence is inherited CRUD.
- **`dto/`** — `CreateJobRequestDTO` (inbound: `jobType`, `consumerUri`, `payload`) and `CreateJobResponseDTO` (outbound: `id`, `jobStatus`). These decouple the wire format from the `Job` entity for the create-job flow; no other DTOs exist yet.
- **`entities/`** — `Job` (JPA entity), plus the `JobType` and `JobStatus` enums.

## Package structure

```
com.param.task_queue
├── TaskQueueApplication.java      Spring Boot entry point
├── controllers/
│   └── JobController.java
├── services/
│   └── JobService.java
├── repositories/
│   └── JobRepository.java
├── entities/
│   ├── Job.java
│   ├── JobType.java
│   └── JobStatus.java
└── dto/
    ├── CreateJobRequestDTO.java
    └── CreateJobResponseDTO.java
```

The base package is `com.param.task_queue` (underscore) rather than `com.param.task-queue`, because a hyphen is not a legal character in a Java package identifier (noted in `HELP.md`).

## Domain model

### `Job` entity

One row per background task. Table name: `jobs` (via `@Table(name = "jobs")`).

| Field | Type | Notes |
|---|---|---|
| `id` | `UUID` | `@Id`, `@GeneratedValue(strategy = GenerationType.UUID)` — server-generated on persist. |
| `jobType` | `JobType` (enum) | `@Enumerated(EnumType.STRING)` — stored as text, not ordinal. |
| `consumerUri` | `String` | The webhook/URI a worker would call to process the job. |
| `payload` | `String` | Raw, untyped data (typically JSON) for the consumer. |
| `jobStatus` | `JobStatus` (enum) | `@Enumerated(EnumType.STRING)`. Drives the lifecycle state machine below. |
| `retryCount` | `Integer` | Set to `0` at creation. Nothing currently increments it. |
| `eligibleToPickAfter` | `Instant` | Set to `Instant.now()` at creation. Nothing currently reads or advances it yet. |
| `createdAt` | `Instant` | Populated by a `@PrePersist` hook (`onCreate`) at insert time. |
| `updatedAt` | `Instant` | Populated by both the `@PrePersist` hook and a `@PreUpdate` hook (`onUpdate`) on every subsequent save. |

Built via Lombok (`@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`) — no hand-written getters/setters/constructors.

### `JobType` enum

What a worker should do with the job. Stored as `STRING` in the DB:

`SEND_EMAIL`, `GENERATE_REPORT`, `PROCESS_IMAGE`, `TRIGGER_WEBHOOK`, `DATA_CLEANUP`

### `JobStatus` enum

Drives the lifecycle below. Stored as `STRING` in the DB:

`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `DEAD`

## Job lifecycle (state machine)

Only the first transition below is implemented in code today (`JobService.createJob()`). Everything downstream of `PENDING` — the polling worker, the `RUNNING`/`COMPLETED`/`FAILED` transitions, and retry/backoff/dead-lettering — is design intent visible in the `JobStatus` enum and the `Job` fields (`retryCount`, `eligibleToPickAfter`), but has no corresponding code yet.

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

`createJob()` builds a `Job` with `jobStatus = PENDING`, `retryCount = 0`, and `eligibleToPickAfter = Instant.now()`, then saves it via `JobRepository`. There is no "max retries" constant or backoff calculation anywhere in the codebase yet — the diagram above reflects the shape the enum/fields are clearly designed for, not implemented behavior.

## Notable design decisions (as evidenced in the code)

- **`payload` is a raw `String`, not a typed/structured column.** This keeps the `jobs` table schema independent of any individual `JobType`'s data shape — each job type owns its own payload contract, and the queue just stores and forwards it. The consumer at `consumerUri` is responsible for interpreting it.
- **`eligibleToPickAfter` is a single field serving two purposes:** an initial-delay scheduling gate at creation time (`Instant.now()` today, but the field would support a future value for delayed jobs) and, per the state-machine design, a retry-backoff timestamp after a `FAILED` attempt. One column covers both cases instead of separate `scheduledAt`/`retryAfter` fields.
- **`consumerUri` lives on the job row itself**, not in worker-side config — so a job is self-describing and a worker doesn't need per-job-type routing logic to know where to deliver it.
- **UUID primary key, server-generated** (`GenerationType.UUID`) rather than a sequential/identity column, avoiding sequential-ID enumeration over the API.
- **Indexing consideration (not yet applied):** the worker query implied by the state machine will be shaped like `WHERE job_status = 'PENDING' AND eligible_to_pick_after <= now()`. A composite index on `(job_status, eligible_to_pick_after)` would matter for that query once the polling worker exists. No such index is defined today — the schema is whatever Hibernate's `ddl-auto=update` derives from the entity, with no explicit `@Index` or migration script.
- **No Flyway/Liquibase.** Schema is entirely driven by `spring.jpa.hibernate.ddl-auto=update` against the `Job` entity mapping — there is no versioned migration history.
