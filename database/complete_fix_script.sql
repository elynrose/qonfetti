-- Complete Database Fix Script for Qonfetty App
-- Run this in Supabase SQL Editor to fix all RLS policy issues

-- 1. First, let's check the current state
SELECT 'Current state check' as step;

-- Check if tables exist
SELECT 'Tables check' as info, 
       COUNT(*) as count 
FROM information_schema.tables 
WHERE table_name IN ('customers', 'customer_points', 'nfc_cards', 'stores', 'rewards');

-- Check current data counts
SELECT 'Data counts' as info,
       (SELECT COUNT(*) FROM customers) as customers,
       (SELECT COUNT(*) FROM customer_points) as customer_points,
       (SELECT COUNT(*) FROM nfc_cards) as nfc_cards,
       (SELECT COUNT(*) FROM stores) as stores;

-- 2. Fix customer_points RLS policies
SELECT 'Fixing customer_points RLS policies' as step;

-- Drop existing policies
DROP POLICY IF EXISTS "Store owners can view customer points" ON customer_points;
DROP POLICY IF EXISTS "Store owners can insert customer points" ON customer_points;
DROP POLICY IF EXISTS "Store owners can update customer points" ON customer_points;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON customer_points;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON customer_points;
DROP POLICY IF EXISTS "Enable update access for authenticated users" ON customer_points;

-- Create new comprehensive policies for customer_points
CREATE POLICY "Store owners can view customer points" ON customer_points
    FOR SELECT USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can insert customer points" ON customer_points
    FOR INSERT WITH CHECK (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can update customer points" ON customer_points
    FOR UPDATE USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    ) WITH CHECK (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 3. Fix nfc_cards RLS policies
SELECT 'Fixing nfc_cards RLS policies' as step;

-- Drop existing policies
DROP POLICY IF EXISTS "Store owners can view NFC cards" ON nfc_cards;
DROP POLICY IF EXISTS "Store owners can insert NFC cards" ON nfc_cards;
DROP POLICY IF EXISTS "Store owners can update NFC cards" ON nfc_cards;
DROP POLICY IF EXISTS "Store owners can delete NFC cards" ON nfc_cards;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON nfc_cards;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON nfc_cards;
DROP POLICY IF EXISTS "Enable update access for authenticated users" ON nfc_cards;
DROP POLICY IF EXISTS "Enable delete access for authenticated users" ON nfc_cards;

-- Create new comprehensive policies for nfc_cards
CREATE POLICY "Store owners can view NFC cards" ON nfc_cards
    FOR SELECT USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can insert NFC cards" ON nfc_cards
    FOR INSERT WITH CHECK (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can update NFC cards" ON nfc_cards
    FOR UPDATE USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    ) WITH CHECK (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can delete NFC cards" ON nfc_cards
    FOR DELETE USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 4. Fix customers RLS policies
SELECT 'Fixing customers RLS policies' as step;

-- Drop existing policies
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON customers;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON customers;
DROP POLICY IF EXISTS "Enable update access for authenticated users" ON customers;
DROP POLICY IF EXISTS "Enable delete access for authenticated users" ON customers;

-- Create new policies for customers (allow all authenticated users to read/write customers)
CREATE POLICY "Enable read access for authenticated users" ON customers
    FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Enable insert access for authenticated users" ON customers
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

CREATE POLICY "Enable update access for authenticated users" ON customers
    FOR UPDATE USING (auth.role() = 'authenticated')
    WITH CHECK (auth.role() = 'authenticated');

CREATE POLICY "Enable delete access for authenticated users" ON customers
    FOR DELETE USING (auth.role() = 'authenticated');

-- 5. Fix stores RLS policies
SELECT 'Fixing stores RLS policies' as step;

-- Drop existing policies
DROP POLICY IF EXISTS "Store owners can view their stores" ON stores;
DROP POLICY IF EXISTS "Store owners can update their stores" ON stores;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON stores;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON stores;
DROP POLICY IF EXISTS "Enable update access for authenticated users" ON stores;

-- Create new policies for stores
CREATE POLICY "Store owners can view their stores" ON stores
    FOR SELECT USING (owner_id = auth.uid());

CREATE POLICY "Store owners can update their stores" ON stores
    FOR UPDATE USING (owner_id = auth.uid())
    WITH CHECK (owner_id = auth.uid());

CREATE POLICY "Enable insert access for authenticated users" ON stores
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- 6. Fix rewards RLS policies
SELECT 'Fixing rewards RLS policies' as step;

-- Drop existing policies
DROP POLICY IF EXISTS "Store owners can view rewards" ON rewards;
DROP POLICY IF EXISTS "Store owners can insert rewards" ON rewards;
DROP POLICY IF EXISTS "Store owners can update rewards" ON rewards;
DROP POLICY IF EXISTS "Store owners can delete rewards" ON rewards;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON rewards;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON rewards;
DROP POLICY IF EXISTS "Enable update access for authenticated users" ON rewards;
DROP POLICY IF EXISTS "Enable delete access for authenticated users" ON rewards;

-- Create new policies for rewards
CREATE POLICY "Store owners can view rewards" ON rewards
    FOR SELECT USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
        OR is_shared = true
    );

CREATE POLICY "Store owners can insert rewards" ON rewards
    FOR INSERT WITH CHECK (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can update rewards" ON rewards
    FOR UPDATE USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    ) WITH CHECK (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can delete rewards" ON rewards
    FOR DELETE USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 7. Fix reward_claims RLS policies
SELECT 'Fixing reward_claims RLS policies' as step;

-- Drop existing policies
DROP POLICY IF EXISTS "Store owners can view reward claims" ON reward_claims;
DROP POLICY IF EXISTS "Store owners can insert reward claims" ON reward_claims;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON reward_claims;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON reward_claims;

-- Create new policies for reward_claims
CREATE POLICY "Store owners can view reward claims" ON reward_claims
    FOR SELECT USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can insert reward claims" ON reward_claims
    FOR INSERT WITH CHECK (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 8. Ensure RLS is enabled on all tables
SELECT 'Enabling RLS on all tables' as step;

ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_points ENABLE ROW LEVEL SECURITY;
ALTER TABLE nfc_cards ENABLE ROW LEVEL SECURITY;
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;
ALTER TABLE rewards ENABLE ROW LEVEL SECURITY;
ALTER TABLE reward_claims ENABLE ROW LEVEL SECURITY;

-- 9. Grant necessary permissions
SELECT 'Granting permissions' as step;

GRANT ALL ON customers TO authenticated;
GRANT ALL ON customer_points TO authenticated;
GRANT ALL ON nfc_cards TO authenticated;
GRANT ALL ON stores TO authenticated;
GRANT ALL ON rewards TO authenticated;
GRANT ALL ON reward_claims TO authenticated;

-- 10. Test the fixes
SELECT 'Testing fixes' as step;

-- Test customer creation and store association
SELECT 'Current store ownership' as info,
       s.id as store_id,
       s.name as store_name,
       s.owner_id,
       COUNT(cp.customer_id) as customer_count
FROM stores s
LEFT JOIN customer_points cp ON s.id = cp.store_id
GROUP BY s.id, s.name, s.owner_id;

-- Test RLS policies
SELECT 'RLS policy test' as info,
       (SELECT COUNT(*) FROM customer_points WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())) as accessible_customer_points,
       (SELECT COUNT(*) FROM nfc_cards WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())) as accessible_nfc_cards,
       (SELECT COUNT(*) FROM rewards WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid()) OR is_shared = true) as accessible_rewards;

-- 11. Create a function to help with NFC card registration validation
SELECT 'Creating NFC validation function' as step;

CREATE OR REPLACE FUNCTION validate_nfc_registration(
    p_card_id TEXT,
    p_member_id TEXT,
    p_customer_id UUID,
    p_store_id UUID
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Check if customer exists
    IF NOT EXISTS (SELECT 1 FROM customers WHERE id = p_customer_id) THEN
        RAISE EXCEPTION 'Customer does not exist';
    END IF;
    
    -- Check if customer is associated with the store
    IF NOT EXISTS (SELECT 1 FROM customer_points WHERE customer_id = p_customer_id AND store_id = p_store_id) THEN
        RAISE EXCEPTION 'Customer does not exist or is not associated with this store';
    END IF;
    
    -- Check if card is already registered
    IF EXISTS (SELECT 1 FROM nfc_cards WHERE card_id = p_card_id AND is_active = true) THEN
        RAISE EXCEPTION 'NFC card is already registered';
    END IF;
    
    RETURN TRUE;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION validate_nfc_registration TO authenticated;

-- 12. Create trigger for NFC card registration validation
SELECT 'Creating NFC validation trigger' as step;

CREATE OR REPLACE FUNCTION nfc_card_validation_trigger()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Validate the registration
    PERFORM validate_nfc_registration(
        NEW.card_id,
        NEW.member_id,
        NEW.customer_id::UUID,
        NEW.store_id::UUID
    );
    
    RETURN NEW;
END;
$$;

-- Drop existing trigger if it exists
DROP TRIGGER IF EXISTS nfc_card_validation_trigger ON nfc_cards;

-- Create the trigger
CREATE TRIGGER nfc_card_validation_trigger
    BEFORE INSERT ON nfc_cards
    FOR EACH ROW
    EXECUTE FUNCTION nfc_card_validation_trigger();

-- 13. Final verification
SELECT 'Final verification' as step;

-- Check all policies are in place
SELECT 'Policy verification' as info,
       schemaname,
       tablename,
       policyname,
       permissive,
       roles,
       cmd,
       qual,
       with_check
FROM pg_policies
WHERE tablename IN ('customers', 'customer_points', 'nfc_cards', 'stores', 'rewards', 'reward_claims')
ORDER BY tablename, policyname;

-- Test data access
SELECT 'Final data access test' as info,
       (SELECT COUNT(*) FROM customers) as total_customers,
       (SELECT COUNT(*) FROM customer_points) as total_customer_points,
       (SELECT COUNT(*) FROM nfc_cards) as total_nfc_cards,
       (SELECT COUNT(*) FROM stores) as total_stores;

SELECT 'Fix completed successfully!' as status; 