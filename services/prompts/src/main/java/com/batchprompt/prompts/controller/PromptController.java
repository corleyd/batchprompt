package com.batchprompt.prompts.controller;

import com.batchprompt.prompts.dto.PromptDto;
import com.batchprompt.prompts.mapper.PromptMapper;
import com.batchprompt.prompts.model.Prompt;
import com.batchprompt.prompts.service.PromptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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