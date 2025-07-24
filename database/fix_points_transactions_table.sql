-- Fix Points Transactions Table
-- Run this in your Supabase SQL Editor

-- First, let's check the current table structure
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'points_transactions' 
AND table_schema = 'public'
ORDER BY ordinal_position;

-- Check if previous_points column allows nulls
SELECT 
    column_name,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'points_transactions' 
AND column_name = 'previous_points'
AND table_schema = 'public';

-- If previous_points is NOT NULL, we need to either:
-- 1. Make it nullable, or
-- 2. Provide a default value

-- Option 1: Make previous_points nullable (recommended)
ALTER TABLE points_transactions 
ALTER COLUMN previous_points DROP NOT NULL;

-- Option 2: Or provide a default value of 0
-- ALTER TABLE points_transactions 
-- ALTER COLUMN previous_points SET DEFAULT 0;

-- Now let's also update the function to be more robust
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
    v_previous_points INTEGER DEFAULT 0;
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
    
    -- Get current points before update (default to 0 if no record exists)
    SELECT COALESCE(points, 0) INTO v_previous_points 
    FROM customer_points 
    WHERE customer_id = p_customer_id AND store_id = v_store_id;
    
    -- Ensure v_previous_points is never null
    IF v_previous_points IS NULL THEN
        v_previous_points := 0;
    END IF;
    
    -- Insert or update customer points
    INSERT INTO customer_points (customer_id, store_id, points)
    VALUES (p_customer_id, v_store_id, p_points)
    ON CONFLICT (customer_id, store_id) DO UPDATE SET
        points = customer_points.points + EXCLUDED.points,
        updated_at = NOW()
    RETURNING points INTO v_new_points;
    
    -- Create transaction record with explicit previous_points value
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
        v_previous_points,  -- This should now be 0 for new customers
        v_new_points,
        'nfc_scan',
        'Points awarded via NFC scan'
    );
    
    RETURN v_new_points;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION award_points_to_customer(UUID, INTEGER, UUID, TEXT) TO authenticated;

-- Verify the changes
SELECT 
    'Table and function updated successfully' as status,
    column_name,
    is_nullable
FROM information_schema.columns 
WHERE table_name = 'points_transactions' 
AND column_name = 'previous_points'
AND table_schema = 'public';

SELECT 'Points transactions fix completed successfully!' as final_status; 