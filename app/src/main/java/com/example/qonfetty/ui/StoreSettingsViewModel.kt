package com.example.qonfetty.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qonfetty.data.*
import com.example.qonfetty.util.SessionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.util.UUID

sealed class StoreSettingsUiState {
    object Loading : StoreSettingsUiState()
    data class Success(val settings: StoreSettings?) : StoreSettingsUiState()
    data class Error(val message: String) : StoreSettingsUiState()
}

sealed class StoreSettingsOperationState {
    object Idle : StoreSettingsOperationState()
    object Loading : StoreSettingsOperationState()
    data class Success(val message: String) : StoreSettingsOperationState()
    data class Error(val message: String) : StoreSettingsOperationState()
}

class StoreSettingsViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage,
    private val sessionManager: SessionManager? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<StoreSettingsUiState>(StoreSettingsUiState.Loading)
    val uiState: StateFlow<StoreSettingsUiState> = _uiState.asStateFlow()
    
    private val _operationState = MutableStateFlow<StoreSettingsOperationState>(StoreSettingsOperationState.Idle)
    val operationState: StateFlow<StoreSettingsOperationState> = _operationState.asStateFlow()
    
    private val _settings = MutableStateFlow<StoreSettings?>(null)
    val settings: StateFlow<StoreSettings?> = _settings.asStateFlow()
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    // Hardcoded categories for now
    private val hardcodedCategories = listOf(
        Category(id = "1", name = "Retail"),
        Category(id = "2", name = "Restaurant"),
        Category(id = "3", name = "Coffee Shop"),
        Category(id = "4", name = "Grocery"),
        Category(id = "5", name = "Pharmacy"),
        Category(id = "6", name = "Beauty & Health"),
        Category(id = "7", name = "Electronics"),
        Category(id = "8", name = "Fashion"),
        Category(id = "9", name = "Home & Garden"),
        Category(id = "10", name = "Sports & Fitness"),
        Category(id = "11", name = "Entertainment"),
        Category(id = "12", name = "Automotive"),
        Category(id = "13", name = "Education"),
        Category(id = "14", name = "Professional Services"),
        Category(id = "15", name = "Other")
    )
    
    init {
        Log.d("StoreSettingsViewModel", "StoreSettingsViewModel initialized")
        loadStoreSettings()
        loadCategories()
        Log.d("StoreSettingsViewModel", "Init completed")
    }
    
    fun loadStoreSettings() {
        viewModelScope.launch {
            _uiState.value = StoreSettingsUiState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    _uiState.value = StoreSettingsUiState.Error("Not authenticated or no store")
                    return@launch
                }
                
                val result = supabaseApi.getStoreSettings(storeId, authToken)
                
                result.fold(
                    onSuccess = { settings ->
                        _settings.value = settings
                        _uiState.value = StoreSettingsUiState.Success(settings)
                        Log.d("StoreSettingsViewModel", "Loaded store settings: ${settings != null}")
                    },
                    onFailure = { exception ->
                        Log.e("StoreSettingsViewModel", "Failed to load store settings: ${exception.message}", exception)
                        SessionUtils.handleSessionExpiration(sessionManager, exception.message)
                        _uiState.value = StoreSettingsUiState.Error(exception.message ?: "Failed to load store settings")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = StoreSettingsUiState.Error(e.message ?: "Unknown error")
                Log.e("StoreSettingsViewModel", "Error loading store settings: ${e.message}", e)
            }
        }
    }
    
    fun loadCategories() {
        Log.d("StoreSettingsViewModel", "loadCategories() called")
        // Use hardcoded categories for now
        _categories.value = hardcodedCategories
        Log.d("StoreSettingsViewModel", "Loaded ${hardcodedCategories.size} hardcoded categories")
        Log.d("StoreSettingsViewModel", "Categories state updated: ${_categories.value.size} categories")
    }
    
    fun saveStoreSettings(
        storeName: String,
        category: String,
        email: String,
        phone: String,
        website: String,
        storeLogoUrl: String? = null,
        pointsPerPurchase: Int,
        promotionalEnabled: Boolean,
        promotionPointsPerPurchase: Int,
        openaiApiKey: String,
        googleMapsApiKey: String
    ) {
        viewModelScope.launch {
            _operationState.value = StoreSettingsOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    _operationState.value = StoreSettingsOperationState.Error("Not authenticated or no store")
                    return@launch
                }
                
                val currentSettings = _settings.value
                
                val result = if (currentSettings != null) {
                    // Update existing settings
                    Log.d("StoreSettingsViewModel", "Updating request with values: storeName='$storeName', category='$category', email='$email', phone='$phone', website='$website', pointsPerPurchase=$pointsPerPurchase, promotionalEnabled=$promotionalEnabled, promotionPointsPerPurchase=$promotionPointsPerPurchase")
                    
                    val updateRequest = StoreSettingsUpdateRequest(
                        id = currentSettings.id!!,
                        storeName = storeName.ifEmpty { "" },
                        category = category.ifEmpty { "" },
                        email = email.ifEmpty { "" },
                        phone = phone.ifEmpty { "" },
                        website = website.ifEmpty { "" },
                        storeLogo = storeLogoUrl,
                        pointsPerPurchase = pointsPerPurchase,
                        promotionalEnabled = promotionalEnabled,
                        promotionPointsPerPurchase = promotionPointsPerPurchase,
                        openaiApiKey = openaiApiKey.ifEmpty { "" },
                        googleMapsApiKey = googleMapsApiKey.ifEmpty { "" }
                    )
                    supabaseApi.updateStoreSettings(updateRequest, authToken)
                } else {
                    // Create new settings
                    Log.d("StoreSettingsViewModel", "Creating request with values: storeName='$storeName', category='$category', email='$email', phone='$phone', website='$website', pointsPerPurchase=$pointsPerPurchase, promotionalEnabled=$promotionalEnabled, promotionPointsPerPurchase=$promotionPointsPerPurchase")
                    
                    // Create a Map instead of data class to avoid serialization issues
                    val createRequest = mapOf(
                        "store_name" to (storeName.ifEmpty { "" }),
                        "category" to (category.ifEmpty { "" }),
                        "email" to (email.ifEmpty { "" }),
                        "phone" to (phone.ifEmpty { "" }),
                        "website" to (website.ifEmpty { "" }),
                        "store_logo" to (storeLogoUrl ?: ""),
                        "points_per_purchase" to pointsPerPurchase,
                        "promotional_enabled" to promotionalEnabled,
                        "promotion_points_per_purchase" to promotionPointsPerPurchase,
                        "openai_api_key" to (openaiApiKey.ifEmpty { "" }),
                        "google_maps_api_key" to (googleMapsApiKey.ifEmpty { "" }),
                        "store_id" to storeId
                    )
                    supabaseApi.createStoreSettings(createRequest, storeId, authToken)
                }
                
                result.fold(
                    onSuccess = { settings ->
                        _settings.value = settings
                        _operationState.value = StoreSettingsOperationState.Success("Store settings saved successfully")
                        _uiState.value = StoreSettingsUiState.Success(settings)
                        Log.d("StoreSettingsViewModel", "Store settings saved successfully")
                    },
                    onFailure = { exception ->
                        _operationState.value = StoreSettingsOperationState.Error(exception.message ?: "Failed to save store settings")
                        Log.e("StoreSettingsViewModel", "Failed to save store settings: ${exception.message}", exception)
                        SessionUtils.handleSessionExpiration(sessionManager, exception.message)
                    }
                )
            } catch (e: Exception) {
                _operationState.value = StoreSettingsOperationState.Error(e.message ?: "Unknown error")
                Log.e("StoreSettingsViewModel", "Error saving store settings: ${e.message}", e)
            }
        }
    }
    
    fun uploadStoreLogo(imageBytes: ByteArray, fileName: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    Log.e("StoreSettingsViewModel", "Not authenticated")
                    return@launch
                }
                
                val result = supabaseApi.uploadStoreLogo(imageBytes, fileName, authToken)
                
                result.fold(
                    onSuccess = { logoUrl ->
                        Log.d("StoreSettingsViewModel", "Store logo uploaded successfully: $logoUrl")
                        onSuccess(logoUrl)
                    },
                    onFailure = { exception ->
                        Log.e("StoreSettingsViewModel", "Failed to upload store logo: ${exception.message}", exception)
                        SessionUtils.handleSessionExpiration(sessionManager, exception.message)
                    }
                )
            } catch (e: Exception) {
                Log.e("StoreSettingsViewModel", "Error uploading store logo: ${e.message}", e)
            }
        }
    }
    
    fun clearOperationState() {
        _operationState.value = StoreSettingsOperationState.Idle
    }
} 