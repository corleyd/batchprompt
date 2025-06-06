package com.batchprompt.notifications.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Email notification model containing email-specific fields.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotification {
    
    /**
     * The recipient's email address.
     */
    private String to;
    
    /**
     * The email subject line.
     */
    private String subject;
    
    /**
     * The HTML content of the email.
     */
    private String htmlContent;
    
    /**
     * The plain text content of the email (fallback).
     */
    private String textContent;
    
    /**
     * Optional sender email (if different from default).
     */
    private String from;
    
    /**
     * Template name for email generation.
     */
    private String templateName;
    
    /**
     * Template variables for dynamic content.
     */
    private Object templateVariables;
}