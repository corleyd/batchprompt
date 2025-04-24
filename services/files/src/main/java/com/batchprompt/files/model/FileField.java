package com.batchprompt.files.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "file_field", schema = "files")
public class FileField {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "field_uuid", updatable = false, nullable = false)
    private UUID fieldUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_uuid", nullable = false)
    private File file;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "field_type", nullable = false)
    private String fieldType;

    @Column(name = "field_order", nullable = false)
    private Integer fieldOrder;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}