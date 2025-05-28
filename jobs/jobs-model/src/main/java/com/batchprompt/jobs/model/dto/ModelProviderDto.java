package com.batchprompt.jobs.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelProviderDto {
    private String modelProviderId;
    private String displayName;
    private Integer displayOrder;
    private List<ModelDto> models;
}
