package com.example.qonfetty.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qonfetty.data.SupabaseApi
import com.example.qonfetty.data.SessionStorage
import com.example.qonfetty.data.PointsTransaction
import com.example.qonfetty.data.PointsTransactionWithCustomer
import com.example.qonfetty.data.TransactionStats
import com.example.qonfetty.data.DataRefreshManager
import com.example.qonfetty.data.SessionManager
import com.example.qonfetty.data.StoreSettings
import com.example.qonfetty.nfc.NfcManager
import com.example.qonfetty.nfc.NfcPointsManager
import com.example.qonfetty.nfc.NfcProcessingResult
import com.example.qonfetty.util.SessionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

class DashboardViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage,
    private val nfcManager: NfcManager,
    private val dataRefreshManager: DataRefreshManager? = null,
    private val sessionManager: SessionManager? = null
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
    
    private val _transactionStats = MutableStateFlow<TransactionStats?>(null)
    val transactionStats: StateFlow<TransactionStats?> = _transactionStats.asStateFlow()
    
    private val _storeInfo = MutableStateFlow<com.example.qonfetty.data.Store?>(null)
    val storeInfo: StateFlow<com.example.qonfetty.data.Store?> = _storeInfo.asStateFlow()
    
    private val _storeSettings = MutableStateFlow<StoreSettings?>(null)
    val storeSettings: StateFlow<StoreSettings?> = _storeSettings.asStateFlow()
    
    private val _promotionalMode = MutableStateFlow(false)
    val promotionalMode: StateFlow<Boolean> = _promotionalMode.asStateFlow()
    
    private val _weeksBack = MutableStateFlow(1) // Start with last 7 days (1 week)
    val weeksBack: StateFlow<Int> = _weeksBack.asStateFlow()
    
    private val _refreshState = MutableStateFlow<DataRefreshManager.RefreshState>(DataRefreshManager.RefreshState.Idle)
    val refreshState: StateFlow<DataRefreshManager.RefreshState> = _refreshState.asStateFlow()
    
    init {
        // Initial data load
        loadRecentActivity()
        loadTransactionStats()
        loadStoreInfo()
        loadStoreSettings()
        
        // Observe live data from DataRefreshManager if available
        dataRefreshManager?.let { manager ->
            viewModelScope.launch {
                manager.recentActivityData.collect { transactions ->
                    if (transactions.isNotEmpty()) {
                        _recentActivity.value = transactions
                        Log.d("DashboardViewModel", "Received live update: ${transactions.size} transactions")
                    }
                }
            }
            
            // Observe transaction stats live updates
            viewModelScope.launch {
                manager.transactionStatsData.collect { stats ->
                    if (stats != null) {
                        _transactionStats.value = stats
                        Log.d("DashboardViewModel", "Received live transaction stats update")
                    }
                }
            }
            
            // Observe refresh state
            viewModelScope.launch {
                manager.refreshState.collect { state ->
                    _refreshState.value = state
                }
            }
        }
        
        // Observe session manager for login state changes
        sessionManager?.let { manager ->
            viewModelScope.launch {
                manager.isLoggedIn.collect { isLoggedIn ->
                    if (isLoggedIn) {
                        Log.d("DashboardViewModel", "User logged in, refreshing dashboard data")
                        refreshDashboardData()
                    } else {
                        Log.d("DashboardViewModel", "User logged out, clearing dashboard data")
                        clearDashboardData()
                    }
                }
            }
        }
    }
    
    /**
     * Refresh all dashboard data when user switches accounts
     */
    private fun refreshDashboardData() {
        Log.d("DashboardViewModel", "Refreshing dashboard data for new user")
        // Reset promotional mode and weeks back to defaults
        _promotionalMode.value = false
        _weeksBack.value = 1
        // Clear scan history for new user
        _scanHistory.value = emptyList()
        // Load fresh data
        loadRecentActivity()
        loadTransactionStats()
        loadStoreInfo()
        loadStoreSettings()
    }
    
    /**
     * Clear dashboard data when user logs out
     */
    private fun clearDashboardData() {
        Log.d("DashboardViewModel", "Clearing dashboard data")
        _recentActivity.value = emptyList()
        _transactionStats.value = null
        _storeInfo.value = null
        _storeSettings.value = null
        _promotionalMode.value = false
        _weeksBack.value = 1
        _scanHistory.value = emptyList()
        _lastScanResult.value = null
        _uiState.value = DashboardUiState.Idle
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
                            SessionUtils.handleSessionExpiration(sessionManager, exception.message)
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
     * Process NFC card scan from dashboard (without awarding points yet)
     */
    fun processNfcCard(tag: android.nfc.Tag) {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Scanning
            
            try {
                val result = nfcPointsManager.processNfcCardWithoutAwarding(tag, _promotionalMode.value)
                
                result.fold(
                    onSuccess = { processingResult ->
                        when (processingResult) {
                            is com.example.qonfetty.nfc.NfcProcessingResult.Success -> {
                                _lastScanResult.value = processingResult
                                _uiState.value = DashboardUiState.ScanConfirmation(processingResult)
                                Log.d("DashboardViewModel", "NFC scan ready for confirmation: ${processingResult.customer.name}")
                            }
                            is com.example.qonfetty.nfc.NfcProcessingResult.UnregisteredCard -> {
                                // Don't set _lastScanResult for unregistered cards
                                // The dialog will be shown based on the UI state
                                _uiState.value = DashboardUiState.ScanConfirmation(processingResult)
                                Log.d("DashboardViewModel", "Unregistered card detected: ${processingResult.memberId}")
                            }
                            is com.example.qonfetty.nfc.NfcProcessingResult.Error -> {
                                _uiState.value = DashboardUiState.ScanError(processingResult.message)
                                Log.e("DashboardViewModel", "NFC processing error: ${processingResult.message}")
                            }
                        }
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
     * Confirm and award points after user confirms
     */
    fun confirmAndAwardPoints() {
        viewModelScope.launch {
            val currentResult = _lastScanResult.value
            if (currentResult is com.example.qonfetty.nfc.NfcProcessingResult.Success) {
                _uiState.value = DashboardUiState.Scanning
                
                try {
                    val result = nfcPointsManager.awardPointsToCustomer(currentResult)
                    
                    result.fold(
                        onSuccess = { processingResult ->
                            if (processingResult is com.example.qonfetty.nfc.NfcProcessingResult.Success) {
                                // Add to scan history
                                val newActivity = ScanActivity(
                                    customerName = processingResult.customer.name,
                                    memberId = processingResult.customer.memberId ?: "Unknown",
                                    pointsAdded = processingResult.pointsAwarded,
                                    totalPoints = processingResult.newTotalPoints,
                                    timestamp = System.currentTimeMillis(),
                                    type = ScanType.EXISTING_CUSTOMER
                                )
                                
                                val currentHistory = _scanHistory.value.toMutableList()
                                currentHistory.add(0, newActivity) // Add to beginning
                                if (currentHistory.size > 10) { // Keep only last 10 scans
                                    currentHistory.removeAt(currentHistory.size - 1)
                                }
                                _scanHistory.value = currentHistory
                                
                                // Trigger immediate refresh of activity data
                                dataRefreshManager?.triggerRefresh(DataRefreshManager.DataType.ACTIVITY)
                                
                                // Also refresh recent activity from database for immediate update
                                loadRecentActivity()
                                
                                _uiState.value = DashboardUiState.ScanSuccess(processingResult)
                                Log.d("DashboardViewModel", "Points awarded successfully: ${processingResult.customer.name}")
                            } else {
                                _uiState.value = DashboardUiState.ScanError("Unexpected result type")
                            }
                        },
                        onFailure = { exception ->
                            val errorMessage = when {
                                exception.message?.contains("401") == true -> "Session expired. Please log in again."
                                exception.message?.contains("403") == true -> "Access denied. Please check your permissions."
                                else -> exception.message ?: "Failed to award points"
                            }
                            _uiState.value = DashboardUiState.ScanError(errorMessage)
                            Log.e("DashboardViewModel", "Failed to award points: ${exception.message}", exception)
                        }
                    )
                } catch (e: Exception) {
                    _uiState.value = DashboardUiState.ScanError(e.message ?: "Unknown error occurred")
                    Log.e("DashboardViewModel", "Error awarding points: ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Show scan result without awarding points (when user clicks "No")
     */
    fun showScanResultWithoutAwarding() {
        val currentResult = _lastScanResult.value
        if (currentResult is com.example.qonfetty.nfc.NfcProcessingResult.Success) {
            // Change state to show the result without awarding points
            _uiState.value = DashboardUiState.ScanSuccess(currentResult)
            Log.d("DashboardViewModel", "Showing scan result without awarding points for: ${currentResult.customer.name}")
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
     * Load store information
     */
    fun loadStoreInfo() {
        viewModelScope.launch {
            try {
                val authToken = sessionStorage.getAuthToken()
                val userId = sessionStorage.getUserId()
                
                if (authToken == null || userId == null) {
                    Log.w("DashboardViewModel", "Not authenticated, cannot load store info")
                    return@launch
                }
                
                val result = supabaseApi.getStoreByOwnerId(userId, authToken)
                
                result.fold(
                    onSuccess = { store ->
                        Log.d("DashboardViewModel", "Successfully loaded store info: ${store?.name}")
                        _storeInfo.value = store
                    },
                    onFailure = { exception ->
                        Log.e("DashboardViewModel", "Failed to load store info: ${exception.message}", exception)
                        SessionUtils.handleSessionExpiration(sessionManager, exception.message)
                    }
                )
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading store info: ${e.message}", e)
            }
        }
    }
    
    /**
     * Load transaction statistics for dashboard
     */
    fun loadTransactionStats() {
        viewModelScope.launch {
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    Log.w("DashboardViewModel", "Not authenticated or no store ID, cannot load transaction stats")
                    return@launch
                }
                
                val weeksBack = _weeksBack.value
                val endDate = System.currentTimeMillis() - ((weeksBack - 1) * 7 * 24 * 60 * 60 * 1000L) // End date for this week
                val startDate = endDate - (7 * 24 * 60 * 60 * 1000L) // Start date for this week (7 days before end)
                
                Log.d("DashboardViewModel", "🔍 LOAD STATS: weeksBack = $weeksBack")
                Log.d("DashboardViewModel", "🔍 LOAD STATS: startDate = ${java.util.Date(startDate)}")
                Log.d("DashboardViewModel", "🔍 LOAD STATS: endDate = ${java.util.Date(endDate)}")
                
                val result = supabaseApi.getTransactionStatsWithDateRange(storeId, startDate, endDate, authToken)
                
                result.fold(
                    onSuccess = { stats ->
                        Log.d("DashboardViewModel", "Successfully loaded transaction stats: $stats")
                        Log.d("DashboardViewModel", "🔍 UI UPDATE: Setting new transaction stats - Purchases: ${stats.totalPurchases}, Claims: ${stats.totalClaimed}")
                        _transactionStats.value = stats
                        Log.d("DashboardViewModel", "🔍 UI UPDATE: Transaction stats state updated")
                    },
                    onFailure = { exception ->
                        Log.e("DashboardViewModel", "Failed to load transaction stats: ${exception.message}", exception)
                        // Don't treat this as a session expiration error since it might just be no data
                        Log.w("DashboardViewModel", "Transaction stats error (might be no data): ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading transaction stats: ${e.message}", e)
            }
        }
    }
    
    /**
     * Load store settings
     */
    fun loadStoreSettings() {
        viewModelScope.launch {
            try {
                val authToken = sessionStorage.getAuthToken()
                val storeId = sessionStorage.getStoreId()
                
                if (authToken == null || storeId == null) {
                    Log.w("DashboardViewModel", "Not authenticated or no store ID, cannot load store settings")
                    return@launch
                }
                
                val result = supabaseApi.getStoreSettings(storeId, authToken)
                
                result.fold(
                    onSuccess = { settings ->
                        Log.d("DashboardViewModel", "Successfully loaded store settings: ${settings?.storeName}")
                        _storeSettings.value = settings
                    },
                    onFailure = { exception ->
                        Log.e("DashboardViewModel", "Failed to load store settings: ${exception.message}", exception)
                        // Don't treat this as a session expiration error since it might just be no data
                        Log.w("DashboardViewModel", "Store settings error (might be no data): ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e("DashboardViewModel", "Error loading store settings: ${e.message}", e)
            }
        }
    }
    
    /**
     * Clear scan history
     */
    fun clearScanHistory() {
        _scanHistory.value = emptyList()
    }
    
    /**
     * Refresh all dashboard data
     */
    fun refreshDashboard() {
        Log.d("DashboardViewModel", "Refreshing dashboard data")
        loadRecentActivity()
        loadTransactionStats()
        loadStoreInfo()
        loadStoreSettings()
    }
    
    /**
     * Toggle promotional mode
     */
    fun togglePromotionalMode() {
        val currentMode = _promotionalMode.value
        _promotionalMode.value = !currentMode
        Log.d("DashboardViewModel", "Promotional mode toggled: ${!currentMode}")
    }
    
    /**
     * Set date range for filtering analytics
     */
    fun setDateRange(startDate: Long?, endDate: Long?) {
        // This function is no longer needed as date range is handled by weeksBack
        // Keeping it for now, but it will not affect the transaction stats loading
        Log.d("DashboardViewModel", "Date range set (ignored for now): ${startDate?.let { java.util.Date(it) }} to ${endDate?.let { java.util.Date(it) }}")
    }
    
    /**
     * Clear date range filter
     */
    fun clearDateRange() {
        // This function is no longer needed as date range is handled by weeksBack
        Log.d("DashboardViewModel", "Date range cleared (ignored for now)")
    }
    
    /**
     * Navigate to previous week (go back further in time)
     */
    fun goToPreviousWeek() {
        val currentWeeks = _weeksBack.value
        if (currentWeeks < 52) { // Limit to 1 year back
            _weeksBack.value = currentWeeks + 1
            Log.d("DashboardViewModel", "🔍 PREVIOUS WEEK: Navigated to ${_weeksBack.value} weeks back")
            Log.d("DashboardViewModel", "🔍 PREVIOUS WEEK: Current weeks value = ${_weeksBack.value}")
            loadTransactionStats()
        } else {
            Log.d("DashboardViewModel", "🔍 PREVIOUS WEEK: Already at maximum (52 weeks)")
        }
    }
    
    /**
     * Navigate to next week (go forward in time)
     */
    fun goToNextWeek() {
        val currentWeeks = _weeksBack.value
        if (currentWeeks > 1) { // Don't go less than 1 week
            _weeksBack.value = currentWeeks - 1
            Log.d("DashboardViewModel", "🔍 NEXT WEEK: Navigated to ${_weeksBack.value} weeks back")
            Log.d("DashboardViewModel", "🔍 NEXT WEEK: Current weeks value = ${_weeksBack.value}")
            loadTransactionStats()
        } else {
            Log.d("DashboardViewModel", "🔍 NEXT WEEK: Already at minimum (1 week)")
        }
    }
    
    /**
     * Get display text for current week range
     */
    fun getWeekRangeText(): String {
        val weeks = _weeksBack.value
        return when (weeks) {
            1 -> "Last 7 Days"
            2 -> "7-14 Days Ago"
            3 -> "14-21 Days Ago"
            4 -> "21-28 Days Ago"
            else -> "${(weeks - 1) * 7}-${weeks * 7} Days Ago"
        }
    }

    /**
     * Register an unregistered card with user permission
     */
    fun registerUnregisteredCard(memberId: String) {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Scanning
            
            try {
                val result = nfcPointsManager.registerUnregisteredCard(memberId, _promotionalMode.value)
                
                result.fold(
                    onSuccess = { processingResult ->
                        when (processingResult) {
                            is NfcProcessingResult.Success -> {
                                _lastScanResult.value = processingResult
                                _uiState.value = DashboardUiState.ScanSuccess(processingResult)
                                Log.d("DashboardViewModel", "Successfully registered card and awarded points to: ${processingResult.customer.name}")
                            }
                            is NfcProcessingResult.UnregisteredCard -> {
                                _uiState.value = DashboardUiState.ScanError("Failed to register card. Please try again.")
                                Log.e("DashboardViewModel", "Card registration failed - still unregistered")
                            }
                            is NfcProcessingResult.Error -> {
                                _uiState.value = DashboardUiState.ScanError(processingResult.message)
                                Log.e("DashboardViewModel", "Card registration error: ${processingResult.message}")
                            }
                        }
                    },
                    onFailure = { exception ->
                        val errorMessage = when {
                            exception.message?.contains("401") == true -> "Session expired. Please log in again."
                            exception.message?.contains("403") == true -> "Access denied. Please check your permissions."
                            else -> exception.message ?: "Failed to register card"
                        }
                        _uiState.value = DashboardUiState.ScanError(errorMessage)
                        Log.e("DashboardViewModel", "Card registration failed: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.ScanError(e.message ?: "Unknown error occurred")
                Log.e("DashboardViewModel", "Error registering card: ${e.message}", e)
            }
        }
    }
}

/**
 * UI state for dashboard
 */
sealed class DashboardUiState {
    object Idle : DashboardUiState()
    object Scanning : DashboardUiState()
    data class ScanConfirmation(val result: NfcProcessingResult) : DashboardUiState()
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