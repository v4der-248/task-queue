package com.param.task_queue.entities;

/**
 * Discriminates what kind of work a {@link Job} represents, so a worker
 * (not yet implemented) knows which handler to invoke — the queue itself
 * does not interpret this value beyond storing it.
 */
public enum JobType {
    SEND_EMAIL,
    GENERATE_REPORT,
    PROCESS_IMAGE,
    TRIGGER_WEBHOOK,
    DATA_CLEANUP
}
