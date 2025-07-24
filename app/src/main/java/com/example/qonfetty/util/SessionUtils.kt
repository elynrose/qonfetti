package com.example.qonfetty.util

import android.util.Log
import com.example.qonfetty.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object SessionUtils {
    
    /**
     * Check if an API error indicates session expiration
     */
    fun isSessionExpiredError(errorMessage: String?): Boolean {
        return errorMessage?.contains("Session expired", ignoreCase = true) == true ||
               errorMessage?.contains("401", ignoreCase = true) == true ||
               errorMessage?.contains("Unauthorized", ignoreCase = true) == true ||
               errorMessage?.contains("Invalid token", ignoreCase = true) == true ||
               errorMessage?.contains("Token expired", ignoreCase = true) == true
    }
    
    /**
     * Handle session expiration by notifying the SessionManager
     */
    fun handleSessionExpiration(sessionManager: SessionManager?, errorMessage: String?) {
        if (isSessionExpiredError(errorMessage)) {
            Log.d("SessionUtils", "Session expiration detected: $errorMessage")
            sessionManager?.handleSessionExpiry()
        }
    }
    
    /**
     * Validate session before making API calls
     */
    suspend fun validateSessionBeforeApiCall(
        sessionManager: SessionManager?,
        coroutineScope: CoroutineScope,
        onSessionValid: () -> Unit,
        onSessionInvalid: () -> Unit = {}
    ) {
        sessionManager?.let { manager ->
            coroutineScope.launch {
                val isValid = manager.validateSession()
                if (isValid) {
                    onSessionValid()
                } else {
                    Log.d("SessionUtils", "Session validation failed")
                    onSessionInvalid()
                }
            }
        } ?: run {
            // If no session manager, proceed with the API call
            onSessionValid()
        }
    }
} 