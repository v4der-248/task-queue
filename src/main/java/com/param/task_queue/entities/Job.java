package com.param.task_queue.entities;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Data;

@Entity
@Data
@Builder
public class Job {
    
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private JobType jobType;

    private String consumerUri;

    private String payload;

    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus;

    private Integer retryCount;

    private Instant eligibleToPickAfter;
    
    private Instant createdAt;

    private Instant updatedAt;
}
