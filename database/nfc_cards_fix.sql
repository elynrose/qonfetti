-- NFC Cards Table Fix Script
-- This script checks and fixes the existing nfc_cards table

-- First, let's check what exists
SELECT 'Checking existing table structure...' as status;

-- Check if table exists and show its structure
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_name = 'nfc_cards'
ORDER BY ordinal_position;

-- Check existing policies
SELECT schemaname, tablename, policyname, permissive, roles, cmd, qual, with_check
FROM pg_policies
WHERE tablename = 'nfc_cards';

-- Check existing indexes
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'nfc_cards';

-- Now let's fix any missing pieces

-- Drop existing policies to recreate them properly
DROP POLICY IF EXISTS "Store owners can view NFC cards at their stores" ON nfc_cards;
DROP POLICY IF EXISTS "Store owners can register NFC cards at their stores" ON nfc_cards;
DROP POLICY IF EXISTS "Store owners can update NFC cards at their stores" ON nfc_cards;
DROP POLICY IF EXISTS "Store owners can delete NFC cards at their stores" ON nfc_cards;

-- Recreate policies
CREATE POLICY "Store owners can view NFC cards at their stores" ON nfc_cards
    FOR SELECT USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can register NFC cards at their stores" ON nfc_cards
    FOR INSERT WITH CHECK (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can update NFC cards at their stores" ON nfc_cards
    FOR UPDATE USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

CREATE POLICY "Store owners can delete NFC cards at their stores" ON nfc_cards
    FOR DELETE USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Ensure RLS is enabled
ALTER TABLE nfc_cards ENABLE ROW LEVEL SECURITY;

-- Create missing indexes if they don't exist
CREATE INDEX IF NOT EXISTS idx_nfc_cards_card_id ON nfc_cards(card_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_member_id ON nfc_cards(member_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_customer_id ON nfc_cards(customer_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_store_id ON nfc_cards(store_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_active ON nfc_cards(is_active);

-- Create unique constraint if it doesn't exist
CREATE UNIQUE INDEX IF NOT EXISTS idx_nfc_cards_unique_active ON nfc_cards(card_id) WHERE is_active = true;

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_nfc_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop and recreate trigger
DROP TRIGGER IF EXISTS update_nfc_cards_updated_at ON nfc_cards;
CREATE TRIGGER update_nfc_cards_updated_at
    BEFORE UPDATE ON nfc_cards
    FOR EACH ROW
    EXECUTE FUNCTION update_nfc_cards_updated_at();

-- Grant necessary permissions
GRANT ALL ON nfc_cards TO authenticated;

-- Show final status
SELECT 'NFC cards table fixed successfully!' as status;

-- Show current table count
SELECT COUNT(*) as total_nfc_cards FROM nfc_cards; 