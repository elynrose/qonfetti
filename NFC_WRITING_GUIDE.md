# NFC Writing Feature Guide

## Overview

The Qonfetty app now supports **writing member IDs directly to NFC cards** instead of just registering card IDs. This allows customers to use their NFC cards across any store by simply tapping their card, which contains their member ID.

## How It Works

### 1. **NFC Writing Process**
- When registering an NFC card, the app **writes the customer's member ID** to the physical NFC card
- The member ID is stored as NDEF text format on the card
- This enables the card to be used at any store location

### 2. **Card Registration Flow**
1. **Customer Selection**: Tap on a customer in the customer list
2. **NFC Registration**: Tap the "+" button to register an NFC card
3. **Writing Process**: 
   - App checks if customer has a member ID
   - App verifies NFC is available and enabled
   - User taps "Start Writing" and holds NFC card near device
   - App writes member ID to the card
   - Card is registered in the database with the member ID

### 3. **Cross-Store Usage**
- Customer can use the same NFC card at any store
- Store staff can scan the card to read the member ID
- Points and transactions are linked to the customer via member ID

## Technical Implementation

### NFC Manager (`NfcManager.kt`)
```kotlin
class NfcManager(private val activity: Activity) {
    // Write member ID to NFC card
    suspend fun writeMemberIdToCard(tag: Tag, memberId: String): Result<String>
    
    // Read member ID from NFC card
    suspend fun readMemberIdFromCard(tag: Tag): Result<String>
}
```

### Key Features
- **NDEF Support**: Writes member ID as NDEF text record
- **Formatable Cards**: Supports both pre-formatted and unformatted NFC cards
- **Error Handling**: Comprehensive error handling for various NFC scenarios
- **Cross-Platform**: Works with standard NFC cards (NTAG, etc.)

### Database Schema
The `nfc_cards` table stores:
- `card_id`: The member ID written to the card (not a physical card UID)
- `member_id`: The customer's member ID
- `customer_id`: Links to the customer record
- `store_id`: Store where the card was registered
- `is_active`: Whether the card is active

## User Interface

### NFC Write Dialog
- **Customer Info**: Shows customer name and member ID
- **NFC Status**: Checks NFC availability and permissions
- **Writing Progress**: Real-time feedback during writing process
- **Error Handling**: Clear error messages for various scenarios

### Visual Feedback
- **Loading Indicator**: Shows writing progress
- **Status Messages**: Real-time updates during writing
- **Success/Error States**: Clear feedback on completion

## Security & Permissions

### Required Permissions
```xml
<uses-permission android:name="android.permission.NFC" />
<uses-feature android:name="android.hardware.nfc" android:required="true" />
```

### NFC Intent Filters
```xml
<intent-filter>
    <action android:name="android.nfc.action.TECH_DISCOVERED" />
    <category android:name="android.intent.category.DEFAULT" />
</intent-filter>
```

### Data Protection
- Member IDs are stored securely on NFC cards
- Database access controlled by Row Level Security (RLS)
- No sensitive customer data stored on cards

## Testing the Feature

### Prerequisites
1. **NFC-Enabled Device**: Android device with NFC capability
2. **NFC Cards**: Standard NFC cards (NTAG213/215/216 recommended)
3. **Customer with Member ID**: Customer must have a member ID set

### Test Steps
1. **Navigate to Customer Detail**: Tap on a customer with a member ID
2. **Start NFC Registration**: Tap the "+" button
3. **Begin Writing**: Tap "Start Writing" in the dialog
4. **Hold NFC Card**: Place NFC card near device back
5. **Verify Success**: Check for success message
6. **Test Card**: Use card at another store location

### Common Issues
- **NFC Disabled**: Enable NFC in device settings
- **Card Not Compatible**: Use NDEF-compatible NFC cards
- **Writing Failed**: Ensure card is not write-protected
- **Member ID Missing**: Customer must have member ID set

## Benefits

### For Customers
- **Convenience**: Single card works across all stores
- **No App Required**: Physical card works independently
- **Universal Access**: Works with any NFC reader

### For Store Staff
- **Quick Identification**: Instant customer lookup via member ID
- **Cross-Store Support**: Same card works at any location
- **Reliable**: Physical card doesn't depend on app/phone

### For Business
- **Customer Loyalty**: Encourages repeat visits across locations
- **Data Consistency**: Member ID ensures accurate tracking
- **Scalability**: Works across multiple store locations

## Future Enhancements

### Potential Features
- **Card Personalization**: Custom designs or branding
- **Advanced Security**: Encryption of member ID data
- **Analytics**: Track card usage patterns
- **Integration**: Connect with existing loyalty systems

### Technical Improvements
- **Batch Writing**: Write multiple cards simultaneously
- **Card Validation**: Verify card compatibility before writing
- **Backup System**: Store card data for recovery
- **API Integration**: Connect with external NFC services

## Troubleshooting

### NFC Not Working
1. Check device NFC is enabled
2. Verify app has NFC permission
3. Test with different NFC card
4. Restart app and device

### Writing Fails
1. Ensure card is not write-protected
2. Check card has sufficient memory
3. Try formatting card first
4. Use different NFC card

### Database Errors
1. Verify customer has member ID
2. Check store ID is valid
3. Ensure user has proper permissions
4. Review server logs for details

## Support

For technical support or questions about the NFC writing feature:
- Check device compatibility
- Verify NFC card specifications
- Review error logs in Android Studio
- Test with known working NFC cards

---

**Note**: This feature requires NFC-capable Android devices and compatible NFC cards. Test thoroughly before production deployment. 