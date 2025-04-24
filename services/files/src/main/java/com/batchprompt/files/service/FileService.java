package com.batchprompt.files.service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.batchprompt.files.config.MinioConfig;
import com.batchprompt.files.model.File;
import com.batchprompt.files.model.FileField;
import com.batchprompt.files.model.FileRecord;
import com.batchprompt.files.repository.FileRecordRepository;
import com.batchprompt.files.repository.FileRepository;
import com.batchprompt.files.service.FileFieldService.FileFieldDto;
import com.batchprompt.files.service.validation.ExcelValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final FileRecordRepository fileRecordRepository;
    private final FileFieldService fileFieldService;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final ObjectMapper objectMapper;
    private final ExcelValidator excelValidator;

    public List<File> getAllFiles() {
        return fileRepository.findAll();
    }

    public List<File> getFilesByUserId(String userId) {
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
    public Page<File> getFilesByUserIdPaginated(String userId, File.FileType fileType, File.Status status, Pageable pageable) {
        if (fileType != null && status != null) {
            return fileRepository.findByUserIdAndFileTypeAndStatus(userId, fileType, status, pageable);
        } else if (fileType != null) {
            return fileRepository.findByUserIdAndFileType(userId, fileType, pageable);
        } else if (status != null) {
            return fileRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            return fileRepository.findByUserId(userId, pageable);
        }
    }

    public List<File> getUploadedFilesByUserId(String userId) {
        return fileRepository.findByUserIdAndFileType(userId, File.FileType.upload);
    }

    public Optional<File> getFileById(UUID fileUuid) {
        return fileRepository.findById(fileUuid);
    }

    @Transactional
    public File uploadFile(MultipartFile file, String fileType, String userId) {
        try {
            // Determine file type
            File.FileType type;
            try {
                type = File.FileType.valueOf(fileType);
            } catch (IllegalArgumentException e) {
                log.error("Invalid file type: {}, defaulting to upload", fileType);
                type = File.FileType.upload;
            }
            
            // For upload type files, enforce Excel format
            if (type == File.FileType.upload && 
                !file.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                throw new IllegalArgumentException("Uploaded data files must be Excel spreadsheets (.xlsx)");
            }

            // Create a new file record
            UUID fileUuid = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            
            File fileEntity = File.builder()
                    .fileUuid(fileUuid)
                    .fileType(type)
                    .userId(userId)
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .status(File.Status.Validation)
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
            File savedFile = fileRepository.save(fileEntity);
            
            // For upload type files, start validation process
            // For result files, set status to Ready immediately
            if (type == File.FileType.upload) {
                validateFile(savedFile.getFileUuid());
            } else {
                savedFile.setStatus(File.Status.Ready);
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
    public File uploadFile(MultipartFile file, String userId) {
        // Maintain backward compatibility
        return uploadFile(file, "upload", userId);
    }

    @Transactional
    public void validateFile(UUID fileUuid) {
        try {
            Optional<File> optionalFile = fileRepository.findById(fileUuid);
            
            if (optionalFile.isPresent()) {
                File file = optionalFile.get();
                
                // Set status to Validation
                file.setStatus(File.Status.Validation);
                file.setUpdatedAt(LocalDateTime.now());
                fileRepository.save(file);
                
                // Get the file from Minio
                InputStream fileContent = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(fileUuid.toString())
                                .build()
                );
                
                // Only validate Excel files
                if (file.getFileType() == File.FileType.upload) {
                    // Validate the Excel file
                    ExcelValidator.ValidationResult validationResult = excelValidator.validateExcelFile(fileContent);
                    
                    if (validationResult.isValid()) {
                        // Update the file status to Processing
                        file.setStatus(File.Status.Processing);
                        file.setUpdatedAt(LocalDateTime.now());
                        fileRepository.save(file);
                        
                        // Process valid records
                        processFileRecords(file, validationResult.getRecords());
                        
                        // Process and save fields information
                        processFileFields(file, validationResult.getFields());
                    } else {
                        // Update the file with validation errors
                        file.setStatus(File.Status.Validation);
                        file.setValidationErrors(validationResult.getErrors());
                        file.setUpdatedAt(LocalDateTime.now());
                        fileRepository.save(file);
                    }
                } else {
                    // For non-upload files, just mark as Ready
                    file.setStatus(File.Status.Ready);
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
    private void processFileFields(File file, List<ExcelValidator.FieldInfo> fields) {
        try {
            // Convert FieldInfo objects to FileFieldDto objects
            List<FileFieldDto> fieldDtos = fields.stream()
                    .map(fieldInfo -> {
                        FileFieldDto dto = new FileFieldDto();
                        dto.setFieldName(fieldInfo.getFieldName());
                        dto.setFieldType(fieldInfo.getFieldType());
                        dto.setDescription(fieldInfo.getDescription());
                        return dto;
                    })
                    .toList();
            
            // Add all fields to the file
            fileFieldService.addFieldsToFile(file.getFileUuid(), fieldDtos);
            
            log.info("Processed {} fields for file {}", fields.size(), file.getFileUuid());
        } catch (Exception e) {
            log.error("Error processing file fields", e);
            throw new RuntimeException("Failed to process file fields", e);
        }
    }

    @Transactional
    public void processFileRecords(File file, List<Map<String, String>> records) {
        try {
            // Delete any existing records for this file
            fileRecordRepository.deleteByFileFileUuid(file.getFileUuid());
            
            // Create and save all file records
            int recordNumber = 0;
            for (Map<String, String> record : records) {
                recordNumber++;
                
                JsonNode jsonRecord = objectMapper.valueToTree(record);
                
                FileRecord fileRecord = FileRecord.builder()
                        .fileRecordUuid(UUID.randomUUID())
                        .file(file)
                        .recordNumber(recordNumber)
                        .record(jsonRecord)
                        .build();
                
                fileRecordRepository.save(fileRecord);
            }
            
            // Update file status to Ready
            file.setStatus(File.Status.Ready);
            file.setUpdatedAt(LocalDateTime.now());
            fileRepository.save(file);
            
        } catch (Exception e) {
            log.error("Error processing file records", e);
            throw new RuntimeException("Failed to process file records", e);
        }
    }

    @Transactional
    public boolean deleteFile(UUID fileUuid) {
        Optional<File> optionalFile = fileRepository.findById(fileUuid);
        
        if (optionalFile.isPresent()) {
            try {
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
                throw new RuntimeException("Failed to delete file", e);
            }
        }
        
        return false;
    }

    public List<FileRecord> getRecordsByFileId(UUID fileUuid) {
        return fileRecordRepository.findByFileFileUuid(fileUuid);
    }
    
    public Optional<FileRecord> getFileRecordById(UUID recordUuid) {
        return fileRecordRepository.findById(recordUuid);
    }

    public Resource getFileContentAsResource(UUID fileUuid) {
        try {
            // First check if the file exists in our database
            Optional<File> optionalFile = fileRepository.findById(fileUuid);
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
            File file = optionalFile.get();
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
}
