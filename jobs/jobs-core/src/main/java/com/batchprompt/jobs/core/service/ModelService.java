package com.batchprompt.jobs.core.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.batchprompt.jobs.core.QueueHelper;
import com.batchprompt.jobs.core.model.Model;
import com.batchprompt.jobs.core.model.ModelProviderEntity;
import com.batchprompt.jobs.core.repository.ModelProviderRepository;
import com.batchprompt.jobs.core.repository.ModelRepository;
import com.batchprompt.jobs.model.dto.ModelDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ModelService {
    
    // Map of model name to ChatModel instance
    private final Map<String, AbstractChatModel> modelMap = new HashMap<>();
    
    // Map of model name to queue name
    private final Map<String, String> queueMap = new HashMap<>();
    
    // Store original model definitions for access to additional properties
    private final List<Model> modelDefinitions = new ArrayList<>();
    
    private final ModelRepository modelRepository;
    private final ModelProviderRepository providerRepository;
    private final QueueHelper queueHelper;
    private final String openaiApiKey;
    private final String googleApiKey;
    private final String xaiApiKey;
    
    /**
     * Constructor to initialize dependencies
     */
    public ModelService(
            ModelRepository modelRepository,
            ModelProviderRepository providerRepository,
            QueueHelper queueHelper,
            ObjectMapper objectMapper,
            @Value("${openai.api-key}") String openaiApiKey,
            @Value("${google.api-key:#{null}}") String googleApiKey,
            @Value("${xai.api-key:#{null}}") String xaiApiKey) {
        
        this.modelRepository = modelRepository;
        this.providerRepository = providerRepository;
        this.queueHelper = queueHelper;
        this.openaiApiKey = openaiApiKey;
        this.googleApiKey = googleApiKey;
        this.xaiApiKey = xaiApiKey;
    }
    
    /**
     * Initialize the model mappings when the application context is refreshed
     */
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        loadModelsFromDatabase();
    }
    
    /**
     * Load all active models from the database and initialize them
     */
    private void loadModelsFromDatabase() {
        log.info("Loading models from database");
        
        // Clear existing maps in case of refresh
        modelMap.clear();
        queueMap.clear();
        modelDefinitions.clear();
        
        // Get all enabled models from the database
        List<Model> enabledModels = modelRepository.findAll();
        
        for (Model modelDef : enabledModels) {
            AbstractChatModel model = null;
            
            try {
                String modelProviderId = modelDef.getProvider().getModelProviderId();
                
                switch (modelProviderId) {
                    case "OPENAI":
                        model = new OpenAIChatModel(modelDef, openaiApiKey);
                        break;
                        
                    case "AWS":
                        model = new AwsConverseChatModel(modelDef);
                        break;
                        
                    case "GOOGLE":
                        model = new GeminiChatModel(modelDef, googleApiKey);
                        break;
                        
                    case "XAI":
                        model = new XaiChatModel(modelDef, xaiApiKey);
                        break;
                        
                    default:
                        log.warn("Unsupported model provider: {}", modelProviderId);
                        continue;
                }
                
                if (model != null) {
                    log.info("Initialized model: {}", modelDef.getModelId());
                    modelMap.put(model.getModelId(), model);
                    
                    // Store the queue name for this model
                    if (modelDef.getTaskQueueName() != null && !modelDef.getTaskQueueName().isEmpty()) {
                        queueMap.put(model.getModelId(), modelDef.getTaskQueueName());
                    }
                    
                    // Store the original model definition
                    modelDefinitions.add(modelDef);
                }

                if (modelDef.getTaskQueueName() != null) {
                    queueHelper.createAndBindQueue(modelDef.getTaskQueueName());
                }
            } catch (Exception e) {
                log.error("Failed to initialize model {}: {}", modelDef.getModelId(), e.getMessage(), e);
            }
        }
        
        log.info("Loaded {} models from database", modelDefinitions.size());
    }
    
    /**
     * Check if the given model name is supported
     * 
     * @param modelName The name of the model to check
     * @return true if the model is supported, false otherwise
     */
    public boolean isModelSupported(String modelName) {
        return modelMap.containsKey(modelName);
    }
    
    /**
     * Get a list of all supported models with detailed information
     * 
     * @return List of ModelDto objects with detailed model information
     */
    public List<ModelDto> getSupportedModelDetails() {
        return modelDefinitions.stream()
            .map(modelDef -> ModelDto.builder()
                .modelId(modelDef.getModelId())
                .modelProviderId(modelDef.getProvider().getModelProviderId())
                .displayName(modelDef.getDisplayName())
                .modelProviderDisplayName(modelDef.getProvider().getDisplayName())
                .taskQueueName(modelDef.getTaskQueueName())
                .modelProviderProperties(modelDef.getModelProviderProperties())
                .build()
            )
            .sorted(Comparator.comparing(ModelDto::getModelId))
            .collect(Collectors.toList());
    }

    /**
     * Get a ChatModel instance by model name
     *
     * @param modelId The id of the model
     * @return The ChatModel instance, or null if not found
     */
    public AbstractChatModel getChatModel(String modelId) {
        return modelMap.get(modelId);
    }
    
    /**
     * Get the queue name for a specific model
     *
     * @param modelId The id of the model
     * @return The queue name for the model, or null if not found
     */
    public String getQueueForModel(String modelId) {
        return queueMap.get(modelId);
    }
    
    /**
     * Refresh models from the database
     * Can be called programmatically or via an admin endpoint to reload models without restarting
     */
    public void refreshModels() {
        loadModelsFromDatabase();
    }
    
    /**
     * Get all available model providers
     * 
     * @return List of all model providers
     */
    public List<ModelProviderEntity> getAllProviders() {
        return providerRepository.findAll();
    }
    
    /**
     * Get all enabled model providers
     * 
     * @return List of enabled model providers
     */
    public List<ModelProviderEntity> getEnabledProviders() {
        return providerRepository.findAll();
    }
    
    /**
     * Get a provider by ID
     * 
     * @param providerId The provider ID to look up
     * @return An optional containing the provider if found
     */
    public Optional<ModelProviderEntity> getProviderById(String modelProviderId) {
        return providerRepository.findById(modelProviderId);
    }
}