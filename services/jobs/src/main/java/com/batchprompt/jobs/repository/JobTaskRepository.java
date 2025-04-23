package com.batchprompt.jobs.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.model.JobTask;

@Repository
public interface JobTaskRepository extends JpaRepository<JobTask, UUID> {
    List<JobTask> findByJobUuid(UUID jobUuid);
    void deleteByJobUuid(UUID jobUuid);
}