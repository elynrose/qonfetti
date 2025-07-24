-- Store Settings Table Setup
-- Run this in your Supabase SQL Editor

-- Create store_settings table
CREATE TABLE IF NOT EXISTS store_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Store Information
    store_name TEXT NOT NULL,
    category TEXT NOT NULL,
    email TEXT NOT NULL,
    phone TEXT,
    website TEXT,
    store_logo TEXT,
    
    -- Rewards Configuration
    points_per_purchase INTEGER DEFAULT 1,
    promotional_enabled BOOLEAN DEFAULT false,
    promotion_points_per_purchase INTEGER DEFAULT 0,
    
    -- API Settings
    openai_api_key TEXT,
    google_maps_api_key TEXT,
    
    -- Metadata
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable Row Level Security
ALTER TABLE store_settings ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
-- Policy 1: Store owners can view their own store settings
CREATE POLICY "Store owners can view their own store settings" ON store_settings
    FOR SELECT
    TO authenticated
    USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Policy 2: Store owners can insert their own store settings
CREATE POLICY "Store owners can insert their own store settings" ON store_settings
    FOR INSERT
    TO authenticated
    WITH CHECK (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Policy 3: Store owners can update their own store settings
CREATE POLICY "Store owners can update their own store settings" ON store_settings
    FOR UPDATE
    TO authenticated
    USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    )
    WITH CHECK (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Policy 4: Store owners can delete their own store settings
CREATE POLICY "Store owners can delete their own store settings" ON store_settings
    FOR DELETE
    TO authenticated
    USING (
        store_id IN (
            SELECT id FROM stores 
            WHERE owner_id = auth.uid()
        )
    );

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_store_settings_store_id ON store_settings(store_id);
CREATE INDEX IF NOT EXISTS idx_store_settings_created_at ON store_settings(created_at);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_store_settings_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_store_settings_updated_at_trigger
    BEFORE UPDATE ON store_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_store_settings_updated_at();

-- Verify the table was created
SELECT 
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'store_settings' 
AND table_schema = 'public'
ORDER BY ordinal_position;

-- Verify RLS policies
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
WHERE tablename = 'store_settings' 
AND schemaname = 'public'; 