package com.batchprompt.prompts.core;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.prompts.core.model.Prompt;
import com.batchprompt.prompts.core.repository.PromptRepository;
import com.batchprompt.prompts.model.dto.PromptJobInfoDto;
import com.batchprompt.jobs.client.JobClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptRepository promptRepository;
    private final JobClient jobClient;

    public List<Prompt> getAllPrompts() {
        return promptRepository.findAll();
    }

    public Optional<Prompt> getPromptById(UUID promptUuid) {
        return promptRepository.findById(promptUuid);
    }

    public List<Prompt> getPromptsByUserId(String userId) {
        return promptRepository.findByUserId(userId);
    }

    public List<Prompt> searchPromptsByName(String name, String userId) {
        return promptRepository.findByNameAndUserIdContainingIgnoreCase(name, userId);
    }

    @Transactional
    public Prompt createPrompt(Prompt prompt) {
        prompt.setPromptUuid(UUID.randomUUID());
        LocalDateTime now = LocalDateTime.now();
        prompt.setCreateTimestamp(now);
        prompt.setUpdateTimestamp(now);
        return promptRepository.save(prompt);
    }

    @Transactional
    public Optional<Prompt> updatePrompt(UUID promptUuid, Prompt promptDetails) {
        return promptRepository.findById(promptUuid)
                .map(existingPrompt -> {
                    existingPrompt.setName(promptDetails.getName());
                    existingPrompt.setDescription(promptDetails.getDescription());
                    existingPrompt.setPromptText(promptDetails.getPromptText());
                    existingPrompt.setOutputMethod(promptDetails.getOutputMethod());
                    existingPrompt.setResponseTextColumnName(promptDetails.getResponseTextColumnName());
                    existingPrompt.setResponseJsonSchema(promptDetails.getResponseJsonSchema());
                    existingPrompt.setUpdateTimestamp(LocalDateTime.now());
                    return promptRepository.save(existingPrompt);
                });
    }

    @Transactional
    public boolean deletePrompt(UUID promptUuid) {
        return promptRepository.findById(promptUuid)
                .map(prompt -> {
                    try {
                        // Check if there are any active jobs for this prompt
                        boolean hasActiveJobs = jobClient.hasActiveJobs(null, promptUuid, null);
                        if (hasActiveJobs) {
                            throw new IllegalStateException("Cannot delete prompt while it has active jobs. Please cancel or wait for jobs to complete.");
                        }
                        
                        promptRepository.delete(prompt);
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to delete prompt: " + e.getMessage(), e);
                    }
                })
                .orElse(false);
    }

    /**
     * Get prompts for a specific user with pagination and sorting
     * 
     * @param userId The user ID to retrieve prompts for
     * @param pageable Pageable object containing pagination and sorting information
     * @return Page of prompts for the user
     */
    public Page<Prompt> getPromptsByUserIdPaginated(String userId, Pageable pageable) {
        return promptRepository.findByUserId(userId, pageable);
    }

    /**
     * Copy a prompt from one user to another
     * 
     * @param sourcePrompt The prompt to copy
     * @param targetUserId The user ID to copy the prompt to
     * @return The newly created prompt
     */
    @Transactional
    public Prompt copyPrompt(Prompt sourcePrompt, String targetUserId) {
        // Create a new prompt with a new UUID
        UUID newPromptUuid = UUID.randomUUID();
        
        // Create a copy with the target user ID
        Prompt newPrompt = Prompt.builder()
                .promptUuid(newPromptUuid)
                .userId(targetUserId)
                .name(sourcePrompt.getName() + " (Copy)")
                .description(sourcePrompt.getDescription())
                .promptText(sourcePrompt.getPromptText())
                .outputMethod(sourcePrompt.getOutputMethod())
                .responseTextColumnName(sourcePrompt.getResponseTextColumnName())
                .responseJsonSchema(sourcePrompt.getResponseJsonSchema())
                .createTimestamp(LocalDateTime.now())
                .updateTimestamp(LocalDateTime.now())
                .build();
                
        // Save the new prompt
        return promptRepository.save(newPrompt);
    }

    public Prompt updateJobInfo(UUID promptUuid, PromptJobInfoDto promptJobInfo) {
        Prompt prompt = promptRepository.findById(promptUuid)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found with UUID: " + promptUuid));

        if (promptJobInfo.getJobRunCountIncrement() != null) {
            Integer currentJobRunCount = prompt.getJobRunCount() != null ? prompt.getJobRunCount() : 0;
            prompt.setJobRunCount(currentJobRunCount + promptJobInfo.getJobRunCountIncrement());
        }
        if (promptJobInfo.getLastJobRunTimestamp() != null && (prompt.getLastJobRunTimestamp() == null || promptJobInfo.getLastJobRunTimestamp().isAfter(prompt.getLastJobRunTimestamp()))) {
            prompt.setLastJobRunTimestamp(promptJobInfo.getLastJobRunTimestamp());
        }
        prompt.setUpdateTimestamp(LocalDateTime.now());
        return promptRepository.save(prompt);
    }
}