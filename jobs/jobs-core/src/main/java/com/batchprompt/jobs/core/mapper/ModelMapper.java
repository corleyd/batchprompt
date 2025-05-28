package com.batchprompt.jobs.core.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.batchprompt.jobs.core.model.Model;
import com.batchprompt.jobs.core.model.ModelProviderEntity;
import com.batchprompt.jobs.model.dto.ModelDto;
import com.batchprompt.jobs.model.dto.ModelProviderDto;

@Component
public class ModelMapper {
    public List<ModelProviderDto> toModelProviderDtoList(List<Model> modelList) {
        return modelList.stream()
            .collect(Collectors.groupingBy(Model::getProvider))
            .entrySet().stream()
            .map(entry -> {
                List<ModelDto> sortedModels = entry.getValue().stream()
                    .sorted(Comparator.comparing(Model::getModelProviderDisplayOrder))
                    .map(this::toModelDto)
                    .collect(Collectors.toList());
                return toModelProviderDto(entry.getKey(), sortedModels);
            })
            .sorted(Comparator.comparing(ModelProviderDto::getDisplayOrder))
            .collect(Collectors.toList());
    }

    public ModelProviderDto toModelProviderDto(ModelProviderEntity modelProviderEntity, List<ModelDto> modelDtos) {
        return ModelProviderDto.builder()
            .modelProviderId(modelProviderEntity.getModelProviderId())
            .displayName(modelProviderEntity.getDisplayName())
            .displayOrder(modelProviderEntity.getDisplayOrder())
            .models(modelDtos)
            .build();
    }

    public ModelDto toModelDto(Model model) {
        return ModelDto.builder()
            .modelId(model.getModelId())
            .displayName(model.getDisplayName())
            .modelProviderId(model.getProvider().getModelProviderId())
            .modelProviderModelId(model.getModelProviderModelId())
            .modelProviderDisplayName(model.getProvider().getDisplayName())
            .modelProviderProperties(model.getModelProviderProperties())
            .modelProviderDisplayOrder(model.getProvider().getDisplayOrder())
            .taskQueueName(model.getTaskQueueName())
            .build();
    }
    
}
