-- Categories table setup
-- This table will store categories that can be used for both stores and rewards

-- Create categories table
CREATE TABLE IF NOT EXISTS categories (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;

-- Create RLS policies
-- Anyone can view active categories
CREATE POLICY "Anyone can view active categories" ON categories
    FOR SELECT USING (is_active = true);

-- Only authenticated users can insert categories (for future admin functionality)
CREATE POLICY "Authenticated users can insert categories" ON categories
    FOR INSERT WITH CHECK (auth.role() = 'authenticated');

-- Only authenticated users can update categories
CREATE POLICY "Authenticated users can update categories" ON categories
    FOR UPDATE USING (auth.role() = 'authenticated');

-- Only authenticated users can delete categories
CREATE POLICY "Authenticated users can delete categories" ON categories
    FOR DELETE USING (auth.role() = 'authenticated');

-- Create index on name for faster lookups
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name);
CREATE INDEX IF NOT EXISTS idx_categories_active ON categories(is_active);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_categories_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_categories_updated_at();

-- Insert initial categories
INSERT INTO categories (name, description) VALUES
    ('Retail', 'General retail stores'),
    ('Restaurant', 'Food and dining establishments'),
    ('Coffee Shop', 'Coffee and beverage shops'),
    ('Grocery', 'Food and grocery stores'),
    ('Pharmacy', 'Pharmacy and drug stores'),
    ('Beauty & Health', 'Beauty, health, and wellness'),
    ('Electronics', 'Electronics and technology'),
    ('Fashion', 'Clothing and fashion retail'),
    ('Home & Garden', 'Home improvement and garden'),
    ('Sports & Fitness', 'Sports equipment and fitness'),
    ('Entertainment', 'Entertainment and leisure'),
    ('Automotive', 'Automotive services and parts'),
    ('Education', 'Educational services and institutions'),
    ('Professional Services', 'Professional and business services'),
    ('Other', 'Other business categories')
ON CONFLICT (name) DO NOTHING;

-- Create a function to get all active categories
CREATE OR REPLACE FUNCTION get_active_categories()
RETURNS TABLE (
    id UUID,
    name VARCHAR(100),
    description TEXT
) AS $$
BEGIN
    RETURN QUERY
    SELECT c.id, c.name, c.description
    FROM categories c
    WHERE c.is_active = true
    ORDER BY c.name;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER; 