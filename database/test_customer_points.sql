-- Test script to check customer_points table and constraints
-- Run this in your Supabase SQL editor

-- Check table structure
SELECT 'Table structure:' as info;
\d customer_points;

-- Check current data
SELECT 'Current customer_points data:' as info;
SELECT COUNT(*) as total_records FROM customer_points;

-- Check RLS policies
SELECT 'RLS policies:' as info;
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual 
FROM pg_policies 
WHERE tablename = 'customer_points';

-- Test inserting a customer_points record
-- Replace with actual customer_id and store_id from your database
SELECT 'Testing customer_points insertion:' as info;

-- First, let's see what customers and stores we have
SELECT 'Available customers:' as info;
SELECT id, name, email FROM customers LIMIT 5;

SELECT 'Available stores:' as info;
SELECT id, name, owner_id FROM stores LIMIT 5;

-- Test insertion with a sample customer and store
-- You'll need to replace these IDs with actual ones from your database
INSERT INTO customer_points (
    customer_id,
    store_id,
    points
) VALUES (
    '6b52c88c-ad80-4702-8dec-3f08971d6736', -- Use the customer ID from the logs
    'a667e115-9e6a-4d39-9ae3-7ab98b63386c', -- Use the store ID from the logs
    0
) ON CONFLICT (customer_id, store_id) DO UPDATE SET
    points = EXCLUDED.points,
    updated_at = NOW();

-- Check if the insertion worked
SELECT 'After insertion test:' as info;
SELECT COUNT(*) as total_records FROM customer_points;

-- Check the specific record we just inserted
SELECT 'Inserted record:' as info;
SELECT * FROM customer_points 
WHERE customer_id = '6b52c88c-ad80-4702-8dec-3f08971d6736' 
AND store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'; 