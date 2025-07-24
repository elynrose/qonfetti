# Authentication Troubleshooting Guide

## Issue: "Failed to fetch customer points" - 401 Unauthorized

### Problem Description
The app is showing "Failed to fetch customer points" errors with 401 Unauthorized status codes. This typically happens when:

1. **Session Expired**: The authentication token has expired
2. **Invalid Token**: The stored token is no longer valid
3. **Server Issues**: Authentication service is temporarily unavailable

### What the App Now Does

#### ✅ **Automatic Session Management**
- **Detects 401 Errors**: App automatically detects authentication failures
- **Clears Session**: Automatically clears expired session data
- **Redirects to Login**: Takes user back to login screen
- **User-Friendly Messages**: Shows "Session expired. Please login again."

#### ✅ **Error Handling Improvements**
- **Customer List**: Handles auth errors gracefully
- **Customer Detail**: Manages NFC operations with auth checks
- **NFC Registration**: Properly handles auth failures during card registration

### How to Fix

#### **For Users:**
1. **Restart the App**: Close and reopen the app
2. **Login Again**: Enter your credentials when prompted
3. **Check Internet**: Ensure stable internet connection
4. **Try Again**: The app will automatically redirect to login

#### **For Developers:**
1. **Check Logs**: Look for "Authentication failed, clearing session" messages
2. **Verify Supabase**: Ensure Supabase authentication is working
3. **Test Login Flow**: Verify login/registration works properly
4. **Check Token Storage**: Verify SessionStorage is working correctly

### Technical Details

#### **Error Detection**
```kotlin
// App now checks for 401 errors in all API calls
if (exception.message?.contains("401") == true || 
    exception.message?.contains("Unauthorized") == true) {
    sessionStorage.clearSession()
    // Redirect to login
}
```

#### **Automatic Redirect**
```kotlin
// MainActivity automatically handles auth state changes
LaunchedEffect(customerUiState) {
    val currentState = customerUiState
    if (currentState is CustomerUiState.Error && 
        currentState.message.contains("Session expired")) {
        viewModel.logout()
        showCustomers = false
    }
}
```

### Prevention Tips

#### **For Better User Experience:**
1. **Regular Token Refresh**: Implement token refresh before expiration
2. **Background Auth Check**: Periodically validate session
3. **Graceful Degradation**: Show cached data when offline
4. **Clear Error Messages**: Provide specific guidance to users

#### **For Development:**
1. **Test Token Expiry**: Simulate expired tokens during testing
2. **Monitor Auth Logs**: Track authentication patterns
3. **Implement Retry Logic**: Add automatic retry for transient failures
4. **Add Offline Support**: Cache data for offline access

### Common Scenarios

#### **Scenario 1: App Left Open for Hours**
- **What Happens**: Token expires while app is open
- **User Experience**: Next API call triggers login redirect
- **Solution**: User logs in again

#### **Scenario 2: Network Issues**
- **What Happens**: Temporary connectivity problems
- **User Experience**: Clear error message with retry option
- **Solution**: Check connection and try again

#### **Scenario 3: Server Maintenance**
- **What Happens**: Supabase authentication service unavailable
- **User Experience**: Appropriate error message
- **Solution**: Wait and try again later

### Testing Authentication

#### **Manual Testing:**
1. **Login**: Verify successful authentication
2. **Use App**: Navigate to customer list/detail
3. **Simulate Expiry**: Wait for token to expire or clear manually
4. **Test Error Handling**: Verify redirect to login works

#### **Automated Testing:**
```kotlin
// Test authentication error handling
@Test
fun testAuthErrorHandling() {
    // Simulate 401 error
    // Verify session is cleared
    // Verify redirect to login
}
```

### Support

If you continue to experience authentication issues:

1. **Check App Logs**: Look for detailed error messages
2. **Verify Credentials**: Ensure email/password are correct
3. **Clear App Data**: Reset app data if needed
4. **Contact Support**: Provide logs and error details

---

**Note**: The app now handles authentication failures gracefully and provides a better user experience when sessions expire. 