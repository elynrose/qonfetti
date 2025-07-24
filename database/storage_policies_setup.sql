-- Storage Policies Setup for Photos Bucket
-- Run this in your Supabase SQL Editor

-- Enable RLS on the photos bucket
ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

-- Policy 1: Allow authenticated users to upload images to photos bucket
CREATE POLICY "Allow authenticated uploads to photos" ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (bucket_id = 'photos');

-- Policy 2: Allow authenticated users to view/download images from photos bucket
CREATE POLICY "Allow authenticated downloads from photos" ON storage.objects
    FOR SELECT
    TO authenticated
    USING (bucket_id = 'photos');

-- Policy 3: Allow authenticated users to update their uploaded images
CREATE POLICY "Allow authenticated updates to photos" ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (bucket_id = 'photos')
    WITH CHECK (bucket_id = 'photos');

-- Policy 4: Allow authenticated users to delete their uploaded images
CREATE POLICY "Allow authenticated deletes from photos" ON storage.objects
    FOR DELETE
    TO authenticated
    USING (bucket_id = 'photos');

-- Verify the policies were created
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
WHERE tablename = 'objects' 
AND schemaname = 'storage'; 