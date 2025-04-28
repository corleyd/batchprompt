package com.batchprompt.prompts.core;

import com.batchprompt.prompts.core.model.Prompt;
import com.batchprompt.prompts.core.repository.PromptRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PromptService {

    private final PromptRepository promptRepository;

    public List<Prompt> getAllPrompts() {
        return promptRepository.findAll();
    }

    public Optional<Prompt> getPromptById(UUID promptUuid) {
        return promptRepository.findById(promptUuid);
    }

    public List<Prompt> getPromptsByUserId(String userId) {
        return promptRepository.findByUserId(userId);
    }

    public List<Prompt> searchPromptsByName(String name) {
        return promptRepository.findByNameContainingIgnoreCase(name);
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
                    existingPrompt.setOutputSchema(promptDetails.getOutputSchema());
                    existingPrompt.setUpdateTimestamp(LocalDateTime.now());
                    return promptRepository.save(existingPrompt);
                });
    }

    @Transactional
    public boolean deletePrompt(UUID promptUuid) {
        return promptRepository.findById(promptUuid)
                .map(prompt -> {
                    promptRepository.delete(prompt);
                    return true;
                })
                .orElse(false);
    }
}