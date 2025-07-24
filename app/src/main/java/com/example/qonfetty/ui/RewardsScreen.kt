package com.example.qonfetty.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import android.util.Log
import coil.request.ImageRequest
import com.example.qonfetty.data.Reward
import com.example.qonfetty.data.Category
import com.example.qonfetty.ui.components.LiveDataIndicator
import com.example.qonfetty.ui.theme.StatusBarSpacer
import java.util.UUID

// Categories will be loaded from database

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val operationState by viewModel.operationState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Reward?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Reward?>(null) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageFileName by remember { mutableStateOf<String?>(null) }
    
    // Pull to refresh state
    val isRefreshing = uiState is RewardsUiState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.loadRewards() }
    )
    
    // Image picker launcher
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            val inputStream = context.contentResolver.openInputStream(selectedUri)
            inputStream?.use { stream ->
                selectedImageBytes = stream.readBytes()
                selectedImageFileName = "reward_${UUID.randomUUID()}.jpg"
            }
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
                        text = "Rewards",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    IconButton(onClick = { 
                        Log.d("RewardsScreen", "Add button clicked")
                        showAddDialog = true 
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Reward")
                    }
                }
                
                // Live data indicator
                val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()
                LiveDataIndicator(
                    refreshState = refreshState,
                    modifier = Modifier.align(Alignment.End)
                )
                
                // Search and filter bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchRewards(it) },
                    label = { Text("Search rewards...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                // Category filter
                CategoryFilter(
                    categories = viewModel.getCategories(),
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.filterByCategory(it) }
                )
                
                // Content
                when (uiState) {
                    is RewardsUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    
                    is RewardsUiState.Success -> {
                        val rewards = (uiState as RewardsUiState.Success).rewards
                        
                        if (rewards.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "No rewards",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "No rewards found" else "No rewards yet",
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    if (searchQuery.isEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Tap the + button to add your first reward",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(rewards) { reward ->
                                    RewardCard(
                                        reward = reward,
                                        onEdit = { showEditDialog = reward },
                                        onDelete = { showDeleteDialog = reward }
                                    )
                                }
                            }
                        }
                    }
                    
                    is RewardsUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Error",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = (uiState as RewardsUiState.Error).message,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadRewards() }) {
                                    Text("Retry")
                                }
                            }
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
    
    // Dialogs
    if (showAddDialog) {
        RewardDialog(
            reward = null,
            categories = categories,
            selectedImageBytes = selectedImageBytes,
            selectedImageFileName = selectedImageFileName,
            onDismiss = { 
                showAddDialog = false
                selectedImageBytes = null
                selectedImageFileName = null
            },
            onSave = { name, description, pointsRequired, photo, price, quantity, category, isShared ->
                Log.d("RewardsScreen", "Save button clicked with name: $name, points: $pointsRequired, hasImage: ${selectedImageBytes != null}")
                viewModel.createRewardWithImage(
                    name = name,
                    description = description,
                    pointsRequired = pointsRequired,
                    imageBytes = selectedImageBytes,
                    imageFileName = selectedImageFileName,
                    price = price,
                    quantity = quantity,
                    category = category,
                    isShared = isShared
                )
                showAddDialog = false
                selectedImageBytes = null
                selectedImageFileName = null
            },
            onImageSelect = { imagePickerLauncher.launch("image/*") }
        )
    }
    
    showEditDialog?.let { reward ->
        RewardDialog(
            reward = reward,
            categories = categories,
            onDismiss = { showEditDialog = null },
            onSave = { name, description, pointsRequired, photo, price, quantity, category, isShared ->
                val updatedReward = reward.copy(
                    name = name,
                    description = description,
                    pointsRequired = pointsRequired,
                    photo = photo,
                    price = price,
                    quantity = quantity,
                    category = category,
                    isShared = isShared
                )
                viewModel.updateReward(updatedReward)
                showEditDialog = null
            },
            onImageSelect = { imagePickerLauncher.launch("image/*") }
        )
    }
    
    showDeleteDialog?.let { reward ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Reward") },
            text = { Text("Are you sure you want to delete '${reward.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReward(reward.id)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    

}

@Composable
private fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All") }
            )
        }
        
        items(categories) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) }
            )
        }
    }
}

@Composable
private fun RewardCard(
    reward: Reward,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reward image
            if (reward.photo != null && reward.photo.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(reward.photo)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Reward image",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                // Placeholder when no image
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "No image",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // Reward details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = reward.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                reward.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${reward.pointsRequired} points",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    reward.price?.let { price ->
                        Text(
                            text = "$${String.format("%.2f", price)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    reward.quantity?.let { quantity ->
                        Text(
                            text = "$quantity available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    reward.category?.let { category ->
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                
                if (reward.isShared) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Shared",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Shared",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            
            // Action buttons
            Column {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

@Composable
private fun RewardDialog(
    reward: Reward?,
    categories: List<Category>,
    selectedImageBytes: ByteArray? = null,
    selectedImageFileName: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, String?, Int, String?, Double?, Int?, String?, Boolean) -> Unit,
    onImageSelect: () -> Unit
) {
    var name by remember { mutableStateOf(reward?.name ?: "") }
    var description by remember { mutableStateOf(reward?.description ?: "") }
    var pointsRequired by remember { mutableStateOf(reward?.pointsRequired?.toString() ?: "") }
    var price by remember { mutableStateOf(reward?.price?.toString() ?: "") }
    var quantity by remember { mutableStateOf(reward?.quantity?.toString() ?: "") }
    var category by remember { mutableStateOf(reward?.category ?: "") }
    var isShared by remember { mutableStateOf(reward?.isShared ?: false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (reward == null) "Add Reward" else "Edit Reward") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
                
                OutlinedTextField(
                    value = pointsRequired,
                    onValueChange = { pointsRequired = it },
                    label = { Text("Points Required *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Category selection using Button and AlertDialog
                var showCategoryDialog by remember { mutableStateOf(false) }
                
                // Display current category
                Text(
                    text = "Category: ${if (category.isEmpty()) "None selected" else category}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Category selection button
                Button(
                    onClick = { showCategoryDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Category")
                }
                
                if (showCategoryDialog) {
                    AlertDialog(
                        onDismissRequest = { showCategoryDialog = false },
                        title = { Text("Select Category") },
                        text = {
                            LazyColumn {
                                items(categories) { categoryOption ->
                                    TextButton(
                                        onClick = {
                                            category = categoryOption.name
                                            showCategoryDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = categoryOption.name,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Start
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showCategoryDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                // Image upload section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedImageBytes != null) {
                        // Show preview of selected image
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(selectedImageBytes)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Selected image preview",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else if (reward?.photo != null) {
                        // Show existing image for editing
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(reward.photo)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Reward image",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Button(onClick = onImageSelect) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (selectedImageBytes != null || reward?.photo != null) "Change Photo" else "Add Photo")
                    }
                }
                
                // Shared toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isShared,
                        onCheckedChange = { isShared = it }
                    )
                    Text("Share with other stores")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val points = pointsRequired.toIntOrNull() ?: 0
                    val priceValue = price.toDoubleOrNull()
                    val quantityValue = quantity.toIntOrNull()
                    
                    if (name.isNotBlank() && points > 0) {
                        onSave(
                            name,
                            description.takeIf { it.isNotBlank() },
                            points,
                            null, // Photo will be handled separately
                            priceValue,
                            quantityValue,
                            category.takeIf { it.isNotBlank() },
                            isShared
                        )
                    }
                },
                enabled = name.isNotBlank() && pointsRequired.toIntOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 