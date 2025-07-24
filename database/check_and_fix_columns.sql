-- Check and Fix Column Names
-- This will help us understand the actual structure and fix the function

-- 1. Check what columns actually exist in points_transactions
SELECT 'Points transactions columns' as info;
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'points_transactions' 
ORDER BY ordinal_position;

-- 2. Show sample data to understand the structure
SELECT 'Sample points_transactions data' as info;
SELECT * FROM points_transactions LIMIT 3;

-- 3. Check if there are any columns with 'point' in the name
SELECT 'Columns with "point" in name' as info;
SELECT column_name 
FROM information_schema.columns 
WHERE table_name = 'points_transactions' 
AND column_name LIKE '%point%';

-- 4. Create a working function based on what we find
-- (We'll update this after seeing the actual structure)
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
        -- Total purchases (from transactions table)
        COALESCE(SUM(CASE WHEN transaction_type = 'purchase' THEN amount ELSE 0 END), 0) as total_purchases,
        
        -- Total claimed (from transactions table)
        COALESCE(SUM(CASE WHEN transaction_type = 'reward_claim' THEN amount ELSE 0 END), 0) as total_claimed,
        
        -- Total transactions (just from transactions table for now)
        COUNT(*)::INTEGER as total_transactions,
        
        -- Total points earned (from transactions table)
        COALESCE(SUM(points_earned), 0)::INTEGER as total_points_earned,
        
        -- Total points used (from transactions table)
        COALESCE(SUM(points_used), 0)::INTEGER as total_points_used
        
    FROM transactions 
    WHERE store_id = store_id_param;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_transaction_stats(UUID) TO authenticated;

-- Test the current function
SELECT 'Testing current function' as info;
SELECT * FROM get_transaction_stats('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID); 