package com.batchprompt.waitlist.api.controller;

import com.batchprompt.waitlist.core.service.EmailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling user feedback submissions.
 */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final EmailService emailService;

    @PostMapping("/submit")
    public ResponseEntity<FeedbackResponse> submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        try {
            log.info("Received feedback submission from: {}", request.getEmail());
            
            // Send feedback email
            emailService.sendFeedbackEmail(
                request.getName(),
                request.getEmail(), 
                request.getSubject(),
                request.getMessage()
            );
            
            log.info("Feedback email sent successfully for: {}", request.getEmail());
            
            return ResponseEntity.ok(new FeedbackResponse("Feedback submitted successfully"));
            
        } catch (Exception e) {
            log.error("Failed to send feedback email for: {}", request.getEmail(), e);
            return ResponseEntity.internalServerError()
                    .body(new FeedbackResponse("Failed to submit feedback. Please try again."));
        }
    }

    @Data
    public static class FeedbackRequest {
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;

        @NotBlank(message = "Subject is required")
        @Size(max = 200, message = "Subject cannot exceed 200 characters")
        private String subject;

        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message cannot exceed 2000 characters")
        private String message;
    }

    @Data
    public static class FeedbackResponse {
        private final String message;
    }
}