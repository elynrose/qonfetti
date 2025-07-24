package com.example.qonfetty.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.util.Log
import java.util.UUID
import com.example.qonfetty.data.StoreSettings
import com.example.qonfetty.data.Category
import com.example.qonfetty.ui.theme.StatusBarSpacer

// Categories will be loaded from database

@Composable
fun StoreSettingsScreen(
    viewModel: StoreSettingsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val operationState by viewModel.operationState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    
    // Form state variables
    var storeName by remember { mutableStateOf(settings?.storeName ?: "") }
    var category by remember { mutableStateOf(settings?.category ?: "") }
    var email by remember { mutableStateOf(settings?.email ?: "") }
    var phone by remember { mutableStateOf(settings?.phone ?: "") }
    var website by remember { mutableStateOf(settings?.website ?: "") }
    var pointsPerPurchase by remember { mutableStateOf(settings?.pointsPerPurchase?.toString() ?: "1") }
    var promotionalEnabled by remember { mutableStateOf(settings?.promotionalEnabled ?: false) }
    var promotionPointsPerPurchase by remember { mutableStateOf(settings?.promotionPointsPerPurchase?.toString() ?: "0") }
    var openaiApiKey by remember { mutableStateOf(settings?.openaiApiKey ?: "") }
    var googleMapsApiKey by remember { mutableStateOf(settings?.googleMapsApiKey ?: "") }
    
    var selectedLogoUrl by remember { mutableStateOf(settings?.storeLogo) }
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageFileName by remember { mutableStateOf<String?>(null) }
    
    var categoryExpanded by remember { mutableStateOf(false) }
    
    // Update form when settings change
    LaunchedEffect(settings) {
        settings?.let { currentSettings ->
            storeName = currentSettings.storeName
            category = currentSettings.category
            email = currentSettings.email
            phone = currentSettings.phone ?: ""
            website = currentSettings.website ?: ""
            pointsPerPurchase = currentSettings.pointsPerPurchase.toString()
            promotionalEnabled = currentSettings.promotionalEnabled
            promotionPointsPerPurchase = currentSettings.promotionPointsPerPurchase.toString()
            openaiApiKey = currentSettings.openaiApiKey ?: ""
            googleMapsApiKey = currentSettings.googleMapsApiKey ?: ""
            selectedLogoUrl = currentSettings.storeLogo
        }
    }
    
    // State for showing alerts
    var showSuccessAlert by remember { mutableStateOf(false) }
    var showErrorAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }
    
    // Show success/error messages
    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is StoreSettingsOperationState.Success -> {
                alertMessage = state.message
                showSuccessAlert = true
                viewModel.clearOperationState()
            }
            is StoreSettingsOperationState.Error -> {
                alertMessage = state.message
                showErrorAlert = true
                viewModel.clearOperationState()
            }
            else -> {}
        }
    }
    
    val context = LocalContext.current
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            val inputStream = context.contentResolver.openInputStream(selectedUri)
            inputStream?.use { stream ->
                selectedImageBytes = stream.readBytes()
                selectedImageFileName = "store_logo_${UUID.randomUUID()}.jpg"
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Status bar spacing
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
                text = "Store Settings",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Content
        when (uiState) {
            is StoreSettingsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            is StoreSettingsUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Store Information Section
                    SettingsSection(title = "Store Information") {
                        // Store Logo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedImageBytes != null) {
                                // Show preview of selected image
                                AsyncImage(
                                    model = selectedImageBytes,
                                    contentDescription = "Selected logo preview",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (selectedLogoUrl != null) {
                                // Show existing logo
                                AsyncImage(
                                    model = selectedLogoUrl,
                                    contentDescription = "Store logo",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            Button(onClick = { imageLauncher.launch("image/*") }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (selectedImageBytes != null || selectedLogoUrl != null) "Change Logo" else "Add Logo")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("Store Name *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Category selection using Button and AlertDialog
                        var showCategoryDialog by remember { mutableStateOf(false) }
                        

                        
                        // Display current category
                        Text(
                            text = "Current Category: ${if (category.isEmpty()) "None selected" else category}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        // Category selection button
                        Button(
                            onClick = { 
                                Log.d("StoreSettings", "Category button clicked, categories: ${categories.size}")
                                showCategoryDialog = true 
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Select Category")
                        }
                        
                        if (showCategoryDialog) {
                            Log.d("StoreSettings", "Showing category dialog with ${categories.size} categories")
                            AlertDialog(
                                onDismissRequest = { 
                                    Log.d("StoreSettings", "Category dialog dismissed")
                                    showCategoryDialog = false 
                                },
                                title = { Text("Select Category") },
                                text = {
                                    LazyColumn {
                                        items(categories) { categoryOption ->
                                            TextButton(
                                                onClick = {
                                                    Log.d("StoreSettings", "Category selected: ${categoryOption.name}")
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
                                    TextButton(onClick = { 
                                        Log.d("StoreSettings", "Category dialog cancelled")
                                        showCategoryDialog = false 
                                    }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number (optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = website,
                            onValueChange = { website = it },
                            label = { Text("Website (optional)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Rewards Configuration Section
                    SettingsSection(title = "Rewards Configuration") {
                        OutlinedTextField(
                            value = pointsPerPurchase,
                            onValueChange = { pointsPerPurchase = it },
                            label = { Text("Points per Purchase") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Enable Promotional Points",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = promotionalEnabled,
                                onCheckedChange = { promotionalEnabled = it }
                            )
                        }
                        
                        if (promotionalEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = promotionPointsPerPurchase,
                                onValueChange = { promotionPointsPerPurchase = it },
                                label = { Text("Promotion Points per Purchase") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // API Settings Section
                    SettingsSection(title = "API Settings (Optional)") {
                        OutlinedTextField(
                            value = openaiApiKey,
                            onValueChange = { openaiApiKey = it },
                            label = { Text("OpenAI API Key (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = googleMapsApiKey,
                            onValueChange = { googleMapsApiKey = it },
                            label = { Text("Google Maps API Key (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Add save button at the bottom
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            // First upload logo if there's a new image selected
                            if (selectedImageBytes != null && selectedImageFileName != null) {
                                viewModel.uploadStoreLogo(selectedImageBytes!!, selectedImageFileName!!) { logoUrl ->
                                    // After logo upload, save settings with the new logo URL
                                    val points = pointsPerPurchase.toIntOrNull() ?: 1
                                    val promoPoints = promotionPointsPerPurchase.toIntOrNull() ?: 0
                                    
                                    if (storeName.isNotBlank() && category.isNotBlank() && email.isNotBlank()) {
                                        viewModel.saveStoreSettings(
                                            storeName = storeName,
                                            category = category,
                                            email = email,
                                            phone = phone,
                                            website = website,
                                            storeLogoUrl = logoUrl,
                                            pointsPerPurchase = points,
                                            promotionalEnabled = promotionalEnabled,
                                            promotionPointsPerPurchase = promoPoints,
                                            openaiApiKey = openaiApiKey,
                                            googleMapsApiKey = googleMapsApiKey
                                        )
                                    }
                                }
                            } else {
                                // No new logo, save settings with existing logo URL
                                val points = pointsPerPurchase.toIntOrNull() ?: 1
                                val promoPoints = promotionPointsPerPurchase.toIntOrNull() ?: 0
                                
                                if (storeName.isNotBlank() && category.isNotBlank() && email.isNotBlank()) {
                                    viewModel.saveStoreSettings(
                                        storeName = storeName,
                                        category = category,
                                        email = email,
                                        phone = phone,
                                        website = website,
                                        storeLogoUrl = selectedLogoUrl,
                                        pointsPerPurchase = points,
                                        promotionalEnabled = promotionalEnabled,
                                        promotionPointsPerPurchase = promoPoints,
                                        openaiApiKey = openaiApiKey,
                                        googleMapsApiKey = googleMapsApiKey
                                    )
                                }
                            }
                        },
                        enabled = storeName.isNotBlank() && category.isNotBlank() && email.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Settings")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            is StoreSettingsUiState.Error -> {
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
                            text = (uiState as StoreSettingsUiState.Error).message,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadStoreSettings() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
        
        // Success Alert Dialog
        if (showSuccessAlert) {
            AlertDialog(
                onDismissRequest = { showSuccessAlert = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Success")
                    }
                },
                text = {
                    Text(alertMessage)
                },
                confirmButton = {
                    TextButton(onClick = { showSuccessAlert = false }) {
                        Text("OK")
                    }
                }
            )
        }
        
        // Error Alert Dialog
        if (showErrorAlert) {
            AlertDialog(
                onDismissRequest = { showErrorAlert = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text("Error")
                    }
                },
                text = {
                    Text(alertMessage)
                },
                confirmButton = {
                    TextButton(onClick = { showErrorAlert = false }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
} 