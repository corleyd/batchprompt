package com.batchprompt.common.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class CommonCoreAutoConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {

        // Configure ObjectMapper with Page deserializer
        SimpleModule pageModule = new SimpleModule();
        pageModule.addDeserializer(Page.class, new PageDeserializer());
        
        return builder -> 
        {
            // Register the custom deserializer for Page objects
            builder.modulesToInstall(pageModule);
            builder.modulesToInstall(new JavaTimeModule());
            builder.postConfigurer(objectMapper -> {
                // Disable timestamp serialization, use ISO-8601 format instead
                objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                
                // Ensure that time zone information is included
                objectMapper.configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, false);
                
                // Be lenient when deserializing unknown properties
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);            
            });
        
        }; 
    }    

    /**
     * Custom deserializer for Spring Data Page objects
     * This generic implementation keeps content as Maps so that specialized deserializers
     * can convert them to proper types when needed.
     */
    private static class PageDeserializer extends JsonDeserializer<Page<?>> {
        @Override
        public Page<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            ObjectMapper mapper = (ObjectMapper) p.getCodec();
            JsonNode node = mapper.readTree(p);
            
            // Extract content and create list of elements - keep as raw objects
            List<Object> content = new ArrayList<>();
            if (node.has("content")) {
                JsonNode contentNode = node.get("content");
                // Deserialize content as an array of objects
                content = mapper.readValue(contentNode.traverse(), 
                        mapper.getTypeFactory().constructCollectionType(List.class, Object.class));
            }
            
            // Extract pagination information
            int number = node.has("number") ? node.get("number").asInt(0) : 0;
            int size = node.has("size") ? node.get("size").asInt(10) : 10;
            long totalElements = node.has("totalElements") ? node.get("totalElements").asLong(0) : 0;
            
            return new PageImpl<>(content, PageRequest.of(number, size), totalElements);
        }
    }
    

}
