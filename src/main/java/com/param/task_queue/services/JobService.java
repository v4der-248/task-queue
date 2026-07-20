package com.param.task_queue.services;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.param.task_queue.entities.Job;
import com.param.task_queue.entities.JobStatus;
import com.param.task_queue.entities.JobType;
import com.param.task_queue.repositories.JobRepository;

/**
 * Business logic for job creation and (eventually) lifecycle transitions.
 * {@link #createJob} is the only transition implemented so far; retry,
 * backoff, and dead-lettering logic do not exist yet.
 */
@Service
public class JobService {
    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    /**
     * Builds and persists a new job, always starting it as {@code PENDING}
     * with {@code retryCount = 0} and {@code eligibleToPickAfter = now()} —
     * there is currently no way to schedule a job for a later pickup time.
     */
    public Job createJob(JobType jobType, String consumerUri, String payload){

        Job job = Job.builder()
            .consumerUri(consumerUri)
            .jobType(jobType)
            .payload(payload)
            .retryCount(0)
            .jobStatus(JobStatus.PENDING)
            .eligibleToPickAfter(Instant.now())
            .build();

        return jobRepository.save(job);
    }
}
