package com.example.qonfetty.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.time.LocalDateTime

@Serializable
data class Customer(
    val id: String? = null,
    val name: String,
    val email: String,
    val phone: String,
    val address: String? = null,
    @SerialName("member_id")
    val memberId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class CustomerPoints(
    val id: String? = null,
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("store_id")
    val storeId: String,
    val points: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class CustomerWithPoints(
    val customer: Customer,
    val points: Int = 0
)

@Serializable
data class CreateCustomerRequest(
    val name: String,
    val email: String,
    val phone: String,
    val address: String? = null
)

@Serializable
data class UpdateCustomerRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null
)

@Serializable
data class CustomerResponse(
    val data: List<CustomerWithPoints>? = null,
    val error: String? = null
)

@Serializable
data class SingleCustomerResponse(
    val data: CustomerWithPoints? = null,
    val error: String? = null
)

@Serializable
data class CustomerErrorResponse(
    val code: Int? = null,
    @SerialName("error_code")
    val errorCode: String? = null,
    val msg: String? = null
)

@Serializable
data class AwardPointsRequest(
    @SerialName("p_customer_id")
    val customerId: String,
    @SerialName("p_points")
    val points: Int,
    @SerialName("p_nfc_card_id")
    val nfcCardId: String? = null
)

@Serializable
data class RegisterNfcCardRequest(
    @SerialName("p_card_id")
    val cardId: String,
    @SerialName("p_member_id")
    val memberId: String,
    @SerialName("p_customer_id")
    val customerId: String
)

@Serializable
data class DeactivateNfcCardRequest(
    @SerialName("is_active")
    val isActive: Boolean
)

@Serializable
data class PointsTransaction(
    val id: String? = null,
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("nfc_card_id")
    val nfcCardId: String? = null,
    @SerialName("points_awarded")
    val pointsAwarded: Int,
    @SerialName("previous_points")
    val previousPoints: Int,
    @SerialName("new_points")
    val newPoints: Int,
    @SerialName("transaction_type")
    val transactionType: String,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class PointsTransactionWithCustomer(
    val id: String? = null,
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("nfc_card_id")
    val nfcCardId: String? = null,
    @SerialName("points_awarded")
    val pointsAwarded: Int,
    @SerialName("previous_points")
    val previousPoints: Int,
    @SerialName("new_points")
    val newPoints: Int,
    @SerialName("transaction_type")
    val transactionType: String,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    // Customer information
    @SerialName("customer_name")
    val customerName: String? = null,
    @SerialName("customer_email")
    val customerEmail: String? = null,
    @SerialName("customer_phone")
    val customerPhone: String? = null
)

@Serializable
data class NfcCard(
    val id: String? = null,
    val cardId: String, // The actual NFC card UID
    val memberId: String, // Links to customer's member_id
    val customerId: String, // Links to customer's id
    val storeId: String, // Store where it was registered
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class NfcCardRegistration(
    @SerialName("card_id")
    val cardId: String,
    @SerialName("member_id")
    val memberId: String,
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
data class Reward(
    val id: String,
    val name: String,
    val description: String?,
    val photo: String? = null,
    val price: Double? = null,
    val quantity: Int? = null,
    val category: String? = null,
    @SerialName("is_shared")
    val isShared: Boolean = false,
    @SerialName("points_required")
    val pointsRequired: Int,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class RewardClaim(
    val id: String,
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("reward_id")
    val rewardId: String,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("claimed_at")
    val claimedAt: String,
    @SerialName("is_claimed")
    val isClaimed: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class NfcCardResponse(
    val id: String,
    @SerialName("card_id")
    val cardId: String,
    @SerialName("member_id")
    val memberId: String,
    @SerialName("customer_id")
    val customerId: String,
    @SerialName("store_id")
    val storeId: String,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

sealed class NfcOperationState {
    object Idle : NfcOperationState()
    object Loading : NfcOperationState()
    data class Success(val message: String) : NfcOperationState()
    data class Error(val message: String) : NfcOperationState()
} 