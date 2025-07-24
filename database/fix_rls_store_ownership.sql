-- Fix RLS Store Ownership Issue
-- This script fixes the RLS policies to allow access to transactions for store owners

-- 1. Check current store ownership
SELECT 
    'Current store ownership check' as info,
    s.id as store_id,
    s.name as store_name,
    s.owner_id as store_owner_id,
    auth.uid() as current_user_id,
    CASE WHEN s.owner_id = auth.uid() THEN 'OWNER' ELSE 'NOT OWNER' END as ownership_status
FROM stores s
WHERE s.id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 2. Update the store owner to the current user
UPDATE stores 
SET owner_id = auth.uid()
WHERE id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 3. Verify the update
SELECT 
    'Updated store ownership' as info,
    s.id as store_id,
    s.name as store_name,
    s.owner_id as store_owner_id,
    auth.uid() as current_user_id,
    CASE WHEN s.owner_id = auth.uid() THEN 'OWNER' ELSE 'NOT OWNER' END as ownership_status
FROM stores s
WHERE s.id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 4. Drop existing problematic policies
DROP POLICY IF EXISTS "Store owners can view points transactions at their stores" ON points_transactions;
DROP POLICY IF EXISTS "Store owners can insert points transactions at their stores" ON points_transactions;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON points_transactions;

-- 5. Create new, working policies
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

-- 6. Test the policies
SELECT 
    'RLS test after fix' as info,
    COUNT(*) as accessible_records
FROM points_transactions
WHERE store_id IN (
    SELECT id FROM stores WHERE owner_id = auth.uid()
);

-- 7. Show the transactions that should now be accessible
SELECT 
    'Accessible transactions after fix' as info,
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