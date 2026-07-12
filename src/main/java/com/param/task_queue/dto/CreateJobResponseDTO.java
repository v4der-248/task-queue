package com.param.task_queue.dto;

import java.util.UUID;

import com.param.task_queue.entities.JobStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateJobResponseDTO {
    private UUID id;

    private JobStatus jobStatus;
}
