-- Analyze the 5 Transaction Records
-- This script will help identify why RLS is blocking access to these records

-- 1. Temporarily disable RLS to see all records
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;

-- 2. Show all 5 records with full details
SELECT 
    id,
    customer_id,
    store_id,
    nfc_card_id,
    points_awarded,
    previous_points,
    new_points,
    transaction_type,
    description,
    created_at,
    'FULL RECORD' as source
FROM points_transactions
ORDER BY created_at DESC;

-- 3. Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;

-- 4. Check current user context
SELECT 
    'Current user' as info,
    auth.uid() as user_id,
    auth.jwt() ->> 'email' as user_email;

-- 5. Check store ownership for the store_id in the transactions
SELECT 
    'Store ownership check' as info,
    s.id as store_id,
    s.name as store_name,
    s.owner_id as store_owner_id,
    auth.uid() as current_user_id,
    CASE WHEN s.owner_id = auth.uid() THEN 'OWNER' ELSE 'NOT OWNER' END as ownership_status
FROM stores s
WHERE s.id IN (
    SELECT DISTINCT store_id 
    FROM points_transactions 
    WHERE store_id IS NOT NULL
);

-- 6. Test RLS policies with current user
SELECT 
    'RLS test with current user' as info,
    COUNT(*) as accessible_records
FROM points_transactions
WHERE store_id IN (
    SELECT id FROM stores WHERE owner_id = auth.uid()
);

-- 7. Show what records should be accessible
SELECT 
    'Records that should be accessible' as info,
    pt.id,
    pt.store_id,
    s.name as store_name,
    s.owner_id as store_owner_id,
    auth.uid() as current_user_id
FROM points_transactions pt
JOIN stores s ON pt.store_id = s.id
WHERE s.owner_id = auth.uid()
ORDER BY pt.created_at DESC; 