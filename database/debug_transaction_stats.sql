-- Debug version of transaction stats function to identify date filtering issues
CREATE OR REPLACE FUNCTION debug_transaction_stats_with_date_range(
    store_id_param UUID,
    start_date_param BIGINT DEFAULT NULL,
    end_date_param BIGINT DEFAULT NULL
)
RETURNS TABLE (
    total_purchases DECIMAL(10,2),
    total_claimed DECIMAL(10,2),
    total_transactions INTEGER,
    total_points_earned INTEGER,
    total_points_used INTEGER,
    debug_info TEXT
) AS $$
DECLARE
    start_date_timestamp TIMESTAMP;
    end_date_timestamp TIMESTAMP;
    debug_msg TEXT;
BEGIN
    -- Convert timestamps for debugging
    IF start_date_param IS NOT NULL THEN
        start_date_timestamp := to_timestamp(start_date_param / 1000);
    END IF;
    
    IF end_date_param IS NOT NULL THEN
        end_date_timestamp := to_timestamp(end_date_param / 1000);
    END IF;
    
    debug_msg := 'Store: ' || store_id_param || 
                 ', Start: ' || COALESCE(start_date_timestamp::TEXT, 'NULL') ||
                 ', End: ' || COALESCE(end_date_timestamp::TEXT, 'NULL');
    
    RETURN QUERY
    SELECT
        COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' AND (combined_data.transaction_type = 'purchase' OR combined_data.transaction_type = 'reward_claim') THEN combined_data.amount ELSE 0 END), 0) as total_purchases,
        COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' AND combined_data.transaction_type = 'reward_claim' THEN combined_data.reward_price ELSE 0 END), 0) as total_claimed,
        COUNT(*)::INTEGER as total_transactions,
        COALESCE(SUM(combined_data.points_earned), 0)::INTEGER as total_points_earned,
        COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' THEN combined_data.points_used ELSE 0 END), 0)::INTEGER as total_points_used,
        debug_msg as debug_info
    FROM (
        SELECT
            'transactions' as source,
            t.transaction_type,
            t.amount,
            t.points_earned,
            t.points_used,
            t.created_at,
            COALESCE(r.price, 0) as reward_price
        FROM transactions t
        LEFT JOIN rewards r ON t.reward_id = r.id
        WHERE t.store_id = store_id_param
        AND (
            start_date_param IS NULL OR 
            t.created_at >= to_timestamp(start_date_param / 1000)
        )
        AND (
            end_date_param IS NULL OR 
            t.created_at <= to_timestamp(end_date_param / 1000)
        )

        UNION ALL

        SELECT
            'points_transactions' as source,
            pt.transaction_type,
            0 as amount,
            pt.points_awarded as points_earned,
            0 as points_used,
            pt.created_at,
            0 as reward_price
        FROM points_transactions pt
        WHERE pt.store_id = store_id_param
        AND (
            start_date_param IS NULL OR 
            pt.created_at >= to_timestamp(start_date_param / 1000)
        )
        AND (
            end_date_param IS NULL OR 
            pt.created_at <= to_timestamp(end_date_param / 1000)
        )
    ) combined_data;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Test the function with different date ranges
SELECT 'Testing debug function with different date ranges:' as info;

-- Test with current date range (should show all data)
SELECT 'Current week (all data):' as test_case, * FROM debug_transaction_stats_with_date_range(
    'your-store-id'::UUID, 
    NULL, 
    NULL
);

-- Test with 1 week ago
SELECT '1 week ago:' as test_case, * FROM debug_transaction_stats_with_date_range(
    'your-store-id'::UUID, 
    EXTRACT(EPOCH FROM NOW() - INTERVAL '7 days') * 1000, 
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Test with 2 weeks ago
SELECT '2 weeks ago:' as test_case, * FROM debug_transaction_stats_with_date_range(
    'your-store-id'::UUID, 
    EXTRACT(EPOCH FROM NOW() - INTERVAL '14 days') * 1000, 
    EXTRACT(EPOCH FROM NOW() - INTERVAL '7 days') * 1000
);

-- Show all transactions with their dates for reference
SELECT 'All transactions with dates:' as info;
SELECT 
    'transactions' as source,
    created_at,
    transaction_type,
    amount
FROM transactions 
WHERE store_id = 'your-store-id'::UUID
ORDER BY created_at DESC
LIMIT 10;

SELECT 
    'points_transactions' as source,
    created_at,
    transaction_type,
    points_awarded
FROM points_transactions 
WHERE store_id = 'your-store-id'::UUID
ORDER BY created_at DESC
LIMIT 10; 