-- Test the transaction stats function
-- Replace 'your-store-id-here' with an actual store ID from your database

-- First, let's check if the function exists
SELECT routine_name, routine_type 
FROM information_schema.routines 
WHERE routine_name = 'get_transaction_stats';

-- Test the function with a sample store ID
-- You'll need to replace this with an actual store ID from your stores table
SELECT * FROM get_transaction_stats('your-store-id-here');

-- Check if there are any transactions in the database
SELECT COUNT(*) as total_transactions FROM transactions;

-- Check if there are any stores
SELECT id, name FROM stores LIMIT 5; 