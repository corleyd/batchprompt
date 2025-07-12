package com.batchprompt.jobs.core.model;

import com.batchprompt.jobs.model.StopReason;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response object from chat model generation containing
 * the response text and token usage information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatModelResponse {
    private String responseText;
    private String errorMessage;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer thinkingTokens;
    private Integer totalTokens;
    private StopReason stopReason;
    private String thinkingText;
    
    public static ChatModelResponse of(String responseText) {
        return ChatModelResponse.builder()
                .responseText(responseText)
                .build();
    }

    public static ChatModelResponse ofError(String errorMessage) {
        return ChatModelResponse.builder()
                .errorMessage(errorMessage)
                .build();
    }
    
    public static ChatModelResponse of(String responseText, Integer promptTokens, 
                                       Integer completionTokens, Integer thinkingTokens, Integer totalTokens) {
        return ChatModelResponse.builder()
                .responseText(responseText)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .thinkingTokens(thinkingTokens)
                .totalTokens(totalTokens)
                .build();
    }
    
    public static ChatModelResponse of(String responseText, Integer promptTokens, 
                                       Integer completionTokens, Integer thinkingTokens, Integer totalTokens,
                                       StopReason stopReason, String thinkingText) {
        return ChatModelResponse.builder()
                .responseText(responseText)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .thinkingTokens(thinkingTokens)
                .totalTokens(totalTokens)
                .stopReason(stopReason)
                .thinkingText(thinkingText)
                .build();
    }
}