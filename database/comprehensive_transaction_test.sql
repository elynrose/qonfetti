-- Comprehensive Points Transaction System Analysis
-- This script will help identify exactly what's happening with the transaction system

-- 1. Check if the table exists and its structure
SELECT 'Table structure check' as step;
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns 
WHERE table_name = 'points_transactions'
ORDER BY ordinal_position;

-- 2. Check RLS status and policies
SELECT 'RLS and policies check' as step;
SELECT schemaname, tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'points_transactions';

SELECT policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'points_transactions';

-- 3. Temporarily disable RLS to see ALL data
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;

-- 4. Check total count of transactions
SELECT 'Total transactions (RLS disabled)' as step, COUNT(*) as count FROM points_transactions;

-- 5. Show ALL transactions with full details
SELECT 
    id,
    customer_id,
    store_id,
    nfc_card_id,
    points_awarded,
    previous_points,
    new_points,
    transaction_type,
    description,
    created_at
FROM points_transactions 
ORDER BY created_at DESC;

-- 6. Check transactions for the specific store
SELECT 'Store transactions' as step, COUNT(*) as count 
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 7. Show transactions for the specific store
SELECT 
    id,
    customer_id,
    points_awarded,
    transaction_type,
    created_at,
    description
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'
ORDER BY created_at DESC;

-- 8. Check if there are any transactions for the specific customer
SELECT 'Customer transactions' as step, COUNT(*) as count 
FROM points_transactions 
WHERE customer_id = 'f60f2cb0-7f68-4f3a-af3e-2a227751819a';

-- 9. Show transactions for the specific customer
SELECT 
    id,
    store_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
WHERE customer_id = 'f60f2cb0-7f68-4f3a-af3e-2a227751819a'
ORDER BY created_at DESC;

-- 10. Check store ownership
SELECT 'Store ownership check' as step, id, name, owner_id 
FROM stores 
WHERE id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 11. Check current user context
SELECT 'Current user context' as step, auth.uid() as current_user_id;

-- 12. Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;

-- 13. Test the exact query the app uses (with RLS enabled)
SELECT 'App query test (RLS enabled)' as step, COUNT(*) as count 
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 14. Test the policy manually
SELECT 'Policy test' as step, COUNT(*) as count 
FROM points_transactions pt
JOIN stores s ON pt.store_id = s.id
WHERE s.owner_id = auth.uid();

-- 15. Check if the store belongs to the current user
SELECT 'Store belongs to current user' as step, 
    CASE 
        WHEN owner_id = auth.uid() THEN 'YES'
        ELSE 'NO'
    END as is_owner
FROM stores 
WHERE id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 16. Test a simple insert to see if it works
SELECT 'Testing insert capability' as step;
INSERT INTO points_transactions (
    customer_id, 
    store_id, 
    points_awarded, 
    previous_points, 
    new_points, 
    transaction_type, 
    description
) VALUES (
    'f60f2cb0-7f68-4f3a-af3e-2a227751819a',
    'a667e115-9e6a-4d39-9ae3-7ab98b63386c',
    1,
    18,
    19,
    'test_insert',
    'Test transaction from SQL'
) RETURNING id;

-- 17. Check if the test insert was successful
SELECT 'Test insert result' as step, COUNT(*) as count 
FROM points_transactions 
WHERE transaction_type = 'test_insert';

-- 18. Clean up test data
DELETE FROM points_transactions WHERE transaction_type = 'test_insert'; 