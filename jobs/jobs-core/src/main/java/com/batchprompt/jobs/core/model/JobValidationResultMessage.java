package com.batchprompt.jobs.core.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a validation message for a job or job task
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "job_validation_message")
public class JobValidationResultMessage {

    @Id
    @Column(name = "job_validation_result_message_uuid")
    private UUID jobValidationMessageUuid;
    
    @Column(name = "job_uuid", nullable = false)
    private UUID jobUuid;
    
    @Column(name = "job_task_uuid")
    private UUID jobTaskUuid;
    
    @Column(name = "record_number")
    private Integer recordNumber;
    
    @Column(name = "field_name")
    private String fieldName;
    
    @Column(name = "message", nullable = false)
    private String message;
}
