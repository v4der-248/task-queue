package com.param.task_queue.entities;

/**
 * Position of a {@link Job} in its lifecycle. Only the transition into
 * {@code PENDING} (on job creation, in {@code JobService}) is implemented
 * today — {@code RUNNING}, {@code COMPLETED}, {@code FAILED}, and {@code DEAD}
 * describe the intended worker-driven state machine (see ARCHITECTURE.md)
 * but nothing currently transitions a job into any of them.
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    DEAD
}
