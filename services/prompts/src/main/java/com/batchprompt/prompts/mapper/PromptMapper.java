package com.batchprompt.prompts.mapper;

import com.batchprompt.prompts.dto.PromptDto;
import com.batchprompt.prompts.model.Prompt;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptMapper {

    public PromptDto toDto(Prompt prompt) {
        String outputSchema = prompt.getOutputSchema() != null ? prompt.getOutputSchema().toString() : null;

        return PromptDto.builder()
                .promptUuid(prompt.getPromptUuid())
                .userId(prompt.getUserId())
                .name(prompt.getName())
                .description(prompt.getDescription())
                .promptText(prompt.getPromptText())
                .outputSchema(outputSchema)
                .createTimestamp(prompt.getCreateTimestamp())
                .updateTimestamp(prompt.getUpdateTimestamp())
                .build();
    }

    public Prompt toEntity(PromptDto promptDto) {
        JsonNode outputSchema = null;
        if (promptDto.getOutputSchema() != null) {
            try {
                outputSchema = new ObjectMapper().readTree(promptDto.getOutputSchema());
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse output schema", e);
            }
        }
        return Prompt.builder()
                .promptUuid(promptDto.getPromptUuid())
                .userId(promptDto.getUserId())
                .name(promptDto.getName())
                .description(promptDto.getDescription())
                .promptText(promptDto.getPromptText())
                .outputSchema(outputSchema)
                .createTimestamp(promptDto.getCreateTimestamp())
                .updateTimestamp(promptDto.getUpdateTimestamp())
                .build();
    }

    public List<PromptDto> toDtoList(List<Prompt> prompts) {
        return prompts.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}