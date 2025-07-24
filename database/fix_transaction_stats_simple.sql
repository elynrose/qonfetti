-- Fix Transaction Analytics - Copy and paste this into Supabase SQL Editor

-- Replace the existing function with this fixed version
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
        COALESCE(SUM(CASE WHEN source = 'transactions' AND transaction_type = 'purchase' THEN amount ELSE 0 END), 0) as total_purchases,
        
        -- Total claimed (from transactions table)
        COALESCE(SUM(CASE WHEN source = 'transactions' AND transaction_type = 'reward_claim' THEN amount ELSE 0 END), 0) as total_claimed,
        
        -- Total transactions (sum of both tables)
        COUNT(*) as total_transactions,
        
        -- Total points earned (from both tables)
        COALESCE(SUM(
            CASE 
                WHEN source = 'transactions' THEN points_earned
                WHEN source = 'points_transactions' THEN points_awarded
                ELSE 0
            END
        ), 0) as total_points_earned,
        
        -- Total points used (from transactions table)
        COALESCE(SUM(CASE WHEN source = 'transactions' THEN points_used ELSE 0 END), 0) as total_points_used
        
    FROM (
        -- Data from transactions table
        SELECT 
            'transactions' as source,
            transaction_type,
            amount,
            points_earned,
            points_used,
            created_at
        FROM transactions 
        WHERE store_id = store_id_param
        
        UNION ALL
        
        -- Data from points_transactions table (NFC scans)
        SELECT 
            'points_transactions' as source,
            transaction_type,
            0 as amount, -- NFC scans don't have purchase amounts
            points_awarded as points_earned,
            0 as points_used, -- NFC scans don't use points
            created_at
        FROM points_transactions 
        WHERE store_id = store_id_param
    ) combined_data;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_transaction_stats(UUID) TO authenticated; 