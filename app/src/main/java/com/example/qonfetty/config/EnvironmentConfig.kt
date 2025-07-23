package com.example.qonfetty.config

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EnvironmentConfig(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "environment_config",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // Supabase Configuration
    suspend fun getSupabaseHost(): String = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_SUPABASE_HOST, DEFAULT_SUPABASE_HOST) ?: DEFAULT_SUPABASE_HOST
    }
    
    suspend fun setSupabaseHost(host: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_SUPABASE_HOST, host).apply()
    }
    
    suspend fun getSupabasePort(): Int = withContext(Dispatchers.IO) {
        encryptedPrefs.getInt(KEY_SUPABASE_PORT, DEFAULT_SUPABASE_PORT)
    }
    
    suspend fun setSupabasePort(port: Int) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putInt(KEY_SUPABASE_PORT, port).apply()
    }
    
    suspend fun getSupabaseDatabase(): String = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_SUPABASE_DATABASE, DEFAULT_SUPABASE_DATABASE) ?: DEFAULT_SUPABASE_DATABASE
    }
    
    suspend fun setSupabaseDatabase(database: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_SUPABASE_DATABASE, database).apply()
    }
    
    suspend fun getSupabaseUser(): String = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_SUPABASE_USER, DEFAULT_SUPABASE_USER) ?: DEFAULT_SUPABASE_USER
    }
    
    suspend fun setSupabaseUser(user: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_SUPABASE_USER, user).apply()
    }
    
    suspend fun getSupabaseAnonKey(): String = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_SUPABASE_ANON_KEY, "") ?: ""
    }
    
    suspend fun setSupabaseAnonKey(key: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_SUPABASE_ANON_KEY, key).apply()
    }
    
    suspend fun getSupabaseServiceKey(): String = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_SUPABASE_SERVICE_KEY, "") ?: ""
    }
    
    suspend fun setSupabaseServiceKey(key: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_SUPABASE_SERVICE_KEY, key).apply()
    }
    
    // Helper method to get the full Supabase URL
    suspend fun getSupabaseUrl(): String = withContext(Dispatchers.IO) {
        "https://${getSupabaseHost()}"
    }
    
    // Helper method to get the database connection string
    suspend fun getDatabaseConnectionString(): String = withContext(Dispatchers.IO) {
        "postgresql://${getSupabaseUser()}:${getSupabaseServiceKey()}@${getSupabaseHost()}:${getSupabasePort()}/${getSupabaseDatabase()}"
    }
    
    // Initialize with default values
    suspend fun initializeWithDefaults() = withContext(Dispatchers.IO) {
        // Always set the default values - no need to check if they exist
        setSupabaseHost(DEFAULT_SUPABASE_HOST)
        setSupabasePort(DEFAULT_SUPABASE_PORT)
        setSupabaseDatabase(DEFAULT_SUPABASE_DATABASE)
        setSupabaseUser(DEFAULT_SUPABASE_USER)
        // You can set your anon key here directly
        setSupabaseAnonKey("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhuZnl3cXJkcWNzbG9rb2xoeGhqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMyODE3OTAsImV4cCI6MjA2ODg1Nzc5MH0.cP30Ao0RL2L3BCInDEY1Aai0WbEdPgn-SjmGv_wxquU")
    }
    
    companion object {
        private const val KEY_SUPABASE_HOST = "supabase_host"
        private const val KEY_SUPABASE_PORT = "supabase_port"
        private const val KEY_SUPABASE_DATABASE = "supabase_database"
        private const val KEY_SUPABASE_USER = "supabase_user"
        private const val KEY_SUPABASE_ANON_KEY = "supabase_anon_key"
        private const val KEY_SUPABASE_SERVICE_KEY = "supabase_service_key"
        
        // Default values from your provided configuration
        private const val DEFAULT_SUPABASE_HOST = "xnfywqrdqcslokolhxhj.supabase.co"
        private const val DEFAULT_SUPABASE_PORT = 5432
        private const val DEFAULT_SUPABASE_DATABASE = "postgres"
        private const val DEFAULT_SUPABASE_USER = "postgres"
    }
} 