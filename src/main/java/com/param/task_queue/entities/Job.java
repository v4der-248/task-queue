package com.param.task_queue.entities;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single queued background task; one row per job. {@link #jobStatus},
 * {@link #retryCount}, and {@link #eligibleToPickAfter} together are meant
 * to drive the job lifecycle (see ARCHITECTURE.md), though today only the
 * initial PENDING creation is implemented — nothing in this codebase yet
 * reads these fields back to run, retry, or dead-letter a job.
 */
@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    // Destination the (not yet implemented) worker calls to hand this job off
    // for processing — a webhook/callback model rather than an in-process handler.
    private String consumerUri;

    // Intentionally untyped (raw JSON as text): keeps the jobs table schema
    // independent of any individual JobType's payload shape. Each job type
    // owns its own payload contract; the consumer at consumerUri parses it.
    private String payload;

    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus;

    // Number of processing attempts so far. Set to 0 on creation; nothing in
    // this codebase currently increments it (that's the retry logic's job,
    // once a worker exists).
    private Integer retryCount;

    // Dual-purpose timestamp: at creation it's an initial-delay gate (a job
    // isn't eligible for pickup until this time), and after a FAILED attempt
    // it's intended to double as the retry-backoff timestamp (pushed forward)
    // instead of using separate scheduledAt/retryAfter fields. Only the
    // creation-time use is implemented today.
    private Instant eligibleToPickAfter;

    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = Instant.now();
    }
}
