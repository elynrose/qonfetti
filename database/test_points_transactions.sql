-- Test Points Transactions Setup
-- Run this in your Supabase SQL editor to verify everything is working

-- 1. Check if the table exists and has the right structure
SELECT 'Table structure check' as step;
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'points_transactions'
ORDER BY ordinal_position;

-- 2. Check if there are any existing transactions
SELECT 'Existing transactions' as step, COUNT(*) as count FROM points_transactions;

-- 3. Check RLS policies
SELECT 'RLS policies' as step, policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'points_transactions';

-- 4. Temporarily disable RLS to see all data
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;

-- 5. Check all transactions (without RLS)
SELECT 'All transactions (RLS disabled)' as step, COUNT(*) as count FROM points_transactions;

-- 6. Show any existing transactions
SELECT 
    id,
    customer_id,
    store_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
ORDER BY created_at DESC;

-- 7. Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;

-- 8. Check current user context
SELECT 
    'Current user context' as info,
    auth.uid() as user_id,
    auth.jwt() ->> 'email' as user_email,
    CASE WHEN auth.uid() IS NOT NULL THEN 'AUTHENTICATED' ELSE 'NOT AUTHENTICATED' END as auth_status;

-- 9. Test the award_points_to_customer function manually
-- (This will only work if you have a valid customer ID)
-- Replace 'faebc866-f73b-4a88-a834-8486c890473f' with an actual customer ID from your database
SELECT 'Manual function test' as step;
-- SELECT award_points_to_customer('faebc866-f73b-4a88-a834-8486c890473f'::uuid, 1);

-- 10. Check if the function exists and has the right signature
SELECT 
    'Function check' as step,
    proname as function_name,
    pg_get_function_arguments(oid) as arguments
FROM pg_proc 
WHERE proname = 'award_points_to_customer';

-- 11. Show the function definition
SELECT 
    'Function definition' as step,
    pg_get_functiondef(oid) as definition
FROM pg_proc 
WHERE proname = 'award_points_to_customer'; 