package com.batchprompt.prompts.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.common.services.ServiceAuthenticationService;
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
    private final ServiceAuthenticationService serviceAuthenticationService;

    @GetMapping
    public ResponseEntity<List<PromptDto>> getAllPrompts() {
        List<Prompt> prompts = promptService.getAllPrompts();
        return ResponseEntity.ok(promptMapper.toDtoList(prompts));
    }

    @GetMapping("/{promptUuid}")
    public ResponseEntity<PromptDto> getPromptById(
        @PathVariable UUID promptUuid,
        @AuthenticationPrincipal Jwt jwt) {
        return promptService.getPromptById(promptUuid)
                .map(promptMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<PromptDto>> getPromptsByUserId(
        @PathVariable String userId,
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createTimestamp") String sort,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        if (!serviceAuthenticationService.canAccessUserData(jwt, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Create Pageable object with sorting
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, sortDirection, sort);
        
        // Get paginated prompts
        Page<Prompt> promptsPage = promptService.getPromptsByUserIdPaginated(userId, pageable);
        
        // Convert to DTOs
        Page<PromptDto> promptDtosPage = promptsPage.map(promptMapper::toDto);
        
        return ResponseEntity.ok(promptDtosPage);
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

    /**
     * Admin endpoint to get prompts for a specific user
     */
    @GetMapping("/admin/{userId}")
    public ResponseEntity<List<Prompt>> getPromptsByUserIdForAdmin(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {
        
        // Verify that the requester is an admin
        if (!serviceAuthenticationService.isAdminUser(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<Prompt> prompts = promptService.getPromptsByUserId(userId);
        return ResponseEntity.ok(prompts);
    }
}