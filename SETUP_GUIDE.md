# Quick Setup Guide

## 🔧 **Configure Supabase Credentials**

### **Step 1: Get Your Supabase Anon Key**

1. Go to your [Supabase Dashboard](https://supabase.com)
2. Select your project
3. Go to **Settings** > **API**
4. Copy the **"anon public"** key

### **Step 2: Update EnvironmentConfig.kt**

Open `app/src/main/java/com/example/qonfetty/config/EnvironmentConfig.kt`

Find this line:
```kotlin
setSupabaseAnonKey("your-anon-key-here") // Replace with your actual anon key
```

Replace `"your-anon-key-here"` with your actual anon key:
```kotlin
setSupabaseAnonKey("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") // Your actual anon key
```

### **Step 3: Build and Install**

```bash
./gradlew assembleDebug
```

The APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## 🎯 **Test the App**

1. **Install the APK** on your Pixel
2. **Open the app**
3. **Tap "Create Account"** to register a new store owner
4. **Enter any email and password**
5. **The app will automatically create a store** for the user
6. **Login with your credentials**

## ✅ **That's It!**

Your app is now configured and ready to use. The Supabase credentials are hardcoded in the environment config file, so no configuration screen is needed.



## 📱 **App Features**

- ✅ **Login/Register** with Supabase
- ✅ **Automatic store creation** for new users
- ✅ **Secure session management**
- ✅ **Store dashboard** for authenticated users

No configuration screen needed - everything is set up in the code! 🎉 