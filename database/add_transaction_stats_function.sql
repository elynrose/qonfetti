-- Add only the transaction statistics function
-- This script assumes the transactions table and policies already exist

-- Function to get transaction statistics for dashboard
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

-- Grant execute permission on the function
GRANT EXECUTE ON FUNCTION get_transaction_stats(UUID) TO authenticated; 