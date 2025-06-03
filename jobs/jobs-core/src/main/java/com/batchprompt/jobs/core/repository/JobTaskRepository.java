package com.batchprompt.jobs.core.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.repository.dto.TaskStatusCount;

@Repository
public interface JobTaskRepository extends JpaRepository<JobTask, UUID> {
    List<JobTask> findByJobUuid(UUID jobUuid);
    Page<JobTask> findByJobUuid(UUID jobUuid, Pageable pageable);
    void deleteByJobUuid(UUID jobUuid);
    
    /**
     * Count tasks by status for a given job in a single query
     * 
     * @param jobUuid The job UUID to count tasks for
     * @return List of TaskStatusCount objects containing the status and count
     */
    @Query("SELECT new com.batchprompt.jobs.core.repository.dto.TaskStatusCount(t.status, COUNT(t), SUM(t.creditUsage)) " +
           "FROM JobTask t WHERE t.jobUuid = :jobUuid GROUP BY t.status")
    List<TaskStatusCount> countTasksByStatus(@Param("jobUuid") UUID jobUuid);
    
    /**
     * Count the total number of tasks for a job
     * 
     * @param jobUuid The job UUID to count tasks for
     * @return The total count of tasks
     */
    @Query("SELECT COUNT(t) FROM JobTask t WHERE t.jobUuid = :jobUuid")
    Long countByJobUuid(@Param("jobUuid") UUID jobUuid);
}