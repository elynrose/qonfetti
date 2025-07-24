-- Debug Transaction Analytics Issue
-- This script will help identify why the numbers aren't adding up

-- 1. Check if the transactions table exists and has data
SELECT 'Transactions table check' as step, COUNT(*) as total_transactions FROM transactions;

-- 2. Check the structure of the transactions table
SELECT 'Table structure' as step;
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'transactions' 
ORDER BY ordinal_position;

-- 3. Show all transactions with details
SELECT 
    'All transactions' as step,
    id,
    store_id,
    customer_id,
    transaction_type,
    amount,
    points_used,
    points_earned,
    description,
    created_at
FROM transactions 
ORDER BY created_at DESC;

-- 4. Check transactions for your specific store
SELECT 'Store transactions count' as step, COUNT(*) as count 
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05';

-- 5. Show transactions for your store
SELECT 
    'Store transactions' as step,
    id,
    customer_id,
    transaction_type,
    amount,
    points_used,
    points_earned,
    created_at
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'
ORDER BY created_at DESC;

-- 6. Test the get_transaction_stats function manually
SELECT 'Manual function test' as step;
SELECT * FROM get_transaction_stats('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID);

-- 7. Compare with manual calculation
SELECT 'Manual calculation' as step,
    COALESCE(SUM(CASE WHEN transaction_type = 'purchase' THEN amount ELSE 0 END), 0) as total_purchases,
    COALESCE(SUM(CASE WHEN transaction_type = 'reward_claim' THEN amount ELSE 0 END), 0) as total_claimed,
    COUNT(*) as total_transactions,
    COALESCE(SUM(points_earned), 0) as total_points_earned,
    COALESCE(SUM(points_used), 0) as total_points_used
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05';

-- 8. Check if there are any transactions with NULL amounts
SELECT 'NULL amounts check' as step, COUNT(*) as count
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05' 
AND amount IS NULL;

-- 9. Check transaction types distribution
SELECT 'Transaction types' as step, 
    transaction_type, 
    COUNT(*) as count,
    SUM(amount) as total_amount,
    SUM(points_earned) as total_points_earned,
    SUM(points_used) as total_points_used
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'
GROUP BY transaction_type;

-- 10. Check RLS policies
SELECT 'RLS policies' as step;
SELECT policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'transactions';

-- 11. Test RLS with current user
SELECT 'RLS test' as step, COUNT(*) as accessible_records
FROM transactions
WHERE store_id IN (
    SELECT id FROM stores WHERE owner_id = auth.uid()
); 