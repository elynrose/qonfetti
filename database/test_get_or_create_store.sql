-- Test get_or_create_store function with parameter
-- Run this in your Supabase SQL editor to test the function

-- 1. Check if the function exists and its signature
SELECT 
    'Function check' as test,
    proname as function_name,
    pg_get_function_arguments(oid) as arguments
FROM pg_proc 
WHERE proname = 'get_or_create_store';

-- 2. Test the function with parameter
SELECT 
    'Testing get_or_create_store function with parameter' as test,
    get_or_create_store('Test Store Name') as store_id;

-- 3. Test the function with default parameter
SELECT 
    'Testing get_or_create_store function with default parameter' as test,
    get_or_create_store() as store_id;

-- 4. Check if there are any stores for the current user
SELECT 
    'User stores' as test,
    id as store_id,
    name as store_name,
    owner_id
FROM stores 
WHERE owner_id = auth.uid();

-- 5. Check if there are any points transactions
SELECT 
    'Points transactions count' as test,
    COUNT(*) as count
FROM points_transactions;

-- 6. Check recent transactions for the current user's store
SELECT 
    'Recent transactions for current store' as test,
    COUNT(*) as count
FROM points_transactions 
WHERE store_id = get_or_create_store();

-- 7. Show sample transactions
SELECT 
    'Sample transactions' as test,
    id,
    customer_id,
    store_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
WHERE store_id = get_or_create_store()
ORDER BY created_at DESC 
LIMIT 5; 