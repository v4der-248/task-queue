package com.param.task_queue.services;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.param.task_queue.entities.Job;
import com.param.task_queue.entities.JobStatus;
import com.param.task_queue.entities.JobType;
import com.param.task_queue.repositories.JobRepository;

@Service
public class JobService {
    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

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
