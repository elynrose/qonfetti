-- Final Diagnostic for RLS Issue
-- This script will identify the exact root cause

-- 1. Check if we're even authenticated
SELECT 
    'Authentication check' as info,
    CASE WHEN auth.uid() IS NOT NULL THEN 'AUTHENTICATED' ELSE 'NOT AUTHENTICATED' END as auth_status,
    auth.uid() as user_id,
    auth.jwt() ->> 'email' as user_email;

-- 2. Check if the stores table exists and has data
SELECT 
    'Stores table check' as info,
    COUNT(*) as total_stores
FROM stores;

-- 3. Check if the points_transactions table exists and has data
SELECT 
    'Points transactions table check' as info,
    COUNT(*) as total_transactions
FROM points_transactions;

-- 4. Check RLS status on both tables
SELECT 
    'RLS status check' as info,
    schemaname,
    tablename,
    rowsecurity
FROM pg_tables 
WHERE tablename IN ('stores', 'points_transactions');

-- 5. Check all policies on points_transactions
SELECT 
    'Policies check' as info,
    policyname,
    permissive,
    roles,
    cmd,
    qual
FROM pg_policies 
WHERE tablename = 'points_transactions';

-- 6. Temporarily disable RLS and show ALL data
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;
ALTER TABLE stores DISABLE ROW LEVEL SECURITY;

-- 7. Show all stores
SELECT 
    'All stores (RLS disabled)' as info,
    s.id as store_id,
    s.name as store_name,
    s.owner_id as store_owner_id
FROM stores s;

-- 8. Show all transactions
SELECT 
    'All transactions (RLS disabled)' as info,
    pt.id,
    pt.customer_id,
    pt.store_id,
    pt.points_awarded,
    pt.transaction_type,
    pt.created_at
FROM points_transactions pt
ORDER BY pt.created_at DESC;

-- 9. Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;

-- 10. Test a simple query without any joins
SELECT 
    'Simple RLS test' as info,
    COUNT(*) as accessible_records
FROM points_transactions;

-- 11. Test with explicit store ownership check
SELECT 
    'Explicit ownership test' as info,
    COUNT(*) as accessible_records
FROM points_transactions pt
WHERE EXISTS (
    SELECT 1 FROM stores s 
    WHERE s.id = pt.store_id 
    AND s.owner_id = auth.uid()
);

-- 12. Check if the specific store exists and who owns it
SELECT 
    'Specific store ownership' as info,
    s.id as store_id,
    s.name as store_name,
    s.owner_id as store_owner_id,
    auth.uid() as current_user_id,
    CASE WHEN s.owner_id = auth.uid() THEN 'OWNER' ELSE 'NOT OWNER' END as ownership_status
FROM stores s
WHERE s.id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 13. Create a completely new store and test
INSERT INTO stores (id, name, owner_id, created_at, updated_at)
VALUES (
    'test-store-' || auth.uid(),
    'Test Store',
    auth.uid(),
    NOW(),
    NOW()
)
ON CONFLICT (id) DO NOTHING;

-- 14. Test with the new store
SELECT 
    'New store test' as info,
    COUNT(*) as accessible_records
FROM points_transactions pt
WHERE pt.store_id = 'test-store-' || auth.uid();

-- 15. Final summary
SELECT 
    'Final summary' as info,
    (SELECT COUNT(*) FROM stores WHERE owner_id = auth.uid()) as owned_stores,
    (SELECT COUNT(*) FROM points_transactions) as total_transactions,
    (SELECT COUNT(*) FROM points_transactions WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())) as accessible_transactions; 