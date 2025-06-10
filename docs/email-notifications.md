# Email Notifications for Waitlist

This document describes the email notification system that sends automated emails when users join the waitlist and when they are invited.

## Overview

The email notification system consists of:

1. **EmailService**: Service for sending emails using JavaMailSender (located in waitlist-core)
2. **Email Templates**: Pre-designed HTML and text email templates built into the EmailService
3. **WaitlistService Integration**: Automatically sends emails on waitlist events

## Email Types

### 1. Waitlist Signup Confirmation
**Trigger**: When a user joins the waitlist  
**Template**: `waitlist-signup`  
**Content**: Welcome message with queue position and status check link

### 2. Waitlist Invitation
**Trigger**: When an admin invites a user  
**Template**: `waitlist-invitation`  
**Content**: Invitation announcement with signup link

## Configuration

### Email Service Configuration

Add the following to your environment or `local.yml`:

```yaml
# Waitlist Email Settings
WAITLIST_EMAIL_ENABLED=true
WAITLIST_EMAIL_FROM=noreply@batchprompt.ai

# SMTP Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Application Settings
APP_BASE_URL=https://batchprompt.com
```

### Gmail Configuration
1. Enable 2-Factor Authentication in your Google account
2. Generate an App Password for the application
3. Use the App Password as `MAIL_PASSWORD`

### Development/Testing
For development, you can disable email notifications:
```yaml
WAITLIST_EMAIL_ENABLED=false
```

## Flow Diagram

```
User Joins Waitlist → WaitlistService.joinWaitlist() → EmailService.sendWaitlistSignupEmail()
                                                                  ↓
                                                          HTML/Text Email Sent

Admin Invites User → WaitlistService.inviteUser() → EmailService.sendWaitlistInvitationEmail()
                                                                 ↓
                                                         HTML/Text Email Sent
```

## Testing

To test the email notifications:

1. **Setup Email Configuration**: Configure SMTP settings in your environment
2. **Start Services**: Start the waitlist-api service
3. **Join Waitlist**: Make a POST request to `/api/waitlist/public/join`
4. **Invite User**: Make a POST request to `/api/waitlist/admin/invite/{entryId}`

Expected behavior:
- User receives a welcome email after joining
- User receives an invitation email when invited by admin

## Troubleshooting

### Common Issues

1. **Emails not sending**
   - Check WAITLIST_EMAIL_ENABLED=true
   - Verify SMTP credentials
   - Check logs for authentication errors

2. **Gmail authentication failures**
   - Ensure 2FA is enabled
   - Use App Password, not regular password
   - Check for "Less secure app access" restrictions

3. **JavaMail configuration issues**
   - Verify MAIL_HOST and MAIL_PORT are correct
   - Check that waitlist-api service is running
   - Review mail configuration in application.yml

### Log Messages
Look for these log entries:
```
INFO  - Waitlist signup email sent successfully to: user@example.com
INFO  - Waitlist invitation email sent successfully to: user@example.com
ERROR - Failed to send waitlist signup email to: user@example.com
```

## Customization

### Email Templates
Templates are defined in `EmailService.java` methods and can be customized by:
1. Modifying the HTML/text content in `generateSignupEmailHtml()` and `generateInvitationEmailHtml()`
2. Adding new template variables
3. Creating new email methods

### Adding New Email Types
1. Add new email method to `EmailService.java` (e.g., `sendReminderEmail()`)
2. Add corresponding private methods for HTML/text generation
3. Call the new method from `WaitlistService` at appropriate events

## Security Considerations

- Store SMTP credentials securely (environment variables, not in code)
- Use App Passwords for Gmail (never store real passwords)
- Consider using dedicated email services for production (SendGrid, SES, etc.)
- Validate email addresses before sending
- Implement rate limiting for email sending