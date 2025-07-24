-- Test Points Transactions After Fix
-- Run this after the quick fix script

-- 1. Insert a test transaction
INSERT INTO points_transactions (
    customer_id, 
    store_id, 
    nfc_card_id, 
    points_awarded, 
    previous_points, 
    new_points, 
    transaction_type, 
    description
) VALUES (
    'f60f2cb0-7f68-4f3a-af3e-2a227751819a', -- Jack dorsey's customer ID
    'a667e115-9e6a-4d39-9ae3-7ab98b63386c', -- Your store ID
    'TEST_CARD_456',
    1,
    16,
    17,
    'test_scan',
    'Test transaction after fix'
);

-- 2. Check if we can read it
SELECT 'Read test' as test, COUNT(*) as count 
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 3. Show all transactions for your store
SELECT 
    id,
    customer_id,
    store_id,
    points_awarded,
    previous_points,
    new_points,
    transaction_type,
    description,
    created_at
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'
ORDER BY created_at DESC;

-- 4. Test the exact query the app uses
SELECT 'App query exact test' as test, COUNT(*) as count 
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c' 
ORDER BY created_at DESC;

-- 5. Clean up test data
DELETE FROM points_transactions 
WHERE description = 'Test transaction after fix';

SELECT 'Test completed - points transactions should now work!' as status; 