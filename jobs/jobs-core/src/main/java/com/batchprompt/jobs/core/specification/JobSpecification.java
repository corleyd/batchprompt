package com.batchprompt.jobs.core.specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.model.JobStatus;

import jakarta.persistence.criteria.Predicate;

/**
 * Specifications for Job entity dynamic queries
 */
public class JobSpecification {
    
    /**
     * Creates a specification that filters jobs based on the provided criteria
     * 
     * @param userId Optional user ID to filter by
     * @param modelId Optional model ID to filter by
     * @param status Optional job status to filter by
     * @return A Specification for the provided filter criteria
     */
    public static Specification<Job> withFilters(String userId, String modelId, UUID promptUuid, JobStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            if (StringUtils.hasText(userId)) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }
            
            if (StringUtils.hasText(modelId)) {
                predicates.add(criteriaBuilder.equal(root.get("modelId"), modelId));
            }
            
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (promptUuid != null) {
                predicates.add(criteriaBuilder.equal(root.get("promptUuid"), promptUuid));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}