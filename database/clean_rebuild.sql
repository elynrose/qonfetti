-- CLEAN DATABASE REBUILD
-- This script will completely rebuild the database from scratch
-- Run this in Supabase SQL Editor

-- 1. Drop all existing tables (if they exist)
DROP TABLE IF EXISTS reward_claims CASCADE;
DROP TABLE IF EXISTS rewards CASCADE;
DROP TABLE IF EXISTS nfc_cards CASCADE;
DROP TABLE IF EXISTS customer_points CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS stores CASCADE;

-- 2. Create stores table
CREATE TABLE stores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. Create customers table
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    email TEXT,
    phone TEXT,
    member_id TEXT UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. Create customer_points table (links customers to stores with points)
CREATE TABLE customer_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    points INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(customer_id, store_id)
);

-- 5. Create nfc_cards table
CREATE TABLE nfc_cards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id TEXT UNIQUE NOT NULL,
    member_id TEXT NOT NULL,
    customer_id UUID REFERENCES customers(id) ON DELETE SET NULL,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. Create rewards table
CREATE TABLE rewards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT,
    points_required INTEGER NOT NULL,
    category TEXT DEFAULT 'general',
    is_shared BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 7. Create reward_claims table
CREATE TABLE reward_claims (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    reward_id UUID NOT NULL REFERENCES rewards(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    claimed_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    points_spent INTEGER NOT NULL
);

-- 8. Create indexes for better performance
CREATE INDEX idx_customers_member_id ON customers(member_id);
CREATE INDEX idx_customer_points_customer_store ON customer_points(customer_id, store_id);
CREATE INDEX idx_nfc_cards_card_id ON nfc_cards(card_id);
CREATE INDEX idx_nfc_cards_member_id ON nfc_cards(member_id);
CREATE INDEX idx_nfc_cards_customer_store ON nfc_cards(customer_id, store_id);
CREATE INDEX idx_rewards_store_active ON rewards(store_id, is_active);
CREATE INDEX idx_reward_claims_customer ON reward_claims(customer_id);

-- 9. Enable Row Level Security
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_points ENABLE ROW LEVEL SECURITY;
ALTER TABLE nfc_cards ENABLE ROW LEVEL SECURITY;
ALTER TABLE rewards ENABLE ROW LEVEL SECURITY;
ALTER TABLE reward_claims ENABLE ROW LEVEL SECURITY;

-- 10. Create simple RLS policies (allow all authenticated users)
-- Stores: Users can manage their own stores
CREATE POLICY "Users can manage their own stores" ON stores
    FOR ALL USING (owner_id = auth.uid());

-- Customers: All authenticated users can read/write
CREATE POLICY "Authenticated users can manage customers" ON customers
    FOR ALL USING (auth.role() = 'authenticated');

-- Customer points: Store owners can manage their customer points
CREATE POLICY "Store owners can manage customer points" ON customer_points
    FOR ALL USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- NFC cards: Store owners can manage their NFC cards
CREATE POLICY "Store owners can manage NFC cards" ON nfc_cards
    FOR ALL USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- Rewards: Store owners can manage their rewards
CREATE POLICY "Store owners can manage rewards" ON rewards
    FOR ALL USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- Reward claims: Store owners can manage their reward claims
CREATE POLICY "Store owners can manage reward claims" ON reward_claims
    FOR ALL USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 11. Create functions for common operations

-- Function to get or create a store for the current user
CREATE OR REPLACE FUNCTION get_or_create_store(store_name TEXT DEFAULT 'My Store')
RETURNS UUID AS $$
DECLARE
    store_id UUID;
BEGIN
    -- Try to get existing store
    SELECT id INTO store_id FROM stores WHERE owner_id = auth.uid() LIMIT 1;
    
    -- If no store exists, create one
    IF store_id IS NULL THEN
        INSERT INTO stores (name, owner_id) VALUES (store_name, auth.uid())
        RETURNING id INTO store_id;
    END IF;
    
    RETURN store_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to register NFC card
CREATE OR REPLACE FUNCTION register_nfc_card(
    p_card_id TEXT,
    p_member_id TEXT,
    p_customer_id UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
    v_store_id UUID;
    v_nfc_id UUID;
BEGIN
    -- Get or create store for current user
    v_store_id := get_or_create_store();
    
    -- Insert NFC card
    INSERT INTO nfc_cards (card_id, member_id, customer_id, store_id)
    VALUES (p_card_id, p_member_id, p_customer_id, v_store_id)
    ON CONFLICT (card_id) DO UPDATE SET
        member_id = EXCLUDED.member_id,
        customer_id = EXCLUDED.customer_id,
        store_id = EXCLUDED.store_id,
        updated_at = NOW()
    RETURNING id INTO v_nfc_id;
    
    RETURN v_nfc_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Function to award points to customer
CREATE OR REPLACE FUNCTION award_points_to_customer(
    p_customer_id UUID,
    p_points INTEGER,
    p_store_id UUID DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    v_store_id UUID;
    v_new_points INTEGER;
BEGIN
    -- Get store ID if not provided
    IF p_store_id IS NULL THEN
        v_store_id := get_or_create_store();
    ELSE
        v_store_id := p_store_id;
    END IF;
    
    -- Insert or update customer points
    INSERT INTO customer_points (customer_id, store_id, points)
    VALUES (p_customer_id, v_store_id, p_points)
    ON CONFLICT (customer_id, store_id) DO UPDATE SET
        points = customer_points.points + EXCLUDED.points,
        updated_at = NOW()
    RETURNING points INTO v_new_points;
    
    RETURN v_new_points;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 12. Create a default store for the current user
SELECT 'Creating default store for current user' as step;
SELECT get_or_create_store('My Store') as store_id;

-- 13. Insert some sample data for testing
INSERT INTO customers (name, email, phone, member_id) VALUES
    ('John Doe', 'john@example.com', '+1234567890', '1234567890'),
    ('Jane Smith', 'jane@example.com', '+0987654321', '0987654321'),
    ('Bob Wilson', 'bob@example.com', '+1122334455', '1122334455')
ON CONFLICT (member_id) DO NOTHING;

-- Insert sample rewards
INSERT INTO rewards (store_id, name, description, points_required, category) 
SELECT 
    get_or_create_store(),
    'Free Coffee',
    'Get a free coffee of your choice',
    100,
    'beverage'
UNION ALL
SELECT 
    get_or_create_store(),
    '10% Discount',
    'Get 10% off your next purchase',
    200,
    'discount'
UNION ALL
SELECT 
    get_or_create_store(),
    'Free Dessert',
    'Get a free dessert with any meal',
    150,
    'food';

-- 14. Verify the setup
SELECT 'Database setup complete!' as status;
SELECT 
    (SELECT COUNT(*) FROM stores) as stores_count,
    (SELECT COUNT(*) FROM customers) as customers_count,
    (SELECT COUNT(*) FROM rewards) as rewards_count; 