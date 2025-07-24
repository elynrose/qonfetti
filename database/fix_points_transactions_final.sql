-- Comprehensive fix for points transactions RLS policies
-- This script will fix the RLS policies and ensure proper access

-- First, let's check what we have
SELECT 'Current table structure:' as info;
\d points_transactions;

SELECT 'Current policies:' as info;
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'points_transactions';

-- Drop all existing policies
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "Enable insert for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "Enable update for users based on store_id" ON points_transactions;
DROP POLICY IF EXISTS "Enable delete for users based on store_id" ON points_transactions;

-- Disable RLS temporarily to check data
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;

-- Check if we have any data
SELECT 'Current data count:' as info, COUNT(*) as count FROM points_transactions;

-- Check store ownership
SELECT 'Store ownership check:' as info;
SELECT s.id, s.name, s.owner_id, u.email as owner_email
FROM stores s
LEFT JOIN auth.users u ON s.owner_id = u.id
WHERE s.id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;

-- Create simple, working policies
CREATE POLICY "Enable all access for store owners" ON points_transactions
    FOR ALL
    TO authenticated
    USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    )
    WITH CHECK (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Test the policy
SELECT 'Testing RLS policy:' as info;
SELECT COUNT(*) as accessible_transactions 
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- If still 0, let's check the view
SELECT 'Testing view access:' as info;
SELECT COUNT(*) as view_transactions 
FROM points_transactions_view 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- Grant explicit permissions
GRANT ALL ON points_transactions TO authenticated;
GRANT ALL ON points_transactions_view TO authenticated;

-- Create a test transaction to verify everything works
INSERT INTO points_transactions (
    id,
    customer_id,
    store_id,
    points_awarded,
    transaction_type,
    description,
    created_at
) VALUES (
    gen_random_uuid(),
    '4dca126f-90f9-447e-9db8-a66aad113875', -- Use the customer we just created
    'a667e115-9e6a-4d39-9ae3-7ab98b63386c',
    10,
    'nfc_scan',
    'Test transaction from SQL script',
    NOW()
);

-- Test final access
SELECT 'Final test - accessible transactions:' as info;
SELECT COUNT(*) as count FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

SELECT 'Final test - view transactions:' as info;
SELECT COUNT(*) as count FROM points_transactions_view 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- Show the test transaction
SELECT 'Test transaction created:' as info;
SELECT * FROM points_transactions_view 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'
ORDER BY created_at DESC
LIMIT 5; 