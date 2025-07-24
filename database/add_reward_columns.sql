-- Add missing columns to rewards table
-- Run this in your Supabase SQL Editor

-- Add photo column
ALTER TABLE rewards 
ADD COLUMN IF NOT EXISTS photo TEXT;

-- Add price column
ALTER TABLE rewards 
ADD COLUMN IF NOT EXISTS price DECIMAL(10,2);

-- Add quantity column
ALTER TABLE rewards 
ADD COLUMN IF NOT EXISTS quantity INTEGER;

-- Add category column
ALTER TABLE rewards 
ADD COLUMN IF NOT EXISTS category TEXT;

-- Add is_shared column
ALTER TABLE rewards 
ADD COLUMN IF NOT EXISTS is_shared BOOLEAN DEFAULT false;

-- Verify the columns were added
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'rewards' 
AND table_schema = 'public'
ORDER BY ordinal_position; 