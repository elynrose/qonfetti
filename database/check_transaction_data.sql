-- Check Transaction Data in Database
-- This script will help identify why app-created transactions aren't showing up

-- 1. Temporarily disable RLS to see all data
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;

-- 2. Check total count and show all transactions
SELECT 'Total transactions' as step, COUNT(*) as count FROM points_transactions;

-- 3. Show all transactions with full details
SELECT 
    id,
    customer_id,
    store_id,
    nfc_card_id,
    points_awarded,
    previous_points,
    new_points,
    transaction_type,
    description,
    created_at,
    CASE 
        WHEN transaction_type = 'test_insert' THEN 'MANUAL SQL'
        WHEN transaction_type = 'nfc_scan' THEN 'APP CREATED'
        ELSE 'OTHER'
    END as source
FROM points_transactions 
ORDER BY created_at DESC;

-- 4. Check for transactions with the specific customer
SELECT 'Customer transactions' as step, COUNT(*) as count 
FROM points_transactions 
WHERE customer_id = 'f60f2cb0-7f68-4f3a-af3e-2a227751819a';

-- 5. Show transactions for the specific customer
SELECT 
    id,
    store_id,
    points_awarded,
    transaction_type,
    created_at,
    description
FROM points_transactions 
WHERE customer_id = 'f60f2cb0-7f68-4f3a-af3e-2a227751819a'
ORDER BY created_at DESC;

-- 6. Check for transactions with the specific store
SELECT 'Store transactions' as step, COUNT(*) as count 
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 7. Show transactions for the specific store
SELECT 
    id,
    customer_id,
    points_awarded,
    transaction_type,
    created_at,
    description
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'
ORDER BY created_at DESC;

-- 8. Check for any transactions created today
SELECT 'Today transactions' as step, COUNT(*) as count 
FROM points_transactions 
WHERE DATE(created_at) = CURRENT_DATE;

-- 9. Show today's transactions
SELECT 
    id,
    customer_id,
    store_id,
    points_awarded,
    transaction_type,
    created_at
FROM points_transactions 
WHERE DATE(created_at) = CURRENT_DATE
ORDER BY created_at DESC;

-- 10. Check for any transactions with nfc_scan type
SELECT 'NFC scan transactions' as step, COUNT(*) as count 
FROM points_transactions 
WHERE transaction_type = 'nfc_scan';

-- 11. Show NFC scan transactions
SELECT 
    id,
    customer_id,
    store_id,
    points_awarded,
    created_at
FROM points_transactions 
WHERE transaction_type = 'nfc_scan'
ORDER BY created_at DESC;

-- 12. Re-enable RLS
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY; 