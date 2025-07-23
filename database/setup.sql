-- Quick Setup Script for Supabase
-- Run this in your Supabase SQL Editor

-- 1. Create stores table
CREATE TABLE IF NOT EXISTS stores (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name TEXT NOT NULL,
    owner_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Enable RLS
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;

-- 3. Create policy for store owners
DROP POLICY IF EXISTS "Store owners can access their own store" ON stores;
CREATE POLICY "Store owners can access their own store" ON stores
    FOR ALL USING (auth.uid() = owner_id);

-- 4. Create function to auto-create store for new users
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
    INSERT INTO public.stores (name, owner_id)
    VALUES ('My Store', new.id);
    RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. Create trigger
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- 6. Grant permissions
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON public.stores TO anon, authenticated;

-- 7. Check if everything is set up
SELECT 'Setup complete!' as status; 