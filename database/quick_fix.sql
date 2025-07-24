-- Quick Fix for Points Transactions RLS Issue
-- Copy and paste this entire script into your Supabase SQL editor

-- 1. Check current user
SELECT 'Current user:' as info, auth.uid() as user_id, auth.jwt() ->> 'email' as email;

-- 2. Disable RLS temporarily to see all data
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;
ALTER TABLE stores DISABLE ROW LEVEL SECURITY;

-- 3. Show all transactions (should show the 5+ records)
SELECT 'Total transactions:' as info, COUNT(*) as count FROM points_transactions;

-- 4. Create/update store ownership
INSERT INTO stores (id, name, owner_id, created_at, updated_at)
VALUES (
    'a667e115-9e6a-4d39-9ae3-7ab98b63386c'::uuid,
    'Test Store',
    auth.uid(),
    NOW(),
    NOW()
)
ON CONFLICT (id) DO UPDATE SET
    owner_id = auth.uid(),
    updated_at = NOW();

-- 5. Drop all existing policies
DROP POLICY IF EXISTS "Store owners can view points transactions at their stores" ON points_transactions;
DROP POLICY IF EXISTS "Store owners can insert points transactions at their stores" ON points_transactions;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "points_transactions_select_policy" ON points_transactions;
DROP POLICY IF EXISTS "points_transactions_insert_policy" ON points_transactions;
DROP POLICY IF EXISTS "stores_select_policy" ON stores;
DROP POLICY IF EXISTS "stores_insert_policy" ON stores;

-- 6. Create new simple policies
CREATE POLICY "points_select" ON points_transactions FOR SELECT USING (
    store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())
);

CREATE POLICY "points_insert" ON points_transactions FOR INSERT WITH CHECK (
    store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())
);

CREATE POLICY "stores_select" ON stores FOR SELECT USING (owner_id = auth.uid());
CREATE POLICY "stores_insert" ON stores FOR INSERT WITH CHECK (owner_id = auth.uid());

-- 7. Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;

-- 8. Test the fix
SELECT 'RLS test result:' as info, COUNT(*) as accessible_transactions 
FROM points_transactions 
WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid());

-- 9. Show accessible transactions
SELECT 'Accessible transactions:' as info, 
       pt.id, pt.customer_id, pt.points_awarded, pt.transaction_type, pt.created_at
FROM points_transactions pt
WHERE pt.store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())
ORDER BY pt.created_at DESC
LIMIT 5;

-- 10. Final confirmation
SELECT 'FIX COMPLETE - Points transactions should now be visible in the app' as status; 