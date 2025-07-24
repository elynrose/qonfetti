# NFC Card Unlinking Test Guide

## Current Status ✅

The NFC card registration is now working perfectly! The logs show:
- ✅ NFC card successfully registered to James Whitcomb
- ✅ Card ID: 2047583972 is in the database
- ✅ Points are being updated correctly (7 → 8)
- ✅ The card shows up in the customer's NFC Cards section

## Testing Unlinking Functionality

### Step 1: Verify Card is Visible
1. Go to James Whitcomb's customer detail page
2. Scroll down to the "NFC Cards" section
3. You should see:
   - Card ID: 2047583972
   - Member ID: 2047583972
   - Registration date
   - Status: Active
   - **Unlink button**

### Step 2: Test Unlinking
1. Tap the "Unlink" button next to the NFC card
2. A confirmation dialog should appear asking:
   - "Are you sure you want to unlink this member ID (2047583972) from the NFC card?"
   - "This will delete the card from the database and cannot be undone."

3. Tap "Unlink" to confirm
4. You should see:
   - Loading state briefly
   - Success message: "NFC card unlinked successfully"
   - The card disappears from the list

### Step 3: Verify Deletion
1. Check the logs for:
   ```
   D SupabaseApi: Deleting NFC card: 2047583972
   D SupabaseApi: NFC card deleted successfully: 2047583972
   D CustomerDetailViewModel: NFC card unlinked: 2047583972
   ```

2. Go back to the customer list and return to James Whitcomb's page
3. The NFC Cards section should show "No NFC cards registered" again

### Step 4: Test Re-registration
1. Scan the same NFC card again from the Dashboard
2. It should:
   - Read the member ID (2047583972)
   - Find James Whitcomb
   - Update his points
   - **Re-register the card** (since it was deleted)

## Expected Behavior

- ✅ **Unlink button appears** for registered cards
- ✅ **Confirmation dialog** prevents accidental deletion
- ✅ **Card disappears** from the list after unlinking
- ✅ **Success message** confirms the operation
- ✅ **Card can be re-registered** after unlinking
- ✅ **Database is updated** (card is actually deleted)

## Troubleshooting

If unlinking doesn't work:
1. Check the logs for error messages
2. Verify the card ID is correct
3. Make sure you're authenticated
4. Check if the database policies allow deletion

Let me know what happens when you test the unlinking! 