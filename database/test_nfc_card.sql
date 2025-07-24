-- Test NFC Card Insertion Script
-- This script tests the NFC cards functionality

-- First, let's see what customers and stores we have
SELECT 'Available customers:' as info;
SELECT id, name, member_id FROM customers LIMIT 5;

SELECT 'Available stores:' as info;
SELECT id, name, owner_id FROM stores LIMIT 5;

-- Let's insert a test NFC card
-- Replace the UUIDs below with actual customer_id and store_id from your database
INSERT INTO nfc_cards (card_id, member_id, customer_id, store_id, is_active)
VALUES (
    'test_card_001', 
    'test_member_001',
    (SELECT id FROM customers LIMIT 1),  -- Use first available customer
    (SELECT id FROM stores LIMIT 1),     -- Use first available store
    true
)
ON CONFLICT (card_id) DO NOTHING;

-- Check if the card was inserted
SELECT 'Test NFC card inserted successfully!' as status;

-- Show all NFC cards
SELECT 
    card_id,
    member_id,
    customer_id,
    store_id,
    is_active,
    created_at
FROM nfc_cards;

-- Test query that the app uses
SELECT 
    card_id,
    member_id,
    customer_id,
    store_id,
    is_active
FROM nfc_cards 
WHERE customer_id = (SELECT id FROM customers LIMIT 1)
AND is_active = true; 