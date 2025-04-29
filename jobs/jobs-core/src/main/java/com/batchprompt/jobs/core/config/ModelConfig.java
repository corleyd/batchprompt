package com.batchprompt.jobs.core.config;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.batchprompt.jobs.core.service.ModelProvider;

@Configuration
@ConfigurationProperties(prefix = "models")
public class ModelConfig {
    
    private List<ModelDefinition> supported;
    
    public List<ModelDefinition> getSupported() {
        return supported;
    }
    
    public void setSupported(List<ModelDefinition> supported) {
        this.supported = supported;
    }
    
    public static class ModelDefinition {
        private String name;
        private ModelProvider provider;
        private String queue;
        private Map<String, Object> properties = new HashMap<>();
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public ModelProvider getProvider() {
            return provider;
        }
        
        public void setProvider(ModelProvider provider) {
            this.provider = provider;
        }
        
        public String getQueue() {
            return queue;
        }
        
        public void setQueue(String queue) {
            this.queue = queue;
        }
        
        public Map<String, Object> getProperties() {
            return properties;
        }
        
        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }
}