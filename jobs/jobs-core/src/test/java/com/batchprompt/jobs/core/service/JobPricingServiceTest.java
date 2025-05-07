package com.batchprompt.jobs.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.model.ModelCost;
import com.batchprompt.jobs.core.repository.ModelCostRepository;
import com.batchprompt.jobs.model.TaskStatus;

@ExtendWith(MockitoExtension.class)
public class JobPricingServiceTest {

    @Mock
    private ModelCostRepository modelCostRepository;

    @InjectMocks
    private JobPricingService jobPricingService;

    private JobTask jobTask;
    private ModelCost modelCost;
    private final String MODEL_ID = "openai-gpt-4o";
    private final UUID JOB_TASK_UUID = UUID.randomUUID();
    private final UUID MODEL_COST_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        // Set up a sample JobTask
        jobTask = new JobTask();
        jobTask.setJobTaskUuid(JOB_TASK_UUID);
        jobTask.setModelId(MODEL_ID);
        jobTask.setStatus(TaskStatus.COMPLETED);
        jobTask.setPromptTokens(1000);
        jobTask.setCompletionTokens(500);
        jobTask.setTotalTokens(1500);

        // Set up a sample ModelCost
        modelCost = new ModelCost();
        modelCost.setModelCostUuid(MODEL_COST_UUID);
        modelCost.setModelId(MODEL_ID);
        modelCost.setEffectiveBeginTimestamp(LocalDateTime.now().minusDays(30));
        modelCost.setInputToken1mCostUsd(2.5);
        modelCost.setOutputToken1mCostUsd(10.0);
        modelCost.setThinkingToken1mCostUsd(0.0);
    }

    @Test
    void testCalculateCost() {
        // Mock repository response
        List<ModelCost> modelCosts = new ArrayList<>();
        modelCosts.add(modelCost);
        when(modelCostRepository.findCurrentCostsForModel(MODEL_ID)).thenReturn(modelCosts);

        // Calculate expected cost
        // (1000 * 2.5 + 500 * 10.0) / 1,000,000 = 0.0025 + 0.005 = 0.0075
        double expectedCost = 0.0075;

        // Test the method
        Double calculatedCost = jobPricingService.calculateCost(jobTask);

        // Verify the result
        assertEquals(expectedCost, calculatedCost, 0.000001, "The calculated cost should match the expected cost");
    }

    @Test
    void testCalculateCostWithThinkingTokens() {
        // Add thinking tokens to the job task
        jobTask.setThinkingTokens(200);
        
        // Update model cost with a thinking token cost
        modelCost.setThinkingToken1mCostUsd(5.0);
        
        // Mock repository response
        List<ModelCost> modelCosts = new ArrayList<>();
        modelCosts.add(modelCost);
        when(modelCostRepository.findCurrentCostsForModel(MODEL_ID)).thenReturn(modelCosts);

        // Calculate expected cost
        // (1000 * 2.5 + 500 * 10.0 + 200 * 5.0) / 1,000,000 = 0.0025 + 0.005 + 0.001 = 0.0085
        double expectedCost = 0.0085;

        // Test the method
        Double calculatedCost = jobPricingService.calculateCost(jobTask);

        // Verify the result
        assertEquals(expectedCost, calculatedCost, 0.000001, "The calculated cost should include thinking tokens");
    }

    @Test
    void testCalculateCostWithTokenRanges() {
        // Create multiple model costs with different token ranges
        ModelCost smallTokensCost = new ModelCost();
        smallTokensCost.setModelCostUuid(UUID.randomUUID());
        smallTokensCost.setModelId(MODEL_ID);
        smallTokensCost.setEffectiveBeginTimestamp(LocalDateTime.now().minusDays(30));
        smallTokensCost.setMinInputTokens(0);
        smallTokensCost.setMaxInputTokens(500);
        smallTokensCost.setInputToken1mCostUsd(1.0);
        smallTokensCost.setOutputToken1mCostUsd(5.0);
        smallTokensCost.setThinkingToken1mCostUsd(0.0);

        ModelCost mediumTokensCost = new ModelCost();
        mediumTokensCost.setModelCostUuid(UUID.randomUUID());
        mediumTokensCost.setModelId(MODEL_ID);
        mediumTokensCost.setEffectiveBeginTimestamp(LocalDateTime.now().minusDays(30));
        mediumTokensCost.setMinInputTokens(501);
        mediumTokensCost.setMaxInputTokens(2000);
        mediumTokensCost.setInputToken1mCostUsd(2.5);
        mediumTokensCost.setOutputToken1mCostUsd(10.0);
        mediumTokensCost.setThinkingToken1mCostUsd(0.0);

        ModelCost largeTokensCost = new ModelCost();
        largeTokensCost.setModelCostUuid(UUID.randomUUID());
        largeTokensCost.setModelId(MODEL_ID);
        largeTokensCost.setEffectiveBeginTimestamp(LocalDateTime.now().minusDays(30));
        largeTokensCost.setMinInputTokens(2001);
        largeTokensCost.setMaxInputTokens(null);
        largeTokensCost.setInputToken1mCostUsd(5.0);
        largeTokensCost.setOutputToken1mCostUsd(15.0);
        largeTokensCost.setThinkingToken1mCostUsd(0.0);

        // Mock repository response with all model costs
        List<ModelCost> modelCosts = new ArrayList<>();
        modelCosts.add(smallTokensCost);
        modelCosts.add(mediumTokensCost);
        modelCosts.add(largeTokensCost);
        when(modelCostRepository.findCurrentCostsForModel(MODEL_ID)).thenReturn(modelCosts);

        // Test with prompt tokens that fall in the medium range
        jobTask.setPromptTokens(1000);
        
        // Calculate expected cost using medium tier
        // (1000 * 2.5 + 500 * 10.0) / 1,000,000 = 0.0025 + 0.005 = 0.0075
        double expectedCost = 0.0075;

        // Test the method
        Double calculatedCost = jobPricingService.calculateCost(jobTask);

        // Verify the result
        assertEquals(expectedCost, calculatedCost, 0.000001, "The cost should be calculated using the medium tier");
        
        // Now test with large token range
        jobTask.setPromptTokens(3000);
        
        // Calculate expected cost using large tier
        // (3000 * 5.0 + 500 * 15.0) / 1,000,000 = 0.015 + 0.0075 = 0.0225
        expectedCost = 0.0225;
        
        // Test the method again
        calculatedCost = jobPricingService.calculateCost(jobTask);
        
        // Verify the result
        assertEquals(expectedCost, calculatedCost, 0.000001, "The cost should be calculated using the large tier");
    }

    @Test
    void testCalculateCostWithNullTokens() {
        // Create a job task with null token counts
        JobTask incompleteTask = new JobTask();
        incompleteTask.setJobTaskUuid(UUID.randomUUID());
        incompleteTask.setModelId(MODEL_ID);
        
        // Test the method
        Double calculatedCost = jobPricingService.calculateCost(incompleteTask);
        
        // Verify the result is null
        assertNull(calculatedCost, "Cost should be null when token counts are missing");
    }

    @Test
    void testCalculateCostWithNoModelCost() {
        // Mock repository to return empty list
        when(modelCostRepository.findCurrentCostsForModel(MODEL_ID)).thenReturn(new ArrayList<>());
        
        // Test the method
        Double calculatedCost = jobPricingService.calculateCost(jobTask);
        
        // Verify the result is null
        assertNull(calculatedCost, "Cost should be null when no price information is available");
    }
    
    @Test
    void testCalculateCostWithNoApplicableRange() {
        // Create model cost with range that doesn't match the job task
        modelCost.setMinInputTokens(5000);
        modelCost.setMaxInputTokens(10000);
        
        // Mock repository response
        List<ModelCost> modelCosts = new ArrayList<>();
        modelCosts.add(modelCost);
        when(modelCostRepository.findCurrentCostsForModel(MODEL_ID)).thenReturn(modelCosts);
        
        // Test the method (job task has 1000 tokens, which is outside the 5000-10000 range)
        Double calculatedCost = jobPricingService.calculateCost(jobTask);
        
        // Verify the result is null
        assertNull(calculatedCost, "Cost should be null when no applicable token range is found");
    }
}