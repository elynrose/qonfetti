# Qonfetty - Store Owner Authentication

A Kotlin Android application with Jetpack Compose that implements Supabase authentication for store owners. The app provides login, registration, forgot password, and logout functionality with secure session storage.

## Features

- **Authentication**: Login, register, and forgot password functionality
- **Secure Storage**: Encrypted session storage using Android Security Crypto
- **Store Management**: Automatically fetches and stores store_id for authenticated users
- **Modern UI**: Built with Jetpack Compose and Material 3
- **HTTP Client**: Uses Ktor for API communication with Supabase

## Setup Instructions

### 1. Supabase Configuration

1. Create a Supabase project at [supabase.com](https://supabase.com)
2. Get your project URL and anon key from the project settings
3. Update the configuration in `EnvironmentConfig.kt`:

```kotlin
// In app/src/main/java/com/example/qonfetty/config/EnvironmentConfig.kt
private const val DEFAULT_SUPABASE_HOST = "db.xnfywqrdqcslokolhxhj.supabase.co"
private const val DEFAULT_SUPABASE_PORT = 5432
private const val DEFAULT_SUPABASE_DATABASE = "postgres"
private const val DEFAULT_SUPABASE_USER = "postgres"

// In the initializeWithDefaults() function, add your anon key:
setSupabaseAnonKey("your-actual-anon-key-here")
```

**Environment Template**
Reference the `env.template` file for the required configuration variables.

### 2. Database Schema

Create the following table in your Supabase database:

```sql
-- Create stores table
CREATE TABLE stores (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name TEXT NOT NULL,
    owner_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Enable Row Level Security
ALTER TABLE stores ENABLE ROW LEVEL SECURITY;

-- Create policy for store owners to access their own store
CREATE POLICY "Store owners can access their own store" ON stores
    FOR ALL USING (auth.uid() = owner_id);
```

### 3. Build and Run

1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Build and run the application on an emulator or device

## Architecture

### Data Layer
- **AuthModels.kt**: Data classes for authentication requests and responses
- **SessionStorage.kt**: Secure encrypted storage for auth tokens and store ID
- **SupabaseApi.kt**: HTTP client for Supabase REST API communication
- **EnvironmentConfig.kt**: Secure configuration management for Supabase credentials

### UI Layer
- **AuthViewModel.kt**: ViewModel managing authentication state and business logic
- **AuthScreen.kt**: Compose UI for login, register, and forgot password forms
- **DashboardScreen.kt**: Dashboard for authenticated store owners

### Key Features

1. **Secure Session Storage**: Uses Android Security Crypto for encrypted storage
2. **Environment Configuration**: Hardcoded Supabase credentials in environment config
3. **Automatic Store Fetching**: Fetches store_id after successful login
4. **Form Validation**: Client-side validation for email and password fields
5. **Error Handling**: Comprehensive error handling and user feedback
6. **State Management**: Reactive UI with StateFlow and Compose state

## Testing

The project includes unit tests for the AuthViewModel:

```bash
./gradlew test
```

## Dependencies

- **Jetpack Compose**: Modern UI toolkit
- **Ktor**: HTTP client for API communication
- **Android Security Crypto**: Encrypted shared preferences
- **Coroutines**: Asynchronous programming
- **Material 3**: Design system

## Security Considerations

- All sensitive data (tokens, store IDs) are stored using encrypted shared preferences
- Network communication uses HTTPS
- Row Level Security (RLS) is enabled on the database
- Input validation is implemented on both client and server side

## Usage

1. **Login**: Enter email and password to authenticate
2. **Register**: Create a new store owner account
3. **Forgot Password**: Request password reset email
4. **Dashboard**: View store information and logout

The app automatically handles session persistence and will redirect to the dashboard if the user is already logged in. 