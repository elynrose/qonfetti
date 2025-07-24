# NFC Test Screen Troubleshooting Guide

## Overview
The NFC Test Screen allows you to test NFC card reading functionality and verify that member IDs can be read from NFC cards and matched with customers in the database.

## How to Use the NFC Test Screen

### 1. Access the Test Screen
- Log into the app
- From the dashboard, tap the "Test NFC" button
- The NFC Test Screen will open

### 2. Test NFC Reading
- Make sure you're logged in (authentication is required for customer lookup)
- Tap an NFC card to your device
- The app will attempt to:
  - Read the member ID from the NFC card
  - Look up the customer in the database using the member ID
  - Display the results

### 3. Understanding the Results
- **Success**: Shows the member ID read from the card and customer information if found
- **Error**: Shows specific error messages for troubleshooting
- **Processing**: Shows a loading indicator while reading the card

## Troubleshooting Steps

### If Nothing Happens When Tapping NFC Card

1. **Check NFC Status**
   - Look at the "NFC Status" card on the test screen
   - If it shows "NFC Not Available", check your device settings
   - Enable NFC in your device settings if disabled

2. **Check Device NFC Support**
   - Ensure your device has NFC hardware
   - Some devices may not support NFC

3. **Check NFC Card Type**
   - Ensure you're using an NFC card (not a magnetic stripe card)
   - The card must be NDEF-compatible
   - Try different NFC cards if available

4. **Check Card Positioning**
   - Hold the card close to the NFC antenna (usually on the back of the device)
   - Keep the card steady for 1-2 seconds
   - Try different positions if the card isn't detected

### If You Get "Failed to Read Member ID" Error

1. **Card Format Issues**
   - The card may not be formatted with NDEF data
   - The card may not contain a text record with the member ID
   - Try using a card that was previously written by the app

2. **Card Compatibility**
   - Some NFC cards may not be fully compatible
   - Try using a different NFC card

### If You Get "No Customer Found" Message

1. **Database Issues**
   - The member ID exists on the card but no customer is found in the database
   - Check if the customer exists in your Supabase database
   - Verify the member_id field in the customers table

2. **Authentication Issues**
   - If you see "Not authenticated" message, log out and log back in
   - Check that your session is valid

### If You Get "Error Finding Customer" Message

1. **Network Issues**
   - Check your internet connection
   - The app may not be able to reach the Supabase database

2. **API Issues**
   - Check the Supabase API configuration
   - Verify the database connection

## Debug Information

### Logs to Check
The app logs detailed information about NFC operations. Check the Android logs for:

- `MainActivity`: NFC intent detection and processing
- `NfcManager`: NFC reading operations
- `SupabaseApi`: Database operations

### Common Log Messages
- `"onNewIntent called with action"`: Shows when NFC intents are received
- `"NFC tag found"`: Shows the NFC tag ID when detected
- `"Calling NFC test callback"`: Shows when NFC test processing starts
- `"Reading member ID from NFC card"`: Shows when NFC reading begins

## Testing with Different Cards

### Test Card Types
1. **Blank NFC Cards**: Should show "No NDEF message found" or "No text record found"
2. **Cards Written by the App**: Should show the member ID and customer information
3. **Other NFC Cards**: May show different data or errors depending on format

### Expected Results
- **Success**: Member ID + Customer info (if found)
- **Card Read Success, No Customer**: Member ID + "No customer found" message
- **Card Read Failure**: Specific error message about why reading failed

## Next Steps

If the NFC test screen is working correctly:
1. The member ID should be read from the card
2. If a customer exists with that member ID, their information should be displayed
3. You can proceed to use the main NFC points system

If issues persist:
1. Check the logs for specific error messages
2. Verify your NFC card is compatible
3. Test with different NFC cards
4. Check your device's NFC functionality with other apps

## Support

If you continue to experience issues:
1. Note the specific error messages
2. Check the Android logs for detailed information
3. Test with multiple NFC cards
4. Verify your device's NFC functionality 