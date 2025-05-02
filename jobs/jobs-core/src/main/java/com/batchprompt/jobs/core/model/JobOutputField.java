package com.batchprompt.jobs.core.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "job_output_field")
public class JobOutputField {

    @Id
    @Column(name = "job_output_field_uuid")
    private UUID jobOutputFieldUuid;
    
    @ManyToOne
    @JoinColumn(name = "job_uuid", nullable = false)
    private Job job;
    
    @Column(name = "field_order", nullable = false)
    private Integer fieldOrder;
    
    @Column(name = "field_uuid", nullable = false)
    private UUID fieldUuid;
}