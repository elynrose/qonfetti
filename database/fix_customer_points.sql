-- Comprehensive fix for customer_points table and customer creation issues
-- Run this in your Supabase SQL editor

-- 1. Check table structure using information_schema
SELECT '=== CUSTOMER_POINTS TABLE STRUCTURE ===' as info;
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'customer_points' 
ORDER BY ordinal_position;

-- 2. Check RLS policies
SELECT '=== CUSTOMER_POINTS RLS POLICIES ===' as info;
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'customer_points';

-- 3. Check current data
SELECT '=== CURRENT CUSTOMER_POINTS DATA ===' as info;
SELECT COUNT(*) as total_records FROM customer_points;

-- 4. Check if RLS is enabled
SELECT '=== RLS STATUS ===' as info;
SELECT schemaname, tablename, rowsecurity 
FROM pg_tables 
WHERE tablename = 'customer_points';

-- 5. Drop existing policies and recreate them
SELECT '=== DROPPING EXISTING POLICIES ===' as info;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON customer_points;
DROP POLICY IF EXISTS "Enable insert for authenticated users" ON customer_points;
DROP POLICY IF EXISTS "Enable update for users based on store_id" ON customer_points;
DROP POLICY IF EXISTS "Enable delete for users based on store_id" ON customer_points;

-- 6. Create simple, working policies
SELECT '=== CREATING NEW POLICIES ===' as info;
CREATE POLICY "Enable all access for store owners" ON customer_points
    FOR ALL
    TO authenticated
    USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    )
    WITH CHECK (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- 7. Grant permissions
GRANT ALL ON customer_points TO authenticated;

-- 8. Test the policy
SELECT '=== TESTING POLICY ===' as info;
SELECT COUNT(*) as accessible_records 
FROM customer_points 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 9. Test inserting a record
SELECT '=== TESTING INSERTION ===' as info;

-- First, check if the customer exists
SELECT 'Customer check:' as info;
SELECT id, name, email FROM customers 
WHERE id = '6b52c88c-ad80-4702-8dec-3f08971d6736';

-- Check if the store exists
SELECT 'Store check:' as info;
SELECT id, name, owner_id FROM stores 
WHERE id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- Test insertion
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

-- 10. Verify the insertion
SELECT '=== VERIFICATION ===' as info;
SELECT COUNT(*) as total_records FROM customer_points;
SELECT * FROM customer_points 
WHERE customer_id = '6b52c88c-ad80-4702-8dec-3f08971d6736' 
AND store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 11. Check for any constraint violations
SELECT '=== CONSTRAINT CHECK ===' as info;
SELECT 
    tc.table_name, 
    tc.constraint_name, 
    tc.constraint_type,
    kcu.column_name,
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu
    ON tc.constraint_name = kcu.constraint_name
    AND tc.table_schema = kcu.table_schema
LEFT JOIN information_schema.constraint_column_usage AS ccu
    ON ccu.constraint_name = tc.constraint_name
    AND ccu.table_schema = tc.table_schema
WHERE tc.table_name = 'customer_points'; 