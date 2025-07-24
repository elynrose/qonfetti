package com.example.qonfetty.nfc

import android.nfc.Tag
import android.util.Log
import com.example.qonfetty.data.*
import com.example.qonfetty.data.SupabaseApi
import com.example.qonfetty.data.SessionStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages NFC card reading, customer points, and rewards processing
 */
class NfcPointsManager(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage,
    private val nfcManager: NfcManager
) {
    
    /**
     * Process NFC card and award points to customer
     */
    suspend fun processNfcCard(tag: Tag): Result<NfcProcessingResult> {
        return try {
            Log.d("NfcPointsManager", "Processing NFC card")
            
            // Read member ID from the card
            val memberIdResult = nfcManager.readMemberIdFromCard(tag)
            val memberId = memberIdResult.getOrNull()
            if (memberId == null) {
                Log.e("NfcPointsManager", "Failed to read member ID from NFC card: ${memberIdResult.exceptionOrNull()?.message}")
                return Result.failure(Exception("Failed to read member ID from NFC card: ${memberIdResult.exceptionOrNull()?.message}"))
            }
            
            Log.d("NfcPointsManager", "Read member ID: $memberId")
            
            // Find customer by member ID
            val customer = findCustomerByMemberId(memberId)
            if (customer == null) {
                Log.e("NfcPointsManager", "Customer not found for member ID: $memberId")
                return Result.failure(Exception("Customer not found. Please register this customer first."))
            }
            
            Log.d("NfcPointsManager", "Found customer: ${customer.name}")
            
            // Look up NFC card
            val nfcCard = findNfcCard(memberId)
            if (nfcCard == null) {
                Log.e("NfcPointsManager", "NFC card not found for member ID: $memberId")
                return Result.failure(Exception("NFC card not registered. Please register this card first."))
            }
            
            Log.d("NfcPointsManager", "Found NFC card: ${nfcCard.cardId}")
            
            // Award points to customer
            val authToken = sessionStorage.getAuthToken()
            if (authToken == null) {
                Log.e("NfcPointsManager", "Not authenticated")
                return Result.failure(Exception("Not authenticated. Please login again."))
            }
            
            Log.d("NfcPointsManager", "Awarding 1 points to customer: ${customer.id} with NFC card: ${nfcCard.cardId}")
            
            val result = supabaseApi.awardPointsToCustomer(
                customerId = customer.id!!,
                points = 1,
                nfcCardId = nfcCard.cardId,
                authToken = authToken
            )
            
            result.fold(
                onSuccess = { customerPoints ->
                    Log.d("NfcPointsManager", "Successfully awarded 1 points to customer: ${customer.id}")
                    
                    // Extract the new points value from the CustomerPoints object
                    val newPoints = customerPoints.points
                    
                    // Calculate previous points (newPoints - pointsAwarded)
                    val previousPoints = newPoints - 1
                    
                    // Get claimable rewards
                    val storeId = sessionStorage.getStoreId()
                    if (storeId.isNullOrEmpty()) {
                        Log.e("NfcPointsManager", "Store ID is null or empty, skipping rewards")
                        val processingResult = NfcProcessingResult.Success(
                            customer = customer,
                            pointsAwarded = 1,
                            newTotalPoints = newPoints,
                            claimableRewards = emptyList(),
                            nfcCardId = nfcCard.cardId
                        )
                        return Result.success(processingResult)
                    }
                    
                    val rewardsResult = supabaseApi.getClaimableRewards(
                        storeId = storeId,
                        currentPoints = newPoints,
                        authToken = authToken
                    )
                    
                    val rewards = rewardsResult.getOrNull() ?: emptyList()
                    
                    val processingResult = NfcProcessingResult.Success(
                        customer = customer,
                        pointsAwarded = 1,
                        newTotalPoints = newPoints,
                        claimableRewards = rewards,
                        nfcCardId = nfcCard.cardId
                    )
                    
                    Result.success(processingResult)
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to award points: ${exception.message}", exception)
                    
                    // Handle specific authorization error
                    val errorMessage = when {
                        exception.message?.contains("not authorized") == true -> {
                            "Customer is not authorized for this store. Please add ${customer.name} to your customer list first."
                        }
                        exception.message?.contains("404") == true -> {
                            "Customer not found or not associated with this store. Please add ${customer.name} to your customer list first."
                        }
                        else -> {
                            "Failed to award points: ${exception.message}"
                        }
                    }
                    
                    Result.failure(Exception(errorMessage))
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error processing NFC card", e)
            Result.failure(Exception("Error processing NFC card: ${e.message}"))
        }
    }
    
    /**
     * Find customer by member ID
     */
    private suspend fun findCustomerByMemberId(memberId: String): Customer? {
        return try {
            Log.d("NfcPointsManager", "Searching for customer with member ID: $memberId")
            
            // Use the existing SupabaseApi method to search for customers
            // We'll need to add a new method to SupabaseApi for this
            val response = supabaseApi.findCustomerByMemberId(memberId, sessionStorage.getAuthToken() ?: "")
            
            response.fold(
                onSuccess = { customer ->
                    if (customer != null) {
                        Log.d("NfcPointsManager", "Found customer: ${customer.name}")
                        return@fold customer
                    } else {
                        Log.d("NfcPointsManager", "No customer found with member ID: $memberId")
                        return@fold null
                    }
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to search for customer: ${exception.message}")
                    return@fold null
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error searching for customer: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Get customer points for specific store
     */
    private suspend fun getCustomerPoints(customerId: String, storeId: String, authToken: String): Result<CustomerPoints?> {
        return try {
            Log.d("NfcPointsManager", "Getting points for customer: $customerId at store: $storeId")
            
            val response = supabaseApi.getCustomerPoints(customerId, storeId, authToken)
            
            response.fold(
                onSuccess = { points ->
                    Log.d("NfcPointsManager", "Found points: ${points?.points ?: 0}")
                    Result.success(points)
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to get customer points: ${exception.message}")
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error getting customer points: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create new customer points record
     */
    private suspend fun createCustomerPoints(customerId: String, storeId: String, points: Int, authToken: String): Result<CustomerPoints> {
        return try {
            Log.d("NfcPointsManager", "Creating points record: customer=$customerId, store=$storeId, points=$points")
            
            val response = supabaseApi.createCustomerPoints(customerId, storeId, points, authToken)
            
            response.fold(
                onSuccess = { createdPoints ->
                    Log.d("NfcPointsManager", "Created points record: ${createdPoints.points} points")
                    Result.success(createdPoints)
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to create points record: ${exception.message}")
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error creating points record: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Increment existing customer points
     */
    private suspend fun incrementCustomerPoints(customerId: String, storeId: String, newPoints: Int, authToken: String): Result<CustomerPoints> {
        return try {
            Log.d("NfcPointsManager", "Incrementing points: customer=$customerId, store=$storeId, newPoints=$newPoints")
            
            val response = supabaseApi.updateCustomerPoints(customerId, storeId, newPoints, authToken)
            
            response.fold(
                onSuccess = { updatedPoints ->
                    Log.d("NfcPointsManager", "Updated points to: ${updatedPoints.points}")
                    Result.success(updatedPoints)
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to update points: ${exception.message}")
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error updating points: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if NFC card is registered to a specific customer
     */
    private suspend fun checkNfcCardRegistration(cardId: String, customerId: String, authToken: String): Result<Boolean> {
        return try {
            Log.d("NfcPointsManager", "Checking if NFC card is registered to customer: $cardId -> $customerId")
            
            val existingCardResult = supabaseApi.getNfcCard(cardId, authToken)
            
            existingCardResult.fold(
                onSuccess = { existingCard ->
                    if (existingCard != null) {
                        val isRegistered = existingCard.customerId == customerId && existingCard.isActive
                        Log.d("NfcPointsManager", "NFC card registration check: $isRegistered (customer: ${existingCard.customerId}, active: ${existingCard.isActive})")
                        Result.success(isRegistered)
                    } else {
                        Log.d("NfcPointsManager", "NFC card not found in database")
                        Result.success(false)
                    }
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to check NFC card registration: ${exception.message}")
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error checking NFC card registration: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Register NFC card to customer if not already registered
     */
    private suspend fun registerNfcCardIfNeeded(cardId: String, memberId: String, customerId: String, authToken: String): Result<NfcCardResponse> {
        return try {
            Log.d("NfcPointsManager", "Registering NFC card if needed: $cardId")
            
            // Check if card is already registered
            val existingCard = supabaseApi.getNfcCard(cardId, authToken).getOrNull()
            
            if (existingCard != null) {
                Log.d("NfcPointsManager", "NFC card already registered: ${existingCard.cardId}")
                return Result.success(existingCard)
            }
            
            // Register the card
            val registrationResult = supabaseApi.registerNfcCard(cardId, memberId, customerId, authToken)
            
            registrationResult.fold(
                onSuccess = { nfcCard ->
                    Log.d("NfcPointsManager", "Successfully registered NFC card: ${nfcCard.cardId}")
                    Result.success(nfcCard)
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to register NFC card: ${exception.message}")
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error registering NFC card: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check for claimable rewards based on points
     */
    private suspend fun checkClaimableRewards(storeId: String, currentPoints: Int, authToken: String): Result<List<Reward>> {
        return try {
            Log.d("NfcPointsManager", "Checking rewards for store: $storeId with points: $currentPoints")
            
            val response = supabaseApi.getClaimableRewards(storeId, currentPoints, authToken)
            
            response.fold(
                onSuccess = { rewards ->
                    Log.d("NfcPointsManager", "Found ${rewards.size} claimable rewards")
                    Result.success(rewards)
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to get rewards: ${exception.message}")
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error checking rewards: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Award points to customer using the new database function
     */
    private suspend fun awardPointsToCustomer(customerId: String, points: Int, nfcCardId: String, authToken: String): Result<CustomerPoints> {
        return try {
            Log.d("NfcPointsManager", "Awarding $points points to customer: $customerId with NFC card: $nfcCardId")
            
            // Use the SupabaseApi function to award points
            val result = supabaseApi.awardPointsToCustomer(customerId, points, nfcCardId, authToken)
            
            result.fold(
                onSuccess = { customerPoints ->
                    Log.d("NfcPointsManager", "Successfully awarded $points points to customer: $customerId")
                    Result.success(customerPoints)
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to award points: ${exception.message}")
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error awarding points: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun findNfcCard(memberId: String): NfcCardResponse? {
        return try {
            Log.d("NfcPointsManager", "Searching for NFC card with member ID: $memberId")
            val authToken = sessionStorage.getAuthToken() ?: return null
            val response = supabaseApi.getNfcCard(memberId, authToken)
            response.fold(
                onSuccess = { nfcCard ->
                    if (nfcCard != null) {
                        Log.d("NfcPointsManager", "Found NFC card: ${nfcCard.cardId}")
                        return@fold nfcCard
                    } else {
                        Log.d("NfcPointsManager", "No NFC card found with member ID: $memberId")
                        return@fold null
                    }
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to search for NFC card: ${exception.message}", exception)
                    return@fold null
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error searching for NFC card: ${e.message}", e)
            return null
        }
    }
}

/**
 * Data class for NFC processing results
 */
sealed class NfcProcessingResult {
    data class Success(
        val customer: Customer,
        val pointsAwarded: Int,
        val newTotalPoints: Int,
        val claimableRewards: List<Reward>,
        val nfcCardId: String
    ) : NfcProcessingResult()

    data class Error(val message: String) : NfcProcessingResult()
} 