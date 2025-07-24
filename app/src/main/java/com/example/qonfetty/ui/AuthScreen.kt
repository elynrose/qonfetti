package com.example.qonfetty.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    inactivityMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    
    var currentScreen by remember { mutableStateOf(AuthScreenType.Login) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Success -> {
                // Clear form on success
                email = ""
                password = ""
                confirmPassword = ""
            }
            else -> {}
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = when (currentScreen) {
                AuthScreenType.Login -> "Store Owner Login"
                AuthScreenType.Register -> "Store Owner Registration"
                AuthScreenType.ForgotPassword -> "Reset Password"
            },
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Inactivity message
        if (inactivityMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = inactivityMessage,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        
        // Password field (not shown for forgot password)
        if (currentScreen != AuthScreenType.ForgotPassword) {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (currentScreen == AuthScreenType.Register) ImeAction.Next else ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        // Confirm password field (only for register)
        if (currentScreen == AuthScreenType.Register) {
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        // Loading indicator
        if (uiState is AuthUiState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp)
            )
        }
        
        // Error message
        if (uiState is AuthUiState.Error) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Success message
        if (uiState is AuthUiState.Success) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = (uiState as AuthUiState.Success).message,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Action button
        Button(
            onClick = {
                when (currentScreen) {
                    AuthScreenType.Login -> {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            viewModel.login(email, password)
                        }
                    }
                    AuthScreenType.Register -> {
                        if (email.isNotBlank() && password.isNotBlank() && 
                            password == confirmPassword && password.length >= 6) {
                            viewModel.register(email, password)
                        }
                    }
                    AuthScreenType.ForgotPassword -> {
                        if (email.isNotBlank()) {
                            viewModel.forgotPassword(email)
                        }
                    }
                }
            },
            enabled = when (currentScreen) {
                AuthScreenType.Login -> email.isNotBlank() && password.isNotBlank()
                AuthScreenType.Register -> email.isNotBlank() && password.isNotBlank() && 
                    password == confirmPassword && password.length >= 6
                AuthScreenType.ForgotPassword -> email.isNotBlank()
            } && uiState !is AuthUiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = when (currentScreen) {
                    AuthScreenType.Login -> "Login"
                    AuthScreenType.Register -> "Register"
                    AuthScreenType.ForgotPassword -> "Send Reset Email"
                }
            )
        }
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            when (currentScreen) {
                AuthScreenType.Login -> {
                    TextButton(
                        onClick = { currentScreen = AuthScreenType.Register }
                    ) {
                        Text("Create Account")
                    }
                    TextButton(
                        onClick = { currentScreen = AuthScreenType.ForgotPassword }
                    ) {
                        Text("Forgot Password?")
                    }
                }
                AuthScreenType.Register -> {
                    TextButton(
                        onClick = { currentScreen = AuthScreenType.Login }
                    ) {
                        Text("Already have an account?")
                    }
                }
                AuthScreenType.ForgotPassword -> {
                    TextButton(
                        onClick = { currentScreen = AuthScreenType.Login }
                    ) {
                        Text("Back to Login")
                    }
                }
            }
        }
        

    }
}

enum class AuthScreenType {
    Login,
    Register,
    ForgotPassword
} 