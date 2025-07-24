-- Transactions table for tracking reward claims and purchase amounts
CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    reward_id UUID REFERENCES rewards(id) ON DELETE SET NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('purchase', 'reward_claim')),
    amount DECIMAL(10,2) NOT NULL,
    points_used INTEGER DEFAULT 0,
    points_earned INTEGER DEFAULT 0,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_transactions_store_id ON transactions(store_id);
CREATE INDEX IF NOT EXISTS idx_transactions_customer_id ON transactions(customer_id);
CREATE INDEX IF NOT EXISTS idx_transactions_reward_id ON transactions(reward_id);
CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);

-- RLS policies for transactions
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;

-- Store owners can only see transactions for their store
CREATE POLICY "Store owners can view their store transactions" ON transactions
    FOR SELECT USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Store owners can insert transactions for their store
CREATE POLICY "Store owners can insert transactions for their store" ON transactions
    FOR INSERT WITH CHECK (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Store owners can update transactions for their store
CREATE POLICY "Store owners can update their store transactions" ON transactions
    FOR UPDATE USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Store owners can delete transactions for their store
CREATE POLICY "Store owners can delete their store transactions" ON transactions
    FOR DELETE USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_transactions_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update updated_at
CREATE TRIGGER update_transactions_updated_at
    BEFORE UPDATE ON transactions
    FOR EACH ROW
    EXECUTE FUNCTION update_transactions_updated_at();

-- Function to get transaction statistics for dashboard
CREATE OR REPLACE FUNCTION get_transaction_stats(store_id_param UUID)
RETURNS TABLE (
    total_purchases DECIMAL(10,2),
    total_claimed DECIMAL(10,2),
    total_transactions INTEGER,
    total_points_earned INTEGER,
    total_points_used INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        COALESCE(SUM(CASE WHEN transaction_type = 'purchase' THEN amount ELSE 0 END), 0) as total_purchases,
        COALESCE(SUM(CASE WHEN transaction_type = 'reward_claim' THEN amount ELSE 0 END), 0) as total_claimed,
        COUNT(*) as total_transactions,
        COALESCE(SUM(points_earned), 0) as total_points_earned,
        COALESCE(SUM(points_used), 0) as total_points_used
    FROM transactions 
    WHERE store_id = store_id_param;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission on the function
GRANT EXECUTE ON FUNCTION get_transaction_stats(UUID) TO authenticated; 