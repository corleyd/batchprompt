package com.batchprompt.jobs.core.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.core.model.JobTask;

@Repository
public interface JobTaskRepository extends JpaRepository<JobTask, UUID> {
    List<JobTask> findByJobUuid(UUID jobUuid);
    Page<JobTask> findByJobUuid(UUID jobUuid, Pageable pageable);
    void deleteByJobUuid(UUID jobUuid);
}