package com.batchprompt.files.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.batchprompt.files.model.dto.FileRecordDto;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Configuration
@ComponentScan
public class FilesClientAutoConfig {
    
    /**
     * Creates a Jackson module for Page<FileRecordDto> deserialization
     * This version breaks the circular dependency by not injecting ObjectMapper
     */
    @Bean
    public SimpleModule fileRecordPageModule() {
        SimpleModule module = new SimpleModule("FileRecordPageModule");
        
        // Add a BeanDeserializerModifier that will handle Page<FileRecordDto> deserialization
        module.setDeserializerModifier(new BeanDeserializerModifier() {
            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, 
                                                         BeanDescription beanDesc, 
                                                         JsonDeserializer<?> deserializer) {
                JavaType type = beanDesc.getType();
                
                // If this is a Page type, let's check if it's a Page<FileRecordDto>
                if (type.isTypeOrSubTypeOf(Page.class) && type.containedTypeCount() > 0) {
                    JavaType contentType = type.containedType(0);
                    if (contentType.isTypeOrSubTypeOf(FileRecordDto.class)) {
                        // This is a Page<FileRecordDto>, so replace the deserializer
                        return new FileRecordPageDeserializer();
                    }
                }
                return deserializer;
            }
        });
        
        return module;
    }
    
    /**
     * Custom deserializer that specifically handles Page<FileRecordDto> objects
     * Modified to avoid circular dependency by not requiring ObjectMapper injection
     */
    private static class FileRecordPageDeserializer extends JsonDeserializer<Page<FileRecordDto>> {
        
        @Override
        public Page<FileRecordDto> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            // Get the mapper from the parser rather than injecting it
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode node = mapper.readTree(p);
            
            // Extract content and convert to FileRecordDto objects
            List<FileRecordDto> content = new ArrayList<>();
            if (node.has("content")) {
                JsonNode contentNode = node.get("content");
                
                // First deserialize to a list of maps
                List<Map<String, Object>> contentMaps = mapper.convertValue(
                    contentNode, 
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                
                // Then convert each map to FileRecordDto
                for (Map<String, Object> map : contentMaps) {
                    FileRecordDto dto = mapper.convertValue(map, FileRecordDto.class);
                    content.add(dto);
                }
            }
            
            // Extract pagination information
            int number = node.has("number") ? node.get("number").asInt(0) : 0;
            int size = node.has("size") ? node.get("size").asInt(10) : 10;
            long totalElements = node.has("totalElements") ? node.get("totalElements").asLong(0) : 0;
            
            return new PageImpl<>(content, PageRequest.of(number, size), totalElements);
        }
    }
    
    /**
     * Jackson Module registrar that runs after the ObjectMapper is fully initialized
     * This breaks the circular dependency by registering our module after ObjectMapper is created
     */
    @Configuration
    public static class JacksonModuleRegistrar {
        
        @Bean
        public Object jacksonModuleRegistrar(ObjectMapper objectMapper, SimpleModule fileRecordPageModule) {
            // Register our module with the fully constructed ObjectMapper
            objectMapper.registerModule(fileRecordPageModule);
            
            // Return a dummy bean - its only purpose is to ensure module registration happens
            return new Object();
        }
    }
}
