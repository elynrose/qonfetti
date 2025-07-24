package com.example.qonfetty.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qonfetty.data.SessionStorage
import com.example.qonfetty.data.SupabaseApi
import com.example.qonfetty.data.Reward
import com.example.qonfetty.data.RewardClaim
import com.example.qonfetty.nfc.NfcManager
import com.example.qonfetty.nfc.NfcPointsManager
import com.example.qonfetty.nfc.NfcProcessingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class NfcPointsViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage,
    private val nfcManager: NfcManager
) : ViewModel() {
    
    private val nfcPointsManager = NfcPointsManager(supabaseApi, sessionStorage, nfcManager)
    
    private val _uiState = MutableStateFlow<NfcPointsUiState>(NfcPointsUiState.Idle)
    val uiState: StateFlow<NfcPointsUiState> = _uiState.asStateFlow()
    
    private val _scanResult = MutableStateFlow<NfcProcessingResult?>(null)
    val scanResult: StateFlow<NfcProcessingResult?> = _scanResult.asStateFlow()
    
    /**
     * Process NFC card and handle points/rewards
     */
    fun processNfcCard(tag: android.nfc.Tag) {
        viewModelScope.launch {
            _uiState.value = NfcPointsUiState.Processing
            
            try {
                val result = nfcPointsManager.processNfcCard(tag)
                
                result.fold(
                    onSuccess = { processingResult ->
                        _scanResult.value = processingResult
                        _uiState.value = NfcPointsUiState.Success(processingResult)
                        if (processingResult is com.example.qonfetty.nfc.NfcProcessingResult.Success) {
                            Log.d("NfcPointsViewModel", "NFC processing successful: ${processingResult.customer.name}")
                        } else {
                            Log.d("NfcPointsViewModel", "NFC processing successful but with error result")
                        }
                    },
                    onFailure = { exception ->
                        _uiState.value = NfcPointsUiState.Error(exception.message ?: "Failed to process NFC card")
                        Log.e("NfcPointsViewModel", "NFC processing failed: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = NfcPointsUiState.Error(e.message ?: "Unknown error occurred")
                Log.e("NfcPointsViewModel", "Error processing NFC card: ${e.message}", e)
            }
        }
    }
    
    /**
     * Claim a reward for the customer
     */
    fun claimReward(reward: Reward) {
        viewModelScope.launch {
            _uiState.value = NfcPointsUiState.Processing
            
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    _uiState.value = NfcPointsUiState.Error("Not authenticated or store not found")
                    return@launch
                }
                
                val currentResult = _scanResult.value
                if (currentResult == null || currentResult !is com.example.qonfetty.nfc.NfcProcessingResult.Success) {
                    _uiState.value = NfcPointsUiState.Error("No valid scan result available")
                    return@launch
                }
                
                // Create reward claim record
                val claimResult = createRewardClaim(
                    customerId = currentResult.customer.id!!,
                    rewardId = reward.id,
                    storeId = storeId,
                    authToken = authToken
                )
                
                claimResult.fold(
                    onSuccess = { claim ->
                        Log.d("NfcPointsViewModel", "Reward claimed successfully: ${reward.name}")
                        _uiState.value = NfcPointsUiState.RewardClaimed(reward.name)
                        
                        // Update the scan result to remove the claimed reward
                        val updatedRewards = currentResult.claimableRewards.filter { it.id != reward.id }
                        // Create a new NfcProcessingResult.Success with updated rewards
                        val updatedResult = com.example.qonfetty.nfc.NfcProcessingResult.Success(
                            customer = currentResult.customer,
                            pointsAwarded = currentResult.pointsAwarded,
                            newTotalPoints = currentResult.newTotalPoints,
                            claimableRewards = updatedRewards,
                            nfcCardId = currentResult.nfcCardId
                        )
                        _scanResult.value = updatedResult
                    },
                    onFailure = { exception ->
                        _uiState.value = NfcPointsUiState.Error("Failed to claim reward: ${exception.message}")
                        Log.e("NfcPointsViewModel", "Failed to claim reward: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = NfcPointsUiState.Error(e.message ?: "Unknown error occurred")
                Log.e("NfcPointsViewModel", "Error claiming reward: ${e.message}", e)
            }
        }
    }
    
    /**
     * Create a reward claim record
     */
    private suspend fun createRewardClaim(
        customerId: String,
        rewardId: String,
        storeId: String,
        authToken: String
    ): Result<RewardClaim> {
        return try {
            Log.d("NfcPointsViewModel", "Creating reward claim: customer=$customerId, reward=$rewardId")
            
            val response = supabaseApi.claimReward(customerId, rewardId, storeId, authToken)
            
            response.fold(
                onSuccess = { claim ->
                    Log.d("NfcPointsViewModel", "Reward claim created successfully")
                    Result.success(claim)
                },
                onFailure = { exception ->
                    Log.e("NfcPointsViewModel", "Failed to create reward claim: ${exception.message}")
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsViewModel", "Error creating reward claim: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Reset the UI state
     */
    fun reset() {
        _uiState.value = NfcPointsUiState.Idle
        _scanResult.value = null
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        if (_uiState.value is NfcPointsUiState.Error) {
            _uiState.value = NfcPointsUiState.Idle
        }
    }
}

/**
 * UI state for NFC points processing
 */
sealed class NfcPointsUiState {
    object Idle : NfcPointsUiState()
    object Processing : NfcPointsUiState()
    data class Success(val result: NfcProcessingResult) : NfcPointsUiState()
    data class Error(val message: String) : NfcPointsUiState()
    data class RewardClaimed(val rewardName: String) : NfcPointsUiState()
}

/**
 * Data class for reward claims
 */
@kotlinx.serialization.Serializable
data class RewardClaim(
    val id: String,
    val customer_id: String,
    val reward_id: String,
    val store_id: String,
    val claimed_at: String,
    val is_claimed: Boolean,
    val created_at: String? = null,
    val updated_at: String? = null
) 