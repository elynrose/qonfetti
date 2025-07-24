-- Verify Points Transactions Fix
-- Run this to check if the fix worked

-- 1. Check current user and authentication
SELECT 'Current user:' as info, auth.uid() as user_id, auth.jwt() ->> 'email' as email;

-- 2. Check store ownership
SELECT 'Store ownership:' as info, 
       s.id as store_id, 
       s.name as store_name, 
       s.owner_id as store_owner_id,
       auth.uid() as current_user_id,
       CASE WHEN s.owner_id = auth.uid() THEN 'OWNER' ELSE 'NOT OWNER' END as ownership_status
FROM stores s
WHERE s.id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'::uuid;

-- 3. Check RLS status
SELECT 'RLS status:' as info, 
       schemaname, tablename, rowsecurity
FROM pg_tables 
WHERE tablename IN ('points_transactions', 'stores');

-- 4. Check policies
SELECT 'Policies:' as info, 
       tablename, policyname, permissive, roles, cmd
FROM pg_policies 
WHERE tablename IN ('points_transactions', 'stores')
ORDER BY tablename, policyname;

-- 5. Test the exact query the app uses
SELECT 'App query test:' as info, COUNT(*) as count
FROM points_transactions
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'::uuid;

-- 6. Test with RLS policy logic
SELECT 'RLS policy test:' as info, COUNT(*) as count
FROM points_transactions
WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid());

-- 7. Show all transactions (if any are accessible)
SELECT 'Accessible transactions:' as info, 
       pt.id, pt.customer_id, pt.points_awarded, pt.transaction_type, pt.created_at
FROM points_transactions pt
WHERE pt.store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())
ORDER BY pt.created_at DESC
LIMIT 5;

-- 8. If still 0, temporarily disable RLS to see all data
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;
SELECT 'All transactions (RLS disabled):' as info, COUNT(*) as count FROM points_transactions;
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY; 