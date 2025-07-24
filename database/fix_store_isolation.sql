-- Fix Store Isolation - Ensure each store owner only sees their own data
-- Run this in your Supabase SQL editor to fix store isolation issues

-- 1. Drop existing policies to start fresh
DROP POLICY IF EXISTS "Store owners can manage points transactions" ON points_transactions;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "points_transactions_select_policy" ON points_transactions;
DROP POLICY IF EXISTS "points_transactions_insert_policy" ON points_transactions;

DROP POLICY IF EXISTS "Store owners can manage NFC cards" ON nfc_cards;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON nfc_cards;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON nfc_cards;
DROP POLICY IF EXISTS "Enable update access for authenticated users" ON nfc_cards;
DROP POLICY IF EXISTS "Enable delete access for authenticated users" ON nfc_cards;

DROP POLICY IF EXISTS "Store owners can manage customer points" ON customer_points;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON customer_points;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON customer_points;
DROP POLICY IF EXISTS "Enable update access for authenticated users" ON customer_points;
DROP POLICY IF EXISTS "Enable delete access for authenticated users" ON customer_points;

DROP POLICY IF EXISTS "Store owners can manage rewards" ON rewards;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON rewards;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON rewards;
DROP POLICY IF EXISTS "Enable update access for authenticated users" ON rewards;
DROP POLICY IF EXISTS "Enable delete access for authenticated users" ON rewards;

-- 2. Create proper store-isolated policies for points_transactions
CREATE POLICY "Store owners can view their own transactions" ON points_transactions
    FOR SELECT USING (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can insert their own transactions" ON points_transactions
    FOR INSERT WITH CHECK (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 3. Create proper store-isolated policies for nfc_cards
CREATE POLICY "Store owners can view their own NFC cards" ON nfc_cards
    FOR SELECT USING (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can insert their own NFC cards" ON nfc_cards
    FOR INSERT WITH CHECK (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can update their own NFC cards" ON nfc_cards
    FOR UPDATE USING (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can delete their own NFC cards" ON nfc_cards
    FOR DELETE USING (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 4. Create proper store-isolated policies for customer_points
CREATE POLICY "Store owners can view their own customer points" ON customer_points
    FOR SELECT USING (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can insert their own customer points" ON customer_points
    FOR INSERT WITH CHECK (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can update their own customer points" ON customer_points
    FOR UPDATE USING (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can delete their own customer points" ON customer_points
    FOR DELETE USING (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 5. Create proper store-isolated policies for rewards
CREATE POLICY "Store owners can view their own rewards" ON rewards
    FOR SELECT USING (
        auth.role() = 'authenticated' AND 
        (store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        ) OR is_shared = true)
    );

CREATE POLICY "Store owners can insert their own rewards" ON rewards
    FOR INSERT WITH CHECK (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can update their own rewards" ON rewards
    FOR UPDATE USING (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can delete their own rewards" ON rewards
    FOR DELETE USING (
        auth.role() = 'authenticated' AND 
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 6. Grant necessary permissions
GRANT ALL ON points_transactions TO authenticated;
GRANT ALL ON nfc_cards TO authenticated;
GRANT ALL ON customer_points TO authenticated;
GRANT ALL ON rewards TO authenticated;

-- 7. Test the policies
SELECT 'Testing store isolation policies' as step;

-- Check current user context
SELECT 
    'Current user context' as info,
    auth.uid() as user_id,
    auth.jwt() ->> 'email' as user_email,
    CASE WHEN auth.uid() IS NOT NULL THEN 'AUTHENTICATED' ELSE 'NOT AUTHENTICATED' END as auth_status;

-- Check user's stores
SELECT 
    'User stores' as info,
    id as store_id,
    name as store_name,
    owner_id
FROM stores 
WHERE owner_id = auth.uid();

-- Test if we can only see our own data
SELECT 
    'Points transactions count (should only show own store)' as test,
    COUNT(*) as count 
FROM points_transactions;

SELECT 
    'NFC cards count (should only show own store)' as test,
    COUNT(*) as count 
FROM nfc_cards;

SELECT 
    'Customer points count (should only show own store)' as test,
    COUNT(*) as count 
FROM customer_points;

SELECT 
    'Rewards count (should show own store + shared)' as test,
    COUNT(*) as count 
FROM rewards;

SELECT 'Store isolation fix completed successfully!' as final_status; 