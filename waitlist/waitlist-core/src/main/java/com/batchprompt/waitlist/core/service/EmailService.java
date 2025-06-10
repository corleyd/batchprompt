package com.batchprompt.waitlist.core.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service for sending email notifications for waitlist events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "waitlist.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailService {

    private final JavaMailSender mailSender;
    
    @Value("${waitlist.email.from:noreply@batchprompt.com}")
    private String fromEmail;
    
    @Value("${app.base-url:https://batchprompt.com}")
    private String baseUrl;

    /**
     * Sends a waitlist signup confirmation email.
     */
    public void sendWaitlistSignupEmail(String email, String name) {
        try {
            String subject = "Welcome to the BatchPrompt Waitlist!";
            String htmlContent = generateSignupEmailHtml(name);
            String textContent = generateSignupEmailText(name);
            
            sendEmail(email, subject, htmlContent, textContent);
            log.info("Waitlist signup email sent successfully to: {}", email);
            
        } catch (Exception e) {
            log.error("Failed to send waitlist signup email to: {}", email, e);
        }
    }

    /**
     * Sends a waitlist invitation email.
     */
    public void sendWaitlistInvitationEmail(String email, String name, String company) {
        try {
            String subject = "🎉 You're invited to BatchPrompt!";
            String htmlContent = generateInvitationEmailHtml(name, company);
            String textContent = generateInvitationEmailText(name, company);
            
            sendEmail(email, subject, htmlContent, textContent);
            log.info("Waitlist invitation email sent successfully to: {}", email);
            
        } catch (Exception e) {
            log.error("Failed to send waitlist invitation email to: {}", email, e);
        }
    }

    /**
     * Sends feedback email to support team.
     */
    public void sendFeedbackEmail(String name, String email, String subject, String message) {
        try {
            String emailSubject = "[BatchPrompt Feedback] " + subject;
            String htmlContent = generateFeedbackEmailHtml(name, email, subject, message);
            String textContent = generateFeedbackEmailText(name, email, subject, message);
            
            // Send to support email
            sendEmail("support@batchprompt.ai", emailSubject, htmlContent, textContent);
            log.info("Feedback email sent successfully from: {}", email);
            
        } catch (Exception e) {
            log.error("Failed to send feedback email from: {}", email, e);
            throw new RuntimeException("Failed to send feedback email", e);
        }
    }

    private void sendEmail(String to, String subject, String htmlContent, String textContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setFrom(fromEmail);
        helper.setText(textContent, htmlContent);
        
        mailSender.send(message);
    }

    private String generateSignupEmailHtml(String name) {
        String statusUrl = baseUrl + "/waitlist/status";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to BatchPrompt</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f8f9fa; padding: 20px; text-align: center; border-radius: 8px; margin-bottom: 30px; }
                    .logo { font-size: 24px; font-weight: bold; color: #007bff; }
                    .content { padding: 20px 0; }
                    .cta-button { background-color: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 20px 0; }
                    .footer { border-top: 1px solid #eee; padding-top: 20px; margin-top: 30px; color: #666; font-size: 14px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="logo">BatchPrompt</div>
                    <p>Thank you for joining our waitlist!</p>
                </div>
                
                <div class="content">
                    <h2>Hi %s!</h2>
                    
                    <p>Welcome to the BatchPrompt waitlist! We're thrilled to have you join us in discovering how AI batch processing can simplify and speed up your workflow.</p>
                    
                    <p>We're working hard to onboard users as quickly as possible while ensuring a great experience for everyone. You'll receive an email notification as soon as your invitation is ready.</p>
                    
                    <p>In the meantime, you can:</p>
                    <ul>
                        <li>Check your waitlist status anytime using the link below</li>
                        <li>Follow us for updates and tips</li>
                        <li>Explore our documentation to prepare for your access</li>
                    </ul>
                    
                    <a href="%s" class="cta-button">Check Your Status</a>
                    
                    <p>Thank you for your patience, and we can't wait to welcome you to BatchPrompt!</p>
                    
                    <p>Best regards,<br>The BatchPrompt Team</p>
                </div>
                
                <div class="footer">
                    <p>This email was sent because you signed up for the BatchPrompt waitlist. If you have any questions, please contact us at support@batchprompt.com.</p>
                </div>
            </body>
            </html>
            """, statusUrl);
    }

    private String generateSignupEmailText(String name) {
        String statusUrl = baseUrl + "/waitlist/status";
        
        return String.format("""
            
            Welcome to the BatchPrompt waitlist! We're excited to have you join our community.
            
            We're working hard to onboard users as quickly as possible. You'll receive an email notification as soon as your invitation is ready.
            
            Check your waitlist status anytime: %s
            
            Thank you for your patience!
            
            Best regards,
            The BatchPrompt Team
            """, statusUrl);
    }

    private String generateInvitationEmailHtml(String name, String company) {
        String signupUrl = baseUrl + "/signup";
        String companySection = (company != null && !company.isEmpty()) ? 
            String.format("<p>We're particularly excited to see how <strong>%s</strong> will use BatchPrompt to enhance your AI workflows.</p>", company) : "";
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>You're Invited to BatchPrompt!</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #007bff, #0056b3); color: white; padding: 30px; text-align: center; border-radius: 8px; margin-bottom: 30px; }
                    .logo { font-size: 28px; font-weight: bold; margin-bottom: 10px; }
                    .invitation-badge { background-color: #28a745; color: white; padding: 8px 20px; border-radius: 25px; display: inline-block; font-weight: bold; margin: 10px 0; }
                    .content { padding: 20px 0; }
                    .cta-button { background-color: #28a745; color: white; padding: 15px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 25px 0; font-weight: bold; font-size: 16px; }
                    .features { background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }
                    .feature-item { margin: 10px 0; padding-left: 20px; position: relative; }
                    .feature-item:before { content: "✓"; color: #28a745; font-weight: bold; position: absolute; left: 0; }
                    .footer { border-top: 1px solid #eee; padding-top: 20px; margin-top: 30px; color: #666; font-size: 14px; }
                    .urgency { background-color: #fff3cd; border: 1px solid #ffeaa7; padding: 15px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                
                <div class="content">
                    
                    <p>Great news! You've been approved for access to BatchPrompt, and we're excited to welcome you to our platform.</p>
                    
                    %s
                    
                    <div class="urgency">
                        <strong>⏰ Ready to get started?</strong> Your invitation is active and ready to use. Click below to create your account and start using BatchPrompt today.
                    </div>
                    
                    <div style="text-align: center;">
                        <a href="%s" class="cta-button">Create Your Account</a>
                    </div>
                    
                    <div class="features">
                        <h3>What you can do with BatchPrompt:</h3>
                        <div class="feature-item">Process large batches of prompts efficiently</div>
                        <div class="feature-item">Access multiple AI models from one platform</div>
                        <div class="feature-item">Track usage and manage costs effectively</div>
                    </div>
                    
                    <p>If you have any questions or need help getting started, our support team is here to help at support@batchprompt.com.</p>
                    
                    <p>Welcome to BatchPrompt!</p>
                    
                    <p>Best regards,<br>The BatchPrompt Team</p>
                </div>
                
                <div class="footer">
                    <p>This invitation was sent because you were approved from the BatchPrompt waitlist. This link will remain active for 7 days.</p>
                </div>
            </body>
            </html>
            """, name, companySection, signupUrl);
    }

    private String generateInvitationEmailText(String name, String company) {
        String signupUrl = baseUrl + "/signup";
        String companySection = (company != null && !company.isEmpty()) ? 
            String.format("We're excited to see how %s will use BatchPrompt!", company) : "";
        
        return String.format("""
            Congratulations %s!
            
            Great news! You've been approved for access to BatchPrompt.
            
            %s
            
            Your invitation is ready! Create your account here: %s
            
            What you can do with BatchPrompt:
            • Process large batches of prompts efficiently
            • Access multiple AI models from one platform  
            • Track usage and manage costs effectively
            
            If you have any questions, contact us at support@batchprompt.com.
            
            Welcome to BatchPrompt!
            
            Best regards,
            The BatchPrompt Team
            """, name, companySection, signupUrl);
    }

    private String generateFeedbackEmailHtml(String name, String email, String subject, String message) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>BatchPrompt Feedback</title>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #f8f9fa; padding: 20px; text-align: center; border-radius: 8px; margin-bottom: 30px; }
                    .logo { font-size: 24px; font-weight: bold; color: #007bff; }
                    .content { padding: 20px 0; }
                    .info-section { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .message-section { background-color: #fff; padding: 20px; border: 1px solid #dee2e6; border-radius: 5px; margin: 20px 0; }
                    .footer { border-top: 1px solid #eee; padding-top: 20px; margin-top: 30px; color: #666; font-size: 14px; }
                    .label { font-weight: bold; color: #495057; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="logo">BatchPrompt</div>
                    <p>New Feedback Received</p>
                </div>
                
                <div class="content">
                    <div class="info-section">
                        <h3>Contact Information</h3>
                        <p><span class="label">Name:</span> %s</p>
                        <p><span class="label">Email:</span> %s</p>
                        <p><span class="label">Subject:</span> %s</p>
                    </div>
                    
                    <div class="message-section">
                        <h3>Message</h3>
                        <div style="white-space: pre-wrap; background: #f8f9fa; padding: 15px; border-radius: 3px;">%s</div>
                    </div>
                </div>
                
                <div class="footer">
                    <p>This email was sent from the BatchPrompt contact form at %s.</p>
                </div>
            </body>
            </html>
            """, 
            escapeHtml(name), 
            escapeHtml(email), 
            escapeHtml(subject), 
            escapeHtml(message),
            java.time.LocalDateTime.now().toString()
        );
    }

    private String generateFeedbackEmailText(String name, String email, String subject, String message) {
        return String.format("""
            New Feedback Received - BatchPrompt

            Contact Information:
            Name: %s
            Email: %s  
            Subject: %s

            Message:
            %s

            ---
            This email was sent from the BatchPrompt contact form at %s.
            """, 
            name, 
            email, 
            subject, 
            message,
            java.time.LocalDateTime.now().toString()
        );
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }
}