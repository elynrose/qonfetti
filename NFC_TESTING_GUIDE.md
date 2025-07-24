# NFC Testing Guide

## What We Fixed

### Issue 1: Serialization Mismatch
The `NfcCardRegistration` data class was missing `@SerialName` annotations, causing a mismatch between the JSON field names sent to Supabase and what the database expected.

**Before:**
```kotlin
data class NfcCardRegistration(
    val cardId: String,        // Sent as "cardId" but DB expects "card_id"
    val memberId: String,      // Sent as "memberId" but DB expects "member_id"
    val customerId: String,    // Sent as "customerId" but DB expects "customer_id"
    // ...
)
```

**After:**
```kotlin
data class NfcCardRegistration(
    @SerialName("card_id") val cardId: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("customer_id") val customerId: String,
    // ...
)
```

### Issue 2: Missing NFC Card Registration
The NFC scanning process was working for reading member IDs and updating points, but it wasn't registering the NFC cards to the database. When a card was scanned, it should:
1. ✅ Read the member ID from the card
2. ✅ Find the customer by member ID  
3. ✅ Update their points
4. ❌ **Register the NFC card to the customer** (this step was missing!)

**Added:** A new `registerNfcCardIfNeeded()` function that automatically registers NFC cards when they're scanned, so they show up in the customer's NFC Cards section.

### Issue 3: Response Parsing Error
Supabase was returning a 201 Created status but with a `null` ContentType, causing the Ktor client to fail when parsing the response body.

**Fixed:** Enhanced the `registerNfcCard()` function to handle cases where Supabase returns a successful status but no response body. The function now:
1. Tries to parse the response body normally
2. If that fails, attempts to fetch the created card separately
3. If fetching fails, creates a minimal response object since we know the card was created
4. Provides detailed logging for debugging

## Testing Steps

### Step 1: Run the Debug Script
1. Go to your Supabase Dashboard
2. Open the SQL Editor
3. Run the `database/debug_nfc_cards.sql` script
4. Check the results to verify:
   - Table structure is correct
   - RLS policies are in place
   - Test insert works

### Step 2: Test NFC Card Registration
1. Open the app on your device
2. Go to any customer's detail page
3. In the NFC Cards section, tap "Register New Card"
4. Enter a test member ID (e.g., "1234567890")
5. Tap "Write to Card" and scan an NFC card
6. Check the logs to see if registration succeeds

### Step 3: Test NFC Card Reading
1. Go to the Dashboard
2. Scan the same NFC card you just wrote to
3. The app should:
   - Read the member ID from the card
   - Find the customer associated with that member ID
   - Add points to their account
   - Show a success message

### Step 4: Verify in Customer Details
1. Go to the customer's detail page
2. Check the NFC Cards section
3. You should see the card you just registered with:
   - Card ID (member ID)
   - Registration date
   - Status (Active)
   - Unlink button

## Expected Log Messages

When everything works correctly, you should see:

```
D SupabaseApi: Registering NFC card with member ID: 1234567890 for customer: [customer-id]
D SupabaseApi: NFC card registered successfully with member ID: 1234567890
D SupabaseApi: Found 1 total NFC cards for customer: [customer-id]
D SupabaseApi: Found 1 active NFC cards for customer: [customer-id]
```

## Troubleshooting

### If you still get 400 Bad Request:
1. Check the Supabase logs in the Dashboard
2. Verify the table structure matches the debug script output
3. Make sure RLS policies are working correctly

### If NFC cards don't show up:
1. Check if the customer ID is correct
2. Verify the store ID matches your current store
3. Check if the card is marked as active

### If NFC reading doesn't work:
1. Make sure the card was written successfully
2. Check if the member ID format is correct
3. Verify the customer exists in the database

## Next Steps

Once NFC card registration is working:
1. Test with multiple customers
2. Test the unlink functionality
3. Test NFC scanning from the dashboard
4. Test points accumulation and rewards

Let me know what happens when you test these steps! 