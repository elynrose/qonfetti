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
     * Main function to process NFC card and manage points
     * @param tag The NFC tag from the card
     * @return Result containing the processing outcome
     */
    suspend fun processNfcCard(tag: Tag): Result<NfcProcessingResult> = withContext(Dispatchers.IO) {
        try {
            Log.d("NfcPointsManager", "Starting NFC card processing")
            
            // Step 1: Read member ID from NFC card
            val memberIdResult = nfcManager.readMemberIdFromCard(tag)
            if (memberIdResult.isFailure) {
                Log.e("NfcPointsManager", "Failed to read member ID from NFC card")
                return@withContext Result.failure(Exception("Failed to read member ID from NFC card: ${memberIdResult.exceptionOrNull()?.message}"))
            }
            
            val memberId = memberIdResult.getOrNull() ?: return@withContext Result.failure(Exception("No member ID found on NFC card"))
            Log.d("NfcPointsManager", "Read member ID from NFC card: $memberId")
            
            // Step 2: Get authentication token
            val authToken = sessionStorage.getAuthToken()
            
            if (authToken == null) {
                Log.e("NfcPointsManager", "Not authenticated")
                return@withContext Result.failure(Exception("Not authenticated"))
            }
            
            // Step 3: Find customer by member ID
            val customerResult = findCustomerByMemberId(memberId, authToken)
            if (customerResult.isFailure) {
                Log.e("NfcPointsManager", "Failed to find customer with member ID: $memberId")
                return@withContext Result.failure(Exception("Customer not found with member ID: $memberId"))
            }
            
            val customer = customerResult.getOrNull() ?: return@withContext Result.failure(Exception("Customer not found"))
            Log.d("NfcPointsManager", "Found customer: ${customer.name} (ID: ${customer.id})")
            
            // Ensure customer ID is not null
            val customerId = customer.id ?: return@withContext Result.failure(Exception("Customer ID is null"))
            
            // Step 4: Check if NFC card is already registered to this customer
            val cardId = memberId // Assuming memberId is the NFC card ID
            val cardCheckResult = checkNfcCardRegistration(cardId, customerId, authToken)
            if (cardCheckResult.isFailure) {
                Log.e("NfcPointsManager", "Failed to check NFC card registration: ${cardCheckResult.exceptionOrNull()?.message}")
                return@withContext Result.failure(Exception("Failed to verify NFC card registration"))
            }
            
            val isCardRegistered = cardCheckResult.getOrNull() ?: false
            if (!isCardRegistered) {
                Log.w("NfcPointsManager", "NFC card not registered to customer. Registering card first...")
                
                // Register the card before awarding points
                val cardRegistrationResult = registerNfcCardIfNeeded(cardId, memberId, customerId, authToken)
                if (cardRegistrationResult.isFailure) {
                    Log.e("NfcPointsManager", "Failed to register NFC card: ${cardRegistrationResult.exceptionOrNull()?.message}")
                    return@withContext Result.failure(Exception("NFC card must be registered before awarding points"))
                } else {
                    Log.d("NfcPointsManager", "NFC card registered successfully, proceeding with points")
                }
            } else {
                Log.d("NfcPointsManager", "NFC card already registered to customer, proceeding with points")
            }
            
            // Step 5: Award points using the new database function
            val pointsResult = awardPointsToCustomer(customerId, 1, cardId, authToken)
            if (pointsResult.isFailure) {
                Log.e("NfcPointsManager", "Failed to award points to customer")
                return@withContext Result.failure(Exception("Failed to award points to customer: ${pointsResult.exceptionOrNull()?.message}"))
            }
            
            val newPoints = pointsResult.getOrNull() ?: return@withContext Result.failure(Exception("Failed to get updated points"))
            Log.d("NfcPointsManager", "Updated customer points to: ${newPoints.points}")
            
            // Step 6: Check for claimable rewards (we'll get the store ID from the points record)
            val rewardsResult = checkClaimableRewards(newPoints.storeId, newPoints.points, authToken)
            val claimableRewards = rewardsResult.getOrNull() ?: emptyList()
            
            Log.d("NfcPointsManager", "Found ${claimableRewards.size} claimable rewards")
            
            // Step 7: Return processing result
            val result = NfcProcessingResult(
                customer = customer,
                memberId = memberId,
                previousPoints = newPoints.points - 1, // Calculate previous points
                currentPoints = newPoints.points,
                pointsAdded = 1,
                claimableRewards = claimableRewards,
                storeId = newPoints.storeId
            )
            
            Log.d("NfcPointsManager", "NFC processing completed successfully")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error processing NFC card: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Find customer by member ID
     */
    private suspend fun findCustomerByMemberId(memberId: String, authToken: String): Result<Customer> {
        return try {
            Log.d("NfcPointsManager", "Searching for customer with member ID: $memberId")
            
            // Use the existing SupabaseApi method to search for customers
            // We'll need to add a new method to SupabaseApi for this
            val response = supabaseApi.findCustomerByMemberId(memberId, authToken)
            
            response.fold(
                onSuccess = { customer ->
                    if (customer != null) {
                        Log.d("NfcPointsManager", "Found customer: ${customer.name}")
                        Result.success(customer)
                    } else {
                        Log.d("NfcPointsManager", "No customer found with member ID: $memberId")
                        Result.failure(Exception("No customer found with member ID: $memberId"))
                    }
                },
                onFailure = { exception ->
                    Log.e("NfcPointsManager", "Failed to search for customer: ${exception.message}")
                    Result.failure(exception)
                }
            )
        } catch (e: Exception) {
            Log.e("NfcPointsManager", "Error searching for customer: ${e.message}", e)
            Result.failure(e)
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
}

/**
 * Data class for NFC processing results
 */
data class NfcProcessingResult(
    val customer: Customer,
    val memberId: String,
    val previousPoints: Int,
    val currentPoints: Int,
    val pointsAdded: Int,
    val claimableRewards: List<Reward>,
    val storeId: String
) 