-- Final Transaction Analytics Fix
-- Run this AFTER running the diagnostic script to understand the structure

-- This function will work with the correct column names
-- Replace 'points_awarded' with the actual column name from your database

CREATE OR REPLACE FUNCTION get_transaction_stats(store_id_param UUID)
RETURNS TABLE (
    total_purchases DECIMAL(10,2),
    total_claimed DECIMAL(10,2),
    total_transactions INTEGER,
    total_points_earned INTEGER,
    total_points_used INTEGER
) AS $$
DECLARE
    points_column_name TEXT;
BEGIN
    -- First, let's find the correct column name for points in points_transactions
    SELECT column_name INTO points_column_name
    FROM information_schema.columns 
    WHERE table_name = 'points_transactions' 
    AND column_name LIKE '%point%'
    AND column_name != 'previous_points'
    AND column_name != 'new_points'
    LIMIT 1;
    
    -- If we found a points column, use it; otherwise, just use transactions table
    IF points_column_name IS NOT NULL THEN
        -- Dynamic SQL to use the correct column name
        EXECUTE format('
            SELECT 
                COALESCE(SUM(CASE WHEN source = ''transactions'' AND transaction_type = ''purchase'' THEN amount ELSE 0 END), 0) as total_purchases,
                COALESCE(SUM(CASE WHEN source = ''transactions'' AND transaction_type = ''reward_claim'' THEN amount ELSE 0 END), 0) as total_claimed,
                COUNT(*) as total_transactions,
                COALESCE(SUM(
                    CASE 
                        WHEN source = ''transactions'' THEN points_earned
                        WHEN source = ''points_transactions'' THEN %I
                        ELSE 0
                    END
                ), 0) as total_points_earned,
                COALESCE(SUM(CASE WHEN source = ''transactions'' THEN points_used ELSE 0 END), 0) as total_points_used
            FROM (
                SELECT 
                    ''transactions'' as source,
                    transaction_type,
                    amount,
                    points_earned,
                    points_used,
                    created_at
                FROM transactions 
                WHERE store_id = $1
                
                UNION ALL
                
                SELECT 
                    ''points_transactions'' as source,
                    transaction_type,
                    0 as amount,
                    %I as points_earned,
                    0 as points_used,
                    created_at
                FROM points_transactions 
                WHERE store_id = $1
            ) combined_data
        ', points_column_name, points_column_name)
        INTO total_purchases, total_claimed, total_transactions, total_points_earned, total_points_used
        USING store_id_param;
    ELSE
        -- Fallback to just transactions table
        SELECT 
            COALESCE(SUM(CASE WHEN transaction_type = 'purchase' THEN amount ELSE 0 END), 0),
            COALESCE(SUM(CASE WHEN transaction_type = 'reward_claim' THEN amount ELSE 0 END), 0),
            COUNT(*),
            COALESCE(SUM(points_earned), 0),
            COALESCE(SUM(points_used), 0)
        INTO total_purchases, total_claimed, total_transactions, total_points_earned, total_points_used
        FROM transactions 
        WHERE store_id = store_id_param;
    END IF;
    
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_transaction_stats(UUID) TO authenticated;

-- Test the function
SELECT 'Testing final function' as info;
SELECT * FROM get_transaction_stats('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID); 