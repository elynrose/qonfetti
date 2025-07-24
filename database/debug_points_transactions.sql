-- Debug Points Transactions Data
-- This script will help identify why points transactions aren't showing up

-- 1. Check if the table exists and has data
SELECT 'Table check' as step, COUNT(*) as total_transactions FROM points_transactions;

-- 2. Check RLS status
SELECT 'RLS status' as step, schemaname, tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'points_transactions';

-- 3. Check policies
SELECT 'Policies' as step, policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'points_transactions';

-- 4. Temporarily disable RLS to see all data
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;

-- 5. Check all transactions (without RLS)
SELECT 'All transactions (RLS disabled)' as step, COUNT(*) as count FROM points_transactions;

-- 6. Show recent transactions with details
SELECT 
    id,
    customer_id,
    store_id,
    points_awarded,
    transaction_type,
    created_at,
    description
FROM points_transactions 
ORDER BY created_at DESC 
LIMIT 10;

-- 7. Check transactions for your specific store
SELECT 'Store transactions' as step, COUNT(*) as count 
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 8. Show transactions for your store
SELECT 
    id,
    customer_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'
ORDER BY created_at DESC;

-- 9. Check if there are any transactions for the specific customer
SELECT 'Customer transactions' as step, COUNT(*) as count 
FROM points_transactions 
WHERE customer_id = 'f60f2cb0-7f68-4f3a-af3e-2a227751819a';

-- 10. Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;

-- 11. Test the exact query the app uses (with RLS enabled)
SELECT 'App query test (RLS enabled)' as step, COUNT(*) as count 
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 12. Check current user context
SELECT 'Current user' as step, auth.uid() as current_user_id;

-- 13. Check if the store belongs to the current user
SELECT 'Store ownership' as step, id, name, owner_id 
FROM stores 
WHERE id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 14. Test the policy manually
SELECT 'Policy test' as step, COUNT(*) as count 
FROM points_transactions pt
JOIN stores s ON pt.store_id = s.id
WHERE s.owner_id = auth.uid(); 