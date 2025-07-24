-- Analytics Summary Dashboard
-- This script shows all your analytics insights in one place

-- Set your store ID
DO $$
DECLARE
    store_id_param UUID := 'a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID;
BEGIN
    -- 1. Overall Transaction Stats
    RAISE NOTICE '=== OVERALL TRANSACTION STATS ===';
    RAISE NOTICE 'Total Purchases: $%', (SELECT total_purchases FROM get_transaction_stats(store_id_param));
    RAISE NOTICE 'Total Claimed: $%', (SELECT total_claimed FROM get_transaction_stats(store_id_param));
    RAISE NOTICE 'Total Transactions: %', (SELECT total_transactions FROM get_transaction_stats(store_id_param));
    RAISE NOTICE 'Total Points Earned: %', (SELECT total_points_earned FROM get_transaction_stats(store_id_param));
    RAISE NOTICE 'Total Points Used: %', (SELECT total_points_used FROM get_transaction_stats(store_id_param));
    
    -- 2. Top 5 Customers
    RAISE NOTICE '';
    RAISE NOTICE '=== TOP 5 CUSTOMERS BY POINTS EARNED ===';
    FOR customer_rec IN 
        SELECT * FROM get_top_customers(store_id_param, 5)
    LOOP
        RAISE NOTICE 'Customer: %, Points Earned: %, Points Used: %, Balance: %, Transactions: %', 
            customer_rec.customer_name, 
            customer_rec.total_points_earned, 
            customer_rec.total_points_used, 
            customer_rec.current_balance, 
            customer_rec.total_transactions;
    END LOOP;
    
    -- 3. Transaction Type Breakdown
    RAISE NOTICE '';
    RAISE NOTICE '=== TRANSACTION TYPE BREAKDOWN ===';
    FOR type_rec IN 
        SELECT * FROM get_transaction_type_analytics(store_id_param)
    LOOP
        RAISE NOTICE 'Type: %, Count: %, Points: %, Amount: $%', 
            type_rec.transaction_type, 
            type_rec.total_count, 
            type_rec.total_points, 
            type_rec.total_amount;
    END LOOP;
    
    -- 4. Last 7 Days Activity
    RAISE NOTICE '';
    RAISE NOTICE '=== LAST 7 DAYS ACTIVITY ===';
    FOR day_rec IN 
        SELECT * FROM get_daily_analytics(store_id_param, 7)
    LOOP
        RAISE NOTICE 'Date: %, Transactions: %, Points Earned: %, Points Used: %, Unique Customers: %', 
            day_rec.activity_date, 
            day_rec.total_transactions, 
            day_rec.total_points_earned, 
            day_rec.total_points_used, 
            day_rec.unique_customers;
    END LOOP;
END $$;

-- Also show the data in table format for easy reading
SELECT '=== OVERALL STATS ===' as section;
SELECT * FROM get_transaction_stats('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID);

SELECT '=== TOP CUSTOMERS ===' as section;
SELECT * FROM get_top_customers('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID, 5);

SELECT '=== TRANSACTION TYPES ===' as section;
SELECT * FROM get_transaction_type_analytics('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID);

SELECT '=== LAST 7 DAYS ===' as section;
SELECT * FROM get_daily_analytics('a54087fb-ba72-4fc4-baa7-2963ad8c9c05'::UUID, 7); 