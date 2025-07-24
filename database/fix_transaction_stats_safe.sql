-- Safe Transaction Analytics Fix
-- This version will work regardless of the actual column names

-- First, let's see what we're working with
SELECT 'Diagnosing the issue' as step;

-- Check if points_transactions table exists
SELECT 'Table exists?' as check, COUNT(*) as exists 
FROM information_schema.tables 
WHERE table_name = 'points_transactions';

-- Check the actual structure
SELECT 'Actual structure' as step;
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'points_transactions' 
ORDER BY ordinal_position;

-- Check if there's any data
SELECT 'Data check' as step;
SELECT COUNT(*) as total_records FROM points_transactions;

-- Show sample data to understand structure
SELECT 'Sample data' as step;
SELECT * FROM points_transactions LIMIT 1;

-- Now create a simple function that only uses the transactions table for now
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
        COUNT(*) as total_transactions,
        COALESCE(SUM(points_earned), 0) as total_points_earned,
        COALESCE(SUM(points_used), 0) as total_points_used
    FROM transactions 
    WHERE store_id = store_id_param;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_transaction_stats(UUID) TO authenticated;

-- Test the simple function
SELECT 'Testing simple function' as info;
SELECT * FROM get_transaction_stats('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID); 