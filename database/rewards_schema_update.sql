-- Rewards Table Schema Update
-- Run this script in your Supabase SQL Editor to add the new fields

-- Add new columns to the rewards table
ALTER TABLE rewards 
ADD COLUMN IF NOT EXISTS photo TEXT,
ADD COLUMN IF NOT EXISTS price DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS quantity INTEGER,
ADD COLUMN IF NOT EXISTS category TEXT,
ADD COLUMN IF NOT EXISTS is_shared BOOLEAN DEFAULT false;

-- Add comments to document the new fields
COMMENT ON COLUMN rewards.photo IS 'URL or path to reward image/photo';
COMMENT ON COLUMN rewards.price IS 'Price of the reward in dollars';
COMMENT ON COLUMN rewards.quantity IS 'Available quantity of the reward';
COMMENT ON COLUMN rewards.category IS 'Category of the reward (e.g., Food, Beverage, Discount, etc.)';
COMMENT ON COLUMN rewards.is_shared IS 'Whether this reward can be claimed at other stores';

-- Create indexes for better performance on new fields
CREATE INDEX IF NOT EXISTS idx_rewards_price ON rewards(price);
CREATE INDEX IF NOT EXISTS idx_rewards_quantity ON rewards(quantity);
CREATE INDEX IF NOT EXISTS idx_rewards_category ON rewards(category);
CREATE INDEX IF NOT EXISTS idx_rewards_is_shared ON rewards(is_shared);
CREATE INDEX IF NOT EXISTS idx_rewards_store_shared ON rewards(store_id, is_shared);

-- Example: Update existing rewards with sample data
-- Uncomment and modify these lines to add sample data to existing rewards

/*
UPDATE rewards 
SET 
    photo = 'https://example.com/images/coffee.jpg',
    price = 4.99,
    quantity = 100,
    category = 'Beverage',
    is_shared = true
WHERE name = 'Free Coffee';

UPDATE rewards 
SET 
    photo = 'https://example.com/images/discount.jpg',
    price = 0.00,
    quantity = 50,
    category = 'Discount',
    is_shared = false
WHERE name = '10% Discount';

UPDATE rewards 
SET 
    photo = 'https://example.com/images/pastry.jpg',
    price = 3.99,
    quantity = 75,
    category = 'Food',
    is_shared = true
WHERE name = 'Free Pastry';
*/

-- Verify the updated schema
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'rewards' 
ORDER BY ordinal_position; 