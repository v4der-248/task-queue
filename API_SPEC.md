# API Spec

This document is the hand-maintained source of truth for the REST contract, because the project has no springdoc-openapi (or other OpenAPI) dependency yet. If one is added later, prefer generating the spec from controller annotations and retire this file rather than maintaining both.

Base path: `/jobs`

Only one endpoint exists in the codebase today. No other routes (list jobs, get job by id, cancel, update status, etc.) are implemented.

---

## `POST /jobs/create`

Creates a new job and persists it in `PENDING` status. Implemented by `JobController.createJob`, which delegates to `JobService.createJob(JobType, String consumerUri, String payload)`.

### Request

Content type: `application/json`

Body maps to `CreateJobRequestDTO`:

| Field | Type | Required? | Notes |
|---|---|---|---|
| `jobType` | string (enum) | Not enforced | One of `SEND_EMAIL`, `GENERATE_REPORT`, `PROCESS_IMAGE`, `TRIGGER_WEBHOOK`, `DATA_CLEANUP`. No `@Valid`/Bean Validation constraints exist on the DTO — a `null` or missing value is passed straight through to `JobService` and persisted as-is. Sending a string that doesn't match any enum constant will fail JSON deserialization before the controller method runs (Spring's default behavior for enum binding), resulting in a `400 Bad Request` with no custom error body, since there is no `@ExceptionHandler`/`@ControllerAdvice` in the project. |
| `consumerUri` | string | Not enforced | No format/URL validation. `null` is accepted and persisted. |
| `payload` | string | Not enforced | Freeform, typically JSON, but not parsed or validated by the queue — stored as-is. |

Example:

```http
POST /jobs/create
Content-Type: application/json

{
  "jobType": "SEND_EMAIL",
  "consumerUri": "https://example.com/webhooks/email",
  "payload": "{\"to\": \"user@example.com\", \"subject\": \"Hello\"}"
}
```

### Response

`201 Created` on success. Body maps to `CreateJobResponseDTO`:

| Field | Type | Description |
|---|---|---|
| `id` | UUID (string) | Server-generated primary key of the created `Job` row. |
| `jobStatus` | string (enum) | Always `PENDING` for a freshly created job, since `JobService.createJob` hard-codes this status. |

Example:

```json
{
  "id": "b3b1c6b2-9b1b-4a1a-8a1a-0a1a2b3c4d5e",
  "jobStatus": "PENDING"
}
```

Only `id` and `jobStatus` are returned — `jobType`, `consumerUri`, `payload`, `retryCount`, `eligibleToPickAfter`, `createdAt`, and `updatedAt` are set on the persisted `Job` but are not included in the response body.

### Error responses

No explicit error handling exists in `JobController` or `JobService` (no `try`/`catch`, no `@ExceptionHandler`, no `@ControllerAdvice` anywhere in the codebase). Any failure — a malformed request body, an unmappable enum value, or a database error from `JobRepository.save` — surfaces as whatever default response Spring Boot/Spring MVC produces for that exception type (e.g. a `400` for unreadable JSON, a `500` for an unhandled runtime exception). There are no custom, documented error response bodies.

---

## Endpoints that do not exist yet

For clarity, since the domain model (`JobStatus`, `retryCount`, `eligibleToPickAfter`) implies a fuller lifecycle: there is currently no endpoint to retrieve a job by id, list/filter jobs, cancel a job, or manually transition a job's status. These are not implemented.
