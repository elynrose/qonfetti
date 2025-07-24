-- Fix for Points Transactions SQL Error
-- This script fixes the GROUP BY clause error

-- 1. Check if there are any views that might be causing the issue
SELECT 'Checking for problematic views' as step;

-- 2. Drop any existing problematic views
DROP VIEW IF EXISTS points_transactions_view;

-- 3. Create a new, simpler view without GROUP BY issues
CREATE VIEW points_transactions_view AS
SELECT 
    pt.id,
    pt.customer_id,
    pt.store_id,
    pt.nfc_card_id,
    pt.points_awarded,
    pt.previous_points,
    pt.new_points,
    pt.transaction_type,
    pt.description,
    pt.created_at,
    c.name as customer_name,
    c.email as customer_email,
    c.member_id as customer_member_id
FROM points_transactions pt
LEFT JOIN customers c ON pt.customer_id = c.id;

-- 4. Grant permissions on the new view
GRANT SELECT ON points_transactions_view TO authenticated;

-- 5. Test the view works
SELECT 'Testing view' as step, COUNT(*) as count FROM points_transactions_view;

-- 6. Show recent transactions from the view
SELECT 
    id,
    customer_name,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions_view 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'
ORDER BY created_at DESC 
LIMIT 10;

-- 7. Test the direct table query (what the app uses)
SELECT 'Testing direct table query' as step, COUNT(*) as count 
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 8. Show recent transactions from direct table
SELECT 
    id,
    customer_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'
ORDER BY created_at DESC 
LIMIT 10; 