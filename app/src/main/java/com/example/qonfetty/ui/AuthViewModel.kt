package com.example.qonfetty.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qonfetty.data.SessionStorage
import com.example.qonfetty.data.SupabaseApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class AuthViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    init {
        checkLoginStatus()
    }
    
    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = sessionStorage.isLoggedIn()
        }
    }
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting login for email: $email")
            _uiState.value = AuthUiState.Loading
            
            supabaseApi.login(email, password)
                .onSuccess { authResponse ->
                    Log.d("AuthViewModel", "Login successful: $authResponse")
                    // Save auth token and user ID
                    authResponse.access_token?.let { sessionStorage.saveAuthToken(it) }
                    authResponse.user?.let { user ->
                        sessionStorage.saveUserId(user.id)
                        
                        // Fetch store ID for the user
                        authResponse.access_token?.let { token ->
                            supabaseApi.getStoreByOwnerId(user.id, token)
                                .onSuccess { store ->
                                    store?.let { 
                                        sessionStorage.saveStoreId(it.id)
                                    }
                                    _uiState.value = AuthUiState.Success("Login successful")
                                    _isLoggedIn.value = true
                                }
                                .onFailure { error ->
                                    Log.e("AuthViewModel", "Failed to fetch store: ${error.message}", error)
                                    _uiState.value = AuthUiState.Error("Failed to fetch store: ${error.message}")
                                }
                        }
                    }
                }
                .onFailure { error ->
                    Log.e("AuthViewModel", "Login failed: ${error.message}", error)
                    _uiState.value = AuthUiState.Error("Login failed: ${error.message}")
                }
        }
    }
    
    fun register(email: String, password: String) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "Starting registration for email: $email")
            _uiState.value = AuthUiState.Loading
            
            supabaseApi.register(email, password)
                .onSuccess { authResponse ->
                    Log.d("AuthViewModel", "Registration successful: $authResponse")
                    // Save auth token and user ID
                    authResponse.access_token?.let { sessionStorage.saveAuthToken(it) }
                    authResponse.user?.let { user ->
                        sessionStorage.saveUserId(user.id)
                        
                        _uiState.value = AuthUiState.Success("Registration successful")
                        _isLoggedIn.value = true
                    }
                }
                .onFailure { error ->
                    Log.e("AuthViewModel", "Registration failed: ${error.message}", error)
                    _uiState.value = AuthUiState.Error("Registration failed: ${error.message}")
                }
        }
    }
    
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            supabaseApi.forgotPassword(email)
                .onSuccess {
                    _uiState.value = AuthUiState.Success("Password reset email sent")
                }
                .onFailure { error ->
                    _uiState.value = AuthUiState.Error("Failed to send reset email: ${error.message}")
                }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            sessionStorage.clearSession()
            _isLoggedIn.value = false
            _uiState.value = AuthUiState.Initial
        }
    }
    
    fun clearError() {
        _uiState.value = AuthUiState.Initial
    }
    
    override fun onCleared() {
        super.onCleared()
        supabaseApi.close()
    }
}

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
} 