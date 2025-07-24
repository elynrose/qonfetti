package com.example.qonfetty.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qonfetty.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

sealed class NfcOperationState {
    object Idle : NfcOperationState()
    object Loading : NfcOperationState()
    data class Success(val message: String) : NfcOperationState()
    data class Error(val message: String) : NfcOperationState()
}

sealed class RewardOperationState {
    object Idle : RewardOperationState()
    object Loading : RewardOperationState()
    data class Success(val message: String) : RewardOperationState()
    data class Error(val message: String) : RewardOperationState()
}

class CustomerDetailViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage
) : ViewModel() {
    
    private val _nfcCards = MutableStateFlow<List<NfcCardResponse>>(emptyList())
    val nfcCards: StateFlow<List<NfcCardResponse>> = _nfcCards.asStateFlow()
    
    private val _nfcOperationState = MutableStateFlow<NfcOperationState>(NfcOperationState.Idle)
    val nfcOperationState: StateFlow<NfcOperationState> = _nfcOperationState.asStateFlow()
    
    private val _totalPoints = MutableStateFlow(0)
    val totalPoints: StateFlow<Int> = _totalPoints.asStateFlow()
    
    private val _claimableRewards = MutableStateFlow<List<Reward>>(emptyList())
    val claimableRewards: StateFlow<List<Reward>> = _claimableRewards.asStateFlow()
    
    private val _rewardOperationState = MutableStateFlow<RewardOperationState>(RewardOperationState.Idle)
    val rewardOperationState: StateFlow<RewardOperationState> = _rewardOperationState.asStateFlow()
    
    fun loadCustomerData(customerId: String) {
        loadCustomerNfcCards(customerId)
        loadCustomerTotalPoints(customerId)
        loadClaimableRewards(customerId)
    }
    
    fun loadCustomerTotalPoints(customerId: String) {
        viewModelScope.launch {
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    return@launch
                }
                
                val result = supabaseApi.getCustomerPoints(customerId, storeId, authToken)
                
                result.fold(
                    onSuccess = { customerPoints ->
                        _totalPoints.value = customerPoints?.points ?: 0
                        Log.d("CustomerDetailViewModel", "Loaded total points: ${customerPoints?.points ?: 0}")
                    },
                    onFailure = { exception ->
                        Log.e("CustomerDetailViewModel", "Failed to load total points: ${exception.message}", exception)
                        _totalPoints.value = 0
                    }
                )
            } catch (e: Exception) {
                Log.e("CustomerDetailViewModel", "Error loading total points: ${e.message}", e)
                _totalPoints.value = 0
            }
        }
    }
    
    fun loadClaimableRewards(customerId: String) {
        viewModelScope.launch {
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    return@launch
                }
                
                // First get current points to determine claimable rewards
                val pointsResult = supabaseApi.getCustomerPoints(customerId, storeId, authToken)
                
                pointsResult.fold(
                    onSuccess = { customerPoints ->
                        val currentPoints = customerPoints?.points ?: 0
                        
                        // Now get claimable rewards based on current points
                        val rewardsResult = supabaseApi.getClaimableRewards(storeId, currentPoints, authToken)
                        
                        rewardsResult.fold(
                            onSuccess = { rewards ->
                                _claimableRewards.value = rewards
                                Log.d("CustomerDetailViewModel", "Loaded ${rewards.size} claimable rewards")
                            },
                            onFailure = { exception ->
                                Log.e("CustomerDetailViewModel", "Failed to load claimable rewards: ${exception.message}", exception)
                                _claimableRewards.value = emptyList()
                            }
                        )
                    },
                    onFailure = { exception ->
                        Log.e("CustomerDetailViewModel", "Failed to get customer points for rewards: ${exception.message}", exception)
                        _claimableRewards.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                Log.e("CustomerDetailViewModel", "Error loading claimable rewards: ${e.message}", e)
                _claimableRewards.value = emptyList()
            }
        }
    }
    
    fun claimReward(reward: Reward, customerId: String) {
        viewModelScope.launch {
            _rewardOperationState.value = RewardOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    _rewardOperationState.value = RewardOperationState.Error("Not authenticated or store not found")
                    return@launch
                }
                
                val result = supabaseApi.claimReward(customerId, reward.id, storeId, authToken)
                
                result.fold(
                    onSuccess = { claim ->
                        _rewardOperationState.value = RewardOperationState.Success("Reward '${reward.name}' claimed successfully!")
                        Log.d("CustomerDetailViewModel", "Reward claimed: ${reward.name}")
                        
                        // Refresh claimable rewards after claiming
                        loadClaimableRewards(customerId)
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(2000)
                        _rewardOperationState.value = RewardOperationState.Idle
                    },
                    onFailure = { exception ->
                        _rewardOperationState.value = RewardOperationState.Error("Failed to claim reward: ${exception.message}")
                        Log.e("CustomerDetailViewModel", "Failed to claim reward: ${exception.message}", exception)
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(3000)
                        _rewardOperationState.value = RewardOperationState.Idle
                    }
                )
            } catch (e: Exception) {
                _rewardOperationState.value = RewardOperationState.Error(e.message ?: "Unknown error")
                Log.e("CustomerDetailViewModel", "Error claiming reward: ${e.message}", e)
                
                // Reset operation state after a delay
                kotlinx.coroutines.delay(3000)
                _rewardOperationState.value = RewardOperationState.Idle
            }
        }
    }
    
    fun loadCustomerNfcCards(customerId: String) {
        viewModelScope.launch {
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    return@launch
                }
                
                // First, let's debug what's in the database
                val debugResult = supabaseApi.getAllNfcCards(authToken)
                debugResult.fold(
                    onSuccess = { allCards ->
                        Log.d("CustomerDetailViewModel", "Debug: Found ${allCards.size} total NFC cards in database")
                    },
                    onFailure = { exception ->
                        Log.e("CustomerDetailViewModel", "Debug: Failed to get all NFC cards: ${exception.message}", exception)
                    }
                )
                
                val result = supabaseApi.getCustomerNfcCards(customerId, authToken)
                
                result.fold(
                    onSuccess = { cards ->
                        _nfcCards.value = cards
                        Log.d("CustomerDetailViewModel", "Loaded ${cards.size} NFC cards for customer: $customerId")
                    },
                    onFailure = { exception ->
                        // Check if it's an authentication error
                        if (exception.message?.contains("401") == true || 
                            exception.message?.contains("Unauthorized") == true) {
                            Log.w("CustomerDetailViewModel", "Authentication failed, clearing session")
                            sessionStorage.clearSession()
                        } else {
                            Log.e("CustomerDetailViewModel", "Failed to load NFC cards: ${exception.message}", exception)
                        }
                        _nfcCards.value = emptyList()
                    }
                )
            } catch (e: Exception) {
                Log.e("CustomerDetailViewModel", "Error loading NFC cards: ${e.message}", e)
                _nfcCards.value = emptyList()
            }
        }
    }
    
    fun registerNfcCard(cardId: String, memberId: String, customerId: String) {
        viewModelScope.launch {
            _nfcOperationState.value = NfcOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _nfcOperationState.value = NfcOperationState.Error("Not authenticated")
                    return@launch
                }
                
                val result = supabaseApi.registerNfcCard(cardId, memberId, customerId, authToken)
                
                result.fold(
                    onSuccess = { registeredCard ->
                        // Add to current list
                        val currentCards = _nfcCards.value.toMutableList()
                        currentCards.add(registeredCard)
                        _nfcCards.value = currentCards
                        
                        _nfcOperationState.value = NfcOperationState.Success("NFC card registered successfully")
                        Log.d("CustomerDetailViewModel", "NFC card registered: ${registeredCard.cardId}")
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(2000)
                        _nfcOperationState.value = NfcOperationState.Idle
                    },
                    onFailure = { exception ->
                        // Check if it's an authentication error
                        if (exception.message?.contains("401") == true || 
                            exception.message?.contains("Unauthorized") == true) {
                            Log.w("CustomerDetailViewModel", "Authentication failed, clearing session")
                            sessionStorage.clearSession()
                            _nfcOperationState.value = NfcOperationState.Error("Session expired. Please login again.")
                        } else {
                            _nfcOperationState.value = NfcOperationState.Error(exception.message ?: "Failed to register NFC card")
                        }
                        Log.e("CustomerDetailViewModel", "Failed to register NFC card: ${exception.message}", exception)
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(3000)
                        _nfcOperationState.value = NfcOperationState.Idle
                    }
                )
            } catch (e: Exception) {
                _nfcOperationState.value = NfcOperationState.Error(e.message ?: "Unknown error")
                Log.e("CustomerDetailViewModel", "Error registering NFC card: ${e.message}", e)
                
                // Reset operation state after a delay
                kotlinx.coroutines.delay(3000)
                _nfcOperationState.value = NfcOperationState.Idle
            }
        }
    }
    
    fun deactivateNfcCard(cardId: String) {
        viewModelScope.launch {
            _nfcOperationState.value = NfcOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _nfcOperationState.value = NfcOperationState.Error("Not authenticated")
                    return@launch
                }
                
                val result = supabaseApi.deactivateNfcCard(cardId, authToken)
                
                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            // Remove from current list
                            val currentCards = _nfcCards.value.toMutableList()
                            currentCards.removeAll { it.cardId == cardId }
                            _nfcCards.value = currentCards
                            
                            _nfcOperationState.value = NfcOperationState.Success("NFC card deactivated successfully")
                            Log.d("CustomerDetailViewModel", "NFC card deactivated: $cardId")
                        } else {
                            _nfcOperationState.value = NfcOperationState.Error("Failed to deactivate NFC card")
                        }
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(2000)
                        _nfcOperationState.value = NfcOperationState.Idle
                    },
                    onFailure = { exception ->
                        // Check if it's an authentication error
                        if (exception.message?.contains("401") == true || 
                            exception.message?.contains("Unauthorized") == true) {
                            Log.w("CustomerDetailViewModel", "Authentication failed, clearing session")
                            sessionStorage.clearSession()
                            _nfcOperationState.value = NfcOperationState.Error("Session expired. Please login again.")
                        } else {
                            _nfcOperationState.value = NfcOperationState.Error(exception.message ?: "Failed to deactivate NFC card")
                        }
                        Log.e("CustomerDetailViewModel", "Failed to deactivate NFC card: ${exception.message}", exception)
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(3000)
                        _nfcOperationState.value = NfcOperationState.Idle
                    }
                )
            } catch (e: Exception) {
                _nfcOperationState.value = NfcOperationState.Error(e.message ?: "Unknown error")
                Log.e("CustomerDetailViewModel", "Error deactivating NFC card: ${e.message}", e)
                
                // Reset operation state after a delay
                kotlinx.coroutines.delay(3000)
                _nfcOperationState.value = NfcOperationState.Idle
            }
        }
    }
    
    fun unlinkNfcCard(cardId: String) {
        viewModelScope.launch {
            _nfcOperationState.value = NfcOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _nfcOperationState.value = NfcOperationState.Error("Not authenticated")
                    return@launch
                }
                
                val result = supabaseApi.deleteNfcCard(cardId, authToken)
                
                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            // Remove from current list
                            val currentCards = _nfcCards.value.toMutableList()
                            currentCards.removeAll { it.cardId == cardId }
                            _nfcCards.value = currentCards
                            
                            _nfcOperationState.value = NfcOperationState.Success("NFC card unlinked successfully")
                            Log.d("CustomerDetailViewModel", "NFC card unlinked: $cardId")
                        } else {
                            _nfcOperationState.value = NfcOperationState.Error("Failed to unlink NFC card")
                        }
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(2000)
                        _nfcOperationState.value = NfcOperationState.Idle
                    },
                    onFailure = { exception ->
                        // Check if it's an authentication error
                        if (exception.message?.contains("401") == true || 
                            exception.message?.contains("Unauthorized") == true) {
                            Log.w("CustomerDetailViewModel", "Authentication failed, clearing session")
                            sessionStorage.clearSession()
                            _nfcOperationState.value = NfcOperationState.Error("Session expired. Please login again.")
                        } else {
                            _nfcOperationState.value = NfcOperationState.Error(exception.message ?: "Failed to unlink NFC card")
                        }
                        Log.e("CustomerDetailViewModel", "Failed to unlink NFC card: ${exception.message}", exception)
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(3000)
                        _nfcOperationState.value = NfcOperationState.Idle
                    }
                )
            } catch (e: Exception) {
                _nfcOperationState.value = NfcOperationState.Error(e.message ?: "Unknown error")
                Log.e("CustomerDetailViewModel", "Error unlinking NFC card: ${e.message}", e)
                
                // Reset operation state after a delay
                kotlinx.coroutines.delay(3000)
                _nfcOperationState.value = NfcOperationState.Idle
            }
        }
    }
} 