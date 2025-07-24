package com.example.qonfetty.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qonfetty.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import java.util.UUID

sealed class AddRewardsUiState {
    object Idle : AddRewardsUiState()
    object Loading : AddRewardsUiState()
    data class Success(val message: String) : AddRewardsUiState()
    data class Error(val message: String) : AddRewardsUiState()
}

class AddRewardsViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AddRewardsUiState>(AddRewardsUiState.Idle)
    val uiState: StateFlow<AddRewardsUiState> = _uiState.asStateFlow()
    
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()
    
    init {
        loadCategories()
    }
    
    /**
     * Load categories from the database
     */
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    Log.e("AddRewardsViewModel", "Not authenticated")
                    return@launch
                }
                
                val result = supabaseApi.getCategories(authToken)
                result.fold(
                    onSuccess = { categories ->
                        _categories.value = categories
                        Log.d("AddRewardsViewModel", "Loaded ${categories.size} categories")
                    },
                    onFailure = { exception ->
                        Log.e("AddRewardsViewModel", "Failed to load categories: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                Log.e("AddRewardsViewModel", "Error loading categories: ${e.message}", e)
            }
        }
    }
    
    /**
     * Create a new reward with image
     */
    fun createReward(
        name: String,
        description: String?,
        pointsRequired: Int,
        imageBytes: ByteArray?,
        imageFileName: String?,
        price: Double? = null,
        quantity: Int? = null,
        category: String? = null,
        isShared: Boolean = false
    ) {
        Log.d("AddRewardsViewModel", "Creating reward: $name, points: $pointsRequired, price: $price, quantity: $quantity, category: $category, isShared: $isShared")
        viewModelScope.launch {
            _uiState.value = AddRewardsUiState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    _uiState.value = AddRewardsUiState.Error("Not authenticated or no store")
                    return@launch
                }
                
                // First upload the image if provided
                var photoUrl: String? = null
                if (imageBytes != null && imageFileName != null) {
                    Log.d("AddRewardsViewModel", "Uploading image: $imageFileName")
                    val uploadResult = supabaseApi.uploadRewardImage(imageBytes, imageFileName, authToken)
                    uploadResult.fold(
                        onSuccess = { url ->
                            photoUrl = url
                            Log.d("AddRewardsViewModel", "Image uploaded successfully: $url")
                        },
                        onFailure = { exception ->
                            _uiState.value = AddRewardsUiState.Error("Failed to upload image: ${exception.message}")
                            Log.e("AddRewardsViewModel", "Failed to upload image: ${exception.message}", exception)
                            return@launch
                        }
                    )
                }
                
                // Create reward data with all fields
                val rewardData = mutableMapOf<String, String>()
                rewardData["id"] = UUID.randomUUID().toString()
                rewardData["name"] = name
                rewardData["description"] = description ?: ""
                rewardData["points_required"] = pointsRequired.toString()
                rewardData["store_id"] = storeId
                rewardData["is_active"] = true.toString()
                
                // Add photo URL if uploaded successfully
                photoUrl?.let { url ->
                    rewardData["photo"] = url
                    Log.d("AddRewardsViewModel", "Adding photo URL to reward: $url")
                }
                
                // Add price if provided
                price?.let { priceValue ->
                    rewardData["price"] = priceValue.toString()
                    Log.d("AddRewardsViewModel", "Adding price to reward: $priceValue")
                }
                
                // Add quantity if provided
                quantity?.let { quantityValue ->
                    rewardData["quantity"] = quantityValue.toString()
                    Log.d("AddRewardsViewModel", "Adding quantity to reward: $quantityValue")
                }
                
                // Add category if provided
                category?.let { categoryValue ->
                    rewardData["category"] = categoryValue
                    Log.d("AddRewardsViewModel", "Adding category to reward: $categoryValue")
                }
                
                // Add is_shared
                rewardData["is_shared"] = isShared.toString()
                Log.d("AddRewardsViewModel", "Adding is_shared to reward: $isShared")
                
                val result = supabaseApi.createReward(rewardData, authToken)
                
                result.fold(
                    onSuccess = { createdReward ->
                        _uiState.value = AddRewardsUiState.Success("Reward '$name' created successfully!")
                        Log.d("AddRewardsViewModel", "Created reward: ${createdReward.name}")
                    },
                    onFailure = { exception ->
                        _uiState.value = AddRewardsUiState.Error(exception.message ?: "Failed to create reward")
                        Log.e("AddRewardsViewModel", "Failed to create reward: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = AddRewardsUiState.Error(e.message ?: "Unknown error")
                Log.e("AddRewardsViewModel", "Error creating reward: ${e.message}", e)
            }
        }
    }
    
    /**
     * Clear the current state
     */
    fun clearState() {
        _uiState.value = AddRewardsUiState.Idle
    }
} 