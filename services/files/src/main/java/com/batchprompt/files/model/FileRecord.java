package com.batchprompt.files.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.UUID;

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
    private File file;

    @Column(name = "record_number", nullable = false)
    private Integer recordNumber;

    @Type(JsonType.class)
    @Column(name = "record", nullable = false, columnDefinition = "jsonb")
    private JsonNode record;
}