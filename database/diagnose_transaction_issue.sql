-- Comprehensive Transaction Analytics Diagnosis
-- Run this in Supabase SQL Editor to identify the issue

-- 1. Check if the function exists and works
SELECT 'Function check' as step;
SELECT routine_name, routine_type 
FROM information_schema.routines 
WHERE routine_name = 'get_transaction_stats';

-- 2. Test the function with your store ID
SELECT 'Function test' as step;
SELECT * FROM get_transaction_stats('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID);

-- 3. Check if there's data in points_transactions
SELECT 'Points transactions data' as step;
SELECT COUNT(*) as total_records FROM points_transactions;
SELECT COUNT(*) as store_records 
FROM points_transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05';

-- 4. Check if there's data in transactions
SELECT 'Transactions data' as step;
SELECT COUNT(*) as total_records FROM transactions;
SELECT COUNT(*) as store_records 
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05';

-- 5. Show sample data from points_transactions
SELECT 'Sample points_transactions' as step;
SELECT 
    id,
    customer_id,
    store_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'
ORDER BY created_at DESC 
LIMIT 5;

-- 6. Show sample data from transactions
SELECT 'Sample transactions' as step;
SELECT 
    id,
    customer_id,
    store_id,
    transaction_type,
    amount,
    points_earned,
    points_used,
    created_at
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'
ORDER BY created_at DESC 
LIMIT 5;

-- 7. Manual calculation to compare
SELECT 'Manual calculation' as step;
WITH combined_data AS (
    -- Data from transactions table
    SELECT 
        'transactions' as source,
        transaction_type,
        amount,
        points_earned,
        points_used,
        created_at
    FROM transactions 
    WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'
    
    UNION ALL
    
    -- Data from points_transactions table (NFC scans)
    SELECT 
        'points_transactions' as source,
        transaction_type,
        0 as amount,
        points_awarded as points_earned,
        0 as points_used,
        created_at
    FROM points_transactions 
    WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'
)
SELECT 
    COALESCE(SUM(CASE WHEN source = 'transactions' AND transaction_type = 'purchase' THEN amount ELSE 0 END), 0) as total_purchases,
    COALESCE(SUM(CASE WHEN source = 'transactions' AND transaction_type = 'reward_claim' THEN amount ELSE 0 END), 0) as total_claimed,
    COUNT(*) as total_transactions,
    COALESCE(SUM(points_earned), 0) as total_points_earned,
    COALESCE(SUM(points_used), 0) as total_points_used
FROM combined_data;

-- 8. Check RLS policies
SELECT 'RLS check' as step;
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename IN ('transactions', 'points_transactions');

-- 9. Test RLS access
SELECT 'RLS test - transactions' as step;
SELECT COUNT(*) as accessible_transactions
FROM transactions
WHERE store_id IN (
    SELECT id FROM stores WHERE owner_id = auth.uid()
);

SELECT 'RLS test - points_transactions' as step;
SELECT COUNT(*) as accessible_points_transactions
FROM points_transactions
WHERE store_id IN (
    SELECT id FROM stores WHERE owner_id = auth.uid()
);

-- 10. Check current user context
SELECT 'User context' as step;
SELECT 
    auth.uid() as current_user_id,
    auth.jwt() ->> 'email' as user_email; 