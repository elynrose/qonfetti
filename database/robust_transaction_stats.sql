-- Robust Transaction Analytics
-- This version handles RLS and column access issues

-- First, let's check RLS status and test access
SELECT 'RLS Status Check' as info;
SELECT tablename, rowsecurity 
FROM pg_tables 
WHERE tablename IN ('transactions', 'points_transactions');

-- Test RLS access to points_transactions
SELECT 'RLS Test - points_transactions' as info;
SELECT COUNT(*) as accessible_records
FROM points_transactions
WHERE store_id IN (
    SELECT id FROM stores WHERE owner_id = auth.uid()
);

-- Test direct access to points_transactions for your store
SELECT 'Direct access test' as info;
SELECT COUNT(*) as total_records 
FROM points_transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05';

-- Show sample data with RLS
SELECT 'Sample data with RLS' as info;
SELECT 
    id,
    customer_id,
    store_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
WHERE store_id = 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'
LIMIT 3;

-- Create a robust function that handles potential issues
CREATE OR REPLACE FUNCTION get_transaction_stats(store_id_param UUID)
RETURNS TABLE (
    total_purchases DECIMAL(10,2),
    total_claimed DECIMAL(10,2),
    total_transactions INTEGER,
    total_points_earned INTEGER,
    total_points_used INTEGER
) AS $$
DECLARE
    points_transactions_count INTEGER;
    transactions_count INTEGER;
BEGIN
    -- First, let's check if we can access points_transactions
    SELECT COUNT(*) INTO points_transactions_count
    FROM points_transactions 
    WHERE store_id = store_id_param;
    
    -- Check transactions table
    SELECT COUNT(*) INTO transactions_count
    FROM transactions 
    WHERE store_id = store_id_param;
    
    -- If we can access both tables, use the combined approach
    IF points_transactions_count > 0 AND transactions_count > 0 THEN
        RETURN QUERY
        SELECT 
            COALESCE(SUM(CASE WHEN source = 'transactions' AND transaction_type = 'purchase' THEN amount ELSE 0 END), 0) as total_purchases,
            COALESCE(SUM(CASE WHEN source = 'transactions' AND transaction_type = 'reward_claim' THEN amount ELSE 0 END), 0) as total_claimed,
            COUNT(*)::INTEGER as total_transactions,
            COALESCE(SUM(
                CASE 
                    WHEN source = 'transactions' THEN points_earned
                    WHEN source = 'points_transactions' THEN points_awarded
                    ELSE 0
                END
            ), 0)::INTEGER as total_points_earned,
            COALESCE(SUM(CASE WHEN source = 'transactions' THEN points_used ELSE 0 END), 0)::INTEGER as total_points_used
        FROM (
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
            
            SELECT 
                'points_transactions' as source,
                transaction_type,
                0 as amount,
                points_awarded as points_earned,
                0 as points_used,
                created_at
            FROM points_transactions 
            WHERE store_id = store_id_param
        ) combined_data;
    ELSE
        -- Fallback to just transactions table
        RETURN QUERY
        SELECT 
            COALESCE(SUM(CASE WHEN transaction_type = 'purchase' THEN amount ELSE 0 END), 0) as total_purchases,
            COALESCE(SUM(CASE WHEN transaction_type = 'reward_claim' THEN amount ELSE 0 END), 0) as total_claimed,
            COUNT(*)::INTEGER as total_transactions,
            COALESCE(SUM(points_earned), 0)::INTEGER as total_points_earned,
            COALESCE(SUM(points_used), 0)::INTEGER as total_points_used
        FROM transactions 
        WHERE store_id = store_id_param;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_transaction_stats(UUID) TO authenticated;

-- Test the robust function
SELECT 'Testing robust function' as info;
SELECT * FROM get_transaction_stats('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID); 