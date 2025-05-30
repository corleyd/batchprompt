package com.batchprompt.jobs.core.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.batchprompt.jobs.model.JobStatus;

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
    
    @Column(name = "model_id", nullable = false)
    private String modelId;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    
    @Column(name = "task_count", nullable = false)
    private Integer taskCount;
    
    @Column(name = "completed_task_count", nullable = false)
    private Integer completedTaskCount;

    @Column(name = "result_file_uuid")
    private UUID resultFileUuid;
    
    @Column(name = "max_tokens")
    private Integer maxTokens;
    
    @Column(name = "temperature")
    private Double temperature;
    
    @Column(name = "max_records")
    private Integer maxRecords;
    
    @Column(name = "start_record_number")
    private Integer startRecordNumber;
    
    @Column(name = "credit_usage")
    private Double creditUsage;

    @Column(name = "cost_estimate")
    private Double costEstimate;
    
    @Column(name = "credit_estimate")
    private Double creditEstimate;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "error_message")
    private String errorMessage;

    @Version
    @Column(name = "version")
    private Integer version;
}