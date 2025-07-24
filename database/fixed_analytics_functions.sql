-- Fixed Analytics Functions
-- These functions resolve the column reference issues

-- 1. Fixed Customer Analytics Function
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
        COALESCE(SUM(combined_data.points_earned), 0)::INTEGER as total_points_earned,
        COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' THEN combined_data.points_used ELSE 0 END), 0)::INTEGER as total_points_used,
        COUNT(*)::INTEGER as total_transactions,
        MAX(combined_data.created_at) as last_activity
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

-- 2. Fixed Daily Analytics Function
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
        DATE(combined_data.created_at) as activity_date,
        COUNT(*)::INTEGER as total_transactions,
        COALESCE(SUM(combined_data.points_earned), 0)::INTEGER as total_points_earned,
        COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' THEN combined_data.points_used ELSE 0 END), 0)::INTEGER as total_points_used,
        COUNT(DISTINCT combined_data.customer_id)::INTEGER as unique_customers
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
    GROUP BY DATE(combined_data.created_at)
    ORDER BY activity_date DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Fixed Transaction Type Analytics Function
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
        combined_data.transaction_type,
        COUNT(*)::INTEGER as total_count,
        COALESCE(SUM(combined_data.points_earned), 0)::INTEGER as total_points,
        COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' THEN combined_data.amount ELSE 0 END), 0) as total_amount
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
    GROUP BY combined_data.transaction_type
    ORDER BY total_count DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 4. Fixed Top Customers Function
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
        COALESCE(SUM(combined_data.points_earned), 0)::INTEGER as total_points_earned,
        COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' THEN combined_data.points_used ELSE 0 END), 0)::INTEGER as total_points_used,
        (COALESCE(SUM(combined_data.points_earned), 0) - COALESCE(SUM(CASE WHEN combined_data.source = 'transactions' THEN combined_data.points_used ELSE 0 END), 0))::INTEGER as current_balance,
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