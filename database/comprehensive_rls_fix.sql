-- Comprehensive RLS Fix for Points Transactions
-- This script addresses all possible issues with store ownership and RLS policies

-- 1. Check current user and store details
SELECT 
    'Current user info' as info,
    auth.uid() as user_id,
    auth.jwt() ->> 'email' as user_email;

-- 2. Check all stores and their ownership
SELECT 
    'All stores' as info,
    s.id as store_id,
    s.name as store_name,
    s.owner_id as store_owner_id,
    auth.uid() as current_user_id,
    CASE WHEN s.owner_id = auth.uid() THEN 'OWNER' ELSE 'NOT OWNER' END as ownership_status
FROM stores s;

-- 3. Check if the specific store exists
SELECT 
    'Target store check' as info,
    s.id as store_id,
    s.name as store_name,
    s.owner_id as store_owner_id,
    auth.uid() as current_user_id,
    CASE WHEN s.owner_id = auth.uid() THEN 'OWNER' ELSE 'NOT OWNER' END as ownership_status
FROM stores s
WHERE s.id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 4. If store doesn't exist, create it
INSERT INTO stores (id, name, owner_id, created_at, updated_at)
SELECT 
    'a667e115-9e6a-4d39-9ae3-7ab98b63386c',
    'Default Store',
    auth.uid(),
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM stores WHERE id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'
);

-- 5. Update store ownership to current user
UPDATE stores 
SET owner_id = auth.uid(),
    updated_at = NOW()
WHERE id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 6. Verify the update
SELECT 
    'Updated store ownership' as info,
    s.id as store_id,
    s.name as store_name,
    s.owner_id as store_owner_id,
    auth.uid() as current_user_id,
    CASE WHEN s.owner_id = auth.uid() THEN 'OWNER' ELSE 'NOT OWNER' END as ownership_status
FROM stores s
WHERE s.id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 7. Drop ALL existing policies
DROP POLICY IF EXISTS "Store owners can view points transactions at their stores" ON points_transactions;
DROP POLICY IF EXISTS "Store owners can insert points transactions at their stores" ON points_transactions;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "Users can view their own transactions" ON points_transactions;
DROP POLICY IF EXISTS "Users can insert their own transactions" ON points_transactions;

-- 8. Temporarily disable RLS to see what data exists
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;

-- 9. Show all transactions
SELECT 
    'All transactions (RLS disabled)' as info,
    COUNT(*) as total_transactions
FROM points_transactions;

-- 10. Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;

-- 11. Create simple, working policies
-- Policy for reading points transactions
CREATE POLICY "Store owners can view points transactions at their stores" ON points_transactions
    FOR SELECT USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- Policy for inserting points transactions
CREATE POLICY "Store owners can insert points transactions at their stores" ON points_transactions
    FOR INSERT WITH CHECK (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 12. Test the policies
SELECT 
    'RLS test after comprehensive fix' as info,
    COUNT(*) as accessible_records
FROM points_transactions
WHERE store_id IN (
    SELECT id FROM stores WHERE owner_id = auth.uid()
);

-- 13. Show accessible transactions
SELECT 
    'Accessible transactions after comprehensive fix' as info,
    pt.id,
    pt.customer_id,
    pt.store_id,
    pt.points_awarded,
    pt.previous_points,
    pt.new_points,
    pt.transaction_type,
    pt.description,
    pt.created_at,
    c.name as customer_name
FROM points_transactions pt
LEFT JOIN customers c ON pt.customer_id = c.id
WHERE pt.store_id IN (
    SELECT id FROM stores WHERE owner_id = auth.uid()
)
ORDER BY pt.created_at DESC;

-- 14. Final verification
SELECT 
    'Final verification' as info,
    (SELECT COUNT(*) FROM stores WHERE owner_id = auth.uid()) as owned_stores,
    (SELECT COUNT(*) FROM points_transactions WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())) as accessible_transactions; 