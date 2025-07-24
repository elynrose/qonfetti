package com.example.qonfetty.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qonfetty.data.TransactionStats
import com.example.qonfetty.data.PointsTransactionWithCustomer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val transactionStats by viewModel.transactionStats.collectAsStateWithLifecycle()
    val recentActivity by viewModel.recentActivity.collectAsStateWithLifecycle()
    val weeksBack by viewModel.weeksBack.collectAsStateWithLifecycle()
    val weekRangeText = viewModel.getWeekRangeText()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Analytics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDashboard() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Week Navigation
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Time Range",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.goToPreviousWeek() },
                                enabled = weeksBack < 52
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Previous Week")
                            }
                            
                            Text(
                                text = weekRangeText,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            IconButton(
                                onClick = { viewModel.goToNextWeek() },
                                enabled = weeksBack > 1
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Next Week")
                            }
                        }
                    }
                }
            }
            
            item {
                // Transaction Statistics
                TransactionStatsCard(
                    stats = transactionStats ?: TransactionStats(
                        totalPurchases = 0.0,
                        totalClaimed = 0.0,
                        totalTransactions = 0,
                        totalPointsEarned = 0,
                        totalPointsUsed = 0
                    ),
                    onRefresh = { viewModel.refreshDashboard() }
                )
            }
            
            item {
                // Recent Activity Header
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Recent Activity Items
            if (recentActivity.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent activity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(recentActivity) { transaction ->
                    TransactionItem(transaction = transaction)
                }
            }
        }
    }
}

@Composable
private fun TransactionItem(transaction: PointsTransactionWithCustomer) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
    val displayFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    
    // Parse the createdAt string to a Date object
    val displayDate = try {
        transaction.createdAt?.let { dateString ->
            val date = dateFormat.parse(dateString)
            displayFormat.format(date)
        } ?: "Unknown date"
    } catch (e: Exception) {
        // Fallback to showing the raw string if parsing fails
        transaction.createdAt ?: "Unknown date"
    }
    
    // Format transaction type for display
    val displayTransactionType = when (transaction.transactionType) {
        "nfc_scan" -> "Card Scan"
        else -> transaction.transactionType.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
    
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
                Column {
                    Text(
                        text = transaction.customerName ?: "Unknown Customer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = displayTransactionType,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${transaction.pointsAwarded} points",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = displayDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
} 