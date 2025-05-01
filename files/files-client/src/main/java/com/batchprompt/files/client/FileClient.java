package com.batchprompt.files.client;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.batchprompt.common.services.ServiceAuthenticationService;
import com.batchprompt.files.model.dto.FileDto;
import com.batchprompt.files.model.dto.FileRecordDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileClient {

    private final RestTemplate restTemplate;
    private final ServiceAuthenticationService authService;
    
    @Value("${services.files.url}")
    private String filesServiceUrl;
    
    @Value("${services.name:jobs-service}")
    private String serviceName;
    
    /**
     * Get a file by UUID
     * 
     * @param fileUuid The UUID of the file to retrieve
     * @param authToken The user's auth token, or null for service-to-service call
     * @return The file DTO
     */
    public FileDto getFile(UUID fileUuid, String authToken) {
        try {
            String token = getEffectiveToken(authToken);
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<FileDto> response = restTemplate.exchange(
                filesServiceUrl + "/api/files/" + fileUuid,
                HttpMethod.GET,
                requestEntity,
                FileDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving file {}: {}", fileUuid, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get records for a file
     * 
     * @param fileUuid The UUID of the file to get records for
     * @param authToken The user's auth token, or null for service-to-service call
     * @return List of file record DTOs
     */
    public List<FileRecordDto> getFileRecords(UUID fileUuid, String authToken) {
        try {
            String token = getEffectiveToken(authToken);
            log.debug("Calling files service with auth token: {}", token);
            
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<List<FileRecordDto>> response = restTemplate.exchange(
                filesServiceUrl + "/api/files/" + fileUuid + "/records",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<FileRecordDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving file records for file {}: {}", fileUuid, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get paginated records for a file
     * 
     * @param fileUuid The UUID of the file to get records for
     * @param page The page number (0-based)
     * @param size The page size
     * @param sortBy The field to sort by (default: recordNumber)
     * @param sortDirection The sort direction (asc or desc)
     * @param authToken The user's auth token, or null for service-to-service call
     * @return Page of file record DTOs
     */
    public Page<FileRecordDto> getFileRecordsPaginated(UUID fileUuid, int page, int size, 
                                                      String sortBy, String sortDirection, String authToken) {
        try {
            String token = getEffectiveToken(authToken);
            log.debug("Calling files service with auth token: {}", token);
            
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            String url = filesServiceUrl + "/api/files/" + fileUuid + "/records?paginate=true" +
                         "&page=" + page + "&size=" + size;
            
            if (sortBy != null && !sortBy.isEmpty()) {
                url += "&sortBy=" + sortBy;
            }
            
            if (sortDirection != null && !sortDirection.isEmpty()) {
                url += "&sortDirection=" + sortDirection;
            }
            
            ResponseEntity<Page<FileRecordDto>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<Page<FileRecordDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving paginated file records for file {}: {}", fileUuid, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get a file record by UUID
     * 
     * @param recordUuid The UUID of the record to retrieve
     * @param authToken The user's auth token, or null for service-to-service call
     * @return The file record DTO
     */
    public FileRecordDto getFileRecord(UUID recordUuid, String authToken) {
        try {
            String token = getEffectiveToken(authToken);
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            log.debug("Calling files service for record {}, token: {}", 
                     recordUuid, token);
            
            ResponseEntity<FileRecordDto> response = restTemplate.exchange(
                filesServiceUrl + "/api/files/records/" + recordUuid,
                HttpMethod.GET,
                requestEntity,
                FileRecordDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving file record {}: {}", recordUuid, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Upload a file
     * 
     * @param fileStream The file input stream
     * @param fileName The file name
     * @param contentType The content type
     * @param fileSize The file size
     * @param fileType The file type
     * @param authToken The user's auth token, or null for service-to-service call
     * @return The uploaded file DTO
     */
    public FileDto uploadFile(InputStream fileStream, String fileName, String contentType, long fileSize, String fileType, String authToken, String userId) {
        try {
            String token = getEffectiveToken(authToken);
            log.debug("Uploading file with token: {}", token);
            
            HttpHeaders headers = createAuthHeaders(token);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Create resource from input stream
            InputStreamResource fileResource = new InputStreamResource(fileStream) {
                @Override
                public String getFilename() {
                    return fileName;
                }
                
                @Override
                public long contentLength() {
                    return fileSize;
                }
            };
            
            // Create multi-part request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Set proper content type for the file part
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.valueOf(contentType));
            HttpEntity<InputStreamResource> fileEntity = new HttpEntity<>(fileResource, fileHeaders);
            
            body.add("file", fileEntity);
            body.add("fileType", fileType);

            String url = filesServiceUrl + "/api/files?userId=" + userId;
            
            log.debug("Sending file upload request to {}", url);
            
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            ResponseEntity<FileDto> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                FileDto.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate a file
     * 
     * @param fileUuid The UUID of the file to validate
     * @param authToken The user's auth token, or null for service-to-service call
     * @return true if validation was successful, false otherwise
     */
    public boolean validateFile(UUID fileUuid, String authToken) {
        try {
            String token = getEffectiveToken(authToken);
            HttpHeaders headers = createAuthHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);
            
            ResponseEntity<Void> response = restTemplate.exchange(
                filesServiceUrl + "/api/files/" + fileUuid + "/validate",
                HttpMethod.POST,
                requestEntity,
                Void.class
            );
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error validating file {}: {}", fileUuid, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get an effective token for the request. If user token is null, get a service token.
     * 
     * @param userToken The user's auth token or null
     * @return An effective token for the request
     */
    private String getEffectiveToken(String userToken) {
        if (userToken != null && !userToken.isEmpty()) {
            return userToken;
        }
        // Use service authentication when no user token is provided
        return authService.getServiceToken(serviceName);
    }
    
    /**
     * Create HTTP headers with authentication token
     * 
     * @param token The authentication token
     * @return HTTP headers with properly formatted auth token
     */
    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        // Ensure token has the proper "Bearer " prefix
        if (token != null) {
            if (!token.startsWith("Bearer ")) {
                token = "Bearer " + token;
            }
            headers.set("Authorization", token);
        }
        return headers;
    }
}