package com.example.qonfetty.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qonfetty.data.CustomerWithPoints
import com.example.qonfetty.data.NfcCardResponse
import com.example.qonfetty.data.NfcOperationState
import com.example.qonfetty.data.Reward
import com.example.qonfetty.nfc.NfcManager
import com.example.qonfetty.nfc.NfcWriteManager
import com.example.qonfetty.ui.theme.StatusBarSpacer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CustomerDetailScreen(
    customerWithPoints: CustomerWithPoints,
    viewModel: CustomerDetailViewModel,
    onBack: () -> Unit,
    nfcManager: NfcManager?,
    modifier: Modifier = Modifier
) {
    val customer = customerWithPoints.customer
    val nfcCards by viewModel.nfcCards.collectAsStateWithLifecycle()
    val nfcOperationState by viewModel.nfcOperationState.collectAsStateWithLifecycle()
    val totalPoints by viewModel.totalPoints.collectAsStateWithLifecycle()
    val claimableRewards by viewModel.claimableRewards.collectAsStateWithLifecycle()
    val rewardOperationState by viewModel.rewardOperationState.collectAsStateWithLifecycle()
    
    var showNfcScanDialog by remember { mutableStateOf(false) }
    var showDeactivateDialog by remember { mutableStateOf<NfcCardResponse?>(null) }
    var showUnlinkDialog by remember { mutableStateOf<NfcCardResponse?>(null) }
    
    // Pull to refresh state
    val isRefreshing = nfcOperationState is NfcOperationState.Loading || rewardOperationState is NfcOperationState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { 
            customer.id?.let { customerId ->
                viewModel.loadCustomerData(customerId)
            }
        }
    )
    
    // Load customer data when screen is displayed
    LaunchedEffect(customer.id) {
        customer.id?.let { customerId ->
            viewModel.loadCustomerData(customerId)
        }
    }
    
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
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    
                    Text(
                        text = "Customer Details",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    IconButton(onClick = { showNfcScanDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Register NFC Card")
                    }
                }
                
                // Content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Customer Info Card
                    item {
                        CustomerInfoCard(customerWithPoints)
                    }
                    
                    // Total Points Earned Card
                    item {
                        TotalPointsCard(totalPoints)
                    }
                    
                    // Eligible Rewards Section
                    item {
                        Text(
                            text = "Eligible Rewards",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    if (claimableRewards.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "No Rewards",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No eligible rewards",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Earn more points to unlock rewards",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(claimableRewards) { reward ->
                            RewardCard(
                                reward = reward,
                                onClaim = { 
                                    customer.id?.let { customerId ->
                                        viewModel.claimReward(reward, customerId)
                                    }
                                }
                            )
                        }
                    }
                    
                    // NFC Cards Section
                    item {
                        Text(
                            text = "NFC Cards",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    if (nfcCards.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "No NFC Cards",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No NFC cards registered",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tap the + button to register an NFC card",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(nfcCards) { nfcCard ->
                            NfcCardItem(
                                nfcCard = nfcCard,
                                onDeactivate = { showDeactivateDialog = nfcCard },
                                onUnlink = { showUnlinkDialog = nfcCard }
                            )
                        }
                    }
                }
            }
            
            // Pull to refresh indicator
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
    
    // NFC Scan Dialog
    if (showNfcScanDialog) {
        NfcWriteDialog(
            customer = customer,
            nfcManager = nfcManager,
            onDismiss = { showNfcScanDialog = false },
            onCardWritten = { memberId ->
                customer.id?.let { customerId ->
                    viewModel.registerNfcCard(memberId, memberId, customerId)
                }
                showNfcScanDialog = false
            }
        )
    }
    
    // Deactivate Dialog
    showDeactivateDialog?.let { nfcCard ->
        AlertDialog(
            onDismissRequest = { showDeactivateDialog = null },
            title = { Text("Deactivate NFC Card") },
            text = { Text("Are you sure you want to deactivate this NFC card? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deactivateNfcCard(nfcCard.cardId)
                        showDeactivateDialog = null
                    }
                ) {
                    Text("Deactivate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Unlink Dialog
    showUnlinkDialog?.let { nfcCard ->
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = null },
            title = { Text("Unlink NFC Card") },
            text = { Text("Are you sure you want to unlink this member ID (${nfcCard.memberId}) from the NFC card? This will delete the card from the database and cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unlinkNfcCard(nfcCard.cardId)
                        showUnlinkDialog = null
                    }
                ) {
                    Text("Unlink")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Operation State Snackbars
    when (nfcOperationState) {
        is NfcOperationState.Success -> {
            LaunchedEffect(nfcOperationState) {
                // Show success message
            }
        }
        is NfcOperationState.Error -> {
            LaunchedEffect(nfcOperationState) {
                // Show error message
            }
        }
        else -> {}
    }
    
    when (rewardOperationState) {
        is RewardOperationState.Success -> {
            LaunchedEffect(rewardOperationState) {
                // Show success message
            }
        }
        is RewardOperationState.Error -> {
            LaunchedEffect(rewardOperationState) {
                // Show error message
            }
        }
        else -> {}
    }
}

@Composable
fun CustomerInfoCard(customerWithPoints: CustomerWithPoints) {
    val customer = customerWithPoints.customer
    
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
                text = customer.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = customer.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "|",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (!customer.address.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = customer.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Member ID",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Member ID: ${customer.memberId ?: "Not set"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun TotalPointsCard(totalPoints: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Total Points Earned",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Keep earning points to unlock rewards!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Points",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$totalPoints",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = "points",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun RewardCard(
    reward: Reward,
    onClaim: () -> Unit
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
            // Header with name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = reward.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (!reward.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reward.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (!reward.category.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Category",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = reward.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Status indicators
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Active status
                    if (reward.isActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Shared status
                    if (reward.isShared) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Shared",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Shared",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Reward details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side - Points and Price
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Points Required",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${reward.pointsRequired} points required",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    if (reward.price != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Price",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "$${String.format("%.2f", reward.price)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    
                    if (reward.quantity != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Quantity",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Qty: ${reward.quantity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                
                // Right side - Claim button
                Button(
                    onClick = onClaim,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Claim",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Claim")
                }
            }
        }
    }
}

@Composable
fun NfcCardItem(
    nfcCard: NfcCardResponse,
    onDeactivate: () -> Unit,
    onUnlink: () -> Unit
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "NFC Card",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "NFC Card",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Card ID: ${nfcCard.cardId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = onDeactivate) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Deactivate",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Member ID section with unlink functionality
            if (nfcCard.memberId.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
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
                                    Icons.Default.Person,
                                    contentDescription = "Member ID",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "This member ID ${nfcCard.memberId} has been linked to a card",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        TextButton(
                            onClick = onUnlink,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Unlink",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("To unlink the card click here")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Card details
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Registration date
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Registration Date",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Registered: ${formatDate(nfcCard.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Status
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Status",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Status: Active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "Unknown"
    
    return try {
        // Try to parse the date and format it nicely
        val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.getDefault()).parse(dateString)
        if (date != null) {
            java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault()).format(date)
        } else {
            dateString.take(10) // Fallback to just the date part
        }
    } catch (e: Exception) {
        dateString.take(10) // Fallback to just the date part
    }
}

@Composable
fun NfcWriteDialog(
    customer: com.example.qonfetty.data.Customer,
    nfcManager: NfcManager?,
    onDismiss: () -> Unit,
    onCardWritten: (String) -> Unit
) {
    var isWriting by remember { mutableStateOf(false) }
    var writeStatus by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    
    val memberId = customer.memberId ?: "Not set"
    
    AlertDialog(
        onDismissRequest = { if (!isWriting) onDismiss() },
        title = { Text("Write Member ID to NFC Card") },
        text = {
            Column {
                Text("Customer: ${customer.name}")
                Text("Member ID: $memberId")
                Spacer(modifier = Modifier.height(16.dp))
                
                if (memberId == "Not set") {
                    Text(
                        text = "Error: Customer does not have a member ID set",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (nfcManager == null) {
                    Text(
                        text = "Error: NFC is not available on this device",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (!nfcManager.isNfcAvailable()) {
                    Text(
                        text = "Error: NFC is disabled. Please enable NFC in your device settings.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (isWriting) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Writing member ID to NFC card...")
                        Text("Please hold an NFC card near your device")
                        writeStatus?.let { status ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = status,
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    Text("Ready to write member ID to NFC card")
                    Text("Tap 'Start Writing' and then hold an NFC card near your device")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (memberId != "Not set" && nfcManager != null && nfcManager.isNfcAvailable()) {
                        isWriting = true
                        writeStatus = "Waiting for NFC card... Hold card near device"
                        isError = false
                    }
                },
                enabled = memberId != "Not set" && nfcManager != null && nfcManager.isNfcAvailable() && !isWriting
            ) {
                Text("Start Writing")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isWriting
            ) {
                Text("Cancel")
            }
        }
    )
    
    // Handle NFC writing logic
    LaunchedEffect(isWriting) {
        if (isWriting && nfcManager != null && memberId != "Not set") {
            writeStatus = "Waiting for NFC card... Hold card near device"
            
            // Set up NFC write callback using NfcWriteManager
            NfcWriteManager.startWrite(
                memberId = memberId,
                nfcManager = nfcManager,
                onSuccess = { writtenMemberId ->
                    writeStatus = "Successfully wrote member ID: $writtenMemberId"
                    isError = false
                    isWriting = false
                    
                    // Call onCardWritten after a brief delay
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        kotlinx.coroutines.delay(1000) // Show success message briefly
                        onCardWritten(writtenMemberId)
                    }
                },
                onError = { errorMessage ->
                    writeStatus = "Failed to write: $errorMessage"
                    isError = true
                    isWriting = false
                    
                    // Show error message for longer
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        kotlinx.coroutines.delay(3000) // Show error message longer
                    }
                }
            )
            
            android.util.Log.d("NfcWriteDialog", "NFC write callback set up for member ID: $memberId")
        }
    }
} 