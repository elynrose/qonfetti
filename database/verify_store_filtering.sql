-- Verify Store-Based Filtering for Recent Activity
-- This script helps verify that RLS policies are correctly filtering data by store

-- 1. Check current user and their store
SELECT 'Current User Info' as check_type, 
       auth.uid() as user_id,
       auth.role() as user_role;

-- 2. Check if user has a store
SELECT 'User Store Check' as check_type,
       id as store_id,
       name as store_name,
       owner_id
FROM stores 
WHERE owner_id = auth.uid();

-- 3. Check total transactions in the system (should be filtered by RLS)
SELECT 'Total Transactions (RLS Filtered)' as check_type,
       COUNT(*) as total_transactions
FROM points_transactions;

-- 4. Check transactions for current user's store
SELECT 'Store Transactions' as check_type,
       COUNT(*) as store_transactions
FROM points_transactions 
WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid());

-- 5. Show sample transactions for current store
SELECT 'Sample Store Transactions' as check_type,
       id,
       customer_id,
       store_id,
       points_awarded,
       transaction_type,
       created_at
FROM points_transactions 
WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())
ORDER BY created_at DESC 
LIMIT 5;

-- 6. Check RLS policies on points_transactions table
SELECT 'RLS Policies Check' as check_type,
       schemaname,
       tablename,
       policyname,
       permissive,
       roles,
       cmd,
       qual,
       with_check
FROM pg_policies 
WHERE tablename = 'points_transactions';

-- 7. Verify that transactions from other stores are not accessible
SELECT 'Cross-Store Access Test' as check_type,
       COUNT(*) as other_store_transactions
FROM points_transactions 
WHERE store_id NOT IN (SELECT id FROM stores WHERE owner_id = auth.uid());

-- 8. Check if there are any transactions without proper store filtering
SELECT 'Unfiltered Transactions Test' as check_type,
       COUNT(*) as unfiltered_count
FROM points_transactions 
WHERE store_id IS NULL;

-- 9. Show recent activity for current store with customer info
SELECT 'Recent Activity with Customer Info' as check_type,
       pt.id,
       pt.customer_id,
       c.name as customer_name,
       pt.store_id,
       pt.points_awarded,
       pt.transaction_type,
       pt.created_at
FROM points_transactions pt
LEFT JOIN customers c ON pt.customer_id = c.id
WHERE pt.store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())
ORDER BY pt.created_at DESC 
LIMIT 10; 