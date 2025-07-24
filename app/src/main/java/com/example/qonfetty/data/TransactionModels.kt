package com.example.qonfetty.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Transaction(
    val id: String,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("reward_id")
    val rewardId: String? = null,
    @SerialName("transaction_type")
    val transactionType: String, // "purchase" or "reward_claim"
    val amount: Double,
    @SerialName("points_used")
    val pointsUsed: Int = 0,
    @SerialName("points_earned")
    val pointsEarned: Int = 0,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class TransactionStats(
    @SerialName("total_purchases")
    val totalPurchases: Double,
    @SerialName("total_claimed")
    val totalClaimed: Double,
    @SerialName("total_transactions")
    val totalTransactions: Int,
    @SerialName("total_points_earned")
    val totalPointsEarned: Int,
    @SerialName("total_points_used")
    val totalPointsUsed: Int
)

@Serializable
data class ClaimRewardRequest(
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("reward_id")
    val rewardId: String,
    val amount: Double,
    val description: String? = null
) 