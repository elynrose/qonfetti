# Customer Details Page Redesign Guide

## Overview

The customer details page has been completely redesigned to provide a comprehensive view of customer information, including total points earned and eligible rewards with claim buttons. The new design maintains all existing features while adding powerful new functionality.

## New Features

### 1. Total Points Earned Card
- **Prominent Display**: Shows the customer's total points earned at the current store
- **Visual Design**: Uses a primary container color with large, bold point display
- **Motivational Text**: Encourages customers to keep earning points

### 2. Eligible Rewards Section
- **Dynamic Display**: Shows all rewards the customer can claim based on their current points
- **Rich Information**: Each reward displays:
  - **Name**: Reward title
  - **Description**: Detailed description of the reward
  - **Photo**: Image URL (when available)
  - **Price**: Cost of the reward (when applicable)
  - **Quantity**: Available quantity (when applicable)
  - **Points Required**: Number of points needed to claim
  - **Status**: Active/inactive status with visual indicator
- **Claim Buttons**: One-click reward claiming with visual feedback

### 3. Enhanced Reward Cards
Each reward card includes:
- **Header Section**: Name, description, and active status
- **Details Section**: Points required, price, and quantity information
- **Action Section**: Claim button with icon

## Database Schema Updates

### Rewards Table Enhancement
The rewards table now includes additional fields:

```sql
-- New columns added to rewards table
ALTER TABLE rewards 
ADD COLUMN photo TEXT,           -- URL or path to reward image
ADD COLUMN price DECIMAL(10,2),  -- Price of the reward
ADD COLUMN quantity INTEGER;     -- Available quantity
```

### Updated Reward Data Model
```kotlin
@Serializable
data class Reward(
    val id: String,
    val name: String,
    val description: String?,
    val photo: String? = null,           // NEW
    val price: Double? = null,           // NEW
    val quantity: Int? = null,           // NEW
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

### CustomerDetailViewModel Enhancements

#### New State Variables
```kotlin
private val _totalPoints = MutableStateFlow(0)
val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()

private val _claimableRewards = MutableStateFlow<List<Reward>>(emptyList())
val claimableRewards: StateFlow<List<Reward>> = _claimableRewards.asStateFlow()

private val _rewardOperationState = MutableStateFlow<RewardOperationState>(RewardOperationState.Idle)
val rewardOperationState: StateFlow<RewardOperationState> = _rewardOperationState.asStateFlow()
```

#### New Functions
```kotlin
// Load all customer data (points, rewards, NFC cards)
fun loadCustomerData(customerId: String)

// Load customer's total points for the current store
fun loadCustomerTotalPoints(customerId: String)

// Load claimable rewards based on customer's current points
fun loadClaimableRewards(customerId: String)

// Claim a reward for the customer
fun claimReward(reward: Reward, customerId: String)
```

### UI Components

#### TotalPointsCard
- Displays total points earned in a prominent card
- Uses primary container color scheme
- Shows motivational text to encourage point earning

#### RewardCard
- Comprehensive reward information display
- Shows all reward details (name, description, price, quantity, points required)
- Includes active status indicator
- Features a prominent claim button
- Uses appropriate icons for different data types

#### Enhanced Layout
- **Section Order**: Customer Info → Total Points → Eligible Rewards → NFC Cards
- **Visual Hierarchy**: Clear section headers with proper spacing
- **Responsive Design**: Adapts to different screen sizes
- **Empty States**: Helpful messages when no rewards are available

## Usage Flow

### For Store Staff:
1. **Navigate to Customers** from the dashboard
2. **Tap on a customer** to view their details
3. **View Total Points**: See how many points the customer has earned
4. **Browse Eligible Rewards**: See what rewards the customer can claim
5. **Claim Rewards**: Tap claim buttons to redeem rewards for customers
6. **Manage NFC Cards**: Register or deactivate NFC cards as needed

### For Customers:
1. **Present NFC Card** at the store
2. **Earn Points**: Points are automatically added to their account
3. **Unlock Rewards**: As points accumulate, new rewards become available
4. **Claim Rewards**: Staff can claim rewards on their behalf

## Database Setup

### 1. Run the Schema Update
Execute the `database/rewards_schema_update.sql` script in your Supabase SQL Editor:

```sql
-- Add new columns to rewards table
ALTER TABLE rewards 
ADD COLUMN IF NOT EXISTS photo TEXT,
ADD COLUMN IF NOT EXISTS price DECIMAL(10,2),
ADD COLUMN IF NOT EXISTS quantity INTEGER;
```

### 2. Add Sample Rewards
```sql
-- Example rewards with new fields
INSERT INTO rewards (name, description, photo, price, quantity, points_required, store_id, is_active) VALUES
('Free Coffee', 'Get a free coffee of your choice', 'https://example.com/coffee.jpg', 4.99, 100, 50, 'your-store-id', true),
('10% Discount', 'Get 10% off your next purchase', 'https://example.com/discount.jpg', 0.00, 50, 100, 'your-store-id', true),
('Free Pastry', 'Get a free pastry with any purchase', 'https://example.com/pastry.jpg', 3.99, 75, 25, 'your-store-id', true);
```

## Technical Features

### Error Handling
- **Authentication Errors**: Automatic session clearing and re-login prompts
- **Network Errors**: Graceful error messages with retry options
- **Validation Errors**: Clear feedback for invalid operations

### State Management
- **Loading States**: Visual indicators during data fetching
- **Success States**: Confirmation messages for successful operations
- **Error States**: Clear error messages with recovery options

### Performance Optimizations
- **Efficient Data Loading**: Loads all customer data in parallel
- **State Caching**: Maintains data between screen navigations
- **Optimistic Updates**: Immediate UI updates with background sync

## Visual Design

### Color Scheme
- **Primary Container**: Used for total points card
- **Surface**: Used for reward and NFC cards
- **Surface Variant**: Used for empty state cards
- **Primary**: Used for active status and claim buttons
- **Secondary**: Used for additional information

### Typography
- **Headline Medium**: Section headers
- **Title Medium**: Card titles and important information
- **Body Medium**: Descriptions and details
- **Body Small**: Secondary information and labels

### Icons
- **Star**: Points and pricing information
- **CheckCircle**: Active status and claim actions
- **ShoppingCart**: Quantity information
- **Info**: NFC card information
- **Person**: Member ID information

## Testing

### Manual Testing Checklist
- [ ] Customer details display correctly
- [ ] Total points show accurate count
- [ ] Eligible rewards filter correctly based on points
- [ ] Reward claiming works and updates the list
- [ ] NFC card management functions properly
- [ ] Error states display appropriate messages
- [ ] Loading states show during operations

### Database Testing
- [ ] Rewards table has new columns (photo, price, quantity)
- [ ] Sample rewards display with all fields
- [ ] Point calculations work correctly
- [ ] Reward claiming creates proper records

## Future Enhancements

### Potential Improvements
1. **Image Loading**: Add image loading for reward photos
2. **Reward Categories**: Group rewards by category
3. **Point History**: Show detailed point earning history
4. **Reward Expiration**: Add expiration dates to rewards
5. **Bulk Operations**: Claim multiple rewards at once
6. **Customer Notifications**: Notify customers of new rewards

### API Enhancements
1. **Reward Images**: Add image upload/management
2. **Reward Analytics**: Track reward popularity and usage
3. **Customer Insights**: Provide customer behavior analytics
4. **Automated Rewards**: Trigger rewards based on customer actions

## Support

For issues or questions:
1. Check the Android logs for detailed error information
2. Verify database schema matches the requirements
3. Test with different customer point levels
4. Ensure authentication is working properly

---

**Note**: The redesigned customer details page provides a comprehensive view of customer information and enables efficient reward management. The new design maintains all existing functionality while adding powerful new features for better customer engagement and reward redemption. 