package com.batchprompt.jobs.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.core.model.ModelProviderEntity;

/**
 * Repository for model provider data
 */
@Repository
public interface ModelProviderRepository extends JpaRepository<ModelProviderEntity, String> {

}