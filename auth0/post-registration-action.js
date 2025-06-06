/**
 * Auth0 Post-Registration Action to validate waitlist status
 * This action runs after a user registers but before the registration is complete.
 * It calls the waitlist API to verify the user is invited before allowing registration.
 * 
 * @param {Event} event - Details about the user and the context in which they are registering.
 * @param {PostUserRegistrationAPI} api - Interface whose methods can be used to change the behavior of the registration.
 */
exports.onExecutePostUserRegistration = async (event, api) => {
  const axios = require('axios');
  
  // Get the user's email from the registration event
  const userEmail = event.user.email;
  
  if (!userEmail) {
    api.access.deny('Registration failed: Email is required');
    return;
  }
  
  try {
    // Call the waitlist validation endpoint
    // Note: Update this URL to match your production API URL
    const apiUrl = event.secrets.WAITLIST_API_URL || 'https://api.batchprompt.ai';
    const validationUrl = `${apiUrl}/api/waitlist/public/validate-signup`;
    
    console.log(`Validating waitlist status for email: ${userEmail}`);
    
    const response = await axios.post(validationUrl, null, {
      params: { email: userEmail },
      timeout: 5000,
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'Auth0-PostRegistration-Action'
      }
    });
    
    // If we get here, the user is authorized (status 200)
    console.log(`Waitlist validation successful for ${userEmail}: ${response.data.message}`);
    
    // Optionally, you can add metadata to the user profile
    api.user.setAppMetadata('waitlist_status', 'INVITED');
    api.user.setAppMetadata('registration_source', 'waitlist_invitation');
    
  } catch (error) {
    console.error(`Waitlist validation failed for ${userEmail}:`, error.message);
    
    if (error.response) {
      // The API responded with an error status
      const errorData = error.response.data;
      let errorMessage = 'Registration not allowed.';
      
      if (errorData.error === 'not_in_waitlist') {
        errorMessage = 'Please join the waitlist first at https://batchprompt.ai/request-access';
      } else if (errorData.error === 'invalid_status') {
        errorMessage = errorData.message || 'Your waitlist status does not allow registration at this time.';
      }
      
      // Deny the registration with a user-friendly message
      api.access.deny(`Registration failed: ${errorMessage}`);
      
    } else if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      // Timeout error - fail gracefully but allow registration to prevent service disruption
      console.error(`Waitlist API timeout for ${userEmail}, allowing registration`);
      api.user.setAppMetadata('waitlist_validation', 'timeout_allowed');
      
    } else {
      // Other network/connection errors - fail gracefully
      console.error(`Waitlist API error for ${userEmail}, allowing registration:`, error.message);
      api.user.setAppMetadata('waitlist_validation', 'error_allowed');
    }
  }
};

/**
 * Optional: Handler for when the action is continued (if using async flows)
 * Currently not needed for this implementation.
 */
exports.onContinuePostUserRegistration = async (event, api) => {
  // Not implemented - included for completeness
};