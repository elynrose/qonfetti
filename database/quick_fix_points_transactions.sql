-- Quick Fix for Points Transactions
-- Run this in your Supabase SQL editor

-- 1. Check if table exists
SELECT 'Table exists check' as test, COUNT(*) as count FROM information_schema.tables WHERE table_name = 'points_transactions';

-- 2. If table doesn't exist, create it
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'points_transactions') THEN
        CREATE TABLE points_transactions (
            id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
            customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
            store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
            nfc_card_id TEXT,
            points_awarded INTEGER NOT NULL,
            previous_points INTEGER NOT NULL,
            new_points INTEGER NOT NULL,
            transaction_type TEXT NOT NULL DEFAULT 'nfc_scan',
            description TEXT,
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
        );
        
        -- Create indexes
        CREATE INDEX idx_points_transactions_customer_id ON points_transactions(customer_id);
        CREATE INDEX idx_points_transactions_store_id ON points_transactions(store_id);
        CREATE INDEX idx_points_transactions_created_at ON points_transactions(created_at);
        
        RAISE NOTICE 'Created points_transactions table';
    ELSE
        RAISE NOTICE 'points_transactions table already exists';
    END IF;
END $$;

-- 3. Disable RLS temporarily to check data
ALTER TABLE points_transactions DISABLE ROW LEVEL SECURITY;

-- 4. Check if there's data
SELECT 'Data check' as test, COUNT(*) as count FROM points_transactions;

-- 5. Show recent transactions
SELECT 
    id,
    customer_id,
    store_id,
    points_awarded,
    created_at,
    transaction_type
FROM points_transactions 
ORDER BY created_at DESC 
LIMIT 10;

-- 6. Re-enable RLS with simple policies
ALTER TABLE points_transactions ENABLE ROW LEVEL SECURITY;

-- Drop existing policies
DROP POLICY IF EXISTS "Store owners can view points transactions at their stores" ON points_transactions;
DROP POLICY IF EXISTS "Store owners can insert points transactions at their stores" ON points_transactions;
DROP POLICY IF EXISTS "Enable read access for authenticated users" ON points_transactions;
DROP POLICY IF EXISTS "Enable insert access for authenticated users" ON points_transactions;

-- Create simple policies
CREATE POLICY "Enable read access for authenticated users" ON points_transactions
    FOR SELECT USING (true);

CREATE POLICY "Enable insert access for authenticated users" ON points_transactions
    FOR INSERT WITH CHECK (true);

-- 7. Grant permissions
GRANT USAGE ON SCHEMA public TO authenticated;
GRANT ALL ON points_transactions TO authenticated;

-- 8. Test the query the app uses
SELECT 'App query test' as test, COUNT(*) as count 
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c';

-- 9. Show transactions for your store
SELECT 
    id,
    customer_id,
    store_id,
    points_awarded,
    created_at,
    transaction_type
FROM points_transactions 
WHERE store_id = 'a667e115-9e6a-4d39-9ae3-7ab98b63386c'
ORDER BY created_at DESC; 