package com.batchprompt.notifications.model;

/**
 * Email template constants for different notification types.
 */
public class EmailTemplate {
    
    public static final String WAITLIST_SIGNUP = "waitlist-signup";
    public static final String WAITLIST_INVITATION = "waitlist-invitation";
    
    /**
     * Template variables for waitlist signup email.
     */
    public static class WaitlistSignup {
        public static final String NAME = "name";
        public static final String POSITION = "position";
        public static final String STATUS_URL = "statusUrl";
    }
    
    /**
     * Template variables for waitlist invitation email.
     */
    public static class WaitlistInvitation {
        public static final String NAME = "name";
        public static final String SIGNUP_URL = "signupUrl";
        public static final String COMPANY = "company";
    }
}