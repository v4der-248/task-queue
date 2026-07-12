package com.param.task_queue.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.param.task_queue.services.JobService;

@RestController
@RequestMapping("/jobs")
public class JobController {
    
    private final JobService jobService;

    public JobController(JobService jobService){
        this.jobService = jobService;
    }
}
