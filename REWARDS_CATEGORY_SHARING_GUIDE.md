# Rewards Category and Sharing Guide

## Overview

The rewards system has been enhanced with category support and cross-store sharing capabilities. Rewards can now be organized into categories and shared across multiple stores, allowing customers to claim rewards from any participating store.

## New Features

### 1. Reward Categories
- **Organized Display**: Rewards are categorized for better organization
- **Visual Indicators**: Category badges with icons and colors
- **Filtering Support**: Future capability to filter rewards by category
- **Common Categories**: Food, Beverage, Discount, Service, etc.

### 2. Cross-Store Reward Sharing
- **Shared Rewards**: Rewards can be marked as shared across stores
- **Universal Access**: Customers can claim shared rewards at any store
- **Store-Specific Rewards**: Non-shared rewards remain store-specific
- **Visual Indicators**: Clear indication of shared vs. store-specific rewards

## Database Schema Updates

### Rewards Table Enhancement
The rewards table now includes category and sharing fields:

```sql
-- New columns added to rewards table
ALTER TABLE rewards 
ADD COLUMN category TEXT,           -- Category of the reward
ADD COLUMN is_shared BOOLEAN DEFAULT false;  -- Whether reward is shared across stores
```

### Updated Reward Data Model
```kotlin
@Serializable
data class Reward(
    val id: String,
    val name: String,
    val description: String?,
    val photo: String? = null,
    val price: Double? = null,
    val quantity: Int? = null,
    val category: String? = null,           // NEW
    @SerialName("is_shared")
    val isShared: Boolean = false,          // NEW
    @SerialName("points_required")
    val pointsRequired: Int,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
```

## Implementation Details

### API Enhancements

#### Enhanced getClaimableRewards Function
The API now fetches both store-specific and shared rewards:

```kotlin
suspend fun getClaimableRewards(storeId: String, currentPoints: Int, authToken: String): Result<List<Reward>> {
    // Get store-specific rewards
    val storeRewards = getRewards("store_id=eq.$storeId&points_required=lte.$currentPoints&is_active=eq.true")
    
    // Get shared rewards from other stores
    val sharedRewards = getRewards("store_id=neq.$storeId&is_shared=eq.true&points_required=lte.$currentPoints&is_active=eq.true")
    
    // Combine both lists
    return storeRewards + sharedRewards
}
```

#### Database Queries
- **Store-Specific Rewards**: `store_id=eq.{storeId}&points_required=lte.{points}&is_active=eq.true`
- **Shared Rewards**: `store_id=neq.{storeId}&is_shared=eq.true&points_required=lte.{points}&is_active=eq.true`

### UI Enhancements

#### Enhanced RewardCard
The reward card now displays:

1. **Category Badge**: Shows the reward category with icon
2. **Shared Status**: Indicates if the reward is shared across stores
3. **Active Status**: Shows if the reward is currently active
4. **All Previous Fields**: Name, description, price, quantity, points required

#### Visual Design
- **Category**: Tertiary color with info icon
- **Shared Status**: Secondary color with star icon
- **Active Status**: Primary color with check circle icon
- **Layout**: Organized status indicators in the header section

## Database Setup

### 1. Run the Schema Update
Execute the updated `database/rewards_schema_update.sql` script:

```sql
-- Add new columns to rewards table
ALTER TABLE rewards 
ADD COLUMN IF NOT EXISTS category TEXT,
ADD COLUMN IF NOT EXISTS is_shared BOOLEAN DEFAULT false;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_rewards_category ON rewards(category);
CREATE INDEX IF NOT EXISTS idx_rewards_is_shared ON rewards(is_shared);
CREATE INDEX IF NOT EXISTS idx_rewards_store_shared ON rewards(store_id, is_shared);
```

### 2. Add Sample Rewards with Categories
```sql
-- Example rewards with categories and sharing
INSERT INTO rewards (name, description, photo, price, quantity, category, is_shared, points_required, store_id, is_active) VALUES
('Free Coffee', 'Get a free coffee of your choice', 'https://example.com/coffee.jpg', 4.99, 100, 'Beverage', true, 50, 'store-id-1', true),
('10% Discount', 'Get 10% off your next purchase', 'https://example.com/discount.jpg', 0.00, 50, 'Discount', false, 100, 'store-id-1', true),
('Free Pastry', 'Get a free pastry with any purchase', 'https://example.com/pastry.jpg', 3.99, 75, 'Food', true, 25, 'store-id-1', true),
('Free Wi-Fi', 'Access to premium Wi-Fi for 1 hour', 'https://example.com/wifi.jpg', 0.00, 200, 'Service', true, 10, 'store-id-2', true),
('Loyalty Card', 'Get a physical loyalty card', 'https://example.com/card.jpg', 0.00, 50, 'Service', false, 150, 'store-id-1', true);
```

## Usage Examples

### Creating Shared Rewards
```sql
-- Create a shared beverage reward
INSERT INTO rewards (
    name, 
    description, 
    category, 
    is_shared, 
    points_required, 
    store_id, 
    is_active
) VALUES (
    'Free Coffee',
    'Get a free coffee of your choice',
    'Beverage',
    true,  -- This makes it shared across stores
    50,
    'your-store-id',
    true
);
```

### Creating Store-Specific Rewards
```sql
-- Create a store-specific discount
INSERT INTO rewards (
    name, 
    description, 
    category, 
    is_shared, 
    points_required, 
    store_id, 
    is_active
) VALUES (
    'Store Special Discount',
    'Get 15% off store-specific items',
    'Discount',
    false,  -- This keeps it store-specific
    75,
    'your-store-id',
    true
);
```

## Common Categories

### Suggested Category Names
- **Food**: Pastries, sandwiches, meals
- **Beverage**: Coffee, tea, soft drinks
- **Discount**: Percentage off, dollar off
- **Service**: Wi-Fi, parking, delivery
- **Merchandise**: Physical items, gift cards
- **Experience**: Events, workshops, classes
- **VIP**: Premium services, exclusive access

### Category Best Practices
1. **Consistent Naming**: Use consistent category names across stores
2. **Clear Descriptions**: Make categories self-explanatory
3. **Logical Grouping**: Group similar rewards together
4. **Future-Proof**: Choose categories that can accommodate new rewards

## Cross-Store Sharing Strategy

### When to Use Shared Rewards
- **Universal Products**: Coffee, basic food items
- **Standard Services**: Wi-Fi, basic amenities
- **Brand Recognition**: Items that build brand loyalty
- **Customer Convenience**: Rewards customers expect everywhere

### When to Use Store-Specific Rewards
- **Unique Items**: Store-specific products or services
- **Local Specialties**: Items unique to a location
- **Inventory Management**: Limited availability items
- **Store Identity**: Rewards that differentiate the store

### Implementation Considerations
1. **Inventory Management**: Shared rewards need inventory tracking
2. **Redemption Tracking**: Track which store redeemed shared rewards
3. **Customer Experience**: Ensure consistent redemption experience
4. **Business Rules**: Define which stores can redeem which shared rewards

## Technical Features

### Performance Optimizations
- **Indexed Queries**: Database indexes for category and sharing fields
- **Efficient Filtering**: Optimized queries for store-specific vs. shared rewards
- **Caching**: Reward data caching for better performance

### Error Handling
- **Network Failures**: Graceful handling of API failures
- **Data Validation**: Validation of category and sharing fields
- **Fallback Behavior**: Fallback to store-specific rewards if shared rewards fail

### Security Considerations
- **Access Control**: Ensure stores can only access appropriate rewards
- **Data Privacy**: Protect customer data across stores
- **Audit Trail**: Track reward creation and modifications

## Testing

### Manual Testing Checklist
- [ ] Categories display correctly in reward cards
- [ ] Shared status indicators show properly
- [ ] Shared rewards appear at different stores
- [ ] Store-specific rewards only appear at their store
- [ ] Reward claiming works for both types
- [ ] Category filtering works (if implemented)

### Database Testing
- [ ] Category field stores and retrieves correctly
- [ ] is_shared field works as expected
- [ ] Indexes improve query performance
- [ ] Cross-store queries return correct results

## Future Enhancements

### Potential Improvements
1. **Category Filtering**: Filter rewards by category in the UI
2. **Category Management**: Admin interface for managing categories
3. **Category Analytics**: Track popular categories and rewards
4. **Dynamic Categories**: Auto-categorize rewards based on content
5. **Category Hierarchies**: Subcategories and nested categories
6. **Category-Based Recommendations**: Suggest rewards based on customer preferences

### Advanced Sharing Features
1. **Selective Sharing**: Share rewards with specific stores only
2. **Sharing Networks**: Create networks of stores that share rewards
3. **Sharing Rules**: Define rules for when rewards can be shared
4. **Sharing Analytics**: Track cross-store reward usage
5. **Sharing Agreements**: Formal agreements between stores

## Support

### Troubleshooting
1. **Shared Rewards Not Appearing**: Check is_shared field and store permissions
2. **Category Not Displaying**: Verify category field is not null
3. **Performance Issues**: Check database indexes are created
4. **Cross-Store Issues**: Verify store authentication and permissions

### Common Issues
- **Missing Categories**: Ensure category field is populated
- **Sharing Not Working**: Check is_shared field and API queries
- **Performance Slow**: Verify database indexes are in place
- **UI Not Updating**: Check for compilation errors and rebuild

---

**Note**: The category and sharing features provide a powerful way to organize rewards and create a unified customer experience across multiple store locations. Proper implementation ensures customers can enjoy rewards from any participating store while maintaining store-specific offerings. 