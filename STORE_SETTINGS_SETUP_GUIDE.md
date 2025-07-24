# Store Settings Setup Guide

## Overview
The Store Settings feature allows store owners to configure their store information, rewards settings, and API configurations. This includes store details, logo upload, points configuration, and API key management.

## Features Included

### 1. Store Information
- **Store Name**: The name of the store
- **Store Logo**: Upload and display store logo
- **Category**: Store category (dropdown with predefined options)
- **Email**: Store contact email
- **Phone Number**: Store contact phone
- **Website**: Store website URL

### 2. Rewards Configuration
- **Points per Purchase**: Default points awarded per purchase
- **Promotional Mode**: Toggle to enable/disable promotional points
- **Promotion Points per Purchase**: Additional points during promotions

### 3. API Settings
- **OpenAI API Key**: For AI-powered features
- **Google Maps API Key**: For location-based features

## Database Setup

### Step 1: Create Store Settings Table
Run the following SQL in your Supabase SQL Editor:

```sql
-- Run database/store_settings_setup.sql
```

This will create:
- `store_settings` table with all necessary columns
- Row Level Security (RLS) policies
- Indexes for performance
- Automatic timestamp updates

### Step 2: Storage Bucket (Already Exists)
The store logos will be stored in the existing `photos` bucket, which already has the necessary storage policies configured.

**Note**: No additional setup is required for storage since the `photos` bucket and its policies are already in place.

## Usage Instructions

### Accessing Store Settings
1. Open the app and log in
2. From the Dashboard, tap the hamburger menu (three lines) in the top right
3. Select "Store Settings"
4. The settings form will be displayed directly on the screen

### Configuring Store Settings
1. **Direct Form Interface**: All settings are displayed directly on the screen in editable form fields
2. **Real-time Editing**: Make changes directly in the form fields
3. **Save Changes**: Tap the "Save" button in the top right to save your changes

### Store Information
- **Store Name**: Enter your store's name (required)
- **Category**: Select from predefined categories (required) - same categories used for rewards
- **Email**: Enter store contact email (required)
- **Phone**: Enter store phone number (optional)
- **Website**: Enter store website URL (optional)
- **Logo**: Tap "Add Logo" or "Change Logo" to upload a store logo

### Rewards Configuration
- **Points per Purchase**: Set default points awarded per purchase (default: 1)
- **Promotional Mode**: Toggle to enable promotional points
- **Promotion Points per Purchase**: Set additional points during promotions (only visible when promotional mode is enabled)

### API Settings (Optional)
- **OpenAI API Key**: Enter your OpenAI API key for AI features (optional)
- **Google Maps API Key**: Enter your Google Maps API key for location features (optional)

### Saving Settings
- Tap "Save" to save all settings
- The app will automatically create or update your store settings
- Settings are tied to your store and will persist across sessions

## Technical Implementation

### Files Created/Modified

#### New Files:
- `app/src/main/java/com/example/qonfetty/data/StoreSettingsModels.kt` - Data models
- `app/src/main/java/com/example/qonfetty/ui/StoreSettingsViewModel.kt` - ViewModel
- `app/src/main/java/com/example/qonfetty/ui/StoreSettingsScreen.kt` - UI Screen
- `app/src/main/java/com/example/qonfetty/ui/theme/Categories.kt` - Shared categories for consistency
- `database/store_settings_setup.sql` - Database setup
- `database/logos_storage_policies.sql` - Storage policies (optional, using existing photos bucket)

#### Modified Files:
- `app/src/main/java/com/example/qonfetty/data/SupabaseApi.kt` - Added API methods
- `app/src/main/java/com/example/qonfetty/MainActivity.kt` - Added navigation
- `app/src/main/java/com/example/qonfetty/ui/DashboardScreen.kt` - Added settings menu option

### API Methods Added
- `getStoreSettings()` - Retrieve store settings
- `createStoreSettings()` - Create new store settings
- `updateStoreSettings()` - Update existing store settings
- `uploadStoreLogo()` - Upload store logo to photos bucket

### Security Features
- Row Level Security (RLS) ensures users can only access their own store settings
- Storage policies control logo upload/download permissions
- All API calls require authentication
- Settings are tied to specific store IDs

## Troubleshooting

### Common Issues

1. **"Not authenticated or no store" Error**
   - Ensure you're logged in
   - Check that your store ID is properly set in session storage

2. **Logo Upload Fails**
   - Verify the `photos` bucket exists in Supabase Storage
   - Check that storage policies are properly configured (should already be set up)
   - Ensure the image file is not too large

3. **Settings Don't Save**
   - Check that required fields (store name, category, email) are filled
   - Verify your internet connection
   - Check the app logs for specific error messages

4. **Database Errors**
   - Ensure the `store_settings` table was created successfully
   - Verify RLS policies are in place
   - Check that the table structure matches the expected schema

### Debug Steps
1. Check the Android logs using `adb logcat`
2. Verify database table structure in Supabase Dashboard
3. Test API endpoints directly in Supabase
4. Check storage bucket permissions

## Future Enhancements

### Potential Additions
- **Store Hours**: Configure business hours
- **Location Settings**: Store address and coordinates
- **Notification Settings**: Configure push notifications
- **Theme Customization**: Store-specific colors and branding
- **Integration Settings**: Connect with external services
- **Analytics Configuration**: Set up tracking and reporting

### API Integrations
- **Payment Gateway Settings**: Configure payment processors
- **Social Media Links**: Add social media accounts
- **Inventory System**: Connect with inventory management
- **CRM Integration**: Customer relationship management settings

## Support

If you encounter any issues with the Store Settings feature:
1. Check this guide for troubleshooting steps
2. Review the database setup scripts
3. Verify all required tables and policies are in place
4. Check the app logs for detailed error messages
5. Ensure all API keys are valid and properly configured 