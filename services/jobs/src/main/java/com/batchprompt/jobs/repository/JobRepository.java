package com.batchprompt.jobs.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.model.Job;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByUserId(String userId);
    List<Job> findByFileUuid(UUID fileUuid);
}