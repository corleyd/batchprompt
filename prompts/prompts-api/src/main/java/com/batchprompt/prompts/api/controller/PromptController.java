package com.batchprompt.prompts.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.prompts.core.PromptService;
import com.batchprompt.prompts.core.mapper.PromptMapper;
import com.batchprompt.prompts.core.model.Prompt;
import com.batchprompt.prompts.model.dto.PromptDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;
    private final PromptMapper promptMapper;

    @GetMapping
    public ResponseEntity<List<PromptDto>> getAllPrompts() {
        List<Prompt> prompts = promptService.getAllPrompts();
        return ResponseEntity.ok(promptMapper.toDtoList(prompts));
    }

    @GetMapping("/{promptUuid}")
    public ResponseEntity<PromptDto> getPromptById(@PathVariable UUID promptUuid) {
        return promptService.getPromptById(promptUuid)
                .map(promptMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PromptDto>> getPromptsByUserId(@PathVariable String userId) {
        List<Prompt> prompts = promptService.getPromptsByUserId(userId);
        return ResponseEntity.ok(promptMapper.toDtoList(prompts));
    }

    @GetMapping("/search")
    public ResponseEntity<List<PromptDto>> searchPromptsByName(@RequestParam String name) {
        List<Prompt> prompts = promptService.searchPromptsByName(name);
        return ResponseEntity.ok(promptMapper.toDtoList(prompts));
    }

    @PostMapping
    public ResponseEntity<PromptDto> createPrompt(@RequestBody PromptDto promptDto) {
        Prompt prompt = promptMapper.toEntity(promptDto);
        Prompt savedPrompt = promptService.createPrompt(prompt);
        return ResponseEntity.status(HttpStatus.CREATED).body(promptMapper.toDto(savedPrompt));
    }

    @PutMapping("/{promptUuid}")
    public ResponseEntity<PromptDto> updatePrompt(@PathVariable UUID promptUuid, @RequestBody PromptDto promptDto) {
        Prompt prompt = promptMapper.toEntity(promptDto);
        return promptService.updatePrompt(promptUuid, prompt)
                .map(promptMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{promptUuid}")
    public ResponseEntity<Void> deletePrompt(@PathVariable UUID promptUuid) {
        boolean deleted = promptService.deletePrompt(promptUuid);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}