package com.batchprompt.jobs.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "job_task")
public class JobTask {
    
    public enum Status {
        Submitted, Processing, Completed, Failed
    }
    
    @Id
    @Column(name = "job_task_uuid")
    private UUID jobTaskUuid;
    
    @Column(name = "job_uuid", nullable = false)
    private UUID jobUuid;
    
    @Column(name = "file_record_uuid", nullable = false)
    private UUID fileRecordUuid;
    
    @Column(name = "model_name", nullable = false)
    private String modelName;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "response_text")
    private String responseText;

    @Column(name = "begin_timestamp")
    private LocalDateTime beginTimestamp;
    
    @Column(name = "end_timestamp")
    private LocalDateTime endTimestamp;
}