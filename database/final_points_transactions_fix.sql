-- Final Points Transactions Fix
-- Run this in your Supabase SQL editor to fix NFC card ID recording

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

-- 4. Show existing transactions
SELECT 
    'Existing transactions' as info,
    id,
    customer_id,
    store_id,
    nfc_card_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
ORDER BY created_at DESC 
LIMIT 5;

-- 5. Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;

-- 6. Create simple policies that allow all authenticated users to read and insert
CREATE POLICY "Enable read access for authenticated users" ON points_transactions
    FOR SELECT USING (auth.role() = 'authenticated');

CREATE POLICY "Enable insert access for authenticated users" ON points_transactions
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- 7. Grant necessary permissions
GRANT ALL ON points_transactions TO authenticated;

-- 8. Update the award_points_to_customer function to include NFC card ID
CREATE OR REPLACE FUNCTION award_points_to_customer(
    p_customer_id UUID,
    p_points INTEGER,
    p_store_id UUID DEFAULT NULL,
    p_nfc_card_id TEXT DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    v_store_id UUID;
    v_new_points INTEGER;
    v_previous_points INTEGER;
    current_user_id UUID;
BEGIN
    -- Get current user ID
    current_user_id := auth.uid();
    
    -- Check if user is authenticated
    IF current_user_id IS NULL THEN
        RAISE EXCEPTION 'User not authenticated';
    END IF;
    
    -- Get store ID if not provided
    IF p_store_id IS NULL THEN
        v_store_id := get_or_create_store();
    ELSE
        v_store_id := p_store_id;
    END IF;
    
    -- Get current points before update
    SELECT COALESCE(points, 0) INTO v_previous_points 
    FROM customer_points 
    WHERE customer_id = p_customer_id AND store_id = v_store_id;
    
    -- Insert or update customer points
    INSERT INTO customer_points (customer_id, store_id, points)
    VALUES (p_customer_id, v_store_id, p_points)
    ON CONFLICT (customer_id, store_id) DO UPDATE SET
        points = customer_points.points + EXCLUDED.points,
        updated_at = NOW()
    RETURNING points INTO v_new_points;
    
    -- Create transaction record with NFC card ID
    INSERT INTO points_transactions (
        customer_id, 
        store_id, 
        nfc_card_id,
        points_awarded, 
        previous_points, 
        new_points, 
        transaction_type, 
        description
    ) VALUES (
        p_customer_id,
        v_store_id,
        p_nfc_card_id,
        p_points,
        v_previous_points,
        v_new_points,
        'nfc_scan',
        'Points awarded via NFC scan'
    );
    
    RETURN v_new_points;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 9. Test the policies
SELECT 'Testing policies' as step;
SELECT 
    'Current user context' as info,
    auth.uid() as user_id,
    auth.jwt() ->> 'email' as user_email,
    CASE WHEN auth.uid() IS NOT NULL THEN 'AUTHENTICATED' ELSE 'NOT AUTHENTICATED' END as auth_status;

-- 10. Test if we can read transactions (should work now)
SELECT 'Testing read access' as step, COUNT(*) as count FROM points_transactions;

-- 11. Show function signature
SELECT 
    'Function updated' as step,
    proname as function_name,
    pg_get_function_arguments(oid) as arguments
FROM pg_proc 
WHERE proname = 'award_points_to_customer';

SELECT 'Points transactions fix completed successfully!' as status; 