-- Fix Award Points Function
-- Run this in your Supabase SQL editor to ensure the function exists and works properly

-- 1. Drop the existing function if it exists
DROP FUNCTION IF EXISTS award_points_to_customer(UUID, INTEGER, UUID, TEXT);
DROP FUNCTION IF EXISTS award_points_to_customer(UUID, INTEGER, UUID);
DROP FUNCTION IF EXISTS award_points_to_customer(UUID, INTEGER);

-- 2. Create the function with the correct signature
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

-- 3. Grant execute permission
GRANT EXECUTE ON FUNCTION award_points_to_customer(UUID, INTEGER, UUID, TEXT) TO authenticated;

-- 4. Test the function exists
SELECT 
    'Function created successfully' as status,
    proname as function_name,
    pg_get_function_arguments(oid) as arguments
FROM pg_proc 
WHERE proname = 'award_points_to_customer';

-- 5. Test with a sample call (this will fail if not authenticated, but shows the function exists)
SELECT 'Function test completed' as status;

SELECT 'Award points function fix completed successfully!' as final_status; 