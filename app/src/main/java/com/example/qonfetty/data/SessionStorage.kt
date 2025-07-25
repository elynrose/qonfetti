package com.example.qonfetty.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionStorage(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    suspend fun saveAuthToken(token: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }
    
    suspend fun getAuthToken(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_AUTH_TOKEN, null)
    }
    
    suspend fun saveStoreId(storeId: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_STORE_ID, storeId).apply()
    }
    
    suspend fun getStoreId(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_STORE_ID, null)
    }
    
    suspend fun saveUserId(userId: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_USER_ID, userId).apply()
    }
    
    suspend fun getUserId(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_USER_ID, null)
    }
    
    suspend fun clearSession() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().clear().apply()
    }
    
    suspend fun isLoggedIn(): Boolean = withContext(Dispatchers.IO) {
        getAuthToken() != null
    }
    
    suspend fun saveCredentials(email: String, password: String) = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().putString(KEY_EMAIL, email).putString(KEY_PASSWORD, password).apply()
    }

    suspend fun getStoredEmail(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_EMAIL, null)
    }

    suspend fun getStoredPassword(): String? = withContext(Dispatchers.IO) {
        encryptedPrefs.getString(KEY_PASSWORD, null)
    }

    suspend fun clearCredentials() = withContext(Dispatchers.IO) {
        encryptedPrefs.edit().remove(KEY_EMAIL).remove(KEY_PASSWORD).apply()
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_STORE_ID = "store_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
    }
} 