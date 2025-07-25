package com.example.qonfetty.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages periodic data refresh and live updates across the app
 */
class DataRefreshManager(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isRefreshing = AtomicBoolean(false)
    
    // Refresh intervals in milliseconds
    companion object {
        const val CUSTOMER_REFRESH_INTERVAL = 30_000L // 30 seconds
        const val DASHBOARD_REFRESH_INTERVAL = 15_000L // 15 seconds
        const val ACTIVITY_REFRESH_INTERVAL = 10_000L // 10 seconds
        const val POINTS_REFRESH_INTERVAL = 20_000L // 20 seconds
        const val REWARDS_REFRESH_INTERVAL = 25_000L // 25 seconds
    }
    
    // State flows for different data types
    private val _customersData = MutableStateFlow<List<CustomerWithPoints>>(emptyList())
    val customersData: StateFlow<List<CustomerWithPoints>> = _customersData.asStateFlow()
    
    private val _recentActivityData = MutableStateFlow<List<PointsTransactionWithCustomer>>(emptyList())
    val recentActivityData: StateFlow<List<PointsTransactionWithCustomer>> = _recentActivityData.asStateFlow()
    
    private val _customerPointsData = MutableStateFlow<Map<String, Int>>(emptyMap())
    val customerPointsData: StateFlow<Map<String, Int>> = _customerPointsData.asStateFlow()
    
    private val _rewardsData = MutableStateFlow<List<Reward>>(emptyList())
    val rewardsData: StateFlow<List<Reward>> = _rewardsData.asStateFlow()
    
    private val _transactionStatsData = MutableStateFlow<TransactionStats?>(null)
    val transactionStatsData: StateFlow<TransactionStats?> = _transactionStatsData.asStateFlow()
    
    // Refresh state
    private val _refreshState = MutableStateFlow<RefreshState>(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()
    
    // Active refresh jobs
    private var customersRefreshJob: Job? = null
    private var dashboardRefreshJob: Job? = null
    private var activityRefreshJob: Job? = null
    private var pointsRefreshJob: Job? = null
    private var rewardsRefreshJob: Job? = null
    private var transactionStatsRefreshJob: Job? = null
    
    sealed class RefreshState {
        object Idle : RefreshState()
        object Refreshing : RefreshState()
        data class Success(val message: String) : RefreshState()
        data class Error(val message: String) : RefreshState()
    }
    
    /**
     * Start all periodic refresh jobs
     */
    fun startPeriodicRefresh() {
        Log.d("DataRefreshManager", "Starting periodic refresh")
        
        // Start customers refresh
        customersRefreshJob = startPeriodicJob(CUSTOMER_REFRESH_INTERVAL) {
            refreshCustomers()
        }
        
        // Start dashboard activity refresh
        activityRefreshJob = startPeriodicJob(ACTIVITY_REFRESH_INTERVAL) {
            refreshRecentActivity()
        }
        
        // Start points refresh
        pointsRefreshJob = startPeriodicJob(POINTS_REFRESH_INTERVAL) {
            refreshCustomerPoints()
        }
        
        // Start rewards refresh
        rewardsRefreshJob = startPeriodicJob(REWARDS_REFRESH_INTERVAL) {
            refreshRewards()
        }
        
        // Start transaction stats refresh
        transactionStatsRefreshJob = startPeriodicJob(DASHBOARD_REFRESH_INTERVAL) {
            refreshTransactionStats()
        }
    }
    
    /**
     * Stop all periodic refresh jobs
     */
    fun stopPeriodicRefresh() {
        Log.d("DataRefreshManager", "Stopping periodic refresh")
        
        customersRefreshJob?.cancel()
        dashboardRefreshJob?.cancel()
        activityRefreshJob?.cancel()
        pointsRefreshJob?.cancel()
        rewardsRefreshJob?.cancel()
        transactionStatsRefreshJob?.cancel()
        
        customersRefreshJob = null
        dashboardRefreshJob = null
        activityRefreshJob = null
        pointsRefreshJob = null
        rewardsRefreshJob = null
        transactionStatsRefreshJob = null
    }
    
    /**
     * Clear all cached data when user switches accounts
     */
    fun clearCachedData() {
        Log.d("DataRefreshManager", "Clearing cached data for new user")
        _customersData.value = emptyList()
        _recentActivityData.value = emptyList()
        _customerPointsData.value = emptyMap()
        _rewardsData.value = emptyList()
        _transactionStatsData.value = null
        _refreshState.value = RefreshState.Idle
    }
    
    /**
     * Manually refresh all data
     */
    suspend fun refreshAllData() {
        if (isRefreshing.getAndSet(true)) {
            Log.d("DataRefreshManager", "Refresh already in progress, skipping")
            return
        }
        
        try {
            _refreshState.value = RefreshState.Refreshing
            
            coroutineScope.launch {
                val results = awaitAll(
                    async { refreshCustomers() },
                    async { refreshRecentActivity() },
                    async { refreshCustomerPoints() },
                    async { refreshTransactionStats() }
                )
                
                val hasErrors = results.any { it.isFailure }
                if (hasErrors) {
                    _refreshState.value = RefreshState.Error("Some data failed to refresh")
                } else {
                    _refreshState.value = RefreshState.Success("All data refreshed successfully")
                }
            }
        } finally {
            isRefreshing.set(false)
        }
    }
    
    /**
     * Refresh customers data
     */
    private suspend fun refreshCustomers(): Result<Unit> {
        return try {
            val authToken = sessionStorage.getAuthToken()
            if (authToken == null) {
                Log.w("DataRefreshManager", "No auth token available for customers refresh")
                return Result.failure(Exception("Not authenticated"))
            }
            
            val result = supabaseApi.getCustomers(authToken)
            result.fold(
                onSuccess = { customers ->
                    _customersData.value = customers
                    Log.d("DataRefreshManager", "Refreshed ${customers.size} customers")
                    Result.success(Unit)
                },
                onFailure = { exception ->
                    Log.e("DataRefreshManager", "Failed to refresh customers: ${exception.message}", exception)
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("DataRefreshManager", "Error refreshing customers: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Refresh recent activity data
     */
    private suspend fun refreshRecentActivity(): Result<Unit> {
        return try {
            val authToken = sessionStorage.getAuthToken()
            val storeId = sessionStorage.getStoreId()
            
            if (authToken == null || storeId == null) {
                Log.w("DataRefreshManager", "No auth token or store ID available for activity refresh")
                return Result.failure(Exception("Not authenticated or no store"))
            }
            
            val result = supabaseApi.getRecentActivityWithCustomerInfo(storeId, authToken)
            result.fold(
                onSuccess = { transactions ->
                    _recentActivityData.value = transactions
                    Log.d("DataRefreshManager", "Refreshed ${transactions.size} recent transactions")
                    Result.success(Unit)
                },
                onFailure = { exception ->
                    Log.e("DataRefreshManager", "Failed to refresh recent activity: ${exception.message}", exception)
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("DataRefreshManager", "Error refreshing recent activity: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Refresh customer points data
     */
    private suspend fun refreshCustomerPoints(): Result<Unit> {
        return try {
            val authToken = sessionStorage.getAuthToken()
            val storeId = sessionStorage.getStoreId()
            
            if (authToken == null || storeId == null) {
                Log.w("DataRefreshManager", "No auth token or store ID available for points refresh")
                return Result.failure(Exception("Not authenticated or no store"))
            }
            
            // Get all customers first
            val customersResult = supabaseApi.getCustomers(authToken)
            customersResult.fold(
                onSuccess = { customers ->
                    val pointsMap = mutableMapOf<String, Int>()
                    
                    // Get points for each customer
                    customers.forEach { customerWithPoints ->
                        customerWithPoints.customer.id?.let { customerId ->
                            val pointsResult = supabaseApi.getCustomerPoints(customerId, storeId, authToken)
                            pointsResult.fold(
                                onSuccess = { customerPoints ->
                                    pointsMap[customerId] = customerPoints?.points ?: 0
                                },
                                onFailure = { exception ->
                                    Log.e("DataRefreshManager", "Failed to get points for customer $customerId: ${exception.message}")
                                    pointsMap[customerId] = 0
                                }
                            )
                        }
                    }
                    
                    _customerPointsData.value = pointsMap
                    Log.d("DataRefreshManager", "Refreshed points for ${pointsMap.size} customers")
                    Result.success(Unit)
                },
                onFailure = { exception ->
                    Log.e("DataRefreshManager", "Failed to get customers for points refresh: ${exception.message}", exception)
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("DataRefreshManager", "Error refreshing customer points: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Refresh rewards data
     */
    private suspend fun refreshRewards(): Result<Unit> {
        return try {
            val authToken = sessionStorage.getAuthToken()
            val storeId = sessionStorage.getStoreId()
            
            if (authToken == null || storeId == null) {
                Log.w("DataRefreshManager", "No auth token or store ID available for rewards refresh")
                return Result.failure(Exception("Not authenticated or no store"))
            }
            
            val result = supabaseApi.getRewards(storeId, authToken)
            result.fold(
                onSuccess = { rewards ->
                    _rewardsData.value = rewards
                    Log.d("DataRefreshManager", "Refreshed ${rewards.size} rewards")
                    Result.success(Unit)
                },
                onFailure = { exception ->
                    Log.e("DataRefreshManager", "Failed to refresh rewards: ${exception.message}", exception)
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("DataRefreshManager", "Error refreshing rewards: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Refresh transaction stats data
     */
    private suspend fun refreshTransactionStats(): Result<Unit> {
        return try {
            val authToken = sessionStorage.getAuthToken()
            val storeId = sessionStorage.getStoreId()
            
            if (authToken == null || storeId == null) {
                Log.w("DataRefreshManager", "No auth token or store ID available for transaction stats refresh")
                return Result.failure(Exception("Not authenticated or no store"))
            }
            
            val result = supabaseApi.getTransactionStats(storeId, authToken)
            result.fold(
                onSuccess = { stats ->
                    _transactionStatsData.value = stats
                    Log.d("DataRefreshManager", "Refreshed transaction stats")
                    Result.success(Unit)
                },
                onFailure = { exception ->
                    Log.e("DataRefreshManager", "Failed to refresh transaction stats: ${exception.message}", exception)
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("DataRefreshManager", "Error refreshing transaction stats: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Start a periodic job with the given interval
     */
    private fun startPeriodicJob(interval: Long, block: suspend () -> Unit): Job {
        return coroutineScope.launch {
            while (isActive) {
                try {
                    block()
                } catch (e: Exception) {
                    Log.e("DataRefreshManager", "Error in periodic job: ${e.message}", e)
                }
                delay(interval)
            }
        }
    }
    
    /**
     * Trigger immediate refresh of specific data type
     */
    fun triggerRefresh(dataType: DataType) {
        coroutineScope.launch {
            when (dataType) {
                DataType.CUSTOMERS -> refreshCustomers()
                DataType.ACTIVITY -> refreshRecentActivity()
                DataType.POINTS -> refreshCustomerPoints()
                DataType.REWARDS -> refreshRewards()
            }
        }
    }
    
    enum class DataType {
        CUSTOMERS, ACTIVITY, POINTS, REWARDS
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopPeriodicRefresh()
        coroutineScope.cancel()
    }
} 