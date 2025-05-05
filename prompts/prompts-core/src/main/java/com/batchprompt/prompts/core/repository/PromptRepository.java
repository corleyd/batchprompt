package com.batchprompt.prompts.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batchprompt.prompts.core.model.Prompt;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, UUID> {
    List<Prompt> findByUserId(String userId);
    Page<Prompt> findByUserId(String userId, Pageable pageable);
    List<Prompt> findByNameContainingIgnoreCase(String name);
}