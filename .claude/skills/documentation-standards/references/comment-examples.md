# Comment examples: good vs. bad

## Restating vs. explaining why

```java
// Bad — restates the code
// Loop through jobs and process each one
jobs.forEach(this::processJob);

// Good — explains a non-obvious constraint
// Must process in submission order: downstream consumers assume
// monotonically increasing sequence numbers per partition.
jobs.forEach(this::processJob);
```

## Padding a Javadoc block vs. saying something useful

```java
// Bad — pure noise, adds nothing the signature doesn't already say
/**
 * Gets the job by id.
 * @param id the job id
 * @return the job
 */
public Job getJob(String id) { ... }

// Good — the signature is obvious; the caveat isn't
/**
 * @implNote Returns a job even if it's in a terminal state (COMPLETED,
 *     FAILED) — the admin dashboard relies on this to show job history,
 *     not just active jobs.
 */
public Job getJob(String id) { ... }
```

## Commented-out code vs. trusting git

```java
// Bad
// Job oldJob = legacyJobLookup(id);
Job job = jobRepository.findById(id);

// Good — just delete it. `git log -p` has the old version if anyone needs it.
Job job = jobRepository.findById(id);
```

## Concurrency/ordering comments (this is where most of your value is in a queue system)

```java
// Good — a concurrency invariant that isn't derivable from the code itself
/**
 * @implNote This method must run inside the same transaction as the
 *     status update in {@code JobStatusService.markInProgress}. Splitting
 *     them into separate transactions reintroduces a window where two
 *     workers can both see status=PENDING and double-claim the job.
 */
@Transactional
public void claimAndStart(Job job) { ... }
```

## Stale comments

If you touch a method and its existing comment no longer matches what the code does, fix the comment in the same edit — don't leave it for later. A comment that lies is worse than no comment.
