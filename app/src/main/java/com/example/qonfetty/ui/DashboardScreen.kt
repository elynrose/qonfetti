package com.example.qonfetty.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qonfetty.data.PointsTransaction
import com.example.qonfetty.data.PointsTransactionWithCustomer
import com.example.qonfetty.nfc.NfcProcessingResult
import com.example.qonfetty.ui.components.LiveDataIndicator
import com.example.qonfetty.ui.theme.StatusBarSpacer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DashboardScreen(
    viewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    onShowCustomers: () -> Unit = {},
    onShowNfcTest: () -> Unit = {},
    onShowRewards: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val scanHistory by dashboardViewModel.scanHistory.collectAsStateWithLifecycle()
    val recentActivity by dashboardViewModel.recentActivity.collectAsStateWithLifecycle()
    val lastScanResult by dashboardViewModel.lastScanResult.collectAsStateWithLifecycle()
    val refreshState by dashboardViewModel.refreshState.collectAsStateWithLifecycle()
    
    // Pull to refresh state
    val isRefreshing = dashboardState is DashboardUiState.Scanning
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { /* Dashboard doesn't need refresh, but we can clear scan history */ }
    )
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Add top spacing to avoid status bar
        StatusBarSpacer()
        
        // Content with proper padding and pull to refresh
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            // Live data indicator
            LiveDataIndicator(
                refreshState = refreshState,
                modifier = Modifier.align(Alignment.End)
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // NFC Scan Result Card (if available)
            lastScanResult?.let { result ->
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    NfcScanResultCard(
                        result = result,
                        onDismiss = { dashboardViewModel.clearScanResult() }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Store Information Card
            StoreInfoCard(uiState = uiState)
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
                        onDismissRequest = { dashboardViewModel.clearScanResult() },
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
                                onClick = { dashboardViewModel.clearScanResult() }
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
            else -> {}
        }
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
                            text = { Text("Write to Card") },
                            leadingIcon = { 
                                Icon(Icons.Filled.Star, contentDescription = null)
                            },
                            onClick = {
                                onShowNfcTest()
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
                            text = { Text("Store Settings") },
                            leadingIcon = { 
                                Icon(Icons.Filled.Settings, contentDescription = null)
                            },
                            onClick = {
                                onShowSettings()
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
    onDismiss: () -> Unit
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
                                text = "Points Added",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "+${result.pointsAwarded}",
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
                            text = "${result.newTotalPoints}",
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
private fun StoreInfoCard(uiState: com.example.qonfetty.ui.AuthUiState) {
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
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "You are successfully logged in to your store management dashboard.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Your store ID and authentication tokens have been securely stored.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Success message if any
            if (uiState is com.example.qonfetty.ui.AuthUiState.Success) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
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