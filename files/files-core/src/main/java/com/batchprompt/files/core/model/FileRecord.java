package com.batchprompt.files.core.model;

import java.util.UUID;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.JsonNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "file_record")
public class FileRecord {

    @Id
    @Column(name = "file_record_uuid")
    private UUID fileRecordUuid;

    @ManyToOne
    @JoinColumn(name = "file_uuid", nullable = false)
    private FileEntity file;

    @Column(name = "record_number", nullable = false)
    private Integer recordNumber;

    @Type(JsonType.class)
    @Column(name = "record", nullable = false, columnDefinition = "jsonb")
    private JsonNode record;
}