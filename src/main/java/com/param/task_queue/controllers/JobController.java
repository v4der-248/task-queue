package com.param.task_queue.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.param.task_queue.dto.CreateJobRequestDTO;
import com.param.task_queue.dto.CreateJobResponseDTO;
import com.param.task_queue.entities.Job;
import com.param.task_queue.services.JobService;

@RestController
@RequestMapping("/jobs")
public class JobController {
    
    private final JobService jobService;

    public JobController(JobService jobService){
        this.jobService = jobService;
    }

    @PostMapping("/create")
    public ResponseEntity<CreateJobResponseDTO> createJob(@RequestBody CreateJobRequestDTO createJobRequestDTO){
        Job job = jobService.createJob(createJobRequestDTO.getJobType(), createJobRequestDTO.getConsumerUri(), createJobRequestDTO.getPayload());

        CreateJobResponseDTO response = CreateJobResponseDTO.builder()
            .id(job.getId())
            .jobStatus(job.getJobStatus())
            .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
