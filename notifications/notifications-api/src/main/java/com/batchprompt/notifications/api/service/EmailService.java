package com.batchprompt.notifications.api.service;

import com.batchprompt.notifications.model.EmailNotification;
import com.batchprompt.notifications.model.EmailTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

/**
 * Service for sending email notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "services.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;
    
    @Value("${services.email.from:noreply@batchprompt.com}")
    private String defaultFromEmail;
    
    @Value("${app.base-url:https://batchprompt.com}")
    private String baseUrl;

    /**
     * Sends an email notification.
     */
    public void sendEmail(EmailNotification emailNotification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(emailNotification.getTo());
            helper.setSubject(emailNotification.getSubject());
            helper.setFrom(emailNotification.getFrom() != null ? emailNotification.getFrom() : defaultFromEmail);
            
            if (emailNotification.getHtmlContent() != null) {
                helper.setText(emailNotification.getTextContent(), emailNotification.getHtmlContent());
            } else {
                helper.setText(emailNotification.getTextContent());
            }
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", emailNotification.getTo());
            
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", emailNotification.getTo(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Sends a waitlist signup confirmation email.
     */
    public void sendWaitlistSignupEmail(String email, String name, int position) {
        Map<String, Object> variables = Map.of(
            EmailTemplate.WaitlistSignup.NAME, name,
            EmailTemplate.WaitlistSignup.POSITION, position,
            EmailTemplate.WaitlistSignup.STATUS_URL, baseUrl + "/waitlist/status"
        );
        
        EmailNotification notification = templateService.generateEmailFromTemplate(
            EmailTemplate.WAITLIST_SIGNUP,
            email,
            variables
        );
        
        sendEmail(notification);
    }

    /**
     * Sends a waitlist invitation email.
     */
    public void sendWaitlistInvitationEmail(String email, String name, String company) {
        Map<String, Object> variables = Map.of(
            EmailTemplate.WaitlistInvitation.NAME, name,
            EmailTemplate.WaitlistInvitation.COMPANY, company != null ? company : "",
            EmailTemplate.WaitlistInvitation.SIGNUP_URL, baseUrl + "/signup"
        );
        
        EmailNotification notification = templateService.generateEmailFromTemplate(
            EmailTemplate.WAITLIST_INVITATION,
            email,
            variables
        );
        
        sendEmail(notification);
    }
}