-- Supabase Database Setup and Seeding Script
-- Run this in your Supabase SQL editor

-- 1. Create the stores table if it doesn't exist
CREATE TABLE IF NOT EXISTS stores (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name TEXT NOT NULL,
    owner_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Enable Row Level Security
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;

-- 3. Create policy for store owners to access their own store
DROP POLICY IF EXISTS "Store owners can access their own store" ON stores;
CREATE POLICY "Store owners can access their own store" ON stores
    FOR ALL USING (auth.uid() = owner_id);

-- 4. Create a function to automatically create a store when a user registers
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.stores (name, owner_id)
    VALUES ('My Store', new.id);
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. Create trigger to automatically create store for new users
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- 6. Insert a test store owner (optional - for testing purposes)
-- Note: This will only work if you have a user with this email already registered
-- You can comment this out if you want to test with your own registration

-- INSERT INTO stores (name, owner_id) 
-- SELECT 'Test Store', id 
-- FROM auth.users 
-- WHERE email = 'storeowner@example.com'
-- ON CONFLICT DO NOTHING;

-- 7. Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_stores_owner_id ON stores(owner_id);
CREATE INDEX IF NOT EXISTS idx_stores_created_at ON stores(created_at);

-- 8. Grant necessary permissions
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON public.stores TO anon, authenticated;
GRANT ALL ON public.stores_id_seq TO anon, authenticated; 