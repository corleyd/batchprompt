package com.batchprompt.jobs.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

public class HeaderUtil {
    
    private HeaderUtil() {
        // Utility class, no instantiation
    }
    
    public static HttpEntity<?> createEntityWithAuthHeader(String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        return new HttpEntity<>(headers);
    }
    
    public static <T> HttpEntity<T> createEntityWithAuthHeader(T body, String authToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authToken);
        return new HttpEntity<>(body, headers);
    }
}