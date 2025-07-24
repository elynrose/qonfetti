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

sealed class RewardsUiState {
    object Loading : RewardsUiState()
    data class Success(val rewards: List<Reward>) : RewardsUiState()
    data class Error(val message: String) : RewardsUiState()
}

sealed class RewardsOperationState {
    object Idle : RewardsOperationState()
    object Loading : RewardsOperationState()
    data class Success(val message: String) : RewardsOperationState()
    data class Error(val message: String) : RewardsOperationState()
}

class RewardsViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage,
    private val dataRefreshManager: DataRefreshManager? = null
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RewardsUiState>(RewardsUiState.Loading)
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()
    
    private val _operationState = MutableStateFlow<RewardsOperationState>(RewardsOperationState.Idle)
    val operationState: StateFlow<RewardsOperationState> = _operationState.asStateFlow()
    
    private val _rewards = MutableStateFlow<List<Reward>>(emptyList())
    val rewards: StateFlow<List<Reward>> = _rewards.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    init {
        loadRewards()
    }
    
    fun loadRewards() {
        viewModelScope.launch {
            _uiState.value = RewardsUiState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    _uiState.value = RewardsUiState.Error("Not authenticated or no store")
                    return@launch
                }
                
                val result = supabaseApi.getRewards(storeId, authToken)
                
                result.fold(
                    onSuccess = { rewards ->
                        _rewards.value = rewards
                        _uiState.value = RewardsUiState.Success(rewards)
                        Log.d("RewardsViewModel", "Loaded ${rewards.size} rewards")
                    },
                    onFailure = { exception ->
                        if (exception.message?.contains("401") == true || 
                            exception.message?.contains("Unauthorized") == true) {
                            Log.w("RewardsViewModel", "Authentication failed, clearing session")
                            sessionStorage.clearSession()
                            _uiState.value = RewardsUiState.Error("Session expired. Please login again.")
                        } else {
                            _uiState.value = RewardsUiState.Error(exception.message ?: "Failed to load rewards")
                        }
                        Log.e("RewardsViewModel", "Failed to load rewards: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = RewardsUiState.Error(e.message ?: "Unknown error")
                Log.e("RewardsViewModel", "Error loading rewards: ${e.message}", e)
            }
        }
    }
    
    fun createRewardWithImage(
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
        Log.d("RewardsViewModel", "createRewardWithImage called with name: $name, points: $pointsRequired, hasImage: ${imageBytes != null}")
        viewModelScope.launch {
            _operationState.value = RewardsOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    _operationState.value = RewardsOperationState.Error("Not authenticated or no store")
                    return@launch
                }
                
                // First upload the image if provided
                var photoUrl: String? = null
                if (imageBytes != null && imageFileName != null) {
                    Log.d("RewardsViewModel", "Uploading image: $imageFileName")
                    val uploadResult = supabaseApi.uploadRewardImage(imageBytes, imageFileName, authToken)
                    uploadResult.fold(
                        onSuccess = { url ->
                            photoUrl = url
                            Log.d("RewardsViewModel", "Image uploaded successfully: $url")
                        },
                        onFailure = { exception ->
                            _operationState.value = RewardsOperationState.Error("Failed to upload image: ${exception.message}")
                            Log.e("RewardsViewModel", "Failed to upload image: ${exception.message}", exception)
                            return@launch
                        }
                    )
                }
                
                // Create reward with only the fields that exist in the current database
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
                    Log.d("RewardsViewModel", "Adding photo URL to reward: $url")
                }
                
                val result = supabaseApi.createReward(rewardData, authToken)
                
                result.fold(
                    onSuccess = { createdReward ->
                        _operationState.value = RewardsOperationState.Success("Reward created successfully")
                        loadRewards() // Refresh the list
                        Log.d("RewardsViewModel", "Created reward: ${createdReward.name}")
                    },
                    onFailure = { exception ->
                        _operationState.value = RewardsOperationState.Error(exception.message ?: "Failed to create reward")
                        Log.e("RewardsViewModel", "Failed to create reward: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _operationState.value = RewardsOperationState.Error(e.message ?: "Unknown error")
                Log.e("RewardsViewModel", "Error creating reward: ${e.message}", e)
            }
        }
    }
    
    fun createReward(
        name: String,
        description: String?,
        pointsRequired: Int,
        photo: String? = null,
        price: Double? = null,
        quantity: Int? = null,
        category: String? = null,
        isShared: Boolean = false
    ) {
        Log.d("RewardsViewModel", "createReward called with name: $name, points: $pointsRequired")
        viewModelScope.launch {
            _operationState.value = RewardsOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    _operationState.value = RewardsOperationState.Error("Not authenticated or no store")
                    return@launch
                }
                
                // Create reward with only the fields that exist in the current database
                val rewardData = mutableMapOf<String, String>()
                rewardData["id"] = UUID.randomUUID().toString()
                rewardData["name"] = name
                rewardData["description"] = description ?: ""
                rewardData["points_required"] = pointsRequired.toString()
                rewardData["store_id"] = storeId
                rewardData["is_active"] = true.toString()
                
                // Add photo URL if provided
                photo?.let { photoUrl ->
                    rewardData["photo"] = photoUrl
                    Log.d("RewardsViewModel", "Adding photo URL to reward: $photoUrl")
                }
                
                val result = supabaseApi.createReward(rewardData, authToken)
                
                result.fold(
                    onSuccess = { createdReward ->
                        _operationState.value = RewardsOperationState.Success("Reward created successfully")
                        loadRewards() // Refresh the list
                        Log.d("RewardsViewModel", "Created reward: ${createdReward.name}")
                    },
                    onFailure = { exception ->
                        _operationState.value = RewardsOperationState.Error(exception.message ?: "Failed to create reward")
                        Log.e("RewardsViewModel", "Failed to create reward: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _operationState.value = RewardsOperationState.Error(e.message ?: "Unknown error")
                Log.e("RewardsViewModel", "Error creating reward: ${e.message}", e)
            }
        }
    }
    
    fun updateReward(reward: Reward) {
        Log.d("RewardsViewModel", "updateReward called with reward: ${reward.name}")
        viewModelScope.launch {
            _operationState.value = RewardsOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _operationState.value = RewardsOperationState.Error("Not authenticated")
                    return@launch
                }
                // Only send supported fields
                val rewardData = mutableMapOf<String, String>()
                rewardData["id"] = reward.id
                rewardData["name"] = reward.name
                rewardData["description"] = reward.description ?: ""
                rewardData["points_required"] = reward.pointsRequired.toString()
                rewardData["store_id"] = reward.storeId
                rewardData["is_active"] = reward.isActive.toString()
                rewardData["category"] = reward.category ?: "general"
                rewardData["is_shared"] = reward.isShared.toString()
                
                // Add photo URL if it exists
                reward.photo?.let { photoUrl ->
                    rewardData["photo"] = photoUrl
                    Log.d("RewardsViewModel", "Adding photo URL to reward update: $photoUrl")
                }
                
                val result = supabaseApi.updateReward(rewardData, authToken)
                
                result.fold(
                    onSuccess = { updatedReward ->
                        _operationState.value = RewardsOperationState.Success("Reward updated successfully")
                        loadRewards() // Refresh the list
                        Log.d("RewardsViewModel", "Updated reward: ${reward.name}")
                    },
                    onFailure = { exception ->
                        _operationState.value = RewardsOperationState.Error(exception.message ?: "Failed to update reward")
                        Log.e("RewardsViewModel", "Failed to update reward: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _operationState.value = RewardsOperationState.Error(e.message ?: "Unknown error")
                Log.e("RewardsViewModel", "Error updating reward: ${e.message}", e)
            }
        }
    }
    
    fun deleteReward(rewardId: String) {
        viewModelScope.launch {
            _operationState.value = RewardsOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _operationState.value = RewardsOperationState.Error("Not authenticated")
                    return@launch
                }
                
                val result = supabaseApi.deleteReward(rewardId, authToken)
                
                result.fold(
                    onSuccess = {
                        _operationState.value = RewardsOperationState.Success("Reward deleted successfully")
                        loadRewards() // Refresh the list
                        Log.d("RewardsViewModel", "Deleted reward: $rewardId")
                    },
                    onFailure = { exception ->
                        _operationState.value = RewardsOperationState.Error(exception.message ?: "Failed to delete reward")
                        Log.e("RewardsViewModel", "Failed to delete reward: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _operationState.value = RewardsOperationState.Error(e.message ?: "Unknown error")
                Log.e("RewardsViewModel", "Error deleting reward: ${e.message}", e)
            }
        }
    }
    
    fun uploadImage(imageBytes: ByteArray, fileName: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _operationState.value = RewardsOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _operationState.value = RewardsOperationState.Error("Not authenticated")
                    return@launch
                }
                
                val result = supabaseApi.uploadRewardImage(imageBytes, fileName, authToken)
                
                result.fold(
                    onSuccess = { imageUrl ->
                        _operationState.value = RewardsOperationState.Success("Image uploaded successfully")
                        onSuccess(imageUrl)
                        Log.d("RewardsViewModel", "Image uploaded: $imageUrl")
                    },
                    onFailure = { exception ->
                        _operationState.value = RewardsOperationState.Error(exception.message ?: "Failed to upload image")
                        Log.e("RewardsViewModel", "Failed to upload image: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _operationState.value = RewardsOperationState.Error(e.message ?: "Unknown error")
                Log.e("RewardsViewModel", "Error uploading image: ${e.message}", e)
            }
        }
    }
    
    fun searchRewards(query: String) {
        _searchQuery.value = query
        filterRewards()
    }
    
    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
        filterRewards()
    }
    
    private fun filterRewards() {
        val query = _searchQuery.value.lowercase()
        val category = _selectedCategory.value
        
        val filteredRewards = _rewards.value.filter { reward ->
            val matchesQuery = query.isEmpty() || 
                reward.name.lowercase().contains(query) ||
                reward.description?.lowercase()?.contains(query) == true ||
                reward.category?.lowercase()?.contains(query) == true
            
            val matchesCategory = category == null || reward.category == category
            
            matchesQuery && matchesCategory
        }
        
        _uiState.value = RewardsUiState.Success(filteredRewards)
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _uiState.value = RewardsUiState.Success(_rewards.value)
    }
    
    fun clearOperationState() {
        _operationState.value = RewardsOperationState.Idle
    }
    
    fun getCategories(): List<String> {
        return _rewards.value.mapNotNull { it.category }.distinct().sorted()
    }
} 