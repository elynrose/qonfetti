-- Test script to check if categories table exists and has data

-- Check if table exists
SELECT EXISTS (
    SELECT FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name = 'categories'
) as table_exists;

-- Check table structure
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'categories' 
ORDER BY ordinal_position;

-- Check if there's any data
SELECT COUNT(*) as total_categories FROM categories;

-- Check active categories
SELECT COUNT(*) as active_categories FROM categories WHERE is_active = true;

-- Show all categories
SELECT id, name, description, is_active, created_at 
FROM categories 
ORDER BY name;

-- Test the function
SELECT * FROM get_active_categories(); 