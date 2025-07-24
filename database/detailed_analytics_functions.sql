-- Detailed Analytics Functions
-- These functions provide deeper insights into your transaction data

-- 1. Function to get customer breakdown
CREATE OR REPLACE FUNCTION get_customer_analytics(store_id_param UUID)
RETURNS TABLE (
    customer_id UUID,
    customer_name TEXT,
    total_points_earned INTEGER,
    total_points_used INTEGER,
    total_transactions INTEGER,
    last_activity TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.id as customer_id,
        c.name as customer_name,
        COALESCE(SUM(
            CASE 
                WHEN source = 'transactions' THEN points_earned
                WHEN source = 'points_transactions' THEN points_awarded
                ELSE 0
            END
        ), 0)::INTEGER as total_points_earned,
        COALESCE(SUM(CASE WHEN source = 'transactions' THEN points_used ELSE 0 END), 0)::INTEGER as total_points_used,
        COUNT(*)::INTEGER as total_transactions,
        MAX(created_at) as last_activity
    FROM (
        SELECT 
            'transactions' as source,
            customer_id,
            points_earned,
            points_used,
            created_at
        FROM transactions 
        WHERE store_id = store_id_param
        
        UNION ALL
        
        SELECT 
            'points_transactions' as source,
            customer_id,
            points_awarded as points_earned,
            0 as points_used,
            created_at
        FROM points_transactions 
        WHERE store_id = store_id_param
    ) combined_data
    JOIN customers c ON combined_data.customer_id = c.id
    GROUP BY c.id, c.name
    ORDER BY total_points_earned DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Function to get daily activity summary
CREATE OR REPLACE FUNCTION get_daily_analytics(store_id_param UUID, days_back INTEGER DEFAULT 30)
RETURNS TABLE (
    activity_date DATE,
    total_transactions INTEGER,
    total_points_earned INTEGER,
    total_points_used INTEGER,
    unique_customers INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        DATE(created_at) as activity_date,
        COUNT(*)::INTEGER as total_transactions,
        COALESCE(SUM(
            CASE 
                WHEN source = 'transactions' THEN points_earned
                WHEN source = 'points_transactions' THEN points_awarded
                ELSE 0
            END
        ), 0)::INTEGER as total_points_earned,
        COALESCE(SUM(CASE WHEN source = 'transactions' THEN points_used ELSE 0 END), 0)::INTEGER as total_points_used,
        COUNT(DISTINCT customer_id)::INTEGER as unique_customers
    FROM (
        SELECT 
            'transactions' as source,
            customer_id,
            points_earned,
            points_used,
            created_at
        FROM transactions 
        WHERE store_id = store_id_param
        AND created_at >= CURRENT_DATE - INTERVAL '1 day' * days_back
        
        UNION ALL
        
        SELECT 
            'points_transactions' as source,
            customer_id,
            points_awarded as points_earned,
            0 as points_used,
            created_at
        FROM points_transactions 
        WHERE store_id = store_id_param
        AND created_at >= CURRENT_DATE - INTERVAL '1 day' * days_back
    ) combined_data
    GROUP BY DATE(created_at)
    ORDER BY activity_date DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Function to get transaction type breakdown
CREATE OR REPLACE FUNCTION get_transaction_type_analytics(store_id_param UUID)
RETURNS TABLE (
    transaction_type TEXT,
    total_count INTEGER,
    total_points INTEGER,
    total_amount DECIMAL(10,2)
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        transaction_type,
        COUNT(*)::INTEGER as total_count,
        COALESCE(SUM(
            CASE 
                WHEN source = 'transactions' THEN points_earned
                WHEN source = 'points_transactions' THEN points_awarded
                ELSE 0
            END
        ), 0)::INTEGER as total_points,
        COALESCE(SUM(CASE WHEN source = 'transactions' THEN amount ELSE 0 END), 0) as total_amount
    FROM (
        SELECT 
            'transactions' as source,
            transaction_type,
            amount,
            points_earned,
            created_at
        FROM transactions 
        WHERE store_id = store_id_param
        
        UNION ALL
        
        SELECT 
            'points_transactions' as source,
            transaction_type,
            0 as amount,
            points_awarded as points_earned,
            created_at
        FROM points_transactions 
        WHERE store_id = store_id_param
    ) combined_data
    GROUP BY transaction_type
    ORDER BY total_count DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Function to get top customers by points
CREATE OR REPLACE FUNCTION get_top_customers(store_id_param UUID, limit_count INTEGER DEFAULT 10)
RETURNS TABLE (
    customer_name TEXT,
    total_points_earned INTEGER,
    total_points_used INTEGER,
    current_balance INTEGER,
    total_transactions INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.name as customer_name,
        COALESCE(SUM(
            CASE 
                WHEN source = 'transactions' THEN points_earned
                WHEN source = 'points_transactions' THEN points_awarded
                ELSE 0
            END
        ), 0)::INTEGER as total_points_earned,
        COALESCE(SUM(CASE WHEN source = 'transactions' THEN points_used ELSE 0 END), 0)::INTEGER as total_points_used,
        (COALESCE(SUM(
            CASE 
                WHEN source = 'transactions' THEN points_earned
                WHEN source = 'points_transactions' THEN points_awarded
                ELSE 0
            END
        ), 0) - COALESCE(SUM(CASE WHEN source = 'transactions' THEN points_used ELSE 0 END), 0))::INTEGER as current_balance,
        COUNT(*)::INTEGER as total_transactions
    FROM (
        SELECT 
            'transactions' as source,
            customer_id,
            points_earned,
            points_used,
            created_at
        FROM transactions 
        WHERE store_id = store_id_param
        
        UNION ALL
        
        SELECT 
            'points_transactions' as source,
            customer_id,
            points_awarded as points_earned,
            0 as points_used,
            created_at
        FROM points_transactions 
        WHERE store_id = store_id_param
    ) combined_data
    JOIN customers c ON combined_data.customer_id = c.id
    GROUP BY c.id, c.name
    ORDER BY total_points_earned DESC
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant permissions
GRANT EXECUTE ON FUNCTION get_customer_analytics(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION get_daily_analytics(UUID, INTEGER) TO authenticated;
GRANT EXECUTE ON FUNCTION get_transaction_type_analytics(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION get_top_customers(UUID, INTEGER) TO authenticated;

-- Test the functions
SELECT 'Testing customer analytics' as info;
SELECT * FROM get_customer_analytics('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID);

SELECT 'Testing daily analytics (last 7 days)' as info;
SELECT * FROM get_daily_analytics('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID, 7);

SELECT 'Testing transaction type analytics' as info;
SELECT * FROM get_transaction_type_analytics('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID);

SELECT 'Testing top customers' as info;
SELECT * FROM get_top_customers('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID, 5); 