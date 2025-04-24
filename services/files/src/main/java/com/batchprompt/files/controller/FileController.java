package com.batchprompt.files.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.batchprompt.commons.services.ServiceAuthenticationService;
import com.batchprompt.files.dto.FileDto;
import com.batchprompt.files.dto.FileRecordDto;
import com.batchprompt.files.mapper.FileMapper;
import com.batchprompt.files.model.File;
import com.batchprompt.files.service.FileService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileMapper fileMapper;
    private final ServiceAuthenticationService serviceAuthenticationService;
    
    // In-memory token store - in production, use a distributed cache like Redis
    private static final Map<String, FileDownloadToken> downloadTokens = new ConcurrentHashMap<>();
    private static final long TOKEN_VALIDITY_MINUTES = 5;

    @GetMapping
    public ResponseEntity<List<FileDto>> getAllFiles() {
        List<File> files = fileService.getAllFiles();
        return ResponseEntity.ok(fileMapper.toDtoList(files));
    }

    @GetMapping("/{fileUuid}")
    public ResponseEntity<FileDto> getFileById(@PathVariable UUID fileUuid) {
        return fileService.getFileById(fileUuid)
                .map(fileMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user")
    public ResponseEntity<List<FileDto>> getFilesByUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<File> files = fileService.getFilesByUserId(userId);
        return ResponseEntity.ok(fileMapper.toDtoList(files));
    }

    @GetMapping("/user/uploads")
    public ResponseEntity<List<FileDto>> getUploadFilesByUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<File> files = fileService.getUploadedFilesByUserId(userId);
        return ResponseEntity.ok(fileMapper.toDtoList(files));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "fileType", defaultValue = "upload") String fileType,
            @RequestParam(name = "userId", required = false) String requestedUserId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId;
        
        // Check if the request is from a service account and a userId was provided
        if (serviceAuthenticationService.isValidServiceJwt(jwt) && requestedUserId != null) {
            userId = requestedUserId;
        } else {
            userId = jwt.getSubject();
        }
        
        File uploadedFile = fileService.uploadFile(file, fileType, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(fileMapper.toDto(uploadedFile));
    }

    @GetMapping("/{fileUuid}/records")
    public ResponseEntity<List<FileRecordDto>> getFileRecords(@PathVariable UUID fileUuid) {
        return fileService.getFileById(fileUuid)
                .map(file -> {
                    List<FileRecordDto> records = fileMapper.toRecordDtoList(
                            fileService.getRecordsByFileId(fileUuid));
                    return ResponseEntity.ok(records);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/records/{recordUuid}")
    public ResponseEntity<FileRecordDto> getFileRecordById(
            @PathVariable UUID recordUuid,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getSubject();
        return fileService.getFileRecordById(recordUuid)
                .map(fileRecord -> {
                    // Check if the user owns the file or the request is from a service account
                    if (!serviceAuthenticationService.isValidServiceJwt(jwt) && !fileRecord.getFile().getUserId().equals(userId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<FileRecordDto>build();
                    }
                    return ResponseEntity.ok(fileMapper.toDto(fileRecord));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{fileUuid}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID fileUuid) {
        boolean deleted = fileService.deleteFile(fileUuid);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PostMapping("/{fileUuid}/validate")
    public ResponseEntity<FileDto> validateFile(@PathVariable UUID fileUuid) {
        return fileService.getFileById(fileUuid)
                .map(file -> {
                    fileService.validateFile(fileUuid);
                    return fileService.getFileById(fileUuid)
                            .map(updatedFile -> ResponseEntity.ok(fileMapper.toDto(updatedFile)))
                            .orElse(ResponseEntity.notFound().build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Generate a one-time download token for a file
     */
    @GetMapping("/{fileUuid}/token")
    public ResponseEntity<String> getDownloadToken(
            @PathVariable UUID fileUuid,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getSubject();
        
        return fileService.getFileById(fileUuid)
                .map(file -> {
                    // Check if user has access to this file
                    if (!serviceAuthenticationService.isValidServiceJwt(jwt) && !file.getUserId().equals(userId)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<String>build();
                    }
                    
                    // Generate a secure, random token
                    String token = generateSecureToken();
                    
                    // Store token with file information
                    FileDownloadToken downloadToken = new FileDownloadToken(
                        fileUuid,
                        file.getFileName(),
                        LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES)
                    );
                    downloadTokens.put(token, downloadToken);
                    return ResponseEntity.ok(token);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Download a file using a valid token
     */
    @GetMapping("{fileUuid}/download/{token}")
    public ResponseEntity<Resource> downloadFileWithToken(
        @PathVariable UUID fileUuid,    
        @PathVariable String token
    ) {
        // Validate the token
        FileDownloadToken downloadToken = downloadTokens.get(token);
        
        if (downloadToken == null || downloadToken.isExpired() || !downloadToken.getFileUuid().equals(fileUuid)) {
            if (downloadToken != null && downloadToken.isExpired()) {
                downloadTokens.remove(token); // Clean up expired tokens
            }
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // Get file from service
            return fileService.getFileById(downloadToken.getFileUuid())
                .map(file -> {
                    try {
                        // Retrieve file content as resource
                        Resource fileResource = fileService.getFileContentAsResource(downloadToken.getFileUuid());
                    
                        // Set headers for browser download
                        String encodedFilename = URLEncoder.encode(downloadToken.getFilename(), StandardCharsets.UTF_8.toString())
                            .replace("+", "%20"); // Fix spaces
                            
                        HttpHeaders headers = new HttpHeaders();
                        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
                        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
                        headers.add(HttpHeaders.PRAGMA, "no-cache");
                        headers.add(HttpHeaders.EXPIRES, "0");
                        
                        // Remove the token after successful use (one-time use)
                        downloadTokens.remove(token);
                        
                        return ResponseEntity.ok()
                            .headers(headers)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(fileResource);
                    } catch (Exception e) {
                        return ResponseEntity.internalServerError().<Resource>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
                
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private String generateSecureToken() {
        return UUID.randomUUID().toString();
    }
    
    // Token class to store file download information
    private static class FileDownloadToken {
        private final UUID fileUuid;
        private final String filename;
        private final LocalDateTime expiryTime;
        
        public FileDownloadToken(UUID fileUuid, String filename, LocalDateTime expiryTime) {
            this.fileUuid = fileUuid;
            this.filename = filename;
            this.expiryTime = expiryTime;
        }
        
        public UUID getFileUuid() {
            return fileUuid;
        }
        
        public String getFilename() {
            return filename;
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiryTime);
        }
    }
}