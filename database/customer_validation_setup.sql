-- Customer Validation Setup for Store Access Control
-- This script adds validation to ensure customers can only get points at stores they're authorized for

-- 1. Add a new table to track customer-store authorizations
CREATE TABLE IF NOT EXISTS customer_store_authorizations (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    store_id UUID NOT NULL REFERENCES stores(id) ON DELETE CASCADE,
    is_authorized BOOLEAN DEFAULT true,
    authorized_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    authorized_by UUID REFERENCES auth.users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(customer_id, store_id)
);

-- 2. Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_customer_store_auth_customer ON customer_store_authorizations(customer_id);
CREATE INDEX IF NOT EXISTS idx_customer_store_auth_store ON customer_store_authorizations(store_id);
CREATE INDEX IF NOT EXISTS idx_customer_store_auth_authorized ON customer_store_authorizations(is_authorized);

-- 3. Enable RLS
ALTER TABLE customer_store_authorizations ENABLE ROW LEVEL SECURITY;

-- 4. Create RLS policies
CREATE POLICY "Store owners can manage customer authorizations" ON customer_store_authorizations
    FOR ALL USING (
        store_id IN (
            SELECT id FROM stores WHERE owner_id = auth.uid()
        )
    );

-- 5. Create a function to check if customer is authorized for a store
CREATE OR REPLACE FUNCTION is_customer_authorized_for_store(
    p_customer_id UUID,
    p_store_id UUID
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Check if customer is explicitly authorized for this store
    RETURN EXISTS (
        SELECT 1 FROM customer_store_authorizations 
        WHERE customer_id = p_customer_id 
        AND store_id = p_store_id 
        AND is_authorized = true
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 6. Create a function to authorize a customer for a store
CREATE OR REPLACE FUNCTION authorize_customer_for_store(
    p_customer_id UUID,
    p_store_id UUID DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    v_store_id UUID;
BEGIN
    -- Get store ID if not provided
    IF p_store_id IS NULL THEN
        v_store_id := get_or_create_store();
    ELSE
        v_store_id := p_store_id;
    END IF;
    
    -- Insert or update authorization
    INSERT INTO customer_store_authorizations (customer_id, store_id, authorized_by)
    VALUES (p_customer_id, v_store_id, auth.uid())
    ON CONFLICT (customer_id, store_id) DO UPDATE SET
        is_authorized = true,
        authorized_at = NOW(),
        authorized_by = auth.uid(),
        updated_at = NOW();
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 7. Create a function to deauthorize a customer from a store
CREATE OR REPLACE FUNCTION deauthorize_customer_from_store(
    p_customer_id UUID,
    p_store_id UUID DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
    v_store_id UUID;
BEGIN
    -- Get store ID if not provided
    IF p_store_id IS NULL THEN
        v_store_id := get_or_create_store();
    ELSE
        v_store_id := p_store_id;
    END IF;
    
    -- Update authorization to false
    UPDATE customer_store_authorizations 
    SET is_authorized = false, updated_at = NOW()
    WHERE customer_id = p_customer_id AND store_id = v_store_id;
    
    RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 8. Modify the award_points_to_customer function to check authorization
CREATE OR REPLACE FUNCTION award_points_to_customer(
    p_customer_id UUID,
    p_points INTEGER,
    p_store_id UUID DEFAULT NULL,
    p_nfc_card_id TEXT DEFAULT NULL
)
RETURNS INTEGER AS $$
DECLARE
    v_store_id UUID;
    v_new_points INTEGER;
    v_previous_points INTEGER;
    current_user_id UUID;
BEGIN
    -- Get current user ID
    current_user_id := auth.uid();
    
    -- Check if user is authenticated
    IF current_user_id IS NULL THEN
        RAISE EXCEPTION 'User not authenticated';
    END IF;
    
    -- Get store ID if not provided
    IF p_store_id IS NULL THEN
        v_store_id := get_or_create_store();
    ELSE
        v_store_id := p_store_id;
    END IF;
    
    -- Check if customer is authorized for this store
    IF NOT is_customer_authorized_for_store(p_customer_id, v_store_id) THEN
        RAISE EXCEPTION 'Customer is not authorized for this store. Please add customer to store first.';
    END IF;
    
    -- Get current points before update
    SELECT COALESCE(points, 0) INTO v_previous_points 
    FROM customer_points 
    WHERE customer_id = p_customer_id AND store_id = v_store_id;
    
    -- Insert or update customer points
    INSERT INTO customer_points (customer_id, store_id, points)
    VALUES (p_customer_id, v_store_id, p_points)
    ON CONFLICT (customer_id, store_id) DO UPDATE SET
        points = customer_points.points + EXCLUDED.points,
        updated_at = NOW()
    RETURNING points INTO v_new_points;
    
    -- Create transaction record with NFC card ID
    INSERT INTO points_transactions (
        customer_id, 
        store_id, 
        nfc_card_id,
        points_awarded, 
        previous_points, 
        new_points, 
        transaction_type, 
        description
    ) VALUES (
        p_customer_id,
        v_store_id,
        p_nfc_card_id,
        p_points,
        v_previous_points,
        v_new_points,
        'nfc_scan',
        'Points awarded via NFC scan'
    );
    
    RETURN v_new_points;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 9. Grant permissions
GRANT EXECUTE ON FUNCTION is_customer_authorized_for_store(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION authorize_customer_for_store(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION deauthorize_customer_from_store(UUID, UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION award_points_to_customer(UUID, INTEGER, UUID, TEXT) TO authenticated;
GRANT ALL ON customer_store_authorizations TO authenticated;

-- 10. Create a view to show customer authorizations for store owners
CREATE OR REPLACE VIEW customer_authorizations_view AS
SELECT 
    csa.id,
    csa.customer_id,
    csa.store_id,
    csa.is_authorized,
    csa.authorized_at,
    csa.authorized_by,
    c.name as customer_name,
    c.email as customer_email,
    c.phone as customer_phone,
    s.name as store_name,
    COALESCE(cp.points, 0) as current_points
FROM customer_store_authorizations csa
JOIN customers c ON csa.customer_id = c.id
JOIN stores s ON csa.store_id = s.id
LEFT JOIN customer_points cp ON csa.customer_id = cp.customer_id AND csa.store_id = cp.store_id
WHERE s.owner_id = auth.uid();

-- Grant access to the view
GRANT SELECT ON customer_authorizations_view TO authenticated;

-- 11. Add trigger to automatically authorize customers when they're added to a store
CREATE OR REPLACE FUNCTION auto_authorize_customer()
RETURNS TRIGGER AS $$
BEGIN
    -- When a customer is added to customer_points, automatically authorize them
    INSERT INTO customer_store_authorizations (customer_id, store_id, authorized_by)
    VALUES (NEW.customer_id, NEW.store_id, auth.uid())
    ON CONFLICT (customer_id, store_id) DO UPDATE SET
        is_authorized = true,
        authorized_at = NOW(),
        authorized_by = auth.uid(),
        updated_at = NOW();
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger
CREATE TRIGGER trigger_auto_authorize_customer
    AFTER INSERT ON customer_points
    FOR EACH ROW
    EXECUTE FUNCTION auto_authorize_customer();

-- 12. Test queries
SELECT '=== CUSTOMER AUTHORIZATION SETUP COMPLETE ===' as status;

-- Show current authorizations for the current user's store
SELECT 'Current customer authorizations:' as info;
SELECT 
    c.name as customer_name,
    csa.is_authorized,
    csa.authorized_at,
    COALESCE(cp.points, 0) as points
FROM customer_store_authorizations csa
JOIN customers c ON csa.customer_id = c.id
JOIN stores s ON csa.store_id = s.id
LEFT JOIN customer_points cp ON csa.customer_id = cp.customer_id AND csa.store_id = cp.store_id
WHERE s.owner_id = auth.uid(); 