package com.batchprompt.files.service;

import com.batchprompt.files.config.MinioConfig;
import com.batchprompt.files.model.File;
import com.batchprompt.files.model.FileRecord;
import com.batchprompt.files.repository.FileRecordRepository;
import com.batchprompt.files.repository.FileRepository;
import com.batchprompt.files.service.validation.ExcelValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final FileRecordRepository fileRecordRepository;
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

    public List<File> getUploadedFilesByUserId(String userId) {
        return fileRepository.findByUserIdAndFileType(userId, File.FileType.upload);
    }

    public Optional<File> getFileById(UUID fileUuid) {
        return fileRepository.findById(fileUuid);
    }

    @Transactional
    public File uploadFile(MultipartFile file, String userId) {
        try {
            // Check if the file is an Excel file
            if (!file.getContentType().equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                throw new IllegalArgumentException("Uploaded file must be an Excel spreadsheet (.xlsx)");
            }

            // Create a new file record
            UUID fileUuid = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();
            
            File fileEntity = File.builder()
                    .fileUuid(fileUuid)
                    .fileType(File.FileType.upload)
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
            
            // Start validation process
            validateFile(savedFile.getFileUuid());
            
            return savedFile;
        } catch (Exception e) {
            log.error("Error uploading file", e);
            throw new RuntimeException("Failed to upload file", e);
        }
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
                
                // Validate the Excel file
                ExcelValidator.ValidationResult validationResult = excelValidator.validateExcelFile(fileContent);
                
                if (validationResult.isValid()) {
                    // Update the file status to Processing
                    file.setStatus(File.Status.Processing);
                    file.setUpdatedAt(LocalDateTime.now());
                    fileRepository.save(file);
                    
                    // Process valid records
                    processFileRecords(file, validationResult.getRecords());
                } else {
                    // Update the file with validation errors
                    file.setStatus(File.Status.Validation);
                    file.setValidationErrors(validationResult.getErrors());
                    file.setUpdatedAt(LocalDateTime.now());
                    fileRepository.save(file);
                }
            }
        } catch (Exception e) {
            log.error("Error validating file", e);
            throw new RuntimeException("Failed to validate file", e);
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
}