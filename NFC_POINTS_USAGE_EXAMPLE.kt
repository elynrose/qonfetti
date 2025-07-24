// Example of how to integrate the NFC Points System into your app

import android.nfc.Tag
import androidx.compose.runtime.*
import androidx.lifecycle.viewModel
import com.example.qonfetty.data.SupabaseApi
import com.example.qonfetty.data.SessionStorage
import com.example.qonfetty.nfc.NfcManager
import com.example.qonfetty.ui.NfcPointsViewModel
import com.example.qonfetty.ui.NfcPointsUiState
import com.example.qonfetty.ui.NfcScanResultScreen

/**
 * Example Compose screen that integrates NFC points processing
 */
@Composable
fun NfcPointsScreen(
    supabaseApi: SupabaseApi,
    sessionStorage: SessionStorage,
    nfcManager: NfcManager,
    modifier: Modifier = Modifier
) {
    val viewModel: NfcPointsViewModel = viewModel {
        NfcPointsViewModel(supabaseApi, sessionStorage, nfcManager)
    }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scanResult by viewModel.scanResult.collectAsStateWithLifecycle()
    
    // State for NFC tag
    var nfcTag by remember { mutableStateOf<Tag?>(null) }
    
    // Process NFC card when tag is discovered
    LaunchedEffect(nfcTag) {
        nfcTag?.let { tag ->
            viewModel.processNfcCard(tag)
            nfcTag = null // Reset after processing
        }
    }
    
    // Handle NFC tag discovery (this would be called from MainActivity)
    fun onNfcTagDiscovered(tag: Tag) {
        nfcTag = tag
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is NfcPointsUiState.Idle -> {
                // Show NFC scan prompt
                NfcScanPrompt(
                    onScanRequested = { /* Trigger NFC scan */ }
                )
            }
            
            is NfcPointsUiState.Processing -> {
                // Show loading indicator
                LoadingIndicator()
            }
            
            is NfcPointsUiState.Success -> {
                // Show scan results
                scanResult?.let { result ->
                    NfcScanResultScreen(
                        result = result,
                        onBack = { viewModel.reset() },
                        onClaimReward = { reward ->
                            viewModel.claimReward(reward)
                        }
                    )
                }
            }
            
            is NfcPointsUiState.Error -> {
                // Show error message
                ErrorMessage(
                    message = uiState.message,
                    onDismiss = { viewModel.clearError() }
                )
            }
            
            is NfcPointsUiState.RewardClaimed -> {
                // Show success message
                SuccessMessage(
                    message = "Reward '${uiState.rewardName}' claimed successfully!",
                    onDismiss = { viewModel.reset() }
                )
            }
        }
    }
}

/**
 * Example of how to handle NFC tag discovery in MainActivity
 */
/*
class MainActivity : ComponentActivity() {
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var nfcPointsViewModel: NfcPointsViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        // Initialize ViewModel
        nfcPointsViewModel = viewModel {
            NfcPointsViewModel(supabaseApi, sessionStorage, nfcManager)
        }
        
        setContent {
            NfcPointsScreen(
                supabaseApi = supabaseApi,
                sessionStorage = sessionStorage,
                nfcManager = nfcManager
            )
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let { nfcTag ->
                // Process the NFC tag
                nfcPointsViewModel.processNfcCard(nfcTag)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Enable NFC foreground dispatch
        nfcAdapter.enableForegroundDispatch(
            this,
            getPendingIntent(),
            getIntentFilters(),
            getTechLists()
        )
    }
    
    override fun onPause() {
        super.onPause()
        // Disable NFC foreground dispatch
        nfcAdapter.disableForegroundDispatch(this)
    }
}
*/

/**
 * Example of how to use the NfcPointsManager directly
 */
/*
suspend fun processNfcCardExample(tag: Tag) {
    val nfcPointsManager = NfcPointsManager(supabaseApi, sessionStorage, nfcManager)
    
    val result = nfcPointsManager.processNfcCard(tag)
    
    result.fold(
        onSuccess = { processingResult ->
            println("Customer: ${processingResult.customer.name}")
            println("Member ID: ${processingResult.memberId}")
            println("Previous Points: ${processingResult.previousPoints}")
            println("Current Points: ${processingResult.currentPoints}")
            println("Points Added: ${processingResult.pointsAdded}")
            println("Claimable Rewards: ${processingResult.claimableRewards.size}")
            
            // Handle rewards
            processingResult.claimableRewards.forEach { reward ->
                println("Available Reward: ${reward.name} (${reward.pointsRequired} points)")
            }
        },
        onFailure = { exception ->
            println("Error processing NFC card: ${exception.message}")
        }
    )
}
*/

/**
 * Example of how to set up rewards in the database
 */
/*
-- Create rewards for a store
INSERT INTO rewards (name, description, points_required, store_id, is_active) VALUES
('Free Coffee', 'Get a free coffee of your choice', 50, 'store-uuid-here', true),
('10% Discount', 'Get 10% off your next purchase', 100, 'store-uuid-here', true),
('Free Pastry', 'Get a free pastry with any purchase', 25, 'store-uuid-here', true),
('VIP Status', 'Unlock VIP benefits and exclusive offers', 500, 'store-uuid-here', true);
*/

/**
 * Example of how to test the system
 */
/*
// Test data setup
val testCustomer = Customer(
    id = "customer-uuid",
    name = "John Doe",
    email = "john@example.com",
    phone = "+1234567890",
    memberId = "1234567890"
)

val testStoreId = "store-uuid"

// Test NFC processing
val testTag = // Mock NFC tag
val result = nfcPointsManager.processNfcCard(testTag)

// Verify results
assert(result.isSuccess)
result.getOrNull()?.let { processingResult ->
    assert(processingResult.customer.name == "John Doe")
    assert(processingResult.currentPoints == 1) // First visit
    assert(processingResult.claimableRewards.isEmpty()) // No rewards yet
}

// Test multiple visits
repeat(50) {
    val result2 = nfcPointsManager.processNfcCard(testTag)
    assert(result2.isSuccess)
}

// Check final points
val finalResult = nfcPointsManager.processNfcCard(testTag)
finalResult.getOrNull()?.let { processingResult ->
    assert(processingResult.currentPoints == 51) // 50 visits + 1
    assert(processingResult.claimableRewards.isNotEmpty()) // Should have rewards now
}
*/ 