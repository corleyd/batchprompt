package com.batchprompt.jobs.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.batchprompt.common.client.ClientAuthenticationService;
import com.batchprompt.common.client.ClientException;
import com.batchprompt.jobs.model.JobStatus;
import com.batchprompt.jobs.model.dto.JobDefinitionDto;
import com.batchprompt.jobs.model.dto.JobDto;
import com.batchprompt.jobs.model.dto.JobTaskDto;
import com.batchprompt.jobs.model.dto.ModelDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JobClient {
    
    private final RestTemplate restTemplate;
    private final ClientAuthenticationService authService;
    
    @Value("${services.jobs.url}")
    private String jobsServiceUrl;
    
    /**
     * Get all jobs with pagination and filtering options
     * 
     * @param userId Optional user ID to filter by
     * @param modelId Optional model ID to filter by
     * @param status Optional status to filter by
     * @param page Page number (zero-based)
     * @param size Page size
     * @param sort Sort field
     * @param direction Sort direction (asc or desc)
     * @param authToken User auth token or null for service-to-service call
     * @return Page of JobDto objects
     */
    public Page<JobDto> getAllJobs(
            String userId, 
            String modelId, 
            JobStatus status, 
            int page, 
            int size, 
            String sort, 
            String direction,
            String authToken) throws ClientException {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(jobsServiceUrl + "/api/jobs")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort)
                .queryParam("direction", direction);
            
            if (userId != null && !userId.isEmpty()) {
                uriBuilder.queryParam("userId", userId);
            }
            
            if (modelId != null && !modelId.isEmpty()) {
                uriBuilder.queryParam("modelId", modelId);
            }
            
            if (status != null) {
                uriBuilder.queryParam("status", status);
            }
            
            ResponseEntity<Page<JobDto>> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Page<JobDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving jobs", e);
            throw new ClientException("Error retrieving jobs", e);
        }
    }
    
    /**
     * Get a job by UUID
     * 
     * @param jobUuid The UUID of the job to retrieve
     * @param authToken The user's auth token, or null for service-to-service call
     * @return The job DTO
     */
    public JobDto getJob(UUID jobUuid, String authToken) throws ClientException {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<JobDto> response = restTemplate.exchange(
                jobsServiceUrl + "/api/jobs/" + jobUuid,
                HttpMethod.GET,
                requestEntity,
                JobDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new ClientException("Error retrieving job with UUID " + jobUuid, e);
        }
    }
    
    /**
     * Get jobs for a specific user with pagination and sorting
     * 
     * @param userId The user ID to retrieve jobs for
     * @param page Page number (zero-based)
     * @param size Page size
     * @param sort Sort field
     * @param direction Sort direction (asc or desc)
     * @param authToken User auth token or null for service-to-service call
     * @return Page of JobDto objects
     */
    public Page<JobDto> getJobsByUserId(
            String userId, 
            int page, 
            int size, 
            String sort, 
            String direction, 
            String authToken) throws ClientException {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(
                jobsServiceUrl + "/api/jobs/user/" + userId)
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort)
                .queryParam("direction", direction);
            
            ResponseEntity<Page<JobDto>> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Page<JobDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving jobs for user {}", userId, e);
            throw new ClientException("Error retrieving jobs for user", e);
        }
    }
    
    /**
     * Get tasks for a specific job with pagination and sorting
     * 
     * @param jobUuid The UUID of the job to retrieve tasks for
     * @param page Page number (zero-based)
     * @param size Page size
     * @param sort Sort field
     * @param direction Sort direction (asc or desc)
     * @param authToken User auth token or null for service-to-service call
     * @return Page of JobTaskDto objects
     */
    public Page<JobTaskDto> getJobTasks(
            UUID jobUuid, 
            int page, 
            int size, 
            String sort, 
            String direction, 
            String authToken) throws ClientException {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(
                jobsServiceUrl + "/api/jobs/" + jobUuid + "/tasks")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("sort", sort)
                .queryParam("direction", direction);
            
            ResponseEntity<Page<JobTaskDto>> response = restTemplate.exchange(
                uriBuilder.toUriString(),
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Page<JobTaskDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving tasks for job {}", jobUuid, e);
            throw new ClientException("Error retrieving job tasks", e);
        }
    }
    
    /**
     * Get all supported models
     * 
     * @param authToken User auth token or null for service-to-service call
     * @return List of ModelDto objects
     */
    public List<ModelDto> getSupportedModels(String authToken) throws ClientException {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<List<ModelDto>> response = restTemplate.exchange(
                jobsServiceUrl + "/api/jobs/models",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<ModelDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving supported models", e);
            throw new ClientException("Error retrieving supported models", e);
        }
    }

    /**
     * Validate a new job
     * 
     * @param jobDefinitionDto The job definition DTO
     * @param authToken User auth token (required)
     * @return The created job DTO
     */
    public JobDto validateJob(JobDefinitionDto jobDefinitionDto, String authToken) throws ClientException {
        try {
           
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<JobDefinitionDto> requestEntity = new HttpEntity<>(jobDefinitionDto, headers);
            
            ResponseEntity<JobDto> response = restTemplate.exchange(
                jobsServiceUrl + "/api/jobs/validate",
                HttpMethod.POST,
                requestEntity,
                JobDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error validating job", e);
            throw new ClientException("Error validating job", e);
        }
    }
    
    /**
     * Submit a job for processing
     * 
     * @param jobUuid The UUID of the job to submit
     * @param authToken User auth token or null for service-to-service call
     * @return The updated job DTO
     */
    public JobDto submitJob(UUID jobUuid, String authToken) throws ClientException {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<JobDto> response = restTemplate.exchange(
                jobsServiceUrl + "/api/jobs/" + jobUuid + "/submit",
                HttpMethod.POST,
                requestEntity,
                JobDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error submitting job {}", jobUuid, e);
            throw new ClientException("Error submitting job", e);
        }
    }
    
    /**
     * Cancel a job
     * 
     * @param jobUuid The UUID of the job to cancel
     * @param authToken User auth token or null for service-to-service call
     * @return The updated job DTO
     */
    public JobDto cancelJob(UUID jobUuid, String authToken) throws ClientException {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<JobDto> response = restTemplate.exchange(
                jobsServiceUrl + "/api/jobs/" + jobUuid + "/cancel",
                HttpMethod.POST,
                requestEntity,
                JobDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error cancelling job {}", jobUuid, e);
            throw new ClientException("Error cancelling job", e);
        }
    }
    
    /**
     * Continue a job that was previously cancelled or failed
     * 
     * @param jobUuid The UUID of the job to continue
     * @param authToken User auth token or null for service-to-service call
     * @return The updated job DTO
     */
    public JobDto continueJob(UUID jobUuid, String authToken) throws ClientException {
        try {
            HttpHeaders headers = authService.createAuthHeaders(authToken);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<JobDto> response = restTemplate.exchange(
                jobsServiceUrl + "/api/jobs/" + jobUuid + "/continue",
                HttpMethod.POST,
                requestEntity,
                JobDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error continuing job {}", jobUuid, e);
            throw new ClientException("Error continuing job", e);
        }
    }
}
