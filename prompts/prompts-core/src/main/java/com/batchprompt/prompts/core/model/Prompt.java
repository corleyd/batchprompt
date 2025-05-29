package com.batchprompt.prompts.core.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.Type;

import com.batchprompt.prompts.model.PromptOutputMethod;
import com.fasterxml.jackson.databind.JsonNode;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "prompt")
public class Prompt {

    @Id
    @Column(name = "prompt_uuid")
    private UUID promptUuid;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "prompt_text", nullable = false, columnDefinition = "text")
    private String promptText;

    @Column(name = "output_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PromptOutputMethod outputMethod;

    @Column(name = "response_text_column_name", nullable = false)
    private String responseTextColumnName;

    @Column(name = "response_json_schema", nullable = false, columnDefinition = "jsonb")
    @Type(JsonType.class)
    private JsonNode responseJsonSchema;

    @Column(name = "create_timestamp", nullable = false)
    private LocalDateTime createTimestamp;

    @Column(name = "update_timestamp", nullable = false)
    private LocalDateTime updateTimestamp;

    @Column(name = "job_run_count", nullable = false)
    private Integer jobRunCount;

    @Column(name = "last_job_run_timestamp")
    private LocalDateTime lastJobRunTimestamp;

    @Column(name = "delete_timestamp")
    private LocalDateTime deleteTimestamp;
 

}