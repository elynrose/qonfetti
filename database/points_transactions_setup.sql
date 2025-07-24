-- Points Transactions Table Setup for Qonfetty App
-- This script creates the points_transactions table to track all points activity

-- Create points_transactions table
CREATE TABLE IF NOT EXISTS points_transactions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    nfc_card_id TEXT, -- The NFC card that was scanned (optional)
    points_awarded INTEGER NOT NULL, -- Points awarded in this transaction
    previous_points INTEGER NOT NULL, -- Points before this transaction
    new_points INTEGER NOT NULL, -- Points after this transaction
    transaction_type TEXT NOT NULL DEFAULT 'nfc_scan', -- Type of transaction (nfc_scan, manual, reward_claim, etc.)
    description TEXT, -- Description of the transaction
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_points_transactions_customer_id ON points_transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_points_transactions_store_id ON points_transactions(store_id);
CREATE INDEX IF NOT EXISTS idx_points_transactions_created_at ON points_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_points_transactions_customer_store ON points_transactions(customer_id, store_id);
CREATE INDEX IF NOT EXISTS idx_points_transactions_date_range ON points_transactions(store_id, created_at);

-- Enable Row Level Security
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;

-- Policy: Store owners can view points transactions at their stores
CREATE POLICY "Store owners can view points transactions at their stores" ON points_transactions
    FOR SELECT USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Policy: Store owners can insert points transactions at their stores
CREATE POLICY "Store owners can insert points transactions at their stores" ON points_transactions
    FOR INSERT WITH CHECK (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Grant necessary permissions
GRANT USAGE ON SCHEMA public TO authenticated;
GRANT ALL ON points_transactions TO authenticated;

-- Create a view for easier querying of points transactions with customer info
CREATE OR REPLACE VIEW points_transactions_view AS
SELECT 
    pt.id,
    pt.customer_id,
    pt.store_id,
    pt.nfc_card_id,
    pt.points_awarded,
    pt.previous_points,
    pt.new_points,
    pt.transaction_type,
    pt.description,
    pt.created_at,
    c.name as customer_name,
    c.email as customer_email,
    c.member_id as customer_member_id
FROM points_transactions pt
JOIN customers c ON pt.customer_id = c.id;

-- Grant permissions on the view
GRANT SELECT ON points_transactions_view TO authenticated; 