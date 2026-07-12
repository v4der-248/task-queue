package com.param.task_queue.dto;

import com.param.task_queue.entities.JobType;

import lombok.Data;

@Data
public class CreateJobRequestDTO {
    private JobType jobType;

    private String consumerUri;

    private String payload;
}