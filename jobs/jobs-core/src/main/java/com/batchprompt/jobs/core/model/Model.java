package com.batchprompt.jobs.core.model;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.JsonNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a model available for use in the system
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "model")
public class Model {
    
    @Id
    @Column(name = "model_id", nullable = false)
    private String modelId;
    
    @Column(name = "display_name", nullable = false)
    private String displayName;
    
    @Column(name = "model_provider_id", nullable = false)
    private String modelProviderName;
    
    @Column(name = "model_provider_model_id", nullable = false)
    private String modelProviderModelId;

    @Column(name = "simulate_structured_output", nullable = false)
    private boolean simulateStructuredOutput;

    @Column(name = "model_provider_properties", columnDefinition = "jsonb")
    @Type(JsonType.class)
    private JsonNode modelProviderProperties;
    
    @Column(name = "model_provider_display_order")
    private Integer modelProviderDisplayOrder;

    @Column(name = "task_queue_name")
    private String taskQueueName;

    @Column(name = "required_role")
    private String requiredRole;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_provider_id", referencedColumnName = "model_provider_id", insertable = false, updatable = false)
    private ModelProviderEntity provider;
}