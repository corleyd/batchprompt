package com.batchprompt.jobs.core.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.batchprompt.jobs.model.TaskStatus;

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
    
   
    @Id
    @Column(name = "job_task_uuid")
    private UUID jobTaskUuid;
    
    @Column(name = "job_uuid", nullable = false)
    private UUID jobUuid;
    
    @Column(name = "file_record_uuid", nullable = false)
    private UUID fileRecordUuid;

    @Column(name = "record_number", nullable = false)
    private Integer recordNumber;
    
    @Column(name = "model_name", nullable = false)
    private String modelName;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TaskStatus status;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "response_text")
    private String responseText;

    @Column(name = "begin_timestamp")
    private LocalDateTime beginTimestamp;
    
    @Column(name = "end_timestamp")
    private LocalDateTime endTimestamp;
}