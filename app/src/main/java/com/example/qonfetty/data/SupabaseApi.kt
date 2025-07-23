package com.example.qonfetty.data

import android.util.Log
import com.example.qonfetty.config.EnvironmentConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class SupabaseApi(private val environmentConfig: EnvironmentConfig) {
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    // Get configuration from environment
    private val baseUrl: String = runBlocking { environmentConfig.getSupabaseUrl() }
    private val anonKey: String = runBlocking { environmentConfig.getSupabaseAnonKey() }
    
    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return try {
            Log.d("SupabaseApi", "Starting login for email: $email")
            
            val response = client.post("$baseUrl/auth/v1/token?grant_type=password") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $anonKey")
                }
                setBody(LoginRequest(email, password))
            }
            
            if (response.status.isSuccess()) {
                val authResponse = response.body<AuthResponse>()
                Log.d("SupabaseApi", "Login successful: $authResponse")
                Result.success(authResponse)
            } else {
                val error = response.body<ErrorResponse>()
                Log.e("SupabaseApi", "Login failed with error: $error")
                Result.failure(Exception(error.msg))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Network error during login: ${e.message}", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun register(email: String, password: String): Result<AuthResponse> {
        return try {
            Log.d("SupabaseApi", "Starting registration for email: $email")
            
            val response = client.post("$baseUrl/auth/v1/signup") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $anonKey")
                }
                setBody(RegisterRequest(email, password))
            }
            
            Log.d("SupabaseApi", "Response status: ${response.status}")
            
            if (response.status.isSuccess()) {
                val authResponse = response.body<AuthResponse>()
                Log.d("SupabaseApi", "Registration successful: $authResponse")
                
                // Handle different response formats
                val finalResponse = if (authResponse.session != null) {
                    AuthResponse(
                        access_token = authResponse.session.access_token,
                        refresh_token = authResponse.session.refresh_token,
                        expires_in = authResponse.session.expires_in,
                        token_type = authResponse.session.token_type,
                        user = authResponse.session.user
                    )
                } else if (authResponse.id != null) {
                    AuthResponse(
                        user = User(
                            id = authResponse.id,
                            email = authResponse.email ?: "",
                            created_at = authResponse.created_at ?: "",
                            updated_at = authResponse.updated_at ?: ""
                        )
                    )
                } else {
                    authResponse
                }
                
                Result.success(finalResponse)
            } else {
                val error = response.body<ErrorResponse>()
                Log.e("SupabaseApi", "Registration failed with error: $error")
                Result.failure(Exception(error.msg))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Network error during registration: ${e.message}", e)
            Result.failure(Exception("Network error: ${e.message}"))
        }
    }
    
    suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            val response = client.post("$baseUrl/auth/v1/recover") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $anonKey")
                }
                setBody(ForgotPasswordRequest(email))
            }
            
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val error = response.body<ErrorResponse>()
                Result.failure(Exception(error.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getStoreByOwnerId(userId: String, authToken: String): Result<Store?> {
        return try {
            val response = client.get("$baseUrl/rest/v1/stores?owner_id=eq.$userId") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            if (response.status.isSuccess()) {
                val stores = response.body<List<Store>>()
                Result.success(stores.firstOrNull())
            } else {
                Result.failure(Exception("Failed to fetch store"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun close() {
        client.close()
    }
} 