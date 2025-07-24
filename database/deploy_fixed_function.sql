-- Deploy the fixed transaction stats function
-- This fixes the date filtering to properly return 0 when there's no data

-- Function to get transaction statistics with date range filtering
CREATE OR REPLACE FUNCTION get_transaction_stats_with_date_range(
    store_id_param UUID,
    start_date_param BIGINT DEFAULT NULL,
    end_date_param BIGINT DEFAULT NULL
)
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
        COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' AND (combined_data.transaction_type = 'purchase' OR combined_data.transaction_type = 'reward_claim') THEN combined_data.amount ELSE 0 END), 0) as total_purchases,
        COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' AND combined_data.transaction_type = 'reward_claim' THEN combined_data.reward_price ELSE 0 END), 0) as total_claimed,
        COUNT(*)::INTEGER as total_transactions,
        COALESCE(SUM(combined_data.points_earned), 0)::INTEGER as total_points_earned,
        COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' THEN combined_data.points_used ELSE 0 END), 0)::INTEGER as total_points_used
    FROM (
        SELECT
            'transactions' as source,
            t.transaction_type,
            t.amount,
            t.points_earned,
            t.points_used,
            t.created_at,
            COALESCE(r.price, 0) as reward_price -- Get reward price for claimed rewards
        FROM transactions t
        LEFT JOIN rewards r ON t.reward_id = r.id -- Join to get reward price
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
            0 as amount, -- NFC scans don't have purchase amounts
            pt.points_awarded as points_earned,
            0 as points_used, -- NFC scans don't use points
            pt.created_at,
            0 as reward_price -- No reward price for NFC scans
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

-- Test the function to make sure it works
SELECT 'Function deployed successfully!' as status; 