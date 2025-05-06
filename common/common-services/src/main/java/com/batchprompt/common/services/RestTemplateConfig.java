package com.batchprompt.common.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        // Configure ObjectMapper with Page deserializer
        SimpleModule pageModule = new SimpleModule();
        pageModule.addDeserializer(Page.class, new PageDeserializer());
        objectMapper.registerModule(pageModule);
        
        // Create message converter with our configured ObjectMapper
        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        
        // Create a FormHttpMessageConverter that supports multipart/form-data
        FormHttpMessageConverter formConverter = new FormHttpMessageConverter();
        formConverter.setSupportedMediaTypes(Arrays.asList(
            MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.MULTIPART_FORM_DATA
        ));
        
        return builder
                .requestFactory(() -> {
                    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(5000);
                    factory.setReadTimeout(30000);
                    return factory;
                })
                // Add both converters - the form converter must come before the Jackson converter
                .messageConverters(formConverter, jacksonConverter)
                .build();
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