# NFC Troubleshooting Guide

## Issue: "Nothing happens when I scan the card and launch the app"

This guide will help you debug and fix NFC scanning issues.

## Step 1: Test NFC Basic Functionality

### 1.1 Access the NFC Test Screen
1. Launch the app and log in
2. On the dashboard, tap the **"Test NFC"** button
3. This will open the NFC test screen

### 1.2 Check NFC Status
- The test screen will show if NFC is available on your device
- If it shows "NFC Not Available", check your device settings:
  - Go to Settings > Connected devices > Connection preferences > NFC
  - Make sure NFC is turned ON

### 1.3 Test NFC Reading
1. On the NFC test screen, tap an NFC card to your device
2. The screen should show test results
3. If successful, you'll see the member ID that was read from the card

## Step 2: Common Issues and Solutions

### Issue 1: NFC Not Detected
**Symptoms:** No response when tapping NFC card
**Solutions:**
- Check if NFC is enabled in device settings
- Make sure the app has NFC permissions
- Try tapping the card more slowly and hold it longer
- Try different positions on the device (usually near the camera)

### Issue 2: "Tag is not NDEF compatible"
**Symptoms:** Error message when reading NFC card
**Solutions:**
- The NFC card might not be formatted with NDEF data
- Try using a different NFC card
- If you're using a blank card, it needs to be formatted first

### Issue 3: "No text record found on NFC card"
**Symptoms:** Card is detected but no member ID is found
**Solutions:**
- The card might not have the member ID written to it
- Check if the card was properly formatted with member ID data
- Try using a card that was written by the app

### Issue 4: "Customer not found with member ID"
**Symptoms:** Card is read successfully but customer lookup fails
**Solutions:**
- Check if the member ID exists in the database
- Verify the customer has a `member_id` field set
- Check database connectivity and authentication

## Step 3: Database Setup Requirements

### Required Tables
Make sure these tables exist in your Supabase database:

```sql
-- Customers table (should already exist)
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone TEXT NOT NULL,
    address TEXT,
    member_id TEXT UNIQUE,  -- This field is required for NFC lookup
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Customer points table (should already exist)
CREATE TABLE customer_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    store_id UUID REFERENCES stores(id),
    points INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(customer_id, store_id)
);

-- Rewards table (new - for the points system)
CREATE TABLE rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT,
    points_required INTEGER NOT NULL,
    store_id UUID REFERENCES stores(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Reward claims table (new - for tracking claimed rewards)
CREATE TABLE reward_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID REFERENCES customers(id),
    reward_id UUID REFERENCES rewards(id),
    store_id UUID REFERENCES stores(id),
    claimed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    is_claimed BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

### Test Data Setup
Add some test data to verify the system works:

```sql
-- Add a test customer with member ID
INSERT INTO customers (name, email, phone, member_id) VALUES
('John Doe', 'john@example.com', '+1234567890', '1234567890');

-- Add some test rewards
INSERT INTO rewards (name, description, points_required, store_id, is_active) VALUES
('Free Coffee', 'Get a free coffee of your choice', 50, 'your-store-id-here', true),
('10% Discount', 'Get 10% off your next purchase', 100, 'your-store-id-here', true);
```

## Step 4: Debugging Steps

### 4.1 Check Logs
Look at the Android logs for detailed error information:
```bash
adb logcat | grep -E "(NfcManager|NfcPointsManager|SupabaseApi)"
```

### 4.2 Test NFC Card Format
1. Use the NFC test screen to read a card
2. Check if the member ID is being read correctly
3. Verify the member ID format matches what's in the database

### 4.3 Test Database Connectivity
1. Make sure you're logged in to the app
2. Check if other database operations work (like viewing customers)
3. Verify the authentication token is valid

### 4.4 Test Points System
1. Once NFC reading works, test the full points flow:
   - Tap NFC card
   - Check if points are added
   - Verify customer lookup works
   - Check if rewards are displayed

## Step 5: Manual Testing

### 5.1 Test NFC Reading Only
1. Go to NFC test screen
2. Tap an NFC card
3. Verify the member ID is read correctly

### 5.2 Test Customer Lookup
1. Use a member ID that exists in the database
2. Check if the customer is found
3. Verify customer details are displayed

### 5.3 Test Points System
1. Use a customer that doesn't have points yet
2. Tap their NFC card
3. Check if 1 point is added
4. Tap again to verify points increment

## Step 6: Common Error Messages

| Error Message | Cause | Solution |
|---------------|-------|----------|
| "NFC Not Available" | NFC disabled or not supported | Enable NFC in device settings |
| "Tag is not NDEF compatible" | Card not formatted properly | Use a different card or format it |
| "No text record found" | No member ID written to card | Write member ID to card first |
| "Customer not found" | Member ID not in database | Add customer with that member ID |
| "Session expired" | Authentication token expired | Log out and log back in |
| "400 Bad Request" | Database schema issue | Check table structure and permissions |

## Step 7: Next Steps

Once NFC reading is working:

1. **Test the full points flow** - Tap cards and verify points are added
2. **Set up rewards** - Add rewards to the database and test claiming
3. **Test with real customers** - Use actual customer member IDs
4. **Monitor performance** - Check for any performance issues with multiple scans

## Support

If you're still having issues:

1. Check the Android logs for specific error messages
2. Verify your database schema matches the requirements
3. Test with different NFC cards
4. Make sure you're using a device with NFC support

---

**Note:** The NFC points system is designed to read member IDs from NFC cards and manage customer points. Make sure your NFC cards are properly formatted with member ID data for the system to work correctly. 