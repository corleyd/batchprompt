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
import com.batchprompt.jobs.model.dto.JobSubmissionDto;
import com.batchprompt.jobs.model.dto.JobTaskDto;

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
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction) {
        
        // Create pageable object with sorting
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // Get paginated and filtered jobs
        Page<Job> jobPage = jobService.getJobsPaginated(userId, modelName, status, pageable);
        
        // Convert to DTOs and preserve pagination metadata
        Page<JobDto> jobDtoPage = jobPage.map(job -> jobService.convertToDto(job));
        
        return ResponseEntity.ok(jobDtoPage);
    }

    @GetMapping("/{jobUuid}")
    public ResponseEntity<JobDto> getJobById(@PathVariable UUID jobUuid) {
        Job job = jobService.getJobById(jobUuid);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(jobService.convertToDto(job));
    }

    @GetMapping("/user")
    public ResponseEntity<?> getJobsByUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction) 
    {
        String userId = jwt.getSubject();
        return getJobsByUserId(userId, jwt, page, size, sort, direction);
    }

    /**
     * Admin endpoint to get jobs for a specific user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getJobsByUserId(
            @PathVariable String userId,
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
        
        // Get jobs for the specified user
        Page<Job> jobsPage = jobService.getJobsByUserIdPaginated(userId, pageable);
        Page<JobDto> dtoPage = jobsPage.map(job -> jobService.convertToDto(job));
        
        return ResponseEntity.ok(dtoPage);
    }    
        

    @GetMapping("/{jobUuid}/tasks")
    public ResponseEntity<?> getJobTasks(
            @PathVariable UUID jobUuid,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction) {
        
        Job job = jobService.getJobById(jobUuid);
        if (job == null) {
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
    public ResponseEntity<List<String>> getSupportedModels() {
        return ResponseEntity.ok(modelService.getSupportedModels());
    }

    @PostMapping("/submit")
    public ResponseEntity<JobDto> submitJob(
            @RequestBody @Valid JobSubmissionDto jobSubmissionDto,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getSubject();
        String authToken = jwt.getTokenValue();
        
        Job job = jobService.submitJob(
                jobSubmissionDto,
                userId,
                "Bearer " + authToken
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.convertToDto(job));
    }
}