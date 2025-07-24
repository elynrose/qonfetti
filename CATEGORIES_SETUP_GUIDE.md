# Categories Setup Guide

## Overview
This guide explains how to set up the categories system in the database for both store settings and rewards. Categories are now stored in the database instead of being hardcoded, making the system more flexible and maintainable.

## Database Setup

### Step 1: Create Categories Table
Run the following SQL in your Supabase SQL Editor:

```sql
-- Run database/categories_setup.sql
```

This will create:
- `categories` table with all necessary columns
- Row Level Security (RLS) policies
- Indexes for performance
- Automatic timestamp updates
- Initial categories data

### Step 2: Verify Categories
After running the setup script, you can verify the categories were created:

```sql
SELECT * FROM categories WHERE is_active = true ORDER BY name;
```

You should see 15 categories:
1. Retail
2. Restaurant
3. Coffee Shop
4. Grocery
5. Pharmacy
6. Beauty & Health
7. Electronics
8. Fashion
9. Home & Garden
10. Sports & Fitness
11. Entertainment
12. Automotive
13. Education
14. Professional Services
15. Other

## Technical Implementation

### Files Created/Modified

#### New Files:
- `database/categories_setup.sql` - Database setup for categories
- `app/src/main/java/com/example/qonfetty/data/CategoryModels.kt` - Category data model

#### Modified Files:
- `app/src/main/java/com/example/qonfetty/data/SupabaseApi.kt` - Added `getCategories()` method
- `app/src/main/java/com/example/qonfetty/ui/StoreSettingsViewModel.kt` - Added categories loading
- `app/src/main/java/com/example/qonfetty/ui/RewardsViewModel.kt` - Added categories loading
- `app/src/main/java/com/example/qonfetty/ui/StoreSettingsScreen.kt` - Updated to use database categories
- `app/src/main/java/com/example/qonfetty/ui/RewardsScreen.kt` - Updated to use database categories

### API Methods Added
- `getCategories(authToken: String)` - Fetches all active categories from the database

### Data Model
```kotlin
@Serializable
data class Category(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
```

## Usage

### Store Settings
- Categories are automatically loaded when the Store Settings screen opens
- The category dropdown will show all active categories from the database
- Categories are sorted alphabetically by name

### Rewards
- Categories are automatically loaded when the Rewards screen opens
- Both add and edit reward dialogs use the same categories from the database
- Categories are sorted alphabetically by name

## Benefits

1. **Centralized Management**: All categories are stored in one place
2. **Dynamic Updates**: Categories can be added/modified without app updates
3. **Consistency**: Both store settings and rewards use the same categories
4. **Flexibility**: Easy to add new categories or modify existing ones
5. **Performance**: Categories are cached and loaded efficiently

## Future Enhancements

### Admin Interface
- Add admin screens to manage categories
- Enable/disable categories
- Add new categories
- Edit category descriptions

### Category Hierarchies
- Support for parent-child category relationships
- Subcategories for more detailed organization

### Category Icons
- Add icon support for visual category identification
- Custom icons for each category type

## Troubleshooting

### Categories Not Loading
1. Verify the `categories` table exists in your database
2. Check that RLS policies are properly configured
3. Ensure the `get_active_categories()` function exists
4. Check app logs for any API errors

### Empty Category Dropdown
1. Verify categories are inserted in the database
2. Check that `is_active = true` for categories
3. Ensure the API call is successful
4. Check network connectivity

### Permission Errors
1. Verify RLS policies allow authenticated users to read categories
2. Check that the user is properly authenticated
3. Ensure the auth token is valid

## Database Schema

```sql
CREATE TABLE categories (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## RLS Policies

- **View**: Anyone can view active categories
- **Insert**: Only authenticated users can insert categories
- **Update**: Only authenticated users can update categories
- **Delete**: Only authenticated users can delete categories

## Maintenance

- Categories are automatically sorted by name
- Inactive categories are filtered out automatically
- Timestamps are automatically updated on changes
- Unique constraint prevents duplicate category names 