package com.batchprompt.files.controller;

import com.batchprompt.files.dto.FileDto;
import com.batchprompt.files.dto.FileRecordDto;
import com.batchprompt.files.mapper.FileMapper;
import com.batchprompt.files.model.File;
import com.batchprompt.files.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileMapper fileMapper;

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
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt.getSubject();
        File uploadedFile = fileService.uploadFile(file, userId);
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
}