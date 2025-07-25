package com.example.qonfetty

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qonfetty.config.EnvironmentConfig
import com.example.qonfetty.data.CustomerWithPoints
import com.example.qonfetty.data.DataRefreshManager
import com.example.qonfetty.data.SessionManager
import com.example.qonfetty.data.SessionStorage
import com.example.qonfetty.data.SupabaseApi
import com.example.qonfetty.data.NfcOperationState
import com.example.qonfetty.nfc.NfcManager
import com.example.qonfetty.nfc.NfcWriteManager
import com.example.qonfetty.ui.AuthScreen
import com.example.qonfetty.ui.AuthViewModel
import com.example.qonfetty.ui.CustomerDetailScreen
import com.example.qonfetty.ui.CustomerDetailViewModel
import com.example.qonfetty.ui.CustomerListScreen
import com.example.qonfetty.ui.CustomerViewModel
import com.example.qonfetty.ui.CustomerUiState
import com.example.qonfetty.ui.DashboardScreen
import com.example.qonfetty.ui.DashboardViewModel
import com.example.qonfetty.ui.NfcPointsViewModel
import com.example.qonfetty.ui.NfcPointsUiState
import com.example.qonfetty.ui.theme.QonfettyTheme
import kotlinx.coroutines.launch
import com.example.qonfetty.ui.DashboardUiState
import com.example.qonfetty.ui.RewardsScreen
import com.example.qonfetty.ui.RewardsViewModel
import com.example.qonfetty.ui.StoreSettingsScreen
import com.example.qonfetty.ui.StoreSettingsViewModel
import com.example.qonfetty.ui.ClaimsScreen
import com.example.qonfetty.ui.ClaimsViewModel
import com.example.qonfetty.ui.ClaimedRewardsScreen
import com.example.qonfetty.ui.ClaimedRewardsViewModel
import com.example.qonfetty.ui.AddRewardsScreen
import com.example.qonfetty.ui.AddRewardsViewModel
import com.example.qonfetty.ui.TransactionsScreen
import com.example.qonfetty.util.BiometricAuthManager

class MainActivity : FragmentActivity() {
    
    private var nfcManager: NfcManager? = null
    private var nfcAdapter: NfcAdapter? = null
    
    // Global authentication state for NFC checks
    private var globalIsLoggedIn: Boolean = false
    
    // Callback to set inactivity message from NFC handling
    private var setInactivityMessageCallback: ((String) -> Unit)? = null
    
    // Global ViewModels for NFC handling
    private var globalNfcPointsViewModel: NfcPointsViewModel? = null
    private var globalDashboardViewModel: DashboardViewModel? = null
    private var globalDataRefreshManager: DataRefreshManager? = null
    
    // Global NFC test callback
    private var globalNfcTestCallback: ((android.nfc.Tag) -> Unit)? = null
    
    // Global NFC write callback
    private var globalNfcWriteCallback: ((android.nfc.Tag) -> Unit)? = null
    
    // Global SessionManager for handling session expiration
    private var globalSessionManager: SessionManager? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        // Configure status bar with logo color for Android 15+
        window.statusBarColor = android.graphics.Color.parseColor("#1E3A8A")
        window.navigationBarColor = android.graphics.Color.parseColor("#1E3A8A")
        
        // Set status bar appearance for light icons on dark background
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false
        
        // For Android 15+, try setting the status bar color in the theme
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 15+ specific configuration
            window.statusBarColor = android.graphics.Color.parseColor("#1E3A8A")
            insetsController.isAppearanceLightStatusBars = false
        }
        
        // Initialize NFC manager
        nfcManager = NfcManager(this)
        
        setContent {
            QonfettyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val environmentConfig = remember { EnvironmentConfig(this) }
                    val sessionStorage = remember { SessionStorage(this) }
                    
                    var supabaseApi by remember { mutableStateOf<SupabaseApi?>(null) }
                    var showCustomers by remember { mutableStateOf(false) }
                    var selectedCustomer by remember { mutableStateOf<CustomerWithPoints?>(null) }
                    var showCustomerDetailFromDashboard by remember { mutableStateOf<String?>(null) }
                    var showNfcScanResult by remember { mutableStateOf(false) }
                    var showNfcTest by remember { mutableStateOf(false) }
                    var showRewards by remember { mutableStateOf(false) }
                    var showSettings by remember { mutableStateOf(false) }
                    var showClaims by remember { mutableStateOf<Pair<CustomerWithPoints, com.example.qonfetty.data.Reward>?>(null) }
                    var showClaimedRewards by remember { mutableStateOf<CustomerWithPoints?>(null) }
                    var showAddRewards by remember { mutableStateOf(false) }
                    var showTransactions by remember { mutableStateOf(false) }
                    var inactivityMessage by remember { mutableStateOf<String?>(null) }
                    
                    // Initialize environment configuration
                    LaunchedEffect(Unit) {
                        environmentConfig.initializeWithDefaults()
                    }
                    
                    // Create SupabaseApi when configuration is ready
                    LaunchedEffect(environmentConfig) {
                        val api = SupabaseApi(environmentConfig)
                        supabaseApi = api
                        
                        // Initialize SessionManager for global session monitoring
                        globalSessionManager = SessionManager(sessionStorage, api)
                        
                        // Initialize DataRefreshManager
                        globalDataRefreshManager = DataRefreshManager(api, sessionStorage)
                        
                        // Initialize global ViewModels for NFC handling
                        globalNfcPointsViewModel = NfcPointsViewModel(api, sessionStorage, nfcManager!!)
                        globalDashboardViewModel = DashboardViewModel(api, sessionStorage, nfcManager!!, globalDataRefreshManager, globalSessionManager)
                    }
                    
                    if (supabaseApi != null && globalSessionManager != null) {
                        val biometricAuthManager = remember { BiometricAuthManager(this) }
                        val viewModel: AuthViewModel = viewModel {
                            AuthViewModel(supabaseApi!!, sessionStorage, biometricAuthManager)
                        }
                        
                        val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                        val sessionExpired by globalSessionManager!!.sessionExpired.collectAsStateWithLifecycle()
                        
                        // Handle session expiration globally
                        LaunchedEffect(sessionExpired) {
                            if (sessionExpired) {
                                Log.d("MainActivity", "Session expired detected, logging out user")
                                viewModel.logout()
                                globalSessionManager?.resetSessionExpired()
                            }
                        }
                        
                        // Update global authentication state and manage data refresh
                        LaunchedEffect(isLoggedIn) {
                            globalIsLoggedIn = isLoggedIn
                            globalSessionManager?.updateLoginState(isLoggedIn)
                            
                            // Start/stop data refresh based on login status
                            if (isLoggedIn) {
                                globalDataRefreshManager?.startPeriodicRefresh()
                                Log.d("MainActivity", "Started periodic data refresh")
                            } else {
                                globalDataRefreshManager?.stopPeriodicRefresh()
                                Log.d("MainActivity", "Stopped periodic data refresh")
                            }
                        }
                        
                        // Set up callback to set inactivity message
                        LaunchedEffect(Unit) {
                            setInactivityMessageCallback = { message ->
                                inactivityMessage = message
                            }
                        }
                        
                        // Clear inactivity message when user logs in successfully
                        LaunchedEffect(isLoggedIn) {
                            if (isLoggedIn) {
                                inactivityMessage = null
                            }
                        }
                        
                        // Observe NFC points UI state for global handling
                        val nfcPointsUiState by globalNfcPointsViewModel?.uiState?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(NfcPointsUiState.Idle) }
                        
                        LaunchedEffect(nfcPointsUiState) {
                            when (val state = nfcPointsUiState) {
                                is NfcPointsUiState.Success -> {
                                    showNfcScanResult = true
                                }
                                is NfcPointsUiState.Error -> {
                                    // Handle global NFC errors
                                    if (state.message.contains("Session expired")) {
                                        inactivityMessage = "You were logged out because of inactivity. Please log back in."
                                        viewModel.logout()
                                    }
                                }
                                else -> {}
                            }
                        }
                        
                        when {
                            showAddRewards -> {
                                Log.d("MainActivity", "Navigation: showAddRewards = $showAddRewards")
                                val addRewardsViewModel: AddRewardsViewModel = viewModel {
                                    AddRewardsViewModel(supabaseApi!!, sessionStorage)
                                }
                                
                                AddRewardsScreen(
                                    viewModel = addRewardsViewModel,
                                    onBack = { 
                                        Log.d("MainActivity", "AddRewardsScreen back button clicked, setting showAddRewards = false")
                                        showAddRewards = false 
                                    }
                                )
                            }
                            showNfcScanResult -> {
                                Log.d("MainActivity", "Navigation: showNfcScanResult = $showNfcScanResult")
                                val scanResult by globalNfcPointsViewModel?.scanResult?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
                                scanResult?.let { result ->
                                    // Show NFC scan result screen
                                    com.example.qonfetty.ui.NfcScanResultScreen(
                                        result = result,
                                        onClaimReward = { reward ->
                                            globalNfcPointsViewModel?.claimReward(reward)
                                        },
                                        onRefresh = {
                                            // For now, just clear the result
                                            globalNfcPointsViewModel?.reset()
                                        }
                                    )
                                }
                            }
                            showNfcTest -> {
                                Log.d("MainActivity", "Navigation: showNfcTest = $showNfcTest")
                                // Show NFC test screen with proper NFC handling
                                var nfcTestResult by remember { mutableStateOf<String?>(null) }
                                var nfcTestError by remember { mutableStateOf<String?>(null) }
                                var isNfcProcessing by remember { mutableStateOf(false) }
                                
                                // Set up NFC test callback
                                LaunchedEffect(showNfcTest) {
                                    Log.d("MainActivity", "LaunchedEffect showNfcTest changed to: $showNfcTest")
                                    if (showNfcTest) {
                                        Log.d("MainActivity", "Setting up NFC test callback")
                                        globalNfcTestCallback = { tag ->
                                            // This will be called when NFC tag is discovered
                                            android.util.Log.d("MainActivity", "NFC test callback triggered")
                                            // Process the NFC tag in a coroutine
                                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                                android.util.Log.d("MainActivity", "Starting NFC test processing")
                                                isNfcProcessing = true
                                                nfcTestError = null
                                                nfcTestResult = null
                                                
                                                try {
                                                    // Try to read member ID from the card
                                                    val memberIdResult = nfcManager!!.readMemberIdFromCard(tag)
                                                    
                                                    memberIdResult.fold(
                                                        onSuccess = { memberId ->
                                                            nfcTestResult = "Successfully read member ID: $memberId"
                                                            
                                                            // Try to find customer with this member ID
                                                            val authToken = sessionStorage.getAuthToken()
                                                            if (authToken != null) {
                                                                val customerResult = supabaseApi!!.findCustomerByMemberId(memberId, authToken)
                                                                customerResult.fold(
                                                                    onSuccess = { customer ->
                                                                        if (customer != null) {
                                                                            nfcTestResult += "\n\nCustomer found: ${customer.name} (${customer.email})"
                                                                        } else {
                                                                            nfcTestResult += "\n\nNo customer found with member ID: $memberId"
                                                                        }
                                                                    },
                                                                    onFailure = { exception ->
                                                                        nfcTestResult += "\n\nError finding customer: ${exception.message}"
                                                                    }
                                                                )
                                                            } else {
                                                                nfcTestResult += "\n\nNot authenticated - cannot search for customer"
                                                            }
                                                        },
                                                        onFailure = { exception ->
                                                            nfcTestError = "Failed to read member ID: ${exception.message}"
                                                        }
                                                    )
                                                } catch (e: Exception) {
                                                    nfcTestError = "Error during NFC test: ${e.message}"
                                                } finally {
                                                    isNfcProcessing = false
                                                }
                                            }
                                        }
                                    } else {
                                        Log.d("MainActivity", "Clearing NFC test callback")
                                        globalNfcTestCallback = null
                                    }
                                }
                                
                                com.example.qonfetty.ui.NfcTestScreen(
                                    supabaseApi = supabaseApi!!,
                                    sessionStorage = sessionStorage,
                                    nfcManager = nfcManager!!,
                                    onBack = { showNfcTest = false },
                                    testResult = nfcTestResult,
                                    errorMessage = nfcTestError,
                                    isProcessing = isNfcProcessing
                                )
                            }
                            showRewards -> {
                                Log.d("MainActivity", "Navigation: showRewards = $showRewards")
                                val rewardsViewModel: RewardsViewModel = viewModel {
                                    RewardsViewModel(supabaseApi!!, sessionStorage, globalDataRefreshManager, globalSessionManager)
                                }
                                
                                RewardsScreen(
                                    viewModel = rewardsViewModel,
                                    onBack = { showRewards = false },
                                    onAddReward = { 
                                        Log.d("MainActivity", "Add reward button clicked, setting showAddRewards = true")
                                        showAddRewards = true 
                                    }
                                )
                            }
                            showSettings -> {
                                val storeSettingsViewModel: StoreSettingsViewModel = viewModel {
                                    StoreSettingsViewModel(supabaseApi!!, sessionStorage, globalSessionManager)
                                }
                                
                                StoreSettingsScreen(
                                    viewModel = storeSettingsViewModel,
                                    onBack = { showSettings = false }
                                )
                            }
                            showClaims != null -> {
                                val (customer, reward) = showClaims!!
                                val claimsViewModel: ClaimsViewModel = viewModel {
                                    ClaimsViewModel(supabaseApi!!, sessionStorage)
                                }
                                
                                ClaimsScreen(
                                    customer = customer,
                                    reward = reward,
                                    viewModel = claimsViewModel,
                                    onBack = { showClaims = null }
                                )
                            }
                            showClaimedRewards != null -> {
                                val customer = showClaimedRewards!!
                                val claimedRewardsViewModel: ClaimedRewardsViewModel = viewModel {
                                    ClaimedRewardsViewModel(supabaseApi!!, sessionStorage)
                                }
                                
                                ClaimedRewardsScreen(
                                    customer = customer,
                                    viewModel = claimedRewardsViewModel,
                                    onBack = { showClaimedRewards = null }
                                )
                            }
                            showTransactions -> {
                                Log.d("MainActivity", "Navigation: showTransactions = $showTransactions")
                                
                                TransactionsScreen(
                                    viewModel = globalDashboardViewModel!!,
                                    onBack = { showTransactions = false }
                                )
                            }
                            selectedCustomer != null -> {
                                val customerDetailViewModel: CustomerDetailViewModel = viewModel {
                                    CustomerDetailViewModel(supabaseApi!!, sessionStorage, globalDataRefreshManager)
                                }
                                
                                // Check if we need to redirect to login due to auth error
                                val nfcOperationState by customerDetailViewModel.nfcOperationState.collectAsStateWithLifecycle()
                                
                                LaunchedEffect(nfcOperationState) {
                                    val currentState = nfcOperationState
                                    if (currentState is NfcOperationState.Error && 
                                        currentState.message.contains("Session expired")) {
                                        inactivityMessage = "You were logged out because of inactivity. Please log back in."
                                        viewModel.logout()
                                        selectedCustomer = null
                                    }
                                }
                                
                                CustomerDetailScreen(
                                    customerWithPoints = selectedCustomer!!,
                                    viewModel = customerDetailViewModel,
                                    onBack = { selectedCustomer = null },
                                    onClaimReward = { customer, reward -> showClaims = customer to reward },
                                    onViewClaimedRewards = { customer -> showClaimedRewards = customer },
                                    nfcManager = nfcManager
                                )
                            }
                            showCustomerDetailFromDashboard != null -> {
                                val customerDetailViewModel: CustomerDetailViewModel = viewModel {
                                    CustomerDetailViewModel(supabaseApi!!, sessionStorage, globalDataRefreshManager)
                                }
                                
                                // Check if we need to redirect to login due to auth error
                                val nfcOperationState by customerDetailViewModel.nfcOperationState.collectAsStateWithLifecycle()
                                
                                LaunchedEffect(nfcOperationState) {
                                    val currentState = nfcOperationState
                                    if (currentState is NfcOperationState.Error && 
                                        currentState.message.contains("Session expired")) {
                                        inactivityMessage = "You were logged out because of inactivity. Please log back in."
                                        viewModel.logout()
                                        showCustomerDetailFromDashboard = null
                                    }
                                }
                                
                                // Load customer data by ID
                                LaunchedEffect(showCustomerDetailFromDashboard) {
                                    val customerId = showCustomerDetailFromDashboard
                                    if (customerId != null) {
                                        customerDetailViewModel.loadCustomerById(customerId)
                                    }
                                }
                                
                                val customerWithPoints by customerDetailViewModel.customerWithPoints.collectAsStateWithLifecycle()
                                
                                customerWithPoints?.let { customer ->
                                    CustomerDetailScreen(
                                        customerWithPoints = customer,
                                        viewModel = customerDetailViewModel,
                                        onBack = { showCustomerDetailFromDashboard = null },
                                        onClaimReward = { customerWithPoints, reward -> showClaims = customerWithPoints to reward },
                                        onViewClaimedRewards = { customerWithPoints -> showClaimedRewards = customerWithPoints },
                                        nfcManager = nfcManager
                                    )
                                }
                            }
                            showCustomers -> {
                                val customerViewModel: CustomerViewModel = viewModel {
                                    CustomerViewModel(supabaseApi!!, sessionStorage, globalDataRefreshManager, globalSessionManager)
                                }
                                
                                // Check if we need to redirect to login due to auth error
                                val customerUiState by customerViewModel.uiState.collectAsStateWithLifecycle()
                                
                                LaunchedEffect(customerUiState) {
                                    val currentState = customerUiState
                                    if (currentState is CustomerUiState.Error && 
                                        currentState.message.contains("Session expired")) {
                                        inactivityMessage = "You were logged out because of inactivity. Please log back in."
                                        viewModel.logout()
                                        showCustomers = false
                                    }
                                }
                                
                                CustomerListScreen(
                                    viewModel = customerViewModel,
                                    onBack = { showCustomers = false },
                                    onCustomerClick = { customer -> selectedCustomer = customer }
                                )
                            }
                            isLoggedIn -> {
                                globalDashboardViewModel?.let { dashboardViewModel ->
                                    // Check for authentication errors in dashboard
                                    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                                    
                                    LaunchedEffect(dashboardUiState) {
                                        val currentState = dashboardUiState
                                        if (currentState is DashboardUiState.ScanError && 
                                            currentState.message.contains("Session expired")) {
                                            inactivityMessage = "You were logged out because of inactivity. Please log back in."
                                            viewModel.logout()
                                        }
                                    }
                                    
                                    DashboardScreen(
                                        viewModel = viewModel,
                                        dashboardViewModel = dashboardViewModel,
                                        onShowCustomers = { showCustomers = true },
                                        onShowNfcTest = { showNfcTest = true },
                                        onShowRewards = { showRewards = true },
                                        onShowSettings = { showSettings = true },
                                        onShowCustomerDetail = { customerId -> showCustomerDetailFromDashboard = customerId },
                                        onShowTransactions = { showTransactions = true }
                                    )
                                }
                            }
                            else -> {
                                AuthScreen(
                                    viewModel = viewModel,
                                    inactivityMessage = inactivityMessage
                                )
                            }
                        }
                    } else {
                        // Loading state
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            // You could add a loading indicator here
                        }
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        nfcManager?.enableNfcForegroundDispatch()
    }
    
    override fun onPause() {
        super.onPause()
        nfcManager?.disableNfcForegroundDispatch()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // nfcManager?.cleanup()
        globalDataRefreshManager?.cleanup()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        android.util.Log.d("MainActivity", "onNewIntent called with action: ${intent.action}")
        
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            
            android.util.Log.d("MainActivity", "NFC intent detected")
            
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let { nfcTag ->
                android.util.Log.d("MainActivity", "NFC tag found: ${nfcTag.id?.joinToString("") { "%02x".format(it) }}")
                
                // Check authentication before processing NFC card
                if (!globalIsLoggedIn) {
                    android.util.Log.w("MainActivity", "NFC card scanned but user not authenticated. Redirecting to login.")
                    // Set inactivity message to inform user why they're being redirected
                    setInactivityMessageCallback?.invoke("Please log in to scan NFC cards and manage customer points.")
                    return
                }
                
                // Check if we're in NFC write mode
                if (NfcWriteManager.isWriting()) {
                    android.util.Log.d("MainActivity", "Calling NFC write callback")
                    NfcWriteManager.getCurrentWriteCallback()?.invoke(nfcTag)
                } else if (globalNfcTestCallback != null) {
                    android.util.Log.d("MainActivity", "Calling NFC test callback")
                    globalNfcTestCallback?.invoke(nfcTag)
                } else {
                    // Clear any lingering test callback if we're not in test mode
                    if (globalNfcTestCallback != null) {
                        android.util.Log.d("MainActivity", "Clearing lingering NFC test callback")
                        globalNfcTestCallback = null
                    }
                    
                    android.util.Log.d("MainActivity", "Processing NFC tag for dashboard")
                    // Process NFC tag with the dashboard ViewModel for automatic handling
                    if (globalDashboardViewModel != null) {
                        android.util.Log.d("MainActivity", "Calling globalDashboardViewModel.processNfcCard")
                        globalDashboardViewModel?.processNfcCard(nfcTag)
                    } else {
                        android.util.Log.e("MainActivity", "globalDashboardViewModel is null! Cannot process NFC card.")
                    }
                }
            } ?: run {
                android.util.Log.e("MainActivity", "No NFC tag found in intent")
            }
        } else {
            android.util.Log.d("MainActivity", "Non-NFC intent: ${intent.action}")
        }
    }
}