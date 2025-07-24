-- Fix Store Ownership Issue
-- This script will diagnose and fix the store ownership problem

-- 1. First, let's check the current user and store situation
SELECT 'Current user info' as step;
SELECT 
    auth.uid() as current_user_id,
    auth.email() as current_user_email;

-- 2. Check if the user has a store
SELECT 'Store ownership check' as step;
SELECT 
    s.id as store_id,
    s.name as store_name,
    s.owner_id as store_owner_id,
    s.owner_id = auth.uid() as is_owner
FROM stores s
WHERE s.owner_id = auth.uid();

-- 3. Check all stores in the database
SELECT 'All stores in database' as step;
SELECT 
    id,
    name,
    owner_id,
    created_at
FROM stores
ORDER BY created_at DESC;

-- 4. If no store exists for the current user, create one
DO $$
DECLARE
    user_store_count INTEGER;
    new_store_id UUID;
BEGIN
    -- Check if user already has a store
    SELECT COUNT(*) INTO user_store_count
    FROM stores 
    WHERE owner_id = auth.uid();
    
    IF user_store_count = 0 THEN
        -- Create a new store for the user
        INSERT INTO stores (id, name, owner_id, created_at, updated_at)
        VALUES (
            gen_random_uuid(),
            'My Store',
            auth.uid(),
            NOW(),
            NOW()
        )
        RETURNING id INTO new_store_id;
        
        RAISE NOTICE 'Created new store with ID: % for user: %', new_store_id, auth.uid();
    ELSE
        RAISE NOTICE 'User already has % store(s)', user_store_count;
    END IF;
END $$;

-- 5. Verify the fix
SELECT 'Verification after fix' as step;
SELECT 
    s.id as store_id,
    s.name as store_name,
    s.owner_id as store_owner_id,
    s.owner_id = auth.uid() as is_owner,
    COUNT(cp.customer_id) as customer_count
FROM stores s
LEFT JOIN customer_points cp ON s.id = cp.store_id
WHERE s.owner_id = auth.uid()
GROUP BY s.id, s.name, s.owner_id;

-- 6. Test RLS policies
SELECT 'RLS policy test' as step;
SELECT 
    (SELECT COUNT(*) FROM stores WHERE owner_id = auth.uid()) as accessible_stores,
    (SELECT COUNT(*) FROM customer_points WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())) as accessible_customer_points,
    (SELECT COUNT(*) FROM nfc_cards WHERE store_id IN (SELECT id FROM stores WHERE owner_id = auth.uid())) as accessible_nfc_cards;

-- 7. If there are multiple stores, let's see which one should be the primary
SELECT 'Store selection for user' as step;
SELECT 
    id,
    name,
    owner_id,
    created_at,
    CASE 
        WHEN owner_id = auth.uid() THEN 'OWNED BY CURRENT USER'
        ELSE 'OWNED BY OTHER USER'
    END as ownership_status
FROM stores
ORDER BY 
    CASE WHEN owner_id = auth.uid() THEN 0 ELSE 1 END,
    created_at DESC;

SELECT 'Store ownership fix completed!' as status; 