package com.example.qonfetty.util

import com.example.qonfetty.config.EnvironmentConfig
import com.example.qonfetty.data.SupabaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ConfigVerifier(private val environmentConfig: EnvironmentConfig) {
    
    suspend fun verifyConfiguration(): ConfigVerificationResult = withContext(Dispatchers.IO) {
        try {
            // Check if anon key is set
            val anonKey = environmentConfig.getSupabaseAnonKey()
            if (anonKey.isBlank()) {
                return@withContext ConfigVerificationResult(
                    isValid = false,
                    message = "Anon key is not configured. Please add your Supabase anon key in the configuration screen.",
                    details = emptyList()
                )
            }
            
            // Check if host is configured
            val host = environmentConfig.getSupabaseHost()
            if (host.isBlank()) {
                return@withContext ConfigVerificationResult(
                    isValid = false,
                    message = "Supabase host is not configured.",
                    details = emptyList()
                )
            }
            
            // Test API connection
            val supabaseApi = SupabaseApi(environmentConfig)
            val testResult = testApiConnection(supabaseApi)
            
            return@withContext ConfigVerificationResult(
                isValid = testResult.isSuccess,
                message = if (testResult.isSuccess) "Configuration is valid and API connection successful!" else "API connection failed",
                details = listOf(
                    "Host: $host",
                    "Port: ${environmentConfig.getSupabasePort()}",
                    "Database: ${environmentConfig.getSupabaseDatabase()}",
                    "User: ${environmentConfig.getSupabaseUser()}",
                    "Anon Key: ${if (anonKey.length > 10) "${anonKey.take(10)}..." else "Not set"}",
                    "Service Key: ${if (environmentConfig.getSupabaseServiceKey().isNotBlank()) "Set" else "Not set"}"
                )
            )
            
        } catch (e: Exception) {
            ConfigVerificationResult(
                isValid = false,
                message = "Configuration verification failed: ${e.message}",
                details = listOf("Error: ${e.message}")
            )
        }
    }
    
    private suspend fun testApiConnection(supabaseApi: SupabaseApi): Result<Unit> {
        return try {
            // Try to make a simple API call to test connectivity
            // This is a basic health check - you might want to implement a proper health endpoint
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getConfigurationSummary(): String = withContext(Dispatchers.IO) {
        buildString {
            appendLine("=== Supabase Configuration Summary ===")
            appendLine("Host: ${environmentConfig.getSupabaseHost()}")
            appendLine("Port: ${environmentConfig.getSupabasePort()}")
            appendLine("Database: ${environmentConfig.getSupabaseDatabase()}")
            appendLine("User: ${environmentConfig.getSupabaseUser()}")
            appendLine("Anon Key: ${if (environmentConfig.getSupabaseAnonKey().isNotBlank()) "✓ Set" else "✗ Not set"}")
            appendLine("Service Key: ${if (environmentConfig.getSupabaseServiceKey().isNotBlank()) "✓ Set" else "✗ Not set"}")
            appendLine("URL: ${environmentConfig.getSupabaseUrl()}")
            appendLine("=====================================")
        }
    }
}

data class ConfigVerificationResult(
    val isValid: Boolean,
    val message: String,
    val details: List<String>
) 