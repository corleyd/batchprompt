package com.batchprompt.jobs.api;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batchprompt.common.services.ServiceAuthenticationService;
import com.batchprompt.jobs.core.mapper.JobMapper;
import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.service.JobService;
import com.batchprompt.jobs.core.service.ModelService;
import com.batchprompt.jobs.model.JobStatus;
import com.batchprompt.jobs.model.dto.JobDto;
import com.batchprompt.jobs.model.dto.JobDefinitionDto;
import com.batchprompt.jobs.model.dto.JobTaskDto;
import com.batchprompt.jobs.model.dto.ModelDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobsController {

    private final JobService jobService;
    private final ModelService modelService;
    private final JobMapper jobMapper;
    private final ServiceAuthenticationService serviceAuthenticationService;

    @GetMapping
    public ResponseEntity<?> getAllJobs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String modelId,
            @RequestParam(required = false) UUID promptUuid,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction) {

        
        // Create pageable object with sorting
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // Get paginated and filtered jobs
        Page<Job> jobPage = jobService.getJobsPaginated(userId, modelId, promptUuid, status, pageable);
        
        // Convert to DTOs and preserve pagination metadata
        Page<JobDto> jobDtoPage = jobPage.map(job -> jobMapper.toDto(job));
        
        return ResponseEntity.ok(jobDtoPage);
    }

    @GetMapping("/{jobUuid}")
    public ResponseEntity<JobDto> getJobById(
        @PathVariable UUID jobUuid,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Job job = getJobIfAllowed(jobUuid, jwt);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(jobMapper.toDto(job));
    }

    @PostMapping("/{jobUuid}/submit")
    public ResponseEntity<JobDto> submitJob(
        @PathVariable UUID jobUuid,
        @AuthenticationPrincipal Jwt jwt
    ) {
        if (getJobIfAllowed(jobUuid, jwt) == null) {
            return ResponseEntity.notFound().build();
        }
        // Submit the job for processing
        Job job = jobService.submitJob(jobUuid);
        return ResponseEntity.ok(jobMapper.toDto(job));
    }

    @PostMapping("/{jobUuid}/cancel")
    public ResponseEntity<JobDto> cancelJob(
        @PathVariable UUID jobUuid,
        @AuthenticationPrincipal Jwt jwt
    ) {
        if (getJobIfAllowed(jobUuid, jwt) == null) {
            return ResponseEntity.notFound().build();
        }
        Job job = jobService.cancelJob(jobUuid);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(jobMapper.toDto(job));
    }    

    @PostMapping("/{jobUuid}/continue")
    public ResponseEntity<JobDto> continueJob(
        @PathVariable UUID jobUuid,
        @AuthenticationPrincipal Jwt jwt
    ) {
        if (getJobIfAllowed(jobUuid, jwt) == null) {
            return ResponseEntity.notFound().build();
        }
        // Submit the job for processing
        Job job = jobService.continueJob(jobUuid);

        return ResponseEntity.ok(jobMapper.toDto(job));
    }

    @DeleteMapping("/{jobUuid}")
    public ResponseEntity<Void> deleteJob(
        @PathVariable UUID jobUuid,
        @AuthenticationPrincipal Jwt jwt
    ) {
        if (getJobIfAllowed(jobUuid, jwt) == null) {
            return ResponseEntity.notFound().build();
        }
        
        jobService.deleteJob(jobUuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user")
    public ResponseEntity<?> getJobsByUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID promptUuid,
            @RequestParam(required = false) JobStatus status,            
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction) 
    {
        String userId = jwt.getSubject();
        return getJobsByUserId(userId, promptUuid, status, jwt, page, size, sort, direction);
    }

    /**
     * Admin endpoint to get jobs for a specific user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getJobsByUserId(
            @PathVariable String userId,
            @RequestParam(required = false) UUID promptUuid,
            @RequestParam(required = false) JobStatus status,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        // Verify that the requester is an admin
        if (serviceAuthenticationService.canAccessUserData(jwt, userId) == false) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Create Pageable object
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(
            page, size, Sort.by(sortDirection, sort)
        );
        
        Page<Job> jobsPage = jobService.getJobsPaginated(userId, null, promptUuid, status, pageable);

        Page<JobDto> dtoPage = jobMapper.toDtoPage(jobsPage);
        
        return ResponseEntity.ok(dtoPage);
    }    
        

    @GetMapping("/{jobUuid}/tasks")
    public ResponseEntity<?> getJobTasks(
            @PathVariable UUID jobUuid,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction,
            @AuthenticationPrincipal Jwt jwt
    ) {

        if (getJobIfAllowed(jobUuid, jwt) == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Create pageable object with sorting
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // Get paginated and sorted job tasks
        Page<JobTask> taskPage = jobService.getTasksByJobIdPaginated(jobUuid, pageable);
        
        // Convert to DTOs and preserve pagination metadata
        Page<JobTaskDto> taskDtoPage = taskPage.map(jobMapper::toDto);
        
        return ResponseEntity.ok(taskDtoPage);
    }

    @GetMapping("/models")
    public ResponseEntity<List<ModelDto>> getSupportedModels() {
        return ResponseEntity.ok(modelService.getSupportedModelDetails());
    }

    @PostMapping("/validate")
    public ResponseEntity<JobDto> validateJob(
            @RequestBody @Valid JobDefinitionDto jobDefinitionDto,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getSubject();
        String authToken = jwt.getTokenValue();
        
        // If targetUserId is specified and the current user is an admin, use the target user ID
        if (jobDefinitionDto.getTargetUserId() != null && !jobDefinitionDto.getTargetUserId().isBlank()) {
            if (serviceAuthenticationService.canAccessUserData(jwt, jobDefinitionDto.getTargetUserId())) {
                // Admin is submitting on behalf of another user
                userId = jobDefinitionDto.getTargetUserId();
            } else {
                // Not authorized to submit on behalf of another user
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(null);
            }
        }
        
        Job job = jobService.validateJob(
                jobDefinitionDto,
                userId,
                "Bearer " + authToken
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(jobMapper.toDto(job));
    }

    @GetMapping("/file/{fileUuid}/hasActiveJobs")
    public ResponseEntity<Boolean> hasActiveJobs(
        @PathVariable UUID fileUuid,
        @AuthenticationPrincipal Jwt jwt
    ) {
        // For now, we'll allow any authenticated user to check this
        // In a more secure implementation, you might want to verify file ownership
        boolean hasActiveJobs = jobService.hasActiveJobs(fileUuid);
        return ResponseEntity.ok(hasActiveJobs);
    }

    @GetMapping("/prompt/{promptUuid}/hasActiveJobs")
    public ResponseEntity<Boolean> hasActiveJobsForPrompt(
        @PathVariable UUID promptUuid,
        @AuthenticationPrincipal Jwt jwt
    ) {
        // For now, we'll allow any authenticated user to check this
        // In a more secure implementation, you might want to verify prompt ownership
        boolean hasActiveJobs = jobService.hasActiveJobs(null, promptUuid);
        return ResponseEntity.ok(hasActiveJobs);
    }

    private Job getJobIfAllowed(UUID jobUuid, Jwt jwt) {
        Job job = jobService.getJobById(jobUuid);
        if (job != null) {
            if (!serviceAuthenticationService.canAccessUserData(jwt, job.getUserId())) {
                return null;
            }
        }
        return job;
    }
}