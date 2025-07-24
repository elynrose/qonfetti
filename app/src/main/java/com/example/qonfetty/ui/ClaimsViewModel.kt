package com.example.qonfetty.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qonfetty.data.*
import com.example.qonfetty.data.Transaction
import com.example.qonfetty.data.ClaimRewardRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

sealed class ClaimsUiState {
    object Idle : ClaimsUiState()
    object Loading : ClaimsUiState()
    data class Success(val message: String) : ClaimsUiState()
    data class Error(val message: String) : ClaimsUiState()
}

class ClaimsViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ClaimsUiState>(ClaimsUiState.Idle)
    val uiState: StateFlow<ClaimsUiState> = _uiState.asStateFlow()
    
    /**
     * Claim a reward for a customer
     */
    fun claimReward(customerId: String, rewardId: String, purchaseAmount: Double, description: String? = null) {
        viewModelScope.launch {
            _uiState.value = ClaimsUiState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    _uiState.value = ClaimsUiState.Error("Not authenticated. Please login again.")
                    return@launch
                }
                
                val request = ClaimRewardRequest(
                    customerId = customerId,
                    rewardId = rewardId,
                    amount = purchaseAmount,
                    description = description
                )
                
                val result = supabaseApi.claimReward(request, storeId, authToken)
                
                result.fold(
                    onSuccess = { transaction ->
                        Log.d("ClaimsViewModel", "Reward claimed successfully: ${transaction.id}")
                        
                        // Now deduct points from the customer
                        deductPointsFromCustomer(request.customerId, storeId, authToken, request.rewardId)
                    },
                    onFailure = { exception ->
                        Log.e("ClaimsViewModel", "Failed to claim reward: ${exception.message}", exception)
                        _uiState.value = ClaimsUiState.Error(exception.message ?: "Failed to claim reward")
                    }
                )
            } catch (e: Exception) {
                Log.e("ClaimsViewModel", "Error claiming reward: ${e.message}", e)
                _uiState.value = ClaimsUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    /**
     * Deduct points from customer after successful reward claim
     */
    private suspend fun deductPointsFromCustomer(customerId: String, storeId: String, authToken: String, rewardId: String) {
        try {
            // First get the reward details to know how many points to deduct
            val rewardResult = supabaseApi.getReward(rewardId, authToken)
            val reward = rewardResult.getOrNull()
            if (reward == null) {
                _uiState.value = ClaimsUiState.Error("Reward not found")
                return
            }
            
            // Get current customer points
            val currentPointsResult = supabaseApi.getCustomerPoints(customerId, storeId, authToken)
            
            currentPointsResult.fold(
                onSuccess = { customerPoints ->
                    if (customerPoints != null) {
                        // Calculate new points after deduction
                        val newPoints = customerPoints.points - reward.pointsRequired
                        
                        // Update customer points
                        val updateResult = supabaseApi.updateCustomerPoints(customerId, storeId, newPoints, authToken)
                        
                        updateResult.fold(
                            onSuccess = { updatedPoints ->
                                Log.d("ClaimsViewModel", "Points deducted successfully. New total: ${updatedPoints.points}")
                                _uiState.value = ClaimsUiState.Success("Reward claimed successfully! Points deducted from customer.")
                            },
                            onFailure = { exception ->
                                Log.e("ClaimsViewModel", "Failed to update customer points: ${exception.message}", exception)
                                _uiState.value = ClaimsUiState.Error("Reward claimed but failed to deduct points: ${exception.message}")
                            }
                        )
                    } else {
                        Log.e("ClaimsViewModel", "Customer points not found")
                        _uiState.value = ClaimsUiState.Error("Customer points not found")
                    }
                },
                onFailure = { exception ->
                    Log.e("ClaimsViewModel", "Failed to get customer points: ${exception.message}", exception)
                    _uiState.value = ClaimsUiState.Error("Failed to get customer points: ${exception.message}")
                }
            )
        } catch (e: Exception) {
            Log.e("ClaimsViewModel", "Error deducting points: ${e.message}", e)
            _uiState.value = ClaimsUiState.Error("Error deducting points: ${e.message}")
        }
    }
    
    /**
     * Clear the current state
     */
    fun clearState() {
        _uiState.value = ClaimsUiState.Idle
    }
} 