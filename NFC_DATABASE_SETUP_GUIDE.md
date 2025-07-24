# NFC Database Setup Guide

## Problem Identified

The logs show that there are **0 total NFC cards in database**, which means the `nfc_cards` table either:
1. Doesn't exist in your Supabase database
2. Has syntax errors preventing it from being created
3. Has RLS (Row Level Security) policies blocking access
4. **The table exists but policies are not working correctly**

## Solution

Since you got the error "policy already exists", the table is there but needs fixing. Run the fix script instead.

## Step-by-Step Instructions

### 1. Access Supabase SQL Editor

1. Go to your Supabase project dashboard
2. Click on "SQL Editor" in the left sidebar
3. Click "New Query" to create a new SQL script

### 2. Run the NFC Cards Fix Script

Copy and paste the following SQL script into the SQL Editor:

```sql
-- NFC Cards Table Fix Script
-- This script checks and fixes the existing nfc_cards table

-- First, let's check what exists
SELECT 'Checking existing table structure...' as status;

-- Check if table exists and show its structure
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'nfc_cards'
ORDER BY ordinal_position;

-- Check existing policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'nfc_cards';

-- Check existing indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'nfc_cards';

-- Now let's fix any missing pieces

-- Drop existing policies to recreate them properly
DROP POLICY IF EXISTS "Store owners can view NFC cards at their stores" ON nfc_cards;
DROP POLICY IF EXISTS "Store owners can register NFC cards at their stores" ON nfc_cards;
DROP POLICY IF EXISTS "Store owners can update NFC cards at their stores" ON nfc_cards;
DROP POLICY IF EXISTS "Store owners can delete NFC cards at their stores" ON nfc_cards;

-- Recreate policies
CREATE POLICY "Store owners can view NFC cards at their stores" ON nfc_cards
    FOR SELECT USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can register NFC cards at their stores" ON nfc_cards
    FOR INSERT WITH CHECK (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can update NFC cards at their stores" ON nfc_cards
    FOR UPDATE USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can delete NFC cards at their stores" ON nfc_cards
    FOR DELETE USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Ensure RLS is enabled
ALTER TABLE nfc_cards ENABLE ROW LEVEL SECURITY;

-- Create missing indexes if they don't exist
CREATE INDEX IF NOT EXISTS idx_nfc_cards_card_id ON nfc_cards(card_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_member_id ON nfc_cards(member_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_customer_id ON nfc_cards(customer_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_store_id ON nfc_cards(store_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_active ON nfc_cards(is_active);

-- Create unique constraint if it doesn't exist
CREATE UNIQUE INDEX IF NOT EXISTS idx_nfc_cards_unique_active ON nfc_cards(card_id) WHERE is_active = true;

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_nfc_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop and recreate trigger
DROP TRIGGER IF EXISTS update_nfc_cards_updated_at ON nfc_cards;
CREATE TRIGGER update_nfc_cards_updated_at
    BEFORE UPDATE ON nfc_cards
    FOR EACH ROW
    EXECUTE FUNCTION update_nfc_cards_updated_at();

-- Grant necessary permissions
GRANT ALL ON nfc_cards TO authenticated;

-- Show final status
SELECT 'NFC cards table fixed successfully!' as status;

-- Show current table count
SELECT COUNT(*) as total_nfc_cards FROM nfc_cards;
```

### 3. Execute the Fix Script

1. Click the "Run" button (or press Ctrl+Enter)
2. Wait for the script to complete
3. You should see diagnostic information and "NFC cards table fixed successfully!"

### 4. Test with Sample Data (Optional)

If you want to test the functionality, run this test script:

```sql
-- Test NFC Card Insertion Script
-- This script tests the NFC cards functionality

-- First, let's see what customers and stores we have
SELECT 'Available customers:' as info;
SELECT id, name, member_id FROM customers LIMIT 5;

SELECT 'Available stores:' as info;
SELECT id, name, owner_id FROM stores LIMIT 5;

-- Let's insert a test NFC card
INSERT INTO nfc_cards (card_id, member_id, customer_id, store_id, is_active)
VALUES (
    'test_card_001', 
    'test_member_001',
    (SELECT id FROM customers LIMIT 1),  -- Use first available customer
    (SELECT id FROM stores LIMIT 1),     -- Use first available store
    true
)
ON CONFLICT (card_id) DO NOTHING;

-- Check if the card was inserted
SELECT 'Test NFC card inserted successfully!' as status;

-- Show all NFC cards
SELECT 
    card_id,
    member_id,
    customer_id,
    store_id,
    is_active,
    created_at
FROM nfc_cards;

-- Test query that the app uses
SELECT 
    card_id,
    member_id,
    customer_id,
    store_id,
    is_active
FROM nfc_cards 
WHERE customer_id = (SELECT id FROM customers LIMIT 1)
AND is_active = true;
```

## What This Fix Script Does

### 1. Diagnoses the Problem
- Shows the current table structure
- Lists existing policies
- Shows existing indexes
- Counts current NFC cards

### 2. Fixes Policies
- Drops and recreates all RLS policies
- Ensures proper store ownership validation
- Enables Row Level Security

### 3. Ensures Proper Structure
- Creates missing indexes
- Sets up unique constraints
- Creates/updates triggers
- Grants proper permissions

### 4. Provides Verification
- Shows success status
- Counts total NFC cards
- Displays diagnostic information

## After Running the Script

1. **Check the Output**: Look for "NFC cards table fixed successfully!"
2. **Note the Card Count**: See how many NFC cards currently exist
3. **Test the App**: Open your app and try to register an NFC card
4. **Check Logs**: Look for the debug output showing NFC cards in the database

## Troubleshooting

### If you still see 0 NFC cards:
- The table might be empty (no cards have been registered yet)
- Try registering a new NFC card through the app
- Check if the test script inserted a sample card

### If policies still don't work:
- Verify your user has the correct store ownership
- Check that the `stores` table has the correct `owner_id` values
- Ensure you're authenticated with the correct user account

### If you need to start fresh:
```sql
DROP TABLE IF EXISTS nfc_cards CASCADE;
```
Then run the original setup script.

## Expected Results

After running the fix script:
1. The diagnostic output will show the table structure
2. You'll see "NFC cards table fixed successfully!"
3. The app will be able to register and display NFC cards
4. Customer detail pages will show associated NFC cards
5. The unlink functionality will work properly

Let me know what the script output shows! 