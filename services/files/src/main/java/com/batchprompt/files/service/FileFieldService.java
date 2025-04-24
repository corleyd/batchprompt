package com.batchprompt.files.service;

import com.batchprompt.files.model.File;
import com.batchprompt.files.model.FileField;
import com.batchprompt.files.repository.FileFieldRepository;
import com.batchprompt.files.repository.FileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileFieldService {
    
    private final FileFieldRepository fileFieldRepository;
    private final FileRepository fileRepository;
    
    /**
     * Get all fields for a specific file in order
     * 
     * @param fileUuid the UUID of the file
     * @return list of file fields in order
     */
    public List<FileField> getFileFields(UUID fileUuid) {
        return fileFieldRepository.findByFileFileUuidOrderByFieldOrder(fileUuid);
    }
    
    /**
     * Add a single field to a file
     * 
     * @param fileUuid the UUID of the file
     * @param fieldName the name of the field
     * @param fieldType the type of the field
     * @param description optional description of the field
     * @return the created FileField
     */
    @Transactional
    public FileField addFieldToFile(UUID fileUuid, String fieldName, String fieldType, String description) {
        File file = fileRepository.findById(fileUuid)
                .orElseThrow(() -> new EntityNotFoundException("File not found with UUID: " + fileUuid));
        
        // Get current highest order number for this file, or -1 if no fields exist
        Integer maxOrder = fileFieldRepository.findByFileFileUuidOrderByFieldOrder(fileUuid).stream()
                .map(FileField::getFieldOrder)
                .max(Integer::compareTo)
                .orElse(-1);
        
        // New field gets the next order number
        FileField newField = FileField.builder()
                .file(file)
                .fieldName(fieldName)
                .fieldType(fieldType)
                .fieldOrder(maxOrder + 1)
                .description(description)
                .build();
        
        return fileFieldRepository.save(newField);
    }
    
    /**
     * Update field order for a file
     * 
     * @param fileUuid the UUID of the file
     * @param fieldOrdering list of field UUIDs in desired order
     * @return updated list of fields
     */
    @Transactional
    public List<FileField> updateFieldOrder(UUID fileUuid, List<UUID> fieldOrdering) {
        List<FileField> existingFields = fileFieldRepository.findByFileFileUuidOrderByFieldOrder(fileUuid);
        
        // Validate that the list contains all and only the existing fields
        if (existingFields.size() != fieldOrdering.size() || 
                !existingFields.stream()
                        .map(FileField::getFieldUuid)
                        .collect(Collectors.toSet())
                        .containsAll(fieldOrdering)) {
            throw new IllegalArgumentException("Field ordering must contain all existing fields for the file");
        }
        
        // Update the order of each field
        for (int i = 0; i < fieldOrdering.size(); i++) {
            UUID fieldUuid = fieldOrdering.get(i);
            FileField field = existingFields.stream()
                    .filter(f -> f.getFieldUuid().equals(fieldUuid))
                    .findFirst()
                    .orElseThrow();
            
            field.setFieldOrder(i);
            fileFieldRepository.save(field);
        }
        
        return fileFieldRepository.findByFileFileUuidOrderByFieldOrder(fileUuid);
    }
    
    /**
     * Add multiple fields to a file at once
     * 
     * @param fileUuid the UUID of the file
     * @param fields list of field information (name, type, description)
     * @return list of created fields
     */
    @Transactional
    public List<FileField> addFieldsToFile(UUID fileUuid, List<FileFieldDto> fields) {
        File file = fileRepository.findById(fileUuid)
                .orElseThrow(() -> new EntityNotFoundException("File not found with UUID: " + fileUuid));
        
        List<FileField> existingFields = fileFieldRepository.findByFileFileUuidOrderByFieldOrder(fileUuid);
        int startOrder = existingFields.isEmpty() ? 0 : 
                existingFields.stream()
                        .mapToInt(FileField::getFieldOrder)
                        .max()
                        .orElse(-1) + 1;
        
        List<FileField> newFields = fields.stream()
                .map(dto -> FileField.builder()
                        .file(file)
                        .fieldName(dto.getFieldName())
                        .fieldType(dto.getFieldType())
                        .fieldOrder(startOrder + fields.indexOf(dto))
                        .description(dto.getDescription())
                        .build())
                .collect(Collectors.toList());
        
        return fileFieldRepository.saveAll(newFields);
    }
    
    /**
     * Remove a field from a file
     * 
     * @param fieldUuid the UUID of the field to remove
     */
    @Transactional
    public void removeField(UUID fieldUuid) {
        FileField field = fileFieldRepository.findById(fieldUuid)
                .orElseThrow(() -> new EntityNotFoundException("Field not found with UUID: " + fieldUuid));
        
        UUID fileUuid = field.getFile().getFileUuid();
        fileFieldRepository.delete(field);
        
        // Reorder remaining fields to ensure continuous ordering
        List<FileField> remainingFields = fileFieldRepository.findByFileFileUuidOrderByFieldOrder(fileUuid);
        for (int i = 0; i < remainingFields.size(); i++) {
            FileField remainingField = remainingFields.get(i);
            remainingField.setFieldOrder(i);
            fileFieldRepository.save(remainingField);
        }
    }
    
    /**
     * DTO class for field information
     */
    public static class FileFieldDto {
        private String fieldName;
        private String fieldType;
        private String description;
        
        // Getters and setters
        public String getFieldName() {
            return fieldName;
        }
        
        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }
        
        public String getFieldType() {
            return fieldType;
        }
        
        public void setFieldType(String fieldType) {
            this.fieldType = fieldType;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}