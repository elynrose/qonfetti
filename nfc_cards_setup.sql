-- NFC Cards Table Setup for Qonfetty App
-- This script creates the nfc_cards table and related policies

-- Create nfc_cards table
CREATE TABLE IF NOT EXISTS nfc_cards (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    card_id TEXT NOT NULL UNIQUE, -- The actual NFC card UID
    member_id TEXT NOT NULL, -- Links to customer's member_id
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_nfc_cards_card_id ON nfc_cards(card_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_member_id ON nfc_cards(member_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_customer_id ON nfc_cards(customer_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_store_id ON nfc_cards(store_id);
CREATE INDEX IF NOT EXISTS idx_nfc_cards_active ON nfc_cards(is_active);

-- Create unique constraint to prevent duplicate card registrations
CREATE UNIQUE INDEX IF NOT EXISTS idx_nfc_cards_unique_active ON nfc_cards(card_id) WHERE is_active = true;

-- Enable Row Level Security
ALTER TABLE nfc_cards ENABLE ROW LEVEL SECURITY;

-- Policy: Store owners can view NFC cards registered at their stores
CREATE POLICY "Store owners can view NFC cards at their stores" ON nfc_cards
    FOR SELECT USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Policy: Store owners can register NFC cards at their stores
CREATE POLICY "Store owners can register NFC cards at their stores" ON nfc_cards
    FOR INSERT WITH CHECK (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Policy: Store owners can update NFC cards at their stores
CREATE POLICY "Store owners can update NFC cards at their stores" ON nfc_cards
    FOR UPDATE USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Policy: Store owners can delete NFC cards at their stores
CREATE POLICY "Store owners can delete NFC cards at their stores" ON nfc_cards
    FOR DELETE USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_nfc_cards_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update updated_at
CREATE TRIGGER update_nfc_cards_updated_at
    BEFORE UPDATE ON nfc_cards
    FOR EACH ROW
    EXECUTE FUNCTION update_nfc_cards_updated_at();

-- Function to validate NFC card registration
CREATE OR REPLACE FUNCTION validate_nfc_card_registration()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if customer exists and belongs to the store
    IF NOT EXISTS (
        SELECT 1 FROM customers c
        JOIN customer_points cp ON c.id = cp.customer_id
        WHERE c.id = NEW.customer_id 
        AND cp.store_id = NEW.store_id
    ) THEN
        RAISE EXCEPTION 'Customer does not exist or is not associated with this store';
    END IF;
    
    -- Check if member_id matches the customer
    IF NOT EXISTS (
        SELECT 1 FROM customers 
        WHERE id = NEW.customer_id 
        AND member_id = NEW.member_id
    ) THEN
        RAISE EXCEPTION 'Member ID does not match the customer';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to validate NFC card registration
CREATE TRIGGER validate_nfc_card_registration
    BEFORE INSERT OR UPDATE ON nfc_cards
    FOR EACH ROW
    EXECUTE FUNCTION validate_nfc_card_registration();

-- Grant necessary permissions
GRANT ALL ON nfc_cards TO authenticated;

-- Insert sample data (optional - for testing)
-- INSERT INTO nfc_cards (card_id, member_id, customer_id, store_id) VALUES
-- ('sample_card_001', 'sample_member_001', 'customer_uuid_here', 'store_uuid_here');

COMMENT ON TABLE nfc_cards IS 'Stores NFC card registrations for customers across stores';
COMMENT ON COLUMN nfc_cards.card_id IS 'The unique identifier of the NFC card (UID)';
COMMENT ON COLUMN nfc_cards.member_id IS 'The global member ID of the customer';
COMMENT ON COLUMN nfc_cards.customer_id IS 'Reference to the customer record';
COMMENT ON COLUMN nfc_cards.store_id IS 'The store where the card was registered';
COMMENT ON COLUMN nfc_cards.is_active IS 'Whether the NFC card is currently active'; 