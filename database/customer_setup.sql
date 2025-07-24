-- Customer Management Tables Setup
-- Run this script in your Supabase SQL Editor

-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    phone TEXT NOT NULL,
    address TEXT,
    member_id TEXT UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create customer_points table (junction table for customer-store relationship)
CREATE TABLE IF NOT EXISTS customer_points (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    points INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(customer_id, store_id)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_customers_email ON customers(email);
CREATE INDEX IF NOT EXISTS idx_customers_member_id ON customers(member_id);
CREATE INDEX IF NOT EXISTS idx_customer_points_customer_id ON customer_points(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_points_store_id ON customer_points(store_id);
CREATE INDEX IF NOT EXISTS idx_customer_points_customer_store ON customer_points(customer_id, store_id);

-- Enable Row Level Security (RLS)
ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE customer_points ENABLE ROW LEVEL SECURITY;

-- Create policies for customers table
-- Allow authenticated users to read all customers (for member_id lookup)
CREATE POLICY "Allow authenticated users to read customers" ON customers
    FOR SELECT USING (auth.role() = 'authenticated');

-- Allow authenticated users to insert customers
CREATE POLICY "Allow authenticated users to insert customers" ON customers
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Allow authenticated users to update customers
CREATE POLICY "Allow authenticated users to update customers" ON customers
    FOR UPDATE USING (auth.role() = 'authenticated');

-- Create policies for customer_points table
-- Allow store owners to read their store's customer points
CREATE POLICY "Allow store owners to read their customer points" ON customer_points
    FOR SELECT USING (
        auth.role() = 'authenticated' AND
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- Allow store owners to insert customer points for their store
CREATE POLICY "Allow store owners to insert customer points" ON customer_points
    FOR INSERT WITH CHECK (
        auth.role() = 'authenticated' AND
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- Allow store owners to update customer points for their store
CREATE POLICY "Allow store owners to update customer points" ON customer_points
    FOR UPDATE USING (
        auth.role() = 'authenticated' AND
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- Allow store owners to delete customer points for their store
CREATE POLICY "Allow store owners to delete customer points" ON customer_points
    FOR DELETE USING (
        auth.role() = 'authenticated' AND
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- Create function to automatically generate member_id from email
CREATE OR REPLACE FUNCTION generate_member_id()
RETURNS TRIGGER AS $$
BEGIN
    -- Generate member_id from email hash if not provided
    IF NEW.member_id IS NULL THEN
        NEW.member_id := abs(hash(NEW.email))::TEXT;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically generate member_id
CREATE TRIGGER trigger_generate_member_id
    BEFORE INSERT ON customers
    FOR EACH ROW
    EXECUTE FUNCTION generate_member_id();

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers to automatically update updated_at
CREATE TRIGGER trigger_update_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_update_customer_points_updated_at
    BEFORE UPDATE ON customer_points
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Grant necessary permissions
GRANT USAGE ON SCHEMA public TO authenticated;
GRANT ALL ON customers TO authenticated;
GRANT ALL ON customer_points TO authenticated;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO authenticated; 