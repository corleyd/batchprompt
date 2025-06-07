package com.batchprompt.prompts.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CrossOrigin;
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
import com.batchprompt.prompts.model.dto.PromptJobInfoDto;

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
        @AuthenticationPrincipal Jwt jwt) 
    {

        // Check if the user has permission to access this prompt
        Prompt existingPrompt = promptService.getPromptById(promptUuid)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found with UUID: " + promptUuid));

        if (!serviceAuthenticationService.canAccessUserData(jwt, existingPrompt.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return promptService.getPromptById(promptUuid)
                .map(promptMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user")
    public ResponseEntity<List<PromptDto>> getPromptsByUserId(
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(promptMapper.toDtoList(promptService.getPromptsByUserId(jwt.getSubject())));
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
    public ResponseEntity<List<PromptDto>> searchPromptsByName(
        @RequestParam String name,
        @AuthenticationPrincipal Jwt jwt
    ) {
        List<Prompt> prompts = promptService.searchPromptsByName(name, jwt.getSubject());
        return ResponseEntity.ok(promptMapper.toDtoList(prompts));
    }

    @PostMapping
    public ResponseEntity<PromptDto> createPrompt(
        @RequestBody PromptDto promptDto,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Prompt prompt = promptMapper.toEntity(promptDto);
        prompt.setUserId(jwt.getSubject());
        Prompt savedPrompt = promptService.createPrompt(prompt);
        return ResponseEntity.status(HttpStatus.CREATED).body(promptMapper.toDto(savedPrompt));
    }

    @PutMapping("/{promptUuid}")
    public ResponseEntity<PromptDto> updatePrompt(
        @PathVariable UUID promptUuid, 
        @RequestBody PromptDto promptDto,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Prompt existingPrompt = promptService.getPromptById(promptUuid)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found with UUID: " + promptUuid));
        
                // Check if the user is allowed to update this prompt
        if (!serviceAuthenticationService.canAccessUserData(jwt, existingPrompt.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Prompt prompt = promptMapper.toEntity(promptDto);
        return promptService.updatePrompt(promptUuid, prompt)
                .map(promptMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{promptUuid}")
    public ResponseEntity<?> deletePrompt(
        @PathVariable UUID promptUuid,
        @AuthenticationPrincipal Jwt jwt
    ) {
        try {
            if (!serviceAuthenticationService.canAccessUserData(jwt, jwt.getSubject())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            boolean deleted = promptService.deletePrompt(promptUuid);
            return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
            
        } catch (IllegalStateException e) {
            // Return a 409 Conflict status with the error message for business rule violations
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            // Return a 500 Internal Server Error for other exceptions
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while deleting the prompt");
        }
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
    
    /**
     * Admin endpoint to copy a prompt from one user to another
     */
    @PostMapping("/admin/copy")
    public ResponseEntity<?> copyPromptForAdmin(
            @RequestParam UUID sourcePromptUuid,
            @RequestParam String targetUserId,
            @AuthenticationPrincipal Jwt jwt) {
        
        // Verify that the requester is an admin
        if (!serviceAuthenticationService.isAdminUser(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return promptService.getPromptById(sourcePromptUuid)
                .map(prompt -> {
                    Prompt copiedPrompt = promptService.copyPrompt(prompt, targetUserId);
                    return ResponseEntity.status(HttpStatus.CREATED).body(promptMapper.toDto(copiedPrompt));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{promptUuid}/job-info") 
    public ResponseEntity<PromptDto> updateJobInfo(
        @RequestBody PromptJobInfoDto promptJobInfo,
        @PathVariable UUID promptUuid,
        @AuthenticationPrincipal Jwt jwt) {
        
        // Check if the user has permission to access this prompt
        Prompt existingPrompt = promptService.getPromptById(promptUuid)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found with UUID: " + promptUuid));

        if (!serviceAuthenticationService.canAccessUserData(jwt, existingPrompt.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PromptDto promptDto =promptMapper.toDto(promptService.updateJobInfo(promptUuid, promptJobInfo));
        if (promptDto == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(promptDto);
    }
}