package com.batchprompt.jobs.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a model provider in the system
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "model_provider")
public class ModelProviderEntity {
    
    @Id
    @Column(name = "model_provider_id", nullable = false, unique = true)
    private String modelProviderId;
    
    @Column(name = "display_name", nullable = false)
    private String displayName;

}