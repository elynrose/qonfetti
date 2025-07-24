package com.example.qonfetty.ui.theme

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable composable for adding status bar spacing to prevent layout overlap.
 * Use this at the top of your main content Column to ensure proper spacing below the status bar.
 */
@Composable
fun StatusBarSpacer() {
    Spacer(modifier = Modifier.height(48.dp))
} 