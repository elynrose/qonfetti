-- Quick fix for customer creation issues
-- Run this in your Supabase SQL editor

-- 1. Check current customer_points data
SELECT 'Current customer_points records:' as info;
SELECT COUNT(*) as total_records FROM customer_points;

-- 2. Check if the specific customer exists
SELECT 'Customer check:' as info;
SELECT id, name, email FROM customers 
WHERE id = '6b52c88c-ad80-4702-8dec-3f08971d6736';

-- 3. Check if the store exists
SELECT 'Store check:' as info;
SELECT id, name, owner_id FROM stores 
WHERE id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 4. Check current RLS policies
SELECT 'Current RLS policies:' as info;
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'customer_points';

-- 5. Test direct insertion (bypassing RLS temporarily)
SELECT 'Testing direct insertion...' as info;

-- Temporarily disable RLS
ALTER TABLE customer_points DISABLE ROW LEVEL SECURITY;

-- Try to insert the record
INSERT INTO customer_points (
    customer_id,
    store_id,
    points
) VALUES (
    '6b52c88c-ad80-4702-8dec-3f08971d6736',
    'a667e115-9e6a-4d39-9ae3-7ab98b63386c',
    0
) ON CONFLICT (customer_id, store_id) DO UPDATE SET
    points = EXCLUDED.points,
    updated_at = NOW();

-- Re-enable RLS
ALTER TABLE customer_points ENABLE ROW LEVEL SECURITY;

-- 6. Verify the insertion worked
SELECT 'After insertion:' as info;
SELECT COUNT(*) as total_records FROM customer_points;
SELECT * FROM customer_points 
WHERE customer_id = '6b52c88c-ad80-4702-8dec-3f08971d6736' 
AND store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 7. Test if we can query the record with RLS enabled
SELECT 'Testing RLS access:' as info;
SELECT COUNT(*) as accessible_records 
FROM customer_points 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'; 