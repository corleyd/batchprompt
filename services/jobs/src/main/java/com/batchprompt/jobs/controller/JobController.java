package com.batchprompt.jobs.controller;

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

import com.batchprompt.jobs.dto.JobDto;
import com.batchprompt.jobs.dto.JobSubmissionDto;
import com.batchprompt.jobs.dto.JobTaskDto;
import com.batchprompt.jobs.mapper.JobMapper;
import com.batchprompt.jobs.model.Job;
import com.batchprompt.jobs.service.JobService;
import com.batchprompt.jobs.service.ModelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final ModelService modelService;
    private final JobMapper jobMapper;

    @GetMapping
    public ResponseEntity<List<JobDto>> getAllJobs() {
        List<Job> jobs = jobService.getAllJobs();
        return ResponseEntity.ok(jobMapper.toDtoList(jobs));
    }

    @GetMapping("/{jobUuid}")
    public ResponseEntity<JobDto> getJobById(@PathVariable UUID jobUuid) {
        Job job = jobService.getJobById(jobUuid);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(jobMapper.toDto(job));
    }

    @GetMapping("/user")
    public ResponseEntity<?> getJobsByUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sort,
            @RequestParam(required = false, defaultValue = "desc") String direction) {
        
        String userId = jwt.getSubject();
        
        // Create pageable object with sorting
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        
        // Get paginated and sorted jobs
        Page<Job> jobPage = jobService.getJobsByUserIdPaginated(userId, pageable);
        
        // Convert to DTOs and preserve pagination metadata
        Page<JobDto> jobDtoPage = jobPage.map(jobMapper::toDto);
        
        return ResponseEntity.ok(jobDtoPage);
    }

    @GetMapping("/{jobUuid}/tasks")
    public ResponseEntity<List<JobTaskDto>> getJobTasks(@PathVariable UUID jobUuid) {
        Job job = jobService.getJobById(jobUuid);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(jobMapper.toTaskDtoList(jobService.getTasksByJobId(jobUuid)));
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
                jobSubmissionDto.getFileUuid(),
                jobSubmissionDto.getPromptUuid(),
                jobSubmissionDto.getModelName(),
                userId,
                "Bearer " + authToken
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(jobMapper.toDto(job));
    }
}