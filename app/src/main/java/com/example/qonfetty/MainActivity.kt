package com.example.qonfetty

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.qonfetty.config.EnvironmentConfig
import com.example.qonfetty.data.SessionStorage
import com.example.qonfetty.data.SupabaseApi
import com.example.qonfetty.ui.AuthScreen
import com.example.qonfetty.ui.AuthViewModel
import com.example.qonfetty.ui.DashboardScreen
import com.example.qonfetty.ui.theme.QonfettyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QonfettyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val environmentConfig = remember { EnvironmentConfig(this) }
                    val sessionStorage = remember { SessionStorage(this) }
                    
                    var supabaseApi by remember { mutableStateOf<SupabaseApi?>(null) }
                    
                    // Initialize environment configuration
                    LaunchedEffect(Unit) {
                        environmentConfig.initializeWithDefaults()
                    }
                    
                    // Create SupabaseApi when configuration is ready
                    LaunchedEffect(environmentConfig) {
                        supabaseApi = SupabaseApi(environmentConfig)
                    }
                    
                    if (supabaseApi != null) {
                        val viewModel: AuthViewModel = viewModel {
                            AuthViewModel(supabaseApi!!, sessionStorage)
                        }
                        
                        val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
                        
                        when {
                            isLoggedIn -> {
                                DashboardScreen(viewModel = viewModel)
                            }
                            else -> {
                                AuthScreen(viewModel = viewModel)
                            }
                        }
                    } else {
                        // Loading state
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            // You could add a loading indicator here
                        }
                    }
                }
            }
        }
    }
}