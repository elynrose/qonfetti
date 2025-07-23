# Testing Guide: Supabase Configuration

## 🎯 **Quick Test Steps**

### 1. **Build and Install the App**
```bash
./gradlew assembleDebug
```

### 2. **Launch the App**
- Open the app on your device/emulator
- You should see the login screen

### 3. **Configure Supabase (First Time)**
- Tap "Configure Supabase" button on the login screen
- Enter your Supabase anon key in the "Anon Key" field
- Optionally enter your service key
- Tap "Save Configuration"

### 4. **Verify Configuration**
- In the configuration screen, tap the "Verify" button
- You should see a success message with configuration details
- The verification will show:
  - ✓ Host: db.xnfywqrdqcslokolhxhj.supabase.co
  - ✓ Port: 5432
  - ✓ Database: postgres
  - ✓ User: postgres
  - ✓ Anon Key: Set (with first 10 characters)
  - ✓ Service Key: Set (if provided)

### 5. **Test Authentication**
- Go back to the login screen
- Try to register a new account or login with existing credentials
- If successful, you'll be taken to the dashboard

## 🔍 **What to Look For**

### ✅ **Success Indicators**
- Configuration verification shows "Configuration is valid and API connection successful!"
- Login/registration works without errors
- Dashboard loads after successful authentication
- Store ID is fetched and stored securely

### ❌ **Common Issues**
- **"Anon key is not configured"**: Add your Supabase anon key
- **"API connection failed"**: Check your internet connection and anon key
- **"Login failed"**: Verify your Supabase project settings and RLS policies

## 🛠️ **Troubleshooting**

### **Configuration Issues**
1. **Anon Key Not Set**
   - Go to your Supabase project dashboard
   - Navigate to Settings > API
   - Copy the "anon public" key
   - Add it to the configuration screen

2. **Host Configuration**
   - Default host is already set: `db.xnfywqrdqcslokolhxhj.supabase.co`
   - Verify this matches your Supabase project URL

3. **Database Schema**
   - Ensure you've created the `stores` table in your Supabase database
   - Check that RLS policies are configured correctly

### **Authentication Issues**
1. **Registration Fails**
   - Check Supabase project settings
   - Verify email confirmation settings
   - Check RLS policies for the `auth.users` table

2. **Login Fails**
   - Verify user exists in Supabase
   - Check password requirements
   - Ensure RLS policies allow user access

3. **Store Fetching Fails**
   - Verify `stores` table exists
   - Check RLS policy: `auth.uid() = owner_id`
   - Ensure user has a corresponding store record

## 📱 **App Features to Test**

### **Authentication Flow**
- [ ] Registration with email/password
- [ ] Login with existing credentials
- [ ] Forgot password functionality
- [ ] Logout functionality

### **Configuration Management**
- [ ] Save Supabase credentials
- [ ] Verify configuration
- [ ] Reset to defaults
- [ ] Update configuration

### **Security Features**
- [ ] Encrypted storage of credentials
- [ ] Secure session management
- [ ] Automatic token refresh
- [ ] Secure logout (clears all data)

## 🔧 **Development Testing**

### **Build Verification**
```bash
# Clean build
./gradlew clean build

# Run tests
./gradlew test

# Generate debug APK
./gradlew assembleDebug
```

### **Logs to Monitor**
- Check Android Studio Logcat for:
  - Network requests to Supabase
  - Authentication responses
  - Configuration loading
  - Error messages

## 📋 **Checklist**

- [ ] App builds successfully
- [ ] Configuration screen loads
- [ ] Anon key can be saved
- [ ] Configuration verification passes
- [ ] Registration works
- [ ] Login works
- [ ] Dashboard loads
- [ ] Store ID is fetched
- [ ] Logout works
- [ ] Configuration persists after app restart

## 🆘 **Need Help?**

If you encounter issues:

1. **Check the logs** in Android Studio Logcat
2. **Verify Supabase project settings**
3. **Test with a simple API call** using curl or Postman
4. **Check network connectivity**
5. **Verify RLS policies** in Supabase dashboard

Your configuration should work seamlessly once the anon key is properly set! 