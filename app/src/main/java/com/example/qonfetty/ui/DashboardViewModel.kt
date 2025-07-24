package com.example.qonfetty.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qonfetty.data.SessionStorage
import com.example.qonfetty.data.SupabaseApi
import com.example.qonfetty.data.PointsTransaction
import com.example.qonfetty.data.PointsTransactionWithCustomer
import com.example.qonfetty.nfc.NfcManager
import com.example.qonfetty.nfc.NfcPointsManager
import com.example.qonfetty.nfc.NfcProcessingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class DashboardViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage,
    private val nfcManager: NfcManager
) : ViewModel() {
    
    private val nfcPointsManager = NfcPointsManager(supabaseApi, sessionStorage, nfcManager)
    
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Idle)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val _lastScanResult = MutableStateFlow<NfcProcessingResult?>(null)
    val lastScanResult: StateFlow<NfcProcessingResult?> = _lastScanResult.asStateFlow()
    
    private val _scanHistory = MutableStateFlow<List<ScanActivity>>(emptyList())
    val scanHistory: StateFlow<List<ScanActivity>> = _scanHistory.asStateFlow()
    
    private val _recentActivity = MutableStateFlow<List<PointsTransactionWithCustomer>>(emptyList())
    val recentActivity: StateFlow<List<PointsTransactionWithCustomer>> = _recentActivity.asStateFlow()
    
    init {
        loadRecentActivity()
    }
    
    /**
     * Load recent activity from database
     */
    fun loadRecentActivity() {
        viewModelScope.launch {
            try {
                Log.d("DashboardViewModel", "Starting to load recent activity")
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    Log.w("DashboardViewModel", "Not authenticated, cannot load recent activity")
                    return@launch
                }
                
                // Try to get store ID from session storage first
                val storeId = sessionStorage.getStoreId()
                if (storeId != null) {
                    Log.d("DashboardViewModel", "Using store ID from session: $storeId")
                    val result = supabaseApi.getRecentActivityWithCustomerInfo(storeId, authToken)
                    
                    result.fold(
                        onSuccess = { transactions ->
                            Log.d("DashboardViewModel", "Successfully loaded ${transactions.size} recent transactions with customer info")
                            Log.d("DashboardViewModel", "Transaction details: ${transactions.map { "${it.customerName ?: it.customerId.take(8)}... (${it.pointsAwarded} pts)" }}")
                            _recentActivity.value = transactions
                            Log.d("DashboardViewModel", "Updated _recentActivity.value to ${_recentActivity.value.size} transactions")
                        },
                        onFailure = { exception ->
                            Log.e("DashboardViewModel", "Failed to load recent activity: ${exception.message}", exception)
                        }
                    )
                } else {
                    Log.d("DashboardViewModel", "No store ID in session, trying fallback method")
                    // Fallback to the original method
                    val result = supabaseApi.getRecentActivity(authToken)
                    
                    result.fold(
                        onSuccess = { transactions ->
                            Log.d("DashboardViewModel", "Successfully loaded ${transactions.size} recent transactions (fallback)")
                            // Convert to PointsTransactionWithCustomer format
                            val enrichedTransactions = transactions.map { transaction ->
                                PointsTransactionWithCustomer(
                                    id = transaction.id,
                                    customerId = transaction.customerId,
                                    storeId = transaction.storeId,
                                    nfcCardId = transaction.nfcCardId,
                                    pointsAwarded = transaction.pointsAwarded,
                                    previousPoints = transaction.previousPoints,
                                    newPoints = transaction.newPoints,
                                    transactionType = transaction.transactionType,
                                    description = transaction.description,
                                    createdAt = transaction.createdAt,
                                    customerName = null, // Will be null in fallback
                                    customerEmail = null,
                                    customerPhone = null
                                )
                            }
                            _recentActivity.value = enrichedTransactions
                        },
                        onFailure = { exception ->
                            Log.e("DashboardViewModel", "Failed to load recent activity: ${exception.message}", exception)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading recent activity: ${e.message}", e)
            }
        }
    }
    
    /**
     * Process NFC card scan from dashboard
     */
    fun processNfcCard(tag: android.nfc.Tag) {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Scanning
            
            try {
                val result = nfcPointsManager.processNfcCard(tag)
                
                result.fold(
                    onSuccess = { processingResult ->
                        _lastScanResult.value = processingResult
                        
                        // Add to scan history
                        val newActivity = ScanActivity(
                            customerName = processingResult.customer.name,
                            memberId = processingResult.memberId,
                            pointsAdded = processingResult.pointsAdded,
                            totalPoints = processingResult.currentPoints,
                            timestamp = System.currentTimeMillis(),
                            type = if (processingResult.previousPoints == 0) ScanType.NEW_CUSTOMER else ScanType.EXISTING_CUSTOMER
                        )
                        
                        val currentHistory = _scanHistory.value.toMutableList()
                        currentHistory.add(0, newActivity) // Add to beginning
                        if (currentHistory.size > 10) { // Keep only last 10 scans
                            currentHistory.removeAt(currentHistory.size - 1)
                        }
                        _scanHistory.value = currentHistory
                        
                        // Refresh recent activity from database
                        loadRecentActivity()
                        
                        _uiState.value = DashboardUiState.ScanSuccess(processingResult)
                        Log.d("DashboardViewModel", "NFC scan successful: ${processingResult.customer.name}")
                    },
                    onFailure = { exception ->
                        val errorMessage = when {
                            exception.message?.contains("401") == true -> "Session expired. Please log in again."
                            exception.message?.contains("403") == true -> "Access denied. Please check your permissions."
                            else -> exception.message ?: "Failed to process NFC card"
                        }
                        _uiState.value = DashboardUiState.ScanError(errorMessage)
                        Log.e("DashboardViewModel", "NFC scan failed: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.ScanError(e.message ?: "Unknown error occurred")
                Log.e("DashboardViewModel", "Error processing NFC card: ${e.message}", e)
            }
        }
    }
    
    /**
     * Clear scan result and return to idle state
     */
    fun clearScanResult() {
        _uiState.value = DashboardUiState.Idle
        _lastScanResult.value = null
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        if (_uiState.value is DashboardUiState.ScanError) {
            _uiState.value = DashboardUiState.Idle
        }
    }
    
    /**
     * Clear scan history
     */
    fun clearScanHistory() {
        _scanHistory.value = emptyList()
    }
}

/**
 * UI state for dashboard
 */
sealed class DashboardUiState {
    object Idle : DashboardUiState()
    object Scanning : DashboardUiState()
    data class ScanSuccess(val result: NfcProcessingResult) : DashboardUiState()
    data class ScanError(val message: String) : DashboardUiState()
}

/**
 * Represents a scan activity for the dashboard
 */
data class ScanActivity(
    val customerName: String,
    val memberId: String,
    val pointsAdded: Int,
    val totalPoints: Int,
    val timestamp: Long,
    val type: ScanType
)

/**
 * Type of scan activity
 */
enum class ScanType {
    NEW_CUSTOMER,
    EXISTING_CUSTOMER
} 