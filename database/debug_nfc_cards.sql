-- Debug NFC Cards Table
-- Run this in Supabase SQL Editor to diagnose issues

-- 1. Check if table exists and its structure
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'nfc_cards' 
ORDER BY ordinal_position;

-- 2. Check RLS policies
SELECT 
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual,
    with_check
FROM pg_policies 
WHERE tablename = 'nfc_cards';

-- 3. Check if RLS is enabled
SELECT 
    schemaname,
    tablename,
    rowsecurity
FROM pg_tables 
WHERE tablename = 'nfc_cards';

-- 4. Check current data in table
SELECT COUNT(*) as total_cards FROM nfc_cards;

-- 5. Check for any constraints
SELECT 
    conname as constraint_name,
    contype as constraint_type,
    pg_get_constraintdef(oid) as constraint_definition
FROM pg_constraint 
WHERE conrelid = 'nfc_cards'::regclass;

-- 6. Test insert with explicit values
INSERT INTO nfc_cards (
    id,
    card_id,
    member_id,
    customer_id,
    store_id,
    is_active,
    created_at,
    updated_at
) VALUES (
    gen_random_uuid(),
    'TEST_CARD_001',
    '1234567890',
    'f60f2cb0-7f68-4f3a-af3e-2a227751819a',
    'a667e115-9e6a-4d39-9ae3-7ab98b63386c',
    true,
    NOW(),
    NOW()
) ON CONFLICT (card_id, store_id) DO NOTHING;

-- 7. Verify the insert worked
SELECT 
    id,
    card_id,
    member_id,
    customer_id,
    store_id,
    is_active,
    created_at,
    updated_at
FROM nfc_cards 
WHERE card_id = 'TEST_CARD_001';

-- 8. Test the policy by trying to select
SELECT 
    id,
    card_id,
    member_id,
    customer_id,
    store_id,
    is_active,
    created_at,
    updated_at
FROM nfc_cards 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 9. Clean up test data
DELETE FROM nfc_cards WHERE card_id = 'TEST_CARD_001'; 