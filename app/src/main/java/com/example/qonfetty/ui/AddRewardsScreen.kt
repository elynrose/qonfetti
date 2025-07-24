package com.example.qonfetty.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import android.util.Log
import com.example.qonfetty.data.Category
import com.example.qonfetty.ui.theme.StatusBarSpacer
import java.util.UUID

@Composable
fun AddRewardsScreen(
    viewModel: AddRewardsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pointsRequired by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isShared by remember { mutableStateOf(false) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageFileName by remember { mutableStateOf<String?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    
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
                Log.d("AddRewardsScreen", "Image selected: $selectedImageFileName")
            }
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Add top spacing to avoid status bar
        StatusBarSpacer()
        
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            
            Text(
                text = "Add New Reward",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Information Card
            item {
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
                            text = "Basic Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Reward Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = pointsRequired,
                            onValueChange = { pointsRequired = it },
                            label = { Text("Points Required *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
            
            // Pricing & Inventory Card
            item {
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
                            text = "Pricing & Inventory",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Available Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }
            
            // Category & Sharing Card
            item {
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
                            text = "Category & Sharing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Category selection
                        Text(
                            text = "Category: ${if (category.isEmpty()) "None selected" else category}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Button(
                            onClick = { showCategoryDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Category")
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Shared toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = isShared,
                                onCheckedChange = { isShared = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Share with other stores",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Allow customers to claim this reward at any store",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Photo Card
            item {
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
                            text = "Reward Photo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        if (selectedImageBytes != null) {
                            // Show selected image
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                AsyncImage(
                                    model = selectedImageBytes,
                                    contentDescription = "Selected reward photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (selectedImageBytes != null) Icons.Filled.Edit else Icons.Filled.Add,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (selectedImageBytes != null) "Change Photo" else "Add Photo")
                        }
                    }
                }
            }
            
            // Save Button
            item {
                Button(
                    onClick = {
                        val points = pointsRequired.toIntOrNull() ?: 0
                        val priceValue = price.toDoubleOrNull()
                        val quantityValue = quantity.toIntOrNull()
                        
                        if (name.isNotBlank() && points > 0) {
                            viewModel.createReward(
                                name = name,
                                description = description.takeIf { it.isNotBlank() },
                                pointsRequired = points,
                                imageBytes = selectedImageBytes,
                                imageFileName = selectedImageFileName,
                                price = priceValue,
                                quantity = quantityValue,
                                category = category.takeIf { it.isNotBlank() },
                                isShared = isShared
                            )
                        }
                    },
                    enabled = name.isNotBlank() && 
                             pointsRequired.toIntOrNull() != null && 
                             pointsRequired.toIntOrNull()!! > 0 &&
                             uiState !is AddRewardsUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState is AddRewardsUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Reward")
                    }
                }
            }
            
            // Add some bottom padding
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Category Selection Dialog
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
        
        // Success/Error Dialogs
        when (val currentState = uiState) {
            is AddRewardsUiState.Success -> {
                AlertDialog(
                    onDismissRequest = { 
                        viewModel.clearState()
                        onBack()
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Success",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Success")
                        }
                    },
                    text = {
                        Text(currentState.message)
                    },
                    confirmButton = {
                        Button(
                            onClick = { 
                                viewModel.clearState()
                                onBack()
                            }
                        ) {
                            Text("OK")
                        }
                    }
                )
            }
            is AddRewardsUiState.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.clearState() },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text("Error")
                        }
                    },
                    text = {
                        Text(currentState.message)
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.clearState() }
                        ) {
                            Text("OK")
                        }
                    }
                )
            }
            else -> {}
        }
    }
} 