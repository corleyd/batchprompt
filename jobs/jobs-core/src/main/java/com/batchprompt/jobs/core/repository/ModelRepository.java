package com.batchprompt.jobs.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.jobs.core.model.Model;

/**
 * Repository for model data
 */
@Repository
public interface ModelRepository extends JpaRepository<Model, String> {
    
}