package com.example.qonfetty.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qonfetty.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

sealed class ClaimedRewardsUiState {
    object Idle : ClaimedRewardsUiState()
    object Loading : ClaimedRewardsUiState()
    data class Error(val message: String) : ClaimedRewardsUiState()
}

data class ClaimedRewardWithTransaction(
    val reward: Reward,
    val transaction: Transaction
)

class ClaimedRewardsViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ClaimedRewardsUiState>(ClaimedRewardsUiState.Idle)
    val uiState: StateFlow<ClaimedRewardsUiState> = _uiState.asStateFlow()
    
    private val _claimedRewards = MutableStateFlow<List<ClaimedRewardWithTransaction>>(emptyList())
    val claimedRewards: StateFlow<List<ClaimedRewardWithTransaction>> = _claimedRewards.asStateFlow()
    
    /**
     * Load claimed rewards for a customer
     */
    fun loadClaimedRewards(customerId: String) {
        viewModelScope.launch {
            _uiState.value = ClaimedRewardsUiState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    _uiState.value = ClaimedRewardsUiState.Error("Not authenticated. Please login again.")
                    return@launch
                }
                
                // Get transactions for this customer
                val transactionsResult = supabaseApi.getCustomerTransactions(customerId, storeId, authToken)
                
                transactionsResult.fold(
                    onSuccess = { transactions ->
                        // Filter for reward_claim transactions and get reward details
                        val claimedRewardsList = mutableListOf<ClaimedRewardWithTransaction>()
                        
                        for (transaction in transactions) {
                            if (transaction.transactionType == "reward_claim" && transaction.rewardId != null) {
                                // Get reward details
                                val rewardResult = supabaseApi.getReward(transaction.rewardId, authToken)
                                rewardResult.fold(
                                    onSuccess = { reward ->
                                        claimedRewardsList.add(
                                            ClaimedRewardWithTransaction(
                                                reward = reward,
                                                transaction = transaction
                                            )
                                        )
                                    },
                                    onFailure = { exception ->
                                        Log.w("ClaimedRewardsViewModel", "Failed to get reward ${transaction.rewardId}: ${exception.message}")
                                    }
                                )
                            }
                        }
                        
                        // Sort by claim date (newest first)
                        val sortedRewards = claimedRewardsList.sortedByDescending { 
                            it.transaction.createdAt 
                        }
                        
                        _claimedRewards.value = sortedRewards
                        _uiState.value = ClaimedRewardsUiState.Idle
                        
                        Log.d("ClaimedRewardsViewModel", "Loaded ${sortedRewards.size} claimed rewards")
                    },
                    onFailure = { exception ->
                        Log.e("ClaimedRewardsViewModel", "Failed to load claimed rewards: ${exception.message}", exception)
                        _uiState.value = ClaimedRewardsUiState.Error(exception.message ?: "Failed to load claimed rewards")
                    }
                )
            } catch (e: Exception) {
                Log.e("ClaimedRewardsViewModel", "Error loading claimed rewards: ${e.message}", e)
                _uiState.value = ClaimedRewardsUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    /**
     * Clear the current state
     */
    fun clearState() {
        _uiState.value = ClaimedRewardsUiState.Idle
    }
} 