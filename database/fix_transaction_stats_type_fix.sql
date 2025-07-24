-- Fix Transaction Analytics - Type Fix
-- This version fixes the bigint vs integer type mismatch

-- Create the corrected function with proper type casting
CREATE OR REPLACE FUNCTION get_transaction_stats(store_id_param UUID)
RETURNS TABLE (
    total_purchases DECIMAL(10,2),
    total_claimed DECIMAL(10,2),
    total_transactions INTEGER,
    total_points_earned INTEGER,
    total_points_used INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COALESCE(SUM(CASE WHEN transaction_type = 'purchase' THEN amount ELSE 0 END), 0) as total_purchases,
        COALESCE(SUM(CASE WHEN transaction_type = 'reward_claim' THEN amount ELSE 0 END), 0) as total_claimed,
        COUNT(*)::INTEGER as total_transactions,  -- Cast bigint to integer
        COALESCE(SUM(points_earned), 0)::INTEGER as total_points_earned,  -- Ensure integer
        COALESCE(SUM(points_used), 0)::INTEGER as total_points_used  -- Ensure integer
    FROM transactions 
    WHERE store_id = store_id_param;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_transaction_stats(UUID) TO authenticated;

-- Test the function
SELECT 'Testing type-fixed function' as info;
SELECT * FROM get_transaction_stats('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID); 