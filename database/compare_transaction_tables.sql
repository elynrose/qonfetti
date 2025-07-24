-- Compare Transaction Tables
-- This script will help understand the difference between points_transactions and transactions tables

-- 1. Check points_transactions table
SELECT 'Points Transactions Table' as table_name, COUNT(*) as total_records FROM points_transactions;

-- 2. Check transactions table
SELECT 'Transactions Table' as table_name, COUNT(*) as total_records FROM transactions;

-- 3. Show recent points_transactions
SELECT 
    'Recent Points Transactions' as info,
    id,
    customer_id,
    store_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
ORDER BY created_at DESC 
LIMIT 10;

-- 4. Show recent transactions
SELECT 
    'Recent Transactions' as info,
    id,
    customer_id,
    store_id,
    transaction_type,
    amount,
    points_earned,
    points_used,
    created_at
FROM transactions 
ORDER BY created_at DESC 
LIMIT 10;

-- 5. Check for your specific store in points_transactions
SELECT 'Store Points Transactions' as info, COUNT(*) as count 
FROM points_transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05';

-- 6. Check for your specific store in transactions
SELECT 'Store Transactions' as info, COUNT(*) as count 
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05';

-- 7. Show points_transactions for your store
SELECT 
    'Points Transactions for Store' as info,
    id,
    customer_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'
ORDER BY created_at DESC;

-- 8. Show transactions for your store
SELECT 
    'Transactions for Store' as info,
    id,
    customer_id,
    transaction_type,
    amount,
    points_earned,
    points_used,
    created_at
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'
ORDER BY created_at DESC;

-- 9. Check if there are any reward_claim transactions
SELECT 'Reward Claims' as info, COUNT(*) as count 
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05' 
AND transaction_type = 'reward_claim';

-- 10. Check if there are any purchase transactions
SELECT 'Purchases' as info, COUNT(*) as count 
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05' 
AND transaction_type = 'purchase';

-- 11. Summary of what should be in transaction analytics
SELECT 'Transaction Analytics Summary' as info,
    'Points from NFC scans' as source,
    COUNT(*) as count,
    SUM(points_awarded) as total_points
FROM points_transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'

UNION ALL

SELECT 'Transaction Analytics Summary' as info,
    'Reward claims' as source,
    COUNT(*) as count,
    SUM(points_used) as total_points
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05' 
AND transaction_type = 'reward_claim'

UNION ALL

SELECT 'Transaction Analytics Summary' as info,
    'Purchases' as source,
    COUNT(*) as count,
    SUM(points_earned) as total_points
FROM transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05' 
AND transaction_type = 'purchase'; 