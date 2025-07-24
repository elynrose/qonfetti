package com.example.qonfetty.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.qonfetty.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qonfetty.data.PointsTransaction
import com.example.qonfetty.data.PointsTransactionWithCustomer
import com.example.qonfetty.data.TransactionStats
import com.example.qonfetty.nfc.NfcProcessingResult
import com.example.qonfetty.ui.components.LiveDataIndicator
import com.example.qonfetty.ui.theme.StatusBarSpacer
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    viewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    onShowCustomers: () -> Unit = {},
    onShowNfcTest: () -> Unit = {},
    onShowRewards: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    onShowCustomerDetail: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val scanHistory by dashboardViewModel.scanHistory.collectAsStateWithLifecycle()
    val recentActivity by dashboardViewModel.recentActivity.collectAsStateWithLifecycle()
    val lastScanResult by dashboardViewModel.lastScanResult.collectAsStateWithLifecycle()
    val transactionStats by dashboardViewModel.transactionStats.collectAsStateWithLifecycle()
    val storeInfo by dashboardViewModel.storeInfo.collectAsStateWithLifecycle()
    val storeSettings by dashboardViewModel.storeSettings.collectAsStateWithLifecycle()
    val refreshState by dashboardViewModel.refreshState.collectAsStateWithLifecycle()
    
    // Pull to refresh state
    val isRefreshing = dashboardState is DashboardUiState.Scanning
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { 
            dashboardViewModel.refreshDashboard()
        }
    )
    
    // Content with proper padding and pull to refresh
    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Add top spacing to avoid status bar
            StatusBarSpacer()
            
            // Live data indicator
            LiveDataIndicator(
                refreshState = refreshState,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Header with NFC status and menu
            DashboardHeader(
                dashboardState = dashboardState,
                onClearError = { dashboardViewModel.clearError() },
                onShowCustomers = onShowCustomers,
                onShowNfcTest = onShowNfcTest,
                onShowRewards = onShowRewards,
                onShowSettings = onShowSettings,
                onLogout = { viewModel.logout() }
            )
            
            // NFC Scan Result Card (if available)
            lastScanResult?.let { result ->
                NfcScanResultCard(
                    result = result,
                    onDismiss = { dashboardViewModel.clearScanResult() },
                    onShowCustomerDetail = onShowCustomerDetail,
                    pointsAwarded = dashboardState !is DashboardUiState.ScanConfirmation
                )
            }
            
            // Store Information Card
            StoreInfoCard(
                uiState = uiState, 
                storeInfo = storeInfo, 
                storeSettings = storeSettings,
                promotionalMode = dashboardViewModel.promotionalMode.collectAsStateWithLifecycle().value,
                onTogglePromotionalMode = { dashboardViewModel.togglePromotionalMode() },
                weeksBack = dashboardViewModel.weeksBack.collectAsStateWithLifecycle().value,
                weekRangeText = dashboardViewModel.getWeekRangeText(),
                onPreviousWeek = { dashboardViewModel.goToPreviousWeek() },
                onNextWeek = { dashboardViewModel.goToNextWeek() },
                transactionStats = transactionStats
            )
            
            // Transaction Statistics Card
            TransactionStatsCard(
                stats = transactionStats ?: com.example.qonfetty.data.TransactionStats(
                    totalPurchases = 0.0,
                    totalClaimed = 0.0,
                    totalTransactions = 0,
                    totalPointsEarned = 0,
                    totalPointsUsed = 0
                ),
                onRefresh = { dashboardViewModel.refreshDashboard() }
            )
            
            // Recent Activity
            RecentActivityCard(
                recentActivity = recentActivity,
                onClearHistory = { dashboardViewModel.clearScanHistory() },
                onViewAll = {}
            )
        }
        
        // Pull to refresh indicator
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        
        // Confirmation Dialog for NFC Points Award
        when (val state = dashboardState) {
            is DashboardUiState.ScanConfirmation -> {
                val result = state.result
                if (result is com.example.qonfetty.nfc.NfcProcessingResult.Success) {
                    AlertDialog(
                        onDismissRequest = { dashboardViewModel.showScanResultWithoutAwarding() },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = "Award Points",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Award Points")
                            }
                        },
                        text = {
                            Column {
                                Text(
                                    text = "Award ${result.pointsAwarded} point(s) to ${result.customer.name}?",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Current points: ${result.newTotalPoints - result.pointsAwarded}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "New total: ${result.newTotalPoints}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = { dashboardViewModel.confirmAndAwardPoints() }
                            ) {
                                Text("Yes, Award Points")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { dashboardViewModel.showScanResultWithoutAwarding() }
                            ) {
                                Text("No, Don't Award")
                            }
                        }
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun DashboardHeader(
    dashboardState: DashboardUiState,
    onClearError: () -> Unit,
    onShowCustomers: () -> Unit,
    onShowNfcTest: () -> Unit,
    onShowRewards: () -> Unit,
    onShowSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Store Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ready for NFC scans",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // NFC Status Icon
                when (dashboardState) {
                    is DashboardUiState.Scanning -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "scanning")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )
                        
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Scanning NFC",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    is DashboardUiState.ScanConfirmation -> {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Awaiting confirmation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    is DashboardUiState.ScanSuccess -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Scan successful",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    is DashboardUiState.ScanError -> {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Scan error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Ready for NFC",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Hamburger Menu
                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Store Settings") },
                            leadingIcon = { 
                                Icon(Icons.Filled.Settings, contentDescription = null)
                            },
                            onClick = {
                                onShowSettings()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Manage Customers") },
                            leadingIcon = { 
                                Icon(Icons.Filled.Person, contentDescription = null)
                            },
                            onClick = {
                                onShowCustomers()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Manage Rewards") },
                            leadingIcon = { 
                                Icon(Icons.Filled.Star, contentDescription = null)
                            },
                            onClick = {
                                onShowRewards()
                                expanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Write to Card") },
                            leadingIcon = { 
                                Icon(Icons.Filled.Star, contentDescription = null)
                            },
                            onClick = {
                                onShowNfcTest()
                                expanded = false
                            }
                        )
                        Divider()
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            leadingIcon = { 
                                Icon(Icons.Filled.ExitToApp, contentDescription = null)
                            },
                            onClick = {
                                onLogout()
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        
        // Error message
        if (dashboardState is DashboardUiState.ScanError) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dashboardState.message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearError) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcScanResultCard(
    result: com.example.qonfetty.nfc.NfcProcessingResult,
    onDismiss: () -> Unit,
    onShowCustomerDetail: (String) -> Unit,
    pointsAwarded: Boolean = true
) {
    when (result) {
        is com.example.qonfetty.nfc.NfcProcessingResult.Success -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = result.customer.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Member ID",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = result.customer.memberId ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (pointsAwarded) "Points Added" else "Points Would Add",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = if (pointsAwarded) "+${result.pointsAwarded}" else "0",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Points",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (pointsAwarded) "${result.newTotalPoints}" else "${result.newTotalPoints - result.pointsAwarded}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (result.claimableRewards.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${result.claimableRewards.size} rewards available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { onShowCustomerDetail(result.customer.id!!) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text("View Customer Details")
                            }
                        }
                    }
                }
            }
        }
        is com.example.qonfetty.nfc.NfcProcessingResult.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreInfoCard(
    uiState: com.example.qonfetty.ui.AuthUiState,
    storeInfo: com.example.qonfetty.data.Store?,
    storeSettings: com.example.qonfetty.data.StoreSettings?,
    promotionalMode: Boolean = false,
    onTogglePromotionalMode: () -> Unit = {},
    weeksBack: Int = 0,
    weekRangeText: String = "Current Week",
    onPreviousWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    transactionStats: com.example.qonfetty.data.TransactionStats? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Store Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                // Promotional Mode Toggle Star
                IconButton(
                    onClick = onTogglePromotionalMode,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = if (promotionalMode) "Disable Promotional Mode" else "Enable Promotional Mode",
                        tint = if (promotionalMode) {
                            Color(0xFFFFD700) // Gold color when active
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Store Logo and Name
            storeInfo?.let { store ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Store Logo
                    Card(
                        modifier = Modifier.size(60.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Show actual logo if available, otherwise show initials
                            storeSettings?.storeLogo?.let { logoUrl ->
                                if (logoUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(logoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Store Logo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    // Fallback to initials from store settings name
                                    val displayName = storeSettings?.storeName?.takeIf { it.isNotEmpty() } ?: store.name
                                    Text(
                                        text = displayName.take(2).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            } ?: run {
                                // No store settings, show app logo
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    contentDescription = "Qonfetty Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                    
                    // Store Name and Details
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = storeSettings?.storeName?.takeIf { it.isNotEmpty() } ?: store.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Store ID: ${store.id.take(8)}...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Combined Time Range and Analytics Chart
            transactionStats?.let { stats ->
                // Debug logging for chart values
                Log.d("DashboardScreen", "🔍 CHART: Rendering chart with stats - Purchases: ${stats.totalPurchases}, Claims: ${stats.totalClaimed}")
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // Time Range Section
                        Text(
                            text = "Time Range",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous Week Button
                            IconButton(
                                onClick = onPreviousWeek,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Previous week",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // Current Week Range Text
                            Text(
                                text = weekRangeText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Next Week Button
                            IconButton(
                                onClick = onNextWeek,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = "Next week",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Check if there's any data for this time period
                        if (stats.totalPurchases == 0.0 && stats.totalClaimed == 0.0) {
                            // No data indicator
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No data for this time period",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Try a different time range or add some transactions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Simple bar chart
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Purchases bar
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val maxValue = maxOf(stats.totalPurchases.toDouble(), stats.totalClaimed.toDouble(), 1.0)
                                    val purchaseHeight = (stats.totalPurchases.toDouble() / maxValue * 80).toInt()
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(purchaseHeight.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = MaterialTheme.shapes.small
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$${String.format("%.2f", stats.totalPurchases)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Purchases",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // Claims bar
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val maxValue = maxOf(stats.totalPurchases.toDouble(), stats.totalClaimed.toDouble(), 1.0)
                                    val claimHeight = (stats.totalClaimed.toDouble() / maxValue * 80).toInt()
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(claimHeight.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.secondary,
                                                shape = MaterialTheme.shapes.small
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$${String.format("%.2f", stats.totalClaimed)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Claims",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun RecentActivityCard(
    recentActivity: List<PointsTransactionWithCustomer>,
    onClearHistory: () -> Unit,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onClearHistory) {
                        Text("Clear")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (recentActivity.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No recent activity",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Scan an NFC card to see activity here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentActivity) { transaction ->
                        ActivityItem(activity = transaction)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(activity: PointsTransactionWithCustomer) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val timeString = remember(activity.createdAt) { 
        activity.createdAt?.let { 
            try {
                // Parse ISO 8601 timestamp
                // Simple date parsing that works on older API levels
                val date = try {
                    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.getDefault())
                    formatter.parse(it) ?: Date()
                } catch (e: Exception) {
                    try {
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                        formatter.parse(it) ?: Date()
                    } catch (e2: Exception) {
                        Date() // Fallback to current date
                    }
                }
                dateFormat.format(date)
            } catch (e: Exception) {
                "Recent"
            }
        } ?: "Recent"
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Activity icon
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Activity details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activity.customerName ?: "Customer: ${activity.customerId.take(8)}...",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "+${activity.pointsAwarded} points • ${activity.transactionType.replace("_", " ").capitalize()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (activity.nfcCardId != null) {
                Text(
                    text = "Card: ${activity.nfcCardId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Time and total points
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = timeString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${activity.newPoints} pts",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TransactionStatsCard(
    stats: TransactionStats,
    onRefresh: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Transaction Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh analytics",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Purchase vs Claimed amounts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Total Purchases",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$${String.format("%.2f", stats.totalPurchases)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Total Claimed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$${String.format("%.2f", stats.totalClaimed)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Points earned vs used
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Points Earned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${stats.totalPointsEarned}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Points Used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${stats.totalPointsUsed}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Total transactions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "${stats.totalTransactions}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
} 