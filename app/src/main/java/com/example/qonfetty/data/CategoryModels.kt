package com.example.qonfetty.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Category(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) 