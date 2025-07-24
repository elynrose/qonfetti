-- Check the actual structure of points_transactions table
-- This will help us understand what columns are available

-- 1. Check table structure
SELECT 'Table structure' as step;
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'points_transactions' 
ORDER BY ordinal_position;

-- 2. Show sample data to understand the structure
SELECT 'Sample data' as step;
SELECT * FROM points_transactions LIMIT 3;

-- 3. Check if the table exists
SELECT 'Table existence' as step;
SELECT tablename FROM pg_tables WHERE tablename = 'points_transactions';

-- 4. Check for any similar columns
SELECT 'Similar columns' as step;
SELECT column_name 
FROM information_schema.columns 
WHERE table_name = 'points_transactions' 
AND column_name LIKE '%point%'; 