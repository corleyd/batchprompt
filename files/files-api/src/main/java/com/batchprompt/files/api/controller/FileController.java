package com.batchprompt.files.api.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
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

import com.batchprompt.common.services.ServiceAuthenticationService;
import com.batchprompt.files.core.model.FileEntity;
import com.batchprompt.files.core.model.FileMapper;
import com.batchprompt.files.core.model.FileRecord;
import com.batchprompt.files.core.service.FileService;
import com.batchprompt.files.model.FileStatus;
import com.batchprompt.files.model.FileType;
import com.batchprompt.files.model.dto.FileDto;
import com.batchprompt.files.model.dto.FileRecordDto;

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


    @GetMapping("/user")
    public ResponseEntity<?> getFilesByUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        String userId = jwt.getSubject();
        return getFilesByUserId(jwt, userId, fileType, status, page, size, sortBy, sortDirection);
    }

    
    @GetMapping("/user/uploads")
    public ResponseEntity<List<FileDto>> getUploadFilesByUser(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        List<FileEntity> files = fileService.getUploadedFilesByUserId(userId);
        return ResponseEntity.ok(fileMapper.toDtoList(files));
    }    
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getFilesByUserId(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String userId,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        if (!serviceAuthenticationService.canAccessUserData(jwt, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Convert string parameters to enum types if provided
        FileType fileTypeEnum = null;
        if (fileType != null && !fileType.isEmpty()) {
            try {
                fileTypeEnum = FileType.valueOf(fileType);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid file type: " + fileType);
            }
        }
        
        FileStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = FileStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid status: " + status);
            }
        }
        
        // Create Pageable object with sort direction
        org.springframework.data.domain.Sort.Direction direction = 
            sortDirection.equalsIgnoreCase("asc") ? 
            org.springframework.data.domain.Sort.Direction.ASC : 
            org.springframework.data.domain.Sort.Direction.DESC;
            
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(
                page, size, 
                org.springframework.data.domain.Sort.by(direction, sortBy)
            );
        
        Page<FileDto> files = fileService.getFilesByUserIdPaginated(userId, fileTypeEnum, statusEnum, pageable);
        
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileUuid}")
    public ResponseEntity<FileDto> getFileById(@PathVariable UUID fileUuid) {
        return fileService.getFileById(fileUuid)
                .map(entity -> FileMapper.toDto(entity))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "fileType", defaultValue = "UPLOAD") String fileType,
            @RequestParam(name = "userId", required = false) String requestedUserId,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId;
        
        // Check if the request is from a service account and a userId was provided
        if (serviceAuthenticationService.isValidServiceJwt(jwt) && requestedUserId != null) {
            userId = requestedUserId;
        } else {
            userId = jwt.getSubject();
        }
        
        FileEntity uploadedFile = fileService.uploadFile(file, fileType, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(FileMapper.toDto(uploadedFile));
    }

    @GetMapping("/{fileUuid}/records")
    public ResponseEntity<?> getFileRecords(
            @PathVariable UUID fileUuid,
            @RequestParam(required = false, defaultValue = "false") boolean paginate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "recordNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {
        
        return fileService.getFileById(fileUuid)
                .map(file -> {
                    // If pagination is not requested, return all records as before
                    if (!paginate) {
                        List<FileRecordDto> records = fileMapper.toRecordDtoList(
                                fileService.getRecordsByFileId(fileUuid));
                        return ResponseEntity.ok(records);
                    } 
                    
                    // Otherwise, use pagination
                    // Create Pageable object with sort direction
                    org.springframework.data.domain.Sort.Direction direction = 
                        sortDirection.equalsIgnoreCase("asc") ? 
                        org.springframework.data.domain.Sort.Direction.ASC : 
                        org.springframework.data.domain.Sort.Direction.DESC;
                        
                    org.springframework.data.domain.Pageable pageable = 
                        org.springframework.data.domain.PageRequest.of(
                            page, size, 
                            org.springframework.data.domain.Sort.by(direction, sortBy)
                        );
                    
                    Page<FileRecord> recordsPage = fileService.getRecordsByFileIdPaginated(fileUuid, pageable);
                    Page<FileRecordDto> dtoPage = recordsPage.map(record -> FileMapper.toDto(record));
                    return ResponseEntity.ok(dtoPage);
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
                    return ResponseEntity.ok(FileMapper.toDto(fileRecord));
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
                            .map(updatedFile -> ResponseEntity.ok(FileMapper.toDto(updatedFile)))
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
        
        return fileService.getFileById(fileUuid)
                .map(file -> {
                    // Check if user has access to this file
                    if (!serviceAuthenticationService.canAccessUserData(jwt, file.getUserId())) {
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

    // Admin endpoints to get user files, jobs, and prompts
    @GetMapping("/admin/{userId}")
    public ResponseEntity<?> getFilesByUserIdForAdmin(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        
        // Verify that the requester is an admin
        if (!serviceAuthenticationService.isAdminUser(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Convert string parameters to enum types if provided
        FileType fileTypeEnum = null;
        if (fileType != null && !fileType.isEmpty()) {
            try {
                fileTypeEnum = FileType.valueOf(fileType);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid file type: " + fileType);
            }
        }
        
        FileStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            try {
                statusEnum = FileStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid status: " + status);
            }
        }
        
        // Create Pageable object with sort direction
        org.springframework.data.domain.Sort.Direction direction = 
            sortDirection.equalsIgnoreCase("asc") ? 
            org.springframework.data.domain.Sort.Direction.ASC : 
            org.springframework.data.domain.Sort.Direction.DESC;
            
        org.springframework.data.domain.Pageable pageable = 
            org.springframework.data.domain.PageRequest.of(
                page, size, 
                org.springframework.data.domain.Sort.by(direction, sortBy)
            );
        
        Page<FileDto> files = fileService.getFilesByUserIdPaginated(userId, fileTypeEnum, statusEnum, pageable);
        
        return ResponseEntity.ok(files);
    }

    @PostMapping("/admin/copy")
    public ResponseEntity<?> copyFileForAdmin(
            @RequestParam UUID sourceFileUuid,
            @RequestParam String targetUserId,
            @AuthenticationPrincipal Jwt jwt) {
        
        // Verify that the requester is an admin
        if (!serviceAuthenticationService.isAdminUser(jwt)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return fileService.getFileById(sourceFileUuid)
                .map(file -> {
                    FileEntity copiedFile = fileService.copyFile(file, targetUserId);
                    return ResponseEntity.status(HttpStatus.CREATED).body(FileMapper.toDto(copiedFile));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}