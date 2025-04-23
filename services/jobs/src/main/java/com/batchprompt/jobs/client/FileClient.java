package com.batchprompt.jobs.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.batchprompt.jobs.dto.FileDto;
import com.batchprompt.jobs.dto.FileRecordDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileClient {

    private final RestTemplate restTemplate;
    
    @Value("${services.files.url}")
    private String filesServiceUrl;
    
    public FileDto getFile(UUID fileUuid, String authToken) {
        try {
            ResponseEntity<FileDto> response = restTemplate.exchange(
                filesServiceUrl + "/api/files/" + fileUuid,
                HttpMethod.GET,
                HeaderUtil.createEntityWithAuthHeader(authToken),
                FileDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving file {}", fileUuid, e);
            return null;
        }
    }
    
    public List<FileRecordDto> getFileRecords(UUID fileUuid, String authToken) {
        try {
            ResponseEntity<List<FileRecordDto>> response = restTemplate.exchange(
                filesServiceUrl + "/api/files/" + fileUuid + "/records",
                HttpMethod.GET,
                HeaderUtil.createEntityWithAuthHeader(authToken),
                new ParameterizedTypeReference<List<FileRecordDto>>() {}
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving file records for file {}", fileUuid, e);
            return null;
        }
    }
    
    public FileRecordDto getFileRecord(UUID recordUuid, String authToken) {
        try {
            ResponseEntity<FileRecordDto> response = restTemplate.exchange(
                filesServiceUrl + "/api/files/records/" + recordUuid,
                HttpMethod.GET,
                HeaderUtil.createEntityWithAuthHeader(authToken),
                FileRecordDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            log.error("Error retrieving file record {}", recordUuid, e);
            return null;
        }
    }
}