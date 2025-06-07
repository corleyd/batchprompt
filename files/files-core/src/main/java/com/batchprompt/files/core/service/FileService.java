package com.batchprompt.files.core.service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.batchprompt.files.core.config.MinioConfig;
import com.batchprompt.files.core.model.FileEntity;
import com.batchprompt.files.core.model.FileFieldEntity;
import com.batchprompt.files.core.model.FileMapper;
import com.batchprompt.files.core.model.FileRecord;
import com.batchprompt.files.core.repository.FileFieldRepository;
import com.batchprompt.files.core.repository.FileRecordRepository;
import com.batchprompt.files.core.repository.FileRepository;
import com.batchprompt.files.core.service.validation.ExcelValidator;
import com.batchprompt.files.model.FileStatus;
import com.batchprompt.files.model.FileType;
import com.batchprompt.files.model.dto.FileDto;
import com.batchprompt.files.model.dto.FileFieldDto;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.batchprompt.jobs.client.JobClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final FileRecordRepository fileRecordRepository;
    private final FileFieldRepository fileFieldRepository;
    private final FileFieldService fileFieldService;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final ExcelValidator excelValidator;
    private final JobClient jobClient;

    public List<FileEntity> getAllFiles() {
        return fileRepository.findAll();
    }

    public List<FileEntity> getFilesByUserId(String userId) {
        return fileRepository.findByUserId(userId);
    }

    /**
     * Get paginated files for a specific user with optional filtering and sorting
     * 
     * @param userId The user ID to retrieve files for
     * @param fileType Optional file type filter
     * @param status Optional status filter
     * @param pageable Pageable object containing pagination and sorting information
     * @return Page of files for the user
     */
    public Page<FileDto> getFilesByUserIdPaginated(String userId, FileType fileType, FileStatus status, Pageable pageable) {
        Page<FileEntity> page;
        if (fileType != null && status != null) {
            page = fileRepository.findByUserIdAndFileTypeAndStatus(userId, fileType, status, pageable);
        } else if (fileType != null) {
            page = fileRepository.findByUserIdAndFileType(userId, fileType, pageable);
        } else if (status != null) {
            page = fileRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            page = fileRepository.findByUserId(userId, pageable);
        }
        return page.map(entity -> FileMapper.toDto(entity));

    }

    public List<FileEntity> getUploadedFilesByUserId(String userId) {
        return fileRepository.findByUserIdAndFileType(userId, FileType.UPLOAD);
    }

    public Optional<FileEntity> getFileById(UUID fileUuid) {
        return fileRepository.findById(fileUuid);
    }

    @Transactional
    public FileEntity uploadFile(MultipartFile file, String fileType, String userId) {
        try {
            // Determine file type
            FileType type;
            try {
                type = FileType.valueOf(fileType);
            } catch (IllegalArgumentException e) {
                log.error("Invalid file type: {}, defaulting to upload", fileType);
                type = FileType.UPLOAD;
            }
            
            // For upload type files, enforce Excel format
            if (type == FileType.UPLOAD && 
                !file.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                throw new IllegalArgumentException("Uploaded data files must be Excel spreadsheets (.xlsx)");
            }

            // Create a new file record
            UUID fileUuid = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            
            FileEntity fileEntity = FileEntity.builder()
                    .fileUuid(fileUuid)
                    .fileType(type)
                    .userId(userId)
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .status(FileStatus.VALIDATING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            
            // Save the file to Minio
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileUuid.toString())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            // Save the file metadata to the database
            FileEntity savedFile = fileRepository.save(fileEntity);
            
            // For upload type files, start validation process
            // For result files, set status to Ready immediately
            if (type == FileType.UPLOAD) {
                validateFile(savedFile.getFileUuid());
            } else {
                savedFile.setStatus(FileStatus.READY);
                savedFile.setUpdatedAt(LocalDateTime.now());
                fileRepository.save(savedFile);
            }
            
            return savedFile;
        } catch (Exception e) {
            log.error("Error uploading file", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public FileEntity uploadFile(MultipartFile file, String userId) {
        // Maintain backward compatibility
        return uploadFile(file, "upload", userId);
    }

    @Transactional
    public void validateFile(UUID fileUuid) {
        try {
            Optional<FileEntity> optionalFile = fileRepository.findById(fileUuid);
            
            if (optionalFile.isPresent()) {
                FileEntity file = optionalFile.get();
                
                // Set status to Validation
                file.setStatus(FileStatus.VALIDATING);
                file.setUpdatedAt(LocalDateTime.now());
                fileRepository.save(file);
                
                // Get the file from Minio
                InputStream fileContent = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(fileUuid.toString())
                                .build()
                );
                
                // Only validate upload and result files
                if (file.getFileType() == FileType.UPLOAD || file.getFileType() == FileType.RESULT) {
                    // Delete any existing records for this file
                    fileRecordRepository.deleteByFileFileUuid(file.getFileUuid());
                    
                    // Define a simple record processor to save records directly to the database
                    ExcelValidator.RecordProcessor recordProcessor = fileRecord -> {
                        fileRecordRepository.save(fileRecord);
                    };
                    
                    // Validate the Excel file with streaming processor that passes back FileRecord objects
                    ExcelValidator.ValidationResult validationResult = 
                        excelValidator.validateExcelFile(fileContent, file, recordProcessor);
                    
                    if (validationResult.isValid()) {
                        // Process and save fields information
                        processFileFields(file, validationResult.getFields());
                        
                        // Update file status to Ready
                        file.setStatus(FileStatus.READY);
                        file.setUpdatedAt(LocalDateTime.now());
                        fileRepository.save(file);
                    } else {
                        // Update the file with validation errors
                        file.setStatus(FileStatus.VALIDATING);
                        file.setValidationErrors(validationResult.getErrors());
                        file.setUpdatedAt(LocalDateTime.now());
                        fileRepository.save(file);
                    }
                } else {
                    // For non-upload files, just mark as Ready
                    file.setStatus(FileStatus.READY);
                    file.setUpdatedAt(LocalDateTime.now());
                    fileRepository.save(file);
                }
            }
        } catch (Exception e) {
            log.error("Error validating file", e);
            throw new RuntimeException("Failed to validate file", e);
        }
    }

    /**
     * Process and save field information for a file
     * 
     * @param file The file entity
     * @param fields List of field information from validation
     */
    @Transactional
    private void processFileFields(FileEntity file, List<ExcelValidator.FieldInfo> fields) {
        try {
            // Convert FieldInfo objects to FileFieldDto objects
            List<FileFieldDto> fileFields = fields.stream()
                    .map((ExcelValidator.FieldInfo fieldInfo) ->
                        FileFieldDto.builder()
                            .fieldName(fieldInfo.getFieldName())
                            .fieldType(fieldInfo.getFieldType())
                            .fieldOrder(fieldInfo.getFieldOrder())
                            .description(fieldInfo.getDescription())
                            .build()
                    )
                    .toList();
            
            // Add all fields to the file
            fileFieldService.addFieldsToFile(file.getFileUuid(), fileFields);
            
            log.info("Processed {} fields for file {}", fields.size(), file.getFileUuid());
        } catch (Exception e) {
            log.error("Error processing file fields", e);
            throw new RuntimeException("Failed to process file fields", e);
        }
    }

    @Transactional
    public boolean deleteFile(UUID fileUuid) {
        Optional<FileEntity> optionalFile = fileRepository.findById(fileUuid);
        
        if (optionalFile.isPresent()) {
            FileEntity file = optionalFile.get();
            
            try {
                // Check if file is in VALIDATING or PROCESSING state
                if (file.getStatus() == FileStatus.VALIDATING || file.getStatus() == FileStatus.PROCESSING) {
                    throw new IllegalStateException("Cannot delete file while it is being validated or processed");
                }
                
                // Check if there are any active jobs for this file
                boolean hasActiveJobs = jobClient.hasActiveJobs(fileUuid, null);
                if (hasActiveJobs) {
                    throw new IllegalStateException("Cannot delete file while it has active jobs. Please cancel or wait for jobs to complete.");
                }
                
                // Delete file records first
                fileRecordRepository.deleteByFileFileUuid(fileUuid);
                
                // Delete file metadata
                fileRepository.deleteById(fileUuid);
                
                // Delete file from Minio
                minioClient.removeObject(
                        io.minio.RemoveObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(fileUuid.toString())
                                .build()
                );
                
                return true;
            } catch (Exception e) {
                log.error("Error deleting file", e);
                throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
            }
        }
        
        return false;
    }

    public List<FileRecord> getRecordsByFileId(UUID fileUuid) {
        return fileRecordRepository.findByFileFileUuid(fileUuid);
    }
    
    /**
     * Get paginated records for a specific file
     * 
     * @param fileUuid The UUID of the file to get records for
     * @param pageable Pageable object containing pagination and sorting information
     * @return Page of file records
     */
    public Page<FileRecord> getRecordsByFileIdPaginated(UUID fileUuid, Pageable pageable) {
        return fileRecordRepository.findByFileFileUuid(fileUuid, pageable);
    }
    
    public Optional<FileRecord> getFileRecordById(UUID recordUuid) {
        return fileRecordRepository.findById(recordUuid);
    }

    public Resource getFileContentAsResource(UUID fileUuid) {
        try {
            // First check if the file exists in our database
            Optional<FileEntity> optionalFile = fileRepository.findById(fileUuid);
            if (optionalFile.isEmpty()) {
                throw new RuntimeException("File not found with ID: " + fileUuid);
            }

            // Get the file from MinIO
            InputStream fileContent = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(fileUuid.toString())
                            .build()
            );

            // Convert to Spring Resource that will close the stream when the resource is closed
            FileEntity file = optionalFile.get();
            return new org.springframework.core.io.InputStreamResource(fileContent) {
                @Override
                public String getFilename() {
                    return file.getFileName();
                }
                
                @Override
                public long contentLength() {
                    return file.getFileSize();
                }

            };
        } catch (Exception e) {
            log.error("Error retrieving file content", e);
            throw new RuntimeException("Failed to retrieve file content: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public FileEntity copyFile(FileEntity sourceFile, String targetUserId) {
        try {
            // Create a new file entity with a new UUID
            UUID newFileUuid = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            
            FileEntity newFile = FileEntity.builder()
                    .fileUuid(newFileUuid)
                    .fileType(sourceFile.getFileType())
                    .userId(targetUserId)
                    .fileName(sourceFile.getFileName())
                    .contentType(sourceFile.getContentType())
                    .fileSize(sourceFile.getFileSize())
                    .status(FileStatus.READY) // Set status to READY directly
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            
            // Save the new file metadata to the database
            FileEntity savedFile = fileRepository.save(newFile);
            
            // Copy file content from source to target in MinIO
            InputStream sourceContent = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(sourceFile.getFileUuid().toString())
                            .build()
            );
            
            // Upload the source content to the new file object
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(newFileUuid.toString())
                            .stream(sourceContent, sourceFile.getFileSize(), -1)
                            .contentType(sourceFile.getContentType())
                            .build()
            );
            
            // Copy records from source file to new file
            List<FileRecord> sourceRecords = fileRecordRepository.findByFileFileUuid(sourceFile.getFileUuid());
            
            // Create new records with the same data but linked to the new file
            for (FileRecord sourceRecord : sourceRecords) {
                FileRecord newRecord = FileRecord.builder()
                        .fileRecordUuid(UUID.randomUUID())
                        .file(newFile)
                        .recordNumber(sourceRecord.getRecordNumber())
                        .record(sourceRecord.getRecord())
                        .build();
                
                fileRecordRepository.save(newRecord);
            }
            
            // Copy fields from source file to new file
            List<FileFieldEntity> sourceFields = fileFieldRepository.findByFileFileUuidOrderByFieldOrder(sourceFile.getFileUuid());
            
            for (FileFieldEntity sourceField : sourceFields) {
                FileFieldEntity newField = FileFieldEntity.builder()
                        .file(newFile)
                        .fieldName(sourceField.getFieldName())
                        .fieldType(sourceField.getFieldType())
                        .fieldOrder(sourceField.getFieldOrder())
                        .description(sourceField.getDescription())
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                        
                fileFieldRepository.save(newField);
            }
            
            log.info("Successfully copied file {} to user {}, new file UUID: {}", 
                    sourceFile.getFileUuid(), targetUserId, newFileUuid);
            
            return savedFile;
        } catch (Exception e) {
            log.error("Error copying file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to copy file: " + e.getMessage(), e);
        }
    }
}