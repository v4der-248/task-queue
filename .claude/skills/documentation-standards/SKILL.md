---
name: documentation-standards
description: Conventions for code comments, Javadoc, and spec docs (README, API spec, ADRs, PRDs) in Java/Spring Boot codebases. Use when writing or reviewing comments, documenting a class/service/controller/entity, or writing a PRD, ADR, or API spec.
---

# Documentation Standards

## Comment philosophy

- Write **why**, not **what**. The code already says what it does; a comment restating that is noise. Comment the reason: a business rule, an edge case, a constraint from an external system, a concurrency/ordering assumption, a tradeoff.
- If a comment is needed to explain *what* code does, prefer making the code clearer (better names, extracted method) over adding a comment on top of confusing code. Comment as a second resort, not a first one.
- No commented-out code, no `// changed by X on date`, no restating git history — version control already owns that.
- A stale comment is worse than no comment. If you can't verify a comment still matches the code, fix or remove it rather than leaving it.
- Skip comments on self-evident code (a one-line getter, an obvious loop). Reserve comments for the 20% that actually needs them — this includes almost anything involving ordering, concurrency, retries, or idempotency in a task-queue/distributed setting.

See `references/comment-examples.md` for good/bad side-by-side examples.

## Javadoc conventions

Use standard Javadoc (`/** ... */`), not plain `//` blocks, for anything another class or another engineer will depend on.

**Always add a Javadoc block to:**
- Every public class and public method in a shared/service package
- Every `@RestController` endpoint method (purpose and side effects — not the full request/response contract, see API spec section)
- Every non-trivial algorithm: queue consumers/producers, retry/backoff logic, locking, scheduling — state the invariant it must preserve
- Package-level docs via `package-info.java` for any package with more than a couple of classes

**Skip Javadoc on:**
- Private/internal helper methods whose name + signature already say enough
- Trivial getters/setters, `equals`/`hashCode`/`toString` overrides, and other boilerplate

**Required tags where applicable:** `@param`, `@return`, `@throws`, `@implNote` for non-obvious implementation constraints, `{@code}` for inline code references, `@deprecated` when replacing something instead of deleting it outright.

```java
/**
 * Claims the next available job from the queue and marks it {@code IN_PROGRESS}.
 *
 * @implNote Uses a {@code SELECT ... FOR UPDATE SKIP LOCKED} query so multiple
 *     workers can poll concurrently without claiming the same job twice.
 *     Do not replace with a plain SELECT + UPDATE — that reintroduces the
 *     race this was written to avoid.
 *
 * @return the claimed job, or {@code Optional.empty()} if the queue is empty
 */
public Optional<Job> claimNextJob() {
    // ...
}
```

## Spring Boot specifics

- **Controllers (`@RestController`):** Javadoc states purpose and side effects. The actual request/response contract belongs in springdoc-openapi annotations (`@Operation`, `@ApiResponse`, `@Schema`) on the same method — those annotations *are* the API spec (see below), so don't also write a prose description of the payload in the Javadoc.
- **Services (`@Service`):** Javadoc the business rule being enforced, especially anything a code reviewer couldn't infer from the method name alone (e.g., why a validation exists, why an operation must happen inside a single transaction).
- **Entities (`@Entity`):** Javadoc the class only if it needs context beyond "this is a database row for X." Comment individual fields when they aren't self-explanatory (a flag, a denormalized/cached column, a nullable field with a specific meaning for null vs. absent).
- **Repositories (`@Repository`):** Javadoc custom `@Query` methods to explain *why* a custom query exists instead of a derived method name — usually a performance reason or a query Spring Data can't derive.
- **Configuration classes:** Javadoc on the class explaining what it wires up and why it's separated out, if not obvious from the name.

## Deciding what needs a spec doc vs. just comments

| Change touches... | Action |
|---|---|
| Internal refactor, no behavior/signature change | Inline comments only, if any |
| New/changed public class or method in a shared package | Javadoc block |
| New/changed REST endpoint or request/response shape | Javadoc on the handler **and** springdoc-openapi annotations (single source of truth — don't also hand-maintain a duplicate OpenAPI description) |
| New/changed JPA entity or field | Javadoc/comment on the entity/field |
| Queue, retry, locking, or scheduling logic | Javadoc with `@implNote` stating the invariant being protected |
| A decision with real tradeoffs (library choice, schema design, concurrency strategy) | ADR — see `references/spec-templates.md` |
| New feature/module before it's built | PRD — see `references/spec-templates.md` |
| Setup steps, scripts, env vars, or `application.yml` profiles changed | README update |

When in doubt, under-document rather than pad — a spec doc nobody asked for is as much of a maintenance burden as a missing one.

## Spec doc templates

ADR, PRD, API spec, and README skeletons are in `references/spec-templates.md`. Use them as-is; don't invent a new structure per project.
