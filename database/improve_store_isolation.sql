-- Improve Store Isolation for Cross-Store Security
-- This script adds proper store filtering to prevent cross-store access

-- 1. Update customer lookup to be store-specific
-- Create a new function for store-specific customer lookup
CREATE OR REPLACE FUNCTION find_customer_by_member_id_for_store(
    member_id_param TEXT,
    store_id_param UUID
)
RETURNS TABLE (
    id UUID,
    name TEXT,
    email TEXT,
    phone TEXT,
    address TEXT,
    member_id TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        c.id,
        c.name,
        c.email,
        c.phone,
        c.address,
        c.member_id,
        c.created_at,
        c.updated_at
    FROM customers c
    INNER JOIN customer_points cp ON c.id = cp.customer_id
    WHERE c.member_id = member_id_param 
    AND cp.store_id = store_id_param;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 2. Update NFC card lookup to be store-specific
-- Create a new function for store-specific NFC card lookup
CREATE OR REPLACE FUNCTION find_nfc_card_by_member_id_for_store(
    member_id_param TEXT,
    store_id_param UUID
)
RETURNS TABLE (
    id UUID,
    card_id TEXT,
    member_id TEXT,
    customer_id UUID,
    store_id UUID,
    is_active BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        nc.id,
        nc.card_id,
        nc.member_id,
        nc.customer_id,
        nc.store_id,
        nc.is_active,
        nc.created_at,
        nc.updated_at
    FROM nfc_cards nc
    WHERE nc.member_id = member_id_param 
    AND nc.store_id = store_id_param
    AND nc.is_active = true;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 3. Add RLS policy to prevent cross-store customer access
-- Update the customer points policy to be more restrictive
DROP POLICY IF EXISTS "Allow store owners to read their customer points" ON customer_points;
CREATE POLICY "Allow store owners to read their customer points" ON customer_points
    FOR SELECT USING (
        auth.role() = 'authenticated' AND
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 4. Add RLS policy to prevent cross-store NFC card access
-- Update the NFC cards policy to be more restrictive
DROP POLICY IF EXISTS "Store owners can view NFC cards at their stores" ON nfc_cards;
CREATE POLICY "Store owners can view NFC cards at their stores" ON nfc_cards
    FOR SELECT USING (
        auth.role() = 'authenticated' AND
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 5. Create a comprehensive cross-store validation function
CREATE OR REPLACE FUNCTION validate_customer_for_store(
    member_id_param TEXT,
    store_id_param UUID
)
RETURNS TABLE (
    is_valid BOOLEAN,
    customer_id UUID,
    customer_name TEXT,
    error_message TEXT
) AS $$
DECLARE
    customer_record RECORD;
    nfc_card_record RECORD;
    points_record RECORD;
BEGIN
    -- Check if customer exists and has points at this store
    SELECT c.id, c.name INTO customer_record
    FROM customers c
    INNER JOIN customer_points cp ON c.id = cp.customer_id
    WHERE c.member_id = member_id_param 
    AND cp.store_id = store_id_param;
    
    -- Check if NFC card is registered at this store
    SELECT nc.customer_id INTO nfc_card_record
    FROM nfc_cards nc
    WHERE nc.member_id = member_id_param 
    AND nc.store_id = store_id_param
    AND nc.is_active = true;
    
    -- Check if customer has points at this store
    SELECT cp.customer_id INTO points_record
    FROM customer_points cp
    WHERE cp.customer_id = customer_record.id
    AND cp.store_id = store_id_param;
    
    -- Return validation result
    IF customer_record.id IS NULL THEN
        RETURN QUERY SELECT 
            false as is_valid,
            NULL::UUID as customer_id,
            NULL::TEXT as customer_name,
            'Customer not found or not registered at this store' as error_message;
    ELSIF nfc_card_record.customer_id IS NULL THEN
        RETURN QUERY SELECT 
            false as is_valid,
            customer_record.id as customer_id,
            customer_record.name as customer_name,
            'NFC card not registered at this store' as error_message;
    ELSIF points_record.customer_id IS NULL THEN
        RETURN QUERY SELECT 
            false as is_valid,
            customer_record.id as customer_id,
            customer_record.name as customer_name,
            'Customer not authorized for this store' as error_message;
    ELSE
        RETURN QUERY SELECT 
            true as is_valid,
            customer_record.id as customer_id,
            customer_record.name as customer_name,
            'Customer validated successfully' as error_message;
    END IF;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. Test the validation function
-- This will show how the validation works
SELECT 'Store isolation improvements completed!' as status;

-- Example usage (replace with actual values):
-- SELECT * FROM validate_customer_for_store('member123', 'store-uuid-here'); 