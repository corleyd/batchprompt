# Auth0 Configuration for Waitlist Integration

This directory contains Auth0 Actions and configuration files for integrating the waitlist functionality with Auth0 authentication.

## Files

- `post-login-action.js` - Sets custom claims for user roles during login
- `post-registration-action.js` - Validates waitlist status during user registration
- `batchprompt-auth0-background-generator.html` - Generates Auth0 background images

## Setting Up Waitlist Validation in Auth0

### 1. Create Post-Registration Action

1. Go to Auth0 Dashboard → Actions → Flows
2. Select "Post User Registration" flow
3. Click "Add Action" → "Build Custom"
4. Name: "Waitlist Validation"
5. Copy the contents of `post-registration-action.js` into the code editor

### 2. Configure Secrets

In the Auth0 Action editor, add these secrets:

- `WAITLIST_API_URL`: Your API base URL (e.g., `https://api.batchprompt.ai`)

### 3. Add Dependencies

In the Auth0 Action editor, add this dependency:

```json
{
  "axios": "1.6.0"
}
```

### 4. Deploy the Action

1. Click "Deploy" in the Action editor
2. Go back to the "Post User Registration" flow
3. Drag the "Waitlist Validation" action into the flow
4. Click "Apply"

### 5. Test the Integration

1. **Test with invited user:**
   - Ensure a user has `INVITED` status in waitlist
   - Attempt Auth0 registration with their email
   - Should succeed and user gets `waitlist_status: INVITED` metadata

2. **Test with pending user:**
   - Ensure a user has `PENDING` status in waitlist
   - Attempt Auth0 registration with their email
   - Should fail with appropriate error message

3. **Test with non-waitlist user:**
   - Attempt Auth0 registration with email not in waitlist
   - Should fail with message to join waitlist first

## API Endpoint

The action calls: `POST /api/waitlist/public/validate-signup?email={email}`

**Success Response (200):**
```json
{
  "message": "User is authorized to register",
  "status": "INVITED"
}
```

**Error Response (403):**
```json
{
  "error": "not_in_waitlist",
  "message": "Email not found in waitlist. Please join the waitlist first at https://batchprompt.ai/request-access"
}
```

## Error Handling

- **API timeout/connection errors:** Registration is allowed to prevent service disruption
- **Waitlist validation errors:** Registration is denied with user-friendly messages
- **Missing email:** Registration is denied

## Monitoring

Check Auth0 Action logs for:
- Successful validations
- Failed validations and reasons
- API timeouts or errors

## Fallback Behavior

If the waitlist API is unavailable, the action allows registration but adds metadata indicating the validation failed due to technical issues. This prevents complete service disruption.

## Management API Access

Register your application for Management API access:

Go to your Auth0 Dashboard → Applications → APIs
Select the Auth0 Management API
Authorize your client application and grant the necessary scopes:
read:users
read:user_idp_tokens

