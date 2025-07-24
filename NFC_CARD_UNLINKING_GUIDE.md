# NFC Card Unlinking Guide

## Overview

The customer detail page now includes enhanced NFC card management functionality that allows users to view member IDs associated with NFC cards and unlink them from the database. This feature provides better control over NFC card associations and helps maintain data integrity.

## New Features

### 1. Member ID Display
- **Enhanced NFC Card Display**: Each NFC card now shows the associated member ID in a prominent, highlighted section
- **Clear Association**: The UI clearly indicates "This member ID [ID] has been linked to a card"
- **Visual Distinction**: Member ID information is displayed in a separate card with a subtle background color

### 2. Unlink Functionality
- **Unlink Button**: Each NFC card with a member ID has a "To unlink the card click here" button
- **Confirmation Dialog**: Users must confirm the unlinking action before proceeding
- **Database Deletion**: Unlinking completely removes the NFC card record from the database (not just deactivation)

## Technical Implementation

### Backend Changes

#### 1. New API Endpoint
```kotlin
// Added to SupabaseApi.kt
suspend fun deleteNfcCard(cardId: String, authToken: String): Result<Boolean>
```
- **Method**: DELETE
- **Endpoint**: `/rest/v1/nfc_cards?card_id=eq.{cardId}`
- **Purpose**: Completely removes NFC card from database

#### 2. ViewModel Enhancement
```kotlin
// Added to CustomerDetailViewModel.kt
fun unlinkNfcCard(cardId: String)
```
- **Authentication Check**: Validates user session before proceeding
- **Error Handling**: Comprehensive error handling with user-friendly messages
- **State Management**: Updates UI state and removes card from local list
- **Session Management**: Handles authentication failures gracefully

### Frontend Changes

#### 1. Enhanced NFC Card UI Component
```kotlin
@Composable
fun NfcCardItem(
    nfcCard: NfcCardResponse,
    onDeactivate: () -> Unit,
    onUnlink: () -> Unit  // New parameter
)
```

#### 2. Member ID Display Section
- **Conditional Rendering**: Only shows for cards with member IDs
- **Styled Container**: Uses Material Design card with subtle background
- **Clear Messaging**: "This member ID [ID] has been linked to a card"
- **Action Button**: "To unlink the card click here" with error color

#### 3. Unlink Confirmation Dialog
```kotlin
AlertDialog(
    title = { Text("Unlink NFC Card") },
    text = { Text("Are you sure you want to unlink this member ID (${nfcCard.memberId}) from the NFC card? This will delete the card from the database and cannot be undone.") },
    confirmButton = { TextButton(onClick = { viewModel.unlinkNfcCard(nfcCard.cardId) }) },
    dismissButton = { TextButton(onClick = { showUnlinkDialog = null }) }
)
```

## User Experience

### 1. Visual Design
- **Member ID Highlighting**: Member IDs are displayed in a dedicated section with clear visual separation
- **Action Clarity**: Unlink button uses error color (red) to indicate destructive action
- **Consistent Styling**: Follows Material Design 3 guidelines throughout

### 2. User Flow
1. **View Customer Details**: Navigate to customer detail page
2. **NFC Cards Section**: Scroll to NFC Cards section
3. **Member ID Display**: See member ID information clearly displayed
4. **Unlink Action**: Click "To unlink the card click here"
5. **Confirmation**: Review confirmation dialog with member ID details
6. **Confirm**: Click "Unlink" to proceed or "Cancel" to abort
7. **Feedback**: Success/error message displayed via operation state

### 3. Error Handling
- **Authentication Errors**: Automatic session clearing and re-login prompt
- **Network Errors**: User-friendly error messages with retry guidance
- **Validation**: Prevents actions when not authenticated

## Database Impact

### 1. Complete Deletion
- **Permanent Removal**: NFC card records are completely deleted from the database
- **No Soft Delete**: Unlike deactivation, unlinking removes the record entirely
- **Data Integrity**: Ensures clean database state

### 2. Related Data
- **Member ID Association**: Removes the link between member ID and NFC card
- **Store Association**: Removes store-specific card registration
- **Customer Association**: Removes customer-card relationship

## Security Considerations

### 1. Authentication
- **Token Validation**: All unlink operations require valid authentication token
- **Session Management**: Automatic session clearing on authentication failures
- **Authorization**: Users can only unlink cards for their store

### 2. Confirmation Requirements
- **Double Confirmation**: User must click unlink button AND confirm in dialog
- **Clear Warning**: Dialog explicitly states the action is permanent and cannot be undone
- **Member ID Display**: Shows exact member ID being unlinked for verification

## Usage Examples

### 1. Standard Unlinking Flow
```
Customer Detail Page → NFC Cards Section → 
Member ID Card → "To unlink the card click here" → 
Confirmation Dialog → "Unlink" → Success Message
```

### 2. Error Scenarios
```
Unlink Attempt → Authentication Error → 
Session Cleared → Re-login Required → 
Retry Unlink Operation
```

## Best Practices

### 1. For Users
- **Verify Member ID**: Always confirm the correct member ID before unlinking
- **Understand Impact**: Be aware that unlinking is permanent and cannot be undone
- **Use Deactivation**: Consider deactivating instead of unlinking for temporary disassociation

### 2. For Developers
- **Error Handling**: Always implement comprehensive error handling
- **User Feedback**: Provide clear success/error messages
- **State Management**: Update UI state immediately after successful operations
- **Testing**: Test both success and failure scenarios thoroughly

## Troubleshooting

### 1. Common Issues
- **"Not authenticated"**: User session expired, requires re-login
- **"Failed to unlink NFC card"**: Network or server error, retry operation
- **Card still visible**: Refresh page or check network connection

### 2. Debug Information
- **Logs**: Check Android logs for detailed error information
- **Network**: Verify internet connection and API endpoint accessibility
- **Authentication**: Ensure valid session token is present

## Future Enhancements

### 1. Potential Improvements
- **Bulk Operations**: Unlink multiple cards at once
- **Audit Trail**: Track unlink operations for compliance
- **Recovery Options**: Soft delete with recovery capability
- **Advanced Filtering**: Filter cards by member ID or registration date

### 2. Integration Opportunities
- **Analytics**: Track unlink patterns and reasons
- **Notifications**: Alert users when cards are unlinked
- **Reporting**: Generate reports on card management activities

## Conclusion

The NFC card unlinking functionality provides users with complete control over their NFC card associations while maintaining data integrity and security. The implementation follows best practices for user experience, error handling, and database management, ensuring a robust and reliable feature for customer management. 