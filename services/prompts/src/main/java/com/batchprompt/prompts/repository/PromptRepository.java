package com.batchprompt.prompts.repository;

import com.batchprompt.prompts.model.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, UUID> {
    List<Prompt> findByUserId(String userId);
    List<Prompt> findByNameContainingIgnoreCase(String name);
}