package com.example.qonfetty.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class StoreSettings(
    val id: String? = null,
    
    // Store Information
    @SerialName("store_name")
    val storeName: String = "",
    val category: String = "",
    val email: String = "",
    val phone: String = "",
    val website: String = "",
    @SerialName("store_logo")
    val storeLogo: String? = null,
    
    // Rewards Configuration
    @SerialName("points_per_purchase")
    val pointsPerPurchase: Int = 1,
    @SerialName("promotional_enabled")
    val promotionalEnabled: Boolean = false,
    @SerialName("promotion_points_per_purchase")
    val promotionPointsPerPurchase: Int = 0,
    
    // API Settings
    @SerialName("openai_api_key")
    val openaiApiKey: String = "",
    @SerialName("google_maps_api_key")
    val googleMapsApiKey: String = "",
    
    // Metadata
    @SerialName("store_id")
    val storeId: String = "",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class StoreSettingsRequest(
    val storeName: String,
    val category: String,
    val email: String,
    val phone: String,
    val website: String,
    val storeLogo: String? = null,
    val pointsPerPurchase: Int,
    val promotionalEnabled: Boolean,
    val promotionPointsPerPurchase: Int,
    val openaiApiKey: String,
    val googleMapsApiKey: String
)

@Serializable
data class StoreSettingsUpdateRequest(
    val id: String,
    val storeName: String,
    val category: String,
    val email: String,
    val phone: String,
    val website: String,
    val storeLogo: String? = null,
    val pointsPerPurchase: Int,
    val promotionalEnabled: Boolean,
    val promotionPointsPerPurchase: Int,
    val openaiApiKey: String,
    val googleMapsApiKey: String
) 