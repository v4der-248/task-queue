package com.param.task_queue.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.param.task_queue.entities.Job;

public interface JobRepository extends JpaRepository<Job, UUID> {
    
}
