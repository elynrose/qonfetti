-- Simple test function to verify date filtering
CREATE OR REPLACE FUNCTION test_date_filtering(
    store_id_param UUID,
    start_date_param BIGINT DEFAULT NULL,
    end_date_param BIGINT DEFAULT NULL
)
RETURNS TABLE (
    test_date TIMESTAMP,
    transaction_count INTEGER,
    debug_info TEXT
) AS $$
DECLARE
    start_date_timestamp TIMESTAMP;
    end_date_timestamp TIMESTAMP;
BEGIN
    -- Convert timestamps
    IF start_date_param IS NOT NULL THEN
        start_date_timestamp := to_timestamp(start_date_param / 1000);
    END IF;
    
    IF end_date_param IS NOT NULL THEN
        end_date_timestamp := to_timestamp(end_date_param / 1000);
    END IF;
    
    RETURN QUERY
    SELECT 
        NOW() as test_date,
        COUNT(*)::INTEGER as transaction_count,
        'Store: ' || store_id_param || 
        ', Start: ' || COALESCE(start_date_timestamp::TEXT, 'NULL') ||
        ', End: ' || COALESCE(end_date_timestamp::TEXT, 'NULL') as debug_info
    FROM (
        SELECT created_at FROM transactions 
        WHERE store_id = store_id_param
        AND (
            start_date_param IS NULL OR 
            created_at >= to_timestamp(start_date_param / 1000)
        )
        AND (
            end_date_param IS NULL OR 
            created_at <= to_timestamp(end_date_param / 1000)
        )
        
        UNION ALL
        
        SELECT created_at FROM points_transactions 
        WHERE store_id = store_id_param
        AND (
            start_date_param IS NULL OR 
            created_at >= to_timestamp(start_date_param / 1000)
        )
        AND (
            end_date_param IS NULL OR 
            created_at <= to_timestamp(end_date_param / 1000)
        )
    ) all_transactions;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Test with different date ranges
SELECT 'Testing date filtering with different ranges:' as info;

-- Test 1: All data (no date filter)
SELECT 'All data:' as test_case, * FROM test_date_filtering(
    'your-store-id'::UUID, 
    NULL, 
    NULL
);

-- Test 2: Last 7 days
SELECT 'Last 7 days:' as test_case, * FROM test_date_filtering(
    'your-store-id'::UUID, 
    EXTRACT(EPOCH FROM NOW() - INTERVAL '7 days') * 1000, 
    EXTRACT(EPOCH FROM NOW()) * 1000
);

-- Test 3: 7-14 days ago
SELECT '7-14 days ago:' as test_case, * FROM test_date_filtering(
    'your-store-id'::UUID, 
    EXTRACT(EPOCH FROM NOW() - INTERVAL '14 days') * 1000, 
    EXTRACT(EPOCH FROM NOW() - INTERVAL '7 days') * 1000
);

-- Show all transaction dates for reference
SELECT 'All transaction dates:' as info;
SELECT 
    'transactions' as source,
    created_at,
    DATE(created_at) as date_only
FROM transactions 
WHERE store_id = 'your-store-id'::UUID
ORDER BY created_at DESC
LIMIT 5;

SELECT 
    'points_transactions' as source,
    created_at,
    DATE(created_at) as date_only
FROM points_transactions 
WHERE store_id = 'your-store-id'::UUID
ORDER BY created_at DESC
LIMIT 5; 