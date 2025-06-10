package com.batchprompt.waitlist.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetAutoAcceptanceCountDto {
    
    @NotNull(message = "Count is required")
    @Min(value = 0, message = "Count must be non-negative")
    private Integer count;
    
    private String notes;
}
