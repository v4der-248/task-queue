# Spec doc templates

Use these as-is. Keep them short — a spec doc that's harder to read than the code it describes defeats the point.

---

## ADR (Architecture Decision Record)

Location: `docs/adr/NNNN-short-title.md`, numbered sequentially. One decision per file. Never edit a merged ADR to reflect a later reversal — write a new ADR that supersedes it and link back.

```markdown
# ADR-000X: <short title>

## Status
Proposed | Accepted | Superseded by ADR-00YY

## Context
What problem forced this decision. 2-4 sentences, not a essay.

## Decision
What we're doing, stated plainly.

## Alternatives considered
- Option A — why not
- Option B — why not

## Consequences
What this makes easier, what it makes harder, what it forecloses.
```

---

## PRD (Product Requirements Doc)

Location: `docs/prd/<feature-name>.md`. Written before or alongside a new feature/module — not retrofitted after.

```markdown
# PRD: <feature name>

## Problem
What's broken or missing today, for whom.

## Goals
- ...

## Non-goals
Explicitly out of scope — as important as the goals.

## User stories
- As a <role>, I want <capability>, so that <outcome>.

## Success metrics
How you'll know this worked.

## Open questions
Anything unresolved at write time.
```

---

## API spec

Single source of truth: springdoc-openapi annotations on the controller generate the spec at `/v3/api-docs` and Swagger UI — don't also hand-maintain a separate OpenAPI YAML describing the same contract. Javadoc on the handler documents *purpose and side effects*; the annotations document the *contract* (request/response shape, status codes). Don't write the same description twice.

```java
/**
 * Updates a rate card entry. Only mutable while status = DRAFT — approved
 * entries must go through the approval-reversal flow instead.
 */
@Operation(summary = "Update a rate card entry pending approval")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Updated entry"),
    @ApiResponse(responseCode = "409", description = "Entry is not in DRAFT status")
})
@PatchMapping("/rate-cards/{id}")
public RateCardEntry updateEntry(@PathVariable String id, @RequestBody UpdateEntryRequest req) { ... }
```

If a project has no springdoc dependency and truly needs a hand-written spec, use one OpenAPI 3.1 file (`openapi.yaml` or split by resource under `docs/api/`) as the single source of truth instead — never both.

---

## README

Location: repo root, and one per major subpackage in a monorepo. Keep it current-state only — no changelog, no "future plans" (that's the PRD/ADR's job).

```markdown
# <project name>

One paragraph: what this is and who it's for.

## Setup
Exact commands, in order, that take a fresh clone to running locally.

## Architecture
2-3 sentences + a link to the relevant ADR(s)/PRD for depth. Don't
re-explain the architecture doc here.

## Scripts
| Command | Does |
|---|---|

## Environment variables
| Var | Required | Purpose |
|---|---|---|
```
