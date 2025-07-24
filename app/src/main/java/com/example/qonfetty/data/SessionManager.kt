package com.example.qonfetty.data

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SessionManager(
    private val sessionStorage: SessionStorage,
    private val supabaseApi: SupabaseApi
) {
    
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    init {
        checkInitialSessionState()
    }
    
    private fun checkInitialSessionState() {
        coroutineScope.launch {
            val token = sessionStorage.getAuthToken()
            _isLoggedIn.value = token != null
            if (token == null) {
                _sessionExpired.value = true
            }
        }
    }
    
    fun handleSessionExpiry() {
        Log.d("SessionManager", "Session expired, clearing session and notifying UI")
        coroutineScope.launch {
            sessionStorage.clearSession()
            _isLoggedIn.value = false
            _sessionExpired.value = true
        }
    }
    
    fun resetSessionExpired() {
        _sessionExpired.value = false
    }
    
    fun updateLoginState(isLoggedIn: Boolean) {
        _isLoggedIn.value = isLoggedIn
        if (!isLoggedIn) {
            _sessionExpired.value = true
        }
    }
    
    suspend fun validateSession(): Boolean {
        return try {
            val token = sessionStorage.getAuthToken()
            if (token == null) {
                Log.d("SessionManager", "No auth token found")
                handleSessionExpiry()
                return false
            }
            
            // Try a simple API call to validate the token
            val result = supabaseApi.validateToken(token)
            if (result.isSuccess) {
                Log.d("SessionManager", "Session is valid")
                true
            } else {
                Log.d("SessionManager", "Session validation failed: ${result.exceptionOrNull()?.message}")
                handleSessionExpiry()
                false
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Error validating session: ${e.message}", e)
            handleSessionExpiry()
            false
        }
    }
    
    fun cleanup() {
        coroutineScope.launch {
            sessionStorage.clearSession()
        }
    }
} 