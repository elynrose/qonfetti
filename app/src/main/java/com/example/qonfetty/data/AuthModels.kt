package com.example.qonfetty.data

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class AuthResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Int? = null,
    val token_type: String? = null,
    val user: User? = null,
    val session: Session? = null,
    val id: String? = null,
    val email: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class Session(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int,
    val token_type: String,
    val user: User
)

@Serializable
data class User(
    val id: String,
    val email: String,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class Store(
    val id: String,
    val name: String,
    val owner_id: String,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class StoreResponse(
    val data: List<Store>,
    val error: String? = null
)

@Serializable
data class ErrorResponse(
    val code: Int,
    val error_code: String,
    val msg: String
) 