package com.batchprompt.jobs.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "job")
public class Job {
    
    public enum Status {
        SUBMITTED, PROCESSING, PENDING_OUTPUT, GENERATING_OUTPUT, COMPLETED, COMPLETED_WITH_ERRORS, FAILED
    }
    
    @Id
    @Column(name = "job_uuid")
    private UUID jobUuid;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "file_uuid", nullable = false)
    private UUID fileUuid;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "prompt_uuid", nullable = false)
    private UUID promptUuid;
    
    @Column(name = "model_name", nullable = false)
    private String modelName;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(name = "task_count", nullable = false)
    private Integer taskCount;
    
    @Column(name = "completed_task_count", nullable = false)
    private Integer completedTaskCount;

    @Column(name = "result_file_uuid")
    private UUID resultFileUuid;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Integer version;
}