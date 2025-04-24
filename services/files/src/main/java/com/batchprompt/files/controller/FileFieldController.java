package com.batchprompt.files.controller;

import com.batchprompt.files.model.FileField;
import com.batchprompt.files.service.FileFieldService;
import com.batchprompt.files.service.FileFieldService.FileFieldDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileFieldController {
    
    private final FileFieldService fileFieldService;
    
    /**
     * Get all fields for a file in order
     * 
     * @param fileUuid the file UUID
     * @return list of fields in order
     */
    @GetMapping("/{fileUuid}/fields")
    public ResponseEntity<List<FileField>> getFileFields(@PathVariable UUID fileUuid) {
        return ResponseEntity.ok(fileFieldService.getFileFields(fileUuid));
    }
    
    /**
     * Add a field to a file
     * 
     * @param fileUuid the file UUID
     * @param dto the field information
     * @return created field
     */
    @PostMapping("/{fileUuid}/fields")
    public ResponseEntity<FileField> addField(@PathVariable UUID fileUuid, @RequestBody FileFieldDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileFieldService.addFieldToFile(fileUuid, dto.getFieldName(), dto.getFieldType(), dto.getDescription()));
    }
    
    /**
     * Add multiple fields to a file at once
     * 
     * @param fileUuid the file UUID
     * @param fields list of field information
     * @return list of created fields
     */
    @PostMapping("/{fileUuid}/fields/batch")
    public ResponseEntity<List<FileField>> addFields(@PathVariable UUID fileUuid, @RequestBody List<FileFieldDto> fields) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileFieldService.addFieldsToFile(fileUuid, fields));
    }
    
    /**
     * Update field order
     * 
     * @param fileUuid the file UUID
     * @param fieldIds ordered list of field UUIDs
     * @return updated list of fields
     */
    @PutMapping("/{fileUuid}/fields/order")
    public ResponseEntity<List<FileField>> updateFieldOrder(@PathVariable UUID fileUuid, @RequestBody List<UUID> fieldIds) {
        return ResponseEntity.ok(fileFieldService.updateFieldOrder(fileUuid, fieldIds));
    }
    
    /**
     * Delete a field
     * 
     * @param fileUuid the file UUID
     * @param fieldUuid the field UUID
     * @return empty response
     */
    @DeleteMapping("/{fileUuid}/fields/{fieldUuid}")
    public ResponseEntity<Void> removeField(@PathVariable UUID fileUuid, @PathVariable UUID fieldUuid) {
        fileFieldService.removeField(fieldUuid);
        return ResponseEntity.noContent().build();
    }
}