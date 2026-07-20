package com.param.task_queue.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.param.task_queue.entities.Job;

/**
 * Persistence gateway for {@link Job} rows. No custom query methods yet —
 * all access is inherited CRUD. The eventual polling worker's query
 * ({@code WHERE job_status = 'PENDING' AND eligible_to_pick_after <= now()})
 * is expected to live here once it's implemented.
 */
public interface JobRepository extends JpaRepository<Job, UUID> {

}
