package com.example.qonfetty.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qonfetty.data.DataRefreshManager

@Composable
fun LiveDataIndicator(
    refreshState: DataRefreshManager.RefreshState,
    modifier: Modifier = Modifier
) {
    var showIndicator by remember { mutableStateOf(false) }
    
    // Show indicator when refreshing
    LaunchedEffect(refreshState) {
        when (refreshState) {
            is DataRefreshManager.RefreshState.Refreshing -> {
                showIndicator = true
            }
            is DataRefreshManager.RefreshState.Success -> {
                showIndicator = true
                // Hide after a short delay
                kotlinx.coroutines.delay(2000)
                showIndicator = false
            }
            is DataRefreshManager.RefreshState.Error -> {
                showIndicator = true
                // Hide after a longer delay for errors
                kotlinx.coroutines.delay(4000)
                showIndicator = false
            }
            else -> {
                showIndicator = false
            }
        }
    }
    
    AnimatedVisibility(
        visible = showIndicator,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .height(32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (refreshState) {
                    is DataRefreshManager.RefreshState.Refreshing -> MaterialTheme.colorScheme.primaryContainer
                    is DataRefreshManager.RefreshState.Success -> MaterialTheme.colorScheme.tertiaryContainer
                    is DataRefreshManager.RefreshState.Error -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (refreshState) {
                    is DataRefreshManager.RefreshState.Refreshing -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "refresh")
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
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refreshing",
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(rotation),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = "Live updating...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    is DataRefreshManager.RefreshState.Success -> {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Success",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        
                        Text(
                            text = "Data updated",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    is DataRefreshManager.RefreshState.Error -> {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Error",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Text(
                            text = "Update failed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    else -> {
                        // Should not be visible
                    }
                }
            }
        }
    }
} 