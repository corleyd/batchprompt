package com.batchprompt.jobs.core.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.core.model.JobOutputField;

@Repository
public interface JobOutputFieldRepository extends JpaRepository<JobOutputField, UUID> {
    List<JobOutputField> findByJobJobUuidOrderByFieldOrder(UUID jobUuid);
}