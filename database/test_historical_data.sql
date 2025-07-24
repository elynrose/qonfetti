-- Test Historical Data for Week-based Filtering
-- This script adds sample transaction data across different dates to test the analytics filtering

-- First, let's see what store IDs exist
SELECT 'Available stores:' as info;
SELECT id, name FROM stores LIMIT 5;

-- Insert test transactions across different dates
-- We'll use a sample store ID - replace 'your-store-id' with an actual store ID from your database

-- Transactions from 1 week ago (7 days back)
INSERT INTO transactions (store_id, customer_id, amount, transaction_type, created_at)
SELECT 
    s.id as store_id,
    c.id as customer_id,
    25.00 as amount,
    'purchase' as transaction_type,
    NOW() - INTERVAL '7 days' as created_at
FROM stores s, customers c 
WHERE s.id = c.store_id 
LIMIT 3;

-- Transactions from 2 weeks ago (14 days back)
INSERT INTO transactions (store_id, customer_id, amount, transaction_type, created_at)
SELECT 
    s.id as store_id,
    c.id as customer_id,
    35.00 as amount,
    'purchase' as transaction_type,
    NOW() - INTERVAL '14 days' as created_at
FROM stores s, customers c 
WHERE s.id = c.store_id 
LIMIT 2;

-- Transactions from 3 weeks ago (21 days back)
INSERT INTO transactions (store_id, customer_id, amount, transaction_type, created_at)
SELECT 
    s.id as store_id,
    c.id as customer_id,
    45.00 as amount,
    'purchase' as transaction_type,
    NOW() - INTERVAL '21 days' as created_at
FROM stores s, customers c 
WHERE s.id = c.store_id 
LIMIT 2;

-- Reward claims from different dates
INSERT INTO transactions (store_id, customer_id, amount, transaction_type, created_at)
SELECT 
    s.id as store_id,
    c.id as customer_id,
    15.00 as amount,
    'reward_claim' as transaction_type,
    NOW() - INTERVAL '5 days' as created_at
FROM stores s, customers c 
WHERE s.id = c.store_id 
LIMIT 2;

INSERT INTO transactions (store_id, customer_id, amount, transaction_type, created_at)
SELECT 
    s.id as store_id,
    c.id as customer_id,
    20.00 as amount,
    'reward_claim' as transaction_type,
    NOW() - INTERVAL '12 days' as created_at
FROM stores s, customers c 
WHERE s.id = c.store_id 
LIMIT 1;

-- NFC points transactions from different dates
INSERT INTO points_transactions (store_id, customer_id, nfc_card_id, points_awarded, points_used, transaction_type, created_at)
SELECT 
    s.id as store_id,
    c.id as customer_id,
    nc.id as nfc_card_id,
    5 as points_awarded,
    0 as points_used,
    'scan' as transaction_type,
    NOW() - INTERVAL '3 days' as created_at
FROM stores s, customers c, nfc_cards nc
WHERE s.id = c.store_id AND c.id = nc.customer_id
LIMIT 3;

INSERT INTO points_transactions (store_id, customer_id, nfc_card_id, points_awarded, points_used, transaction_type, created_at)
SELECT 
    s.id as store_id,
    c.id as customer_id,
    nc.id as nfc_card_id,
    3 as points_awarded,
    0 as points_used,
    'scan' as transaction_type,
    NOW() - INTERVAL '10 days' as created_at
FROM stores s, customers c, nfc_cards nc
WHERE s.id = c.store_id AND c.id = nc.customer_id
LIMIT 2;

-- Show the data distribution after insertion
SELECT 'Transaction date distribution:' as info;
SELECT 
    DATE(created_at) as transaction_date,
    COUNT(*) as count,
    SUM(CASE WHEN transaction_type = 'purchase' THEN amount ELSE 0 END) as total_purchases,
    SUM(CASE WHEN transaction_type = 'reward_claim' THEN amount ELSE 0 END) as total_claims
FROM (
    SELECT created_at, transaction_type, amount FROM transactions
    UNION ALL
    SELECT created_at, 'nfc_scan' as transaction_type, 0 as amount FROM points_transactions
) t 
GROUP BY DATE(created_at) 
ORDER BY transaction_date DESC 
LIMIT 10; 