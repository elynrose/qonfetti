-- Fix Points Transactions RLS Policies
-- Run this in your Supabase SQL editor to ensure proper access

-- 1. Drop existing policies to start fresh
DROP POLICY IF EXISTS "Store owners can manage points transactions" ON points_transactions;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "points_transactions_select_policy" ON points_transactions;
DROP POLICY IF EXISTS "points_transactions_insert_policy" ON points_transactions;

-- 2. Temporarily disable RLS to see all data
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;

-- 3. Check current data
SELECT 'Current data count (RLS disabled):' as info, COUNT(*) as count FROM points_transactions;

-- 4. Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;

-- 5. Create simple policies that allow all authenticated users to read and insert
CREATE POLICY "Enable read access for authenticated users" ON points_transactions
    FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Enable insert access for authenticated users" ON points_transactions
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- 6. Grant necessary permissions
GRANT ALL ON points_transactions TO authenticated;

-- 7. Test the policies
SELECT 'Testing policies' as step;
SELECT 
    'Current user context' as info,
    auth.uid() as user_id,
    auth.jwt() ->> 'email' as user_email,
    CASE WHEN auth.uid() IS NOT NULL THEN 'AUTHENTICATED' ELSE 'NOT AUTHENTICATED' END as auth_status;

-- 8. Test if we can read transactions (should work now)
SELECT 'Testing read access' as step, COUNT(*) as count FROM points_transactions;

-- 9. Show any existing transactions
SELECT 
    'Existing transactions' as step,
    id,
    customer_id,
    store_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
ORDER BY created_at DESC 
LIMIT 5;

SELECT 'RLS policies fixed successfully!' as status; 