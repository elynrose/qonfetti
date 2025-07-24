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
    
    // ==================== CUSTOMER MANAGEMENT FUNCTIONS ====================
    
    /**
     * Fetch all customers for the current user's store
     */
    suspend fun getCustomers(authToken: String): Result<List<CustomerWithPoints>> {
        return try {
            Log.d("SupabaseApi", "Fetching customers for current user's store")
            
            // Get customers for the current user's store (RLS policies will handle the filtering)
            val response = client.get("$baseUrl/rest/v1/customer_points") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            Log.d("SupabaseApi", "Customer points response status: ${response.status}")
            
            if (response.status.isSuccess()) {
                try {
                    val customerPoints = response.body<List<CustomerPoints>>()
                    Log.d("SupabaseApi", "Successfully parsed ${customerPoints.size} customer points records")
                    
                    if (customerPoints.isEmpty()) {
                        return Result.success(emptyList())
                    }
                    
                    // Get customer details for each customer_id
                    val customerIds = customerPoints.map { it.customerId }
                    val customerIdsParam = customerIds.joinToString(",")
                    
                    Log.d("SupabaseApi", "Fetching customers with IDs: $customerIdsParam")
                    
                    val customersResponse = client.get("$baseUrl/rest/v1/customers?id=in.($customerIdsParam)") {
                        headers {
                            append("apikey", anonKey)
                            append("Authorization", "Bearer $authToken")
                        }
                    }
                    
                    Log.d("SupabaseApi", "Customers response status: ${customersResponse.status}")
                    
                    if (customersResponse.status.isSuccess()) {
                        val customers = customersResponse.body<List<Customer>>()
                        Log.d("SupabaseApi", "Fetched ${customers.size} customers")
                        
                        // Combine customers with their points
                        val customersWithPoints = customers.map { customer ->
                            val points = customerPoints.find { it.customerId == customer.id }?.points ?: 0
                            CustomerWithPoints(customer = customer, points = points)
                        }
                        
                        Result.success(customersWithPoints)
                    } else {
                        Log.e("SupabaseApi", "Failed to fetch customers: ${customersResponse.status}")
                        Result.failure(Exception("Failed to fetch customers"))
                    }
                } catch (e: Exception) {
                    Log.e("SupabaseApi", "Error parsing customer points response: ${e.message}", e)
                    Result.failure(e)
                }
            } else {
                Log.e("SupabaseApi", "Failed to fetch customer points: ${response.status}")
                Result.failure(Exception("Failed to fetch customer points"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error fetching customers: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Fetch a single customer by ID for the current store
     */
    suspend fun getCustomer(customerId: String, storeId: String, authToken: String): Result<CustomerWithPoints?> {
        return try {
            Log.d("SupabaseApi", "Fetching customer: $customerId for store: $storeId")
            
            // First check if customer has points for this store
            val pointsResponse = client.get("$baseUrl/rest/v1/customer_points?customer_id=eq.$customerId&store_id=eq.$storeId") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            if (pointsResponse.status.isSuccess()) {
                val customerPoints = pointsResponse.body<List<CustomerPoints>>()
                
                if (customerPoints.isEmpty()) {
                    Log.d("SupabaseApi", "Customer not found in this store")
                    return Result.success(null)
                }
                
                // Get customer details
                val customerResponse = client.get("$baseUrl/rest/v1/customers?id=eq.$customerId") {
                    headers {
                        append("apikey", anonKey)
                        append("Authorization", "Bearer $authToken")
                    }
                }
                
                if (customerResponse.status.isSuccess()) {
                    val customers = customerResponse.body<List<Customer>>()
                    val customer = customers.firstOrNull()
                    
                    if (customer != null) {
                        val points = customerPoints.first().points
                        val customerWithPoints = CustomerWithPoints(customer = customer, points = points)
                        Log.d("SupabaseApi", "Fetched customer: $customerWithPoints")
                        Result.success(customerWithPoints)
                    } else {
                        Log.d("SupabaseApi", "Customer not found")
                        Result.success(null)
                    }
                } else {
                    Log.e("SupabaseApi", "Failed to fetch customer: ${customerResponse.status}")
                    Result.failure(Exception("Failed to fetch customer"))
                }
            } else {
                Log.e("SupabaseApi", "Failed to fetch customer points: ${pointsResponse.status}")
                Result.failure(Exception("Failed to fetch customer points"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error fetching customer: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create a new customer and add them to the current user's store
     */
    suspend fun createCustomer(
        request: CreateCustomerRequest,
        authToken: String
    ): Result<CustomerWithPoints> {
        return try {
            Log.d("SupabaseApi", "Creating customer: ${request.email}")
            
            // First check if customer already exists by email
            val existingCustomerResponse = client.get("$baseUrl/rest/v1/customers?email=eq.${request.email}") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            var customer: Customer
            
            if (existingCustomerResponse.status.isSuccess()) {
                val existingCustomers = existingCustomerResponse.body<List<Customer>>()
                val existingCustomer = existingCustomers.firstOrNull()
                
                if (existingCustomer != null) {
                    Log.d("SupabaseApi", "Customer already exists, using existing customer")
                    customer = existingCustomer
                } else {
                    // Create new customer
                    customer = createNewCustomer(request, authToken)
                }
            } else {
                // Create new customer
                customer = createNewCustomer(request, authToken)
            }
            
            // Use the new database function to award 0 points (which creates the customer-store relationship)
            val pointsResponse = client.post("$baseUrl/rest/v1/rpc/award_points_to_customer") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                setBody(AwardPointsRequest(
                    customerId = customer.id!!,
                    points = 0
                ))
            }
            
            if (pointsResponse.status.isSuccess()) {
                Log.d("SupabaseApi", "Customer added to store successfully")
                Result.success(CustomerWithPoints(customer = customer, points = 0))
            } else {
                val responseBody = pointsResponse.body<String>()
                Log.e("SupabaseApi", "Failed to add customer to store. Status: ${pointsResponse.status}, Body: $responseBody")
                Result.failure(Exception("Failed to add customer to store: ${pointsResponse.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Failed to create customer: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing customer
     */
    suspend fun updateCustomer(
        customerId: String,
        request: UpdateCustomerRequest,
        authToken: String
    ): Result<CustomerWithPoints?> {
        return try {
            Log.d("SupabaseApi", "Updating customer: $customerId")
            
            // Update customer
            val response = client.patch("$baseUrl/rest/v1/customers?id=eq.$customerId") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                setBody(request)
            }
            
            if (response.status.isSuccess()) {
                // Fetch updated customer with points for current store
                val customerResponse = client.get("$baseUrl/rest/v1/customers?id=eq.$customerId") {
                    headers {
                        append("apikey", anonKey)
                        append("Authorization", "Bearer $authToken")
                    }
                }
                
                if (customerResponse.status.isSuccess()) {
                    val customers = customerResponse.body<List<Customer>>()
                    val customer = customers.firstOrNull()
                    
                    if (customer != null) {
                        // Get points for this customer in current store
                        val pointsResponse = client.get("$baseUrl/rest/v1/customer_points?customer_id=eq.$customerId") {
                            headers {
                                append("apikey", anonKey)
                                append("Authorization", "Bearer $authToken")
                            }
                        }
                        
                        val points = if (pointsResponse.status.isSuccess()) {
                            val pointsList = pointsResponse.body<List<CustomerPoints>>()
                            pointsList.firstOrNull()?.points ?: 0
                        } else {
                            0
                        }
                        
                        val customerWithPoints = CustomerWithPoints(customer = customer, points = points)
                        Log.d("SupabaseApi", "Customer updated successfully")
                        Result.success(customerWithPoints)
                    } else {
                        Result.failure(Exception("Failed to fetch updated customer"))
                    }
                } else {
                    Result.failure(Exception("Failed to fetch updated customer"))
                }
            } else {
                try {
                    val error = response.body<CustomerErrorResponse>()
                    Log.e("SupabaseApi", "Failed to update customer: ${error.msg}")
                    Result.failure(Exception(error.msg))
                } catch (e: Exception) {
                    Log.e("SupabaseApi", "Failed to update customer: ${response.status} - ${e.message}")
                    Result.failure(Exception("Failed to update customer: ${response.status}"))
                }
            }
            
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error updating customer: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a customer from the store (removes customer_points entry)
     */
    suspend fun deleteCustomer(customerId: String, authToken: String): Result<Boolean> {
        return try {
            Log.d("SupabaseApi", "Deleting customer: $customerId from current store")
            
            // Delete customer_points entry for current store (RLS policies will handle the filtering)
            val response = client.delete("$baseUrl/rest/v1/customer_points?customer_id=eq.$customerId") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            if (response.status.isSuccess()) {
                Log.d("SupabaseApi", "Customer removed from store successfully")
                Result.success(true)
            } else {
                try {
                    val error = response.body<CustomerErrorResponse>()
                    Log.e("SupabaseApi", "Failed to delete customer: ${error.msg}")
                    Result.failure(Exception(error.msg))
                } catch (e: Exception) {
                    Log.e("SupabaseApi", "Failed to delete customer: ${response.status} - ${e.message}")
                    Result.failure(Exception("Failed to delete customer: ${response.status}"))
                }
            }
            
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error deleting customer: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Search customers by name or email
     */
    suspend fun searchCustomers(
        query: String,
        authToken: String
    ): Result<List<CustomerWithPoints>> {
        return try {
            Log.d("SupabaseApi", "Searching customers with query: '$query'")
            
            // Get all customers for this store first
            val allCustomers = getCustomers(authToken).getOrNull() ?: emptyList()
            
            // Filter by name or email
            val filteredCustomers = allCustomers.filter { customerWithPoints ->
                val customer = customerWithPoints.customer
                customer.name.contains(query, ignoreCase = true) ||
                customer.email.contains(query, ignoreCase = true)
            }
            
            Log.d("SupabaseApi", "Found ${filteredCustomers.size} matching customers")
            Result.success(filteredCustomers)
            
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error searching customers: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ==================== PRIVATE HELPER FUNCTIONS ====================
    
    private suspend fun createNewCustomer(request: CreateCustomerRequest, authToken: String): Customer {
        Log.d("SupabaseApi", "Creating new customer in database")
        
        // Generate member_id from email (simple hash)
        val memberId = request.email.hashCode().toString().replace("-", "")
        
        val customer = Customer(
            name = request.name,
            email = request.email,
            phone = request.phone,
            address = request.address,
            memberId = memberId
        )
        
        Log.d("SupabaseApi", "Customer to create: $customer")
        
        val response = client.post("$baseUrl/rest/v1/customers") {
            contentType(ContentType.Application.Json)
            headers {
                append("apikey", anonKey)
                append("Authorization", "Bearer $authToken")
            }
            setBody(customer)
        }
        
        Log.d("SupabaseApi", "Create customer response status: ${response.status}")
        
        if (response.status.isSuccess()) {
            // Supabase might return empty body on 201, so we need to fetch the created customer
            Log.d("SupabaseApi", "Customer creation successful, fetching created customer")
            
            // Fetch the customer we just created by email
            val fetchResponse = client.get("$baseUrl/rest/v1/customers?email=eq.${request.email}") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            Log.d("SupabaseApi", "Fetch customer response status: ${fetchResponse.status}")
            
            if (fetchResponse.status.isSuccess()) {
                val customers = fetchResponse.body<List<Customer>>()
                val createdCustomer = customers.firstOrNull()
                
                if (createdCustomer != null) {
                    Log.d("SupabaseApi", "New customer created: ${createdCustomer.id}")
                    return createdCustomer
                } else {
                    throw Exception("Customer was created but could not be fetched")
                }
            } else {
                throw Exception("Failed to fetch created customer")
            }
        } else {
            // Try to parse error response, but handle cases where response body might be empty or malformed
            try {
                val error = response.body<CustomerErrorResponse>()
                Log.e("SupabaseApi", "Failed to create customer: ${error.msg}")
                throw Exception("Failed to create customer: ${error.msg}")
            } catch (e: Exception) {
                // If parsing fails, it might be because the response body is empty or malformed
                Log.e("SupabaseApi", "Failed to create customer: ${response.status} - ${e.message}")
                throw Exception("Failed to create customer: ${response.status}")
            }
        }
    }

    /**
     * Register an NFC card for a customer using the new database function
     */
    suspend fun registerNfcCard(cardId: String, memberId: String, customerId: String, authToken: String): Result<NfcCardResponse> {
        return try {
            Log.d("SupabaseApi", "Registering NFC card with member ID: $memberId for customer: $customerId")
            
            // Use the new database function instead of direct table insert
            val response = client.post("$baseUrl/rest/v1/rpc/register_nfc_card") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                setBody(RegisterNfcCardRequest(
                    cardId = cardId,
                    memberId = memberId,
                    customerId = customerId
                ))
            }
            
            if (response.status.isSuccess()) {
                val nfcId = response.body<String>()
                Log.d("SupabaseApi", "Successfully registered NFC card: $cardId with ID: $nfcId")
                
                // Create a response object since the function returns just the ID
                val nfcCard = NfcCardResponse(
                    id = nfcId ?: "",
                    cardId = cardId,
                    memberId = memberId,
                    customerId = customerId,
                    storeId = "", // Will be set by the database function
                    isActive = true,
                    createdAt = null,
                    updatedAt = null
                )
                Result.success(nfcCard)
            } else {
                val responseBody = response.body<String>()
                Log.e("SupabaseApi", "Failed to register NFC card. Status: ${response.status}, Body: $responseBody")
                Result.failure(Exception("Failed to register NFC card: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Failed to register NFC card: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get NFC card by card ID
     */
    suspend fun getNfcCard(cardId: String, authToken: String): Result<NfcCardResponse?> {
        return try {
            Log.d("SupabaseApi", "Looking up NFC card: $cardId")
            
            val response = client.get("$baseUrl/rest/v1/nfc_cards?card_id=eq.$cardId&is_active=eq.true") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            if (response.status.isSuccess()) {
                val cards = response.body<List<NfcCardResponse>>()
                if (cards.isNotEmpty()) {
                    Log.d("SupabaseApi", "Found NFC card: ${cards.first().cardId}")
                    Result.success(cards.first())
                } else {
                    Log.d("SupabaseApi", "No NFC card found for: $cardId")
                    Result.success(null)
                }
            } else {
                Log.e("SupabaseApi", "Failed to lookup NFC card: ${response.status}")
                Result.failure(Exception("Failed to lookup NFC card: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error looking up NFC card: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all NFC cards for a customer
     */
    suspend fun getCustomerNfcCards(customerId: String, authToken: String): Result<List<NfcCardResponse>> {
        return try {
            Log.d("SupabaseApi", "Getting NFC cards for customer: $customerId")
            
            // First, let's get all cards for this customer without the is_active filter to debug
            val allCardsResponse = client.get("$baseUrl/rest/v1/nfc_cards?customer_id=eq.$customerId") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            if (allCardsResponse.status.isSuccess()) {
                val allCards = allCardsResponse.body<List<NfcCardResponse>>()
                Log.d("SupabaseApi", "Found ${allCards.size} total NFC cards for customer: $customerId")
                
                // Log details of each card for debugging
                allCards.forEach { card ->
                    Log.d("SupabaseApi", "Card: ${card.cardId}, MemberID: ${card.memberId}, Active: ${card.isActive}")
                }
                
                // Now filter for active cards
                val activeCards = allCards.filter { it.isActive }
                Log.d("SupabaseApi", "Found ${activeCards.size} active NFC cards for customer: $customerId")
                
                Result.success(activeCards)
            } else {
                Log.e("SupabaseApi", "Failed to get customer NFC cards: ${allCardsResponse.status}")
                Result.failure(Exception("Failed to get customer NFC cards: ${allCardsResponse.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error getting customer NFC cards: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Debug function to get all NFC cards in the database
     */
    suspend fun getAllNfcCards(authToken: String): Result<List<NfcCardResponse>> {
        return try {
            Log.d("SupabaseApi", "Getting all NFC cards for debugging")
            
            // Get current store ID first
            val storeId = getCurrentStoreId(authToken)
            if (storeId == null) {
                Log.e("SupabaseApi", "No store found for current user")
                return Result.failure(Exception("No store found for current user"))
            }
            
            val response = client.get("$baseUrl/rest/v1/nfc_cards") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                parameter("store_id", "eq.$storeId")
            }
            
            if (response.status.isSuccess()) {
                val allCards = response.body<List<NfcCardResponse>>()
                Log.d("SupabaseApi", "Found ${allCards.size} total NFC cards in database for store: $storeId")
                
                // Log details of each card for debugging
                allCards.forEach { card ->
                    Log.d("SupabaseApi", "Card: ${card.cardId}, MemberID: ${card.memberId}, CustomerID: ${card.customerId}, Active: ${card.isActive}")
                }
                
                Result.success(allCards)
            } else {
                Log.e("SupabaseApi", "Failed to get all NFC cards: ${response.status}")
                Result.failure(Exception("Failed to get all NFC cards: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error getting all NFC cards: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Deactivate an NFC card
     */
    suspend fun deactivateNfcCard(cardId: String, authToken: String): Result<Boolean> {
        return try {
            Log.d("SupabaseApi", "Deactivating NFC card: $cardId")
            
            val response = client.patch("$baseUrl/rest/v1/nfc_cards?card_id=eq.$cardId") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                setBody(DeactivateNfcCardRequest(isActive = false))
            }
            
            if (response.status.isSuccess()) {
                Log.d("SupabaseApi", "NFC card deactivated successfully: $cardId")
                Result.success(true)
            } else {
                Log.e("SupabaseApi", "Failed to deactivate NFC card: ${response.status}")
                Result.failure(Exception("Failed to deactivate NFC card: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error deactivating NFC card: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete an NFC card from the database
     */
    suspend fun deleteNfcCard(cardId: String, authToken: String): Result<Boolean> {
        return try {
            Log.d("SupabaseApi", "Deleting NFC card: $cardId")
            
            val response = client.delete("$baseUrl/rest/v1/nfc_cards?card_id=eq.$cardId") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            if (response.status.isSuccess()) {
                Log.d("SupabaseApi", "NFC card deleted successfully: $cardId")
                Result.success(true)
            } else {
                Log.e("SupabaseApi", "Failed to delete NFC card: ${response.status}")
                Result.failure(Exception("Failed to delete NFC card: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error deleting NFC card: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Find customer by member ID
     */
    suspend fun findCustomerByMemberId(memberId: String, authToken: String): Result<Customer?> {
        return try {
            Log.d("SupabaseApi", "Searching for customer with member ID: $memberId")
            
            val response = client.get("$baseUrl/rest/v1/customers?member_id=eq.$memberId") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            if (response.status.isSuccess()) {
                val customers = response.body<List<Customer>>()
                if (customers.isNotEmpty()) {
                    val customer = customers.first()
                    Log.d("SupabaseApi", "Found customer: ${customer.name}")
                    Result.success(customer)
                } else {
                    Log.d("SupabaseApi", "No customer found with member ID: $memberId")
                    Result.success(null)
                }
            } else {
                Log.e("SupabaseApi", "Failed to search for customer: ${response.status}")
                Result.failure(Exception("Failed to search for customer: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error searching for customer: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get customer points for specific store
     */
    suspend fun getCustomerPoints(customerId: String, storeId: String, authToken: String): Result<CustomerPoints?> {
        return try {
            Log.d("SupabaseApi", "Getting points for customer: $customerId at store: $storeId")
            
            val response = client.get("$baseUrl/rest/v1/customer_points?customer_id=eq.$customerId&store_id=eq.$storeId") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            if (response.status.isSuccess()) {
                val pointsList = response.body<List<CustomerPoints>>()
                val points = pointsList.firstOrNull()
                Log.d("SupabaseApi", "Found points: ${points?.points ?: 0}")
                Result.success(points)
            } else {
                Log.e("SupabaseApi", "Failed to get customer points: ${response.status}")
                Result.failure(Exception("Failed to get customer points: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error getting customer points: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create new customer points record
     */
    suspend fun createCustomerPoints(customerId: String, storeId: String, points: Int, authToken: String): Result<CustomerPoints> {
        return try {
            Log.d("SupabaseApi", "Creating points record: customer=$customerId, store=$storeId, points=$points")
            
            val pointsRecord = CustomerPoints(
                customerId = customerId,
                storeId = storeId,
                points = points
            )
            
            val response = client.post("$baseUrl/rest/v1/customer_points") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                setBody(pointsRecord)
            }
            
            if (response.status.isSuccess()) {
                val createdPoints = response.body<CustomerPoints>()
                Log.d("SupabaseApi", "Created points record: ${createdPoints.points} points")
                Result.success(createdPoints)
            } else {
                Log.e("SupabaseApi", "Failed to create points record: ${response.status}")
                Result.failure(Exception("Failed to create points record: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error creating points record: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update existing customer points
     */
    suspend fun updateCustomerPoints(customerId: String, storeId: String, newPoints: Int, authToken: String): Result<CustomerPoints> {
        return try {
            Log.d("SupabaseApi", "Updating points: customer=$customerId, store=$storeId, newPoints=$newPoints")
            
            val updateData = mapOf("points" to newPoints)
            
            val response = client.patch("$baseUrl/rest/v1/customer_points?customer_id=eq.$customerId&store_id=eq.$storeId") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                setBody(updateData)
            }
            
            if (response.status.isSuccess()) {
                // Get the updated record
                val getResponse = client.get("$baseUrl/rest/v1/customer_points?customer_id=eq.$customerId&store_id=eq.$storeId") {
                    headers {
                        append("apikey", anonKey)
                        append("Authorization", "Bearer $authToken")
                    }
                }
                
                if (getResponse.status.isSuccess()) {
                    val pointsList = getResponse.body<List<CustomerPoints>>()
                    val updatedPoints = pointsList.firstOrNull()
                    if (updatedPoints != null) {
                        Log.d("SupabaseApi", "Updated points to: ${updatedPoints.points}")
                        Result.success(updatedPoints)
                    } else {
                        Result.failure(Exception("Failed to retrieve updated points"))
                    }
                } else {
                    Result.failure(Exception("Failed to retrieve updated points: ${getResponse.status}"))
                }
            } else {
                Log.e("SupabaseApi", "Failed to update points: ${response.status}")
                Result.failure(Exception("Failed to update points: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error updating points: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Award points to a customer using the database function
     */
    suspend fun awardPointsToCustomer(customerId: String, points: Int, nfcCardId: String, authToken: String): Result<CustomerPoints> {
        return try {
            Log.d("SupabaseApi", "Awarding $points points to customer: $customerId with NFC card: $nfcCardId")
            
            // Use the database function to award points
            val response = client.post("$baseUrl/rest/v1/rpc/award_points_to_customer") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                setBody(AwardPointsRequest(
                    customerId = customerId,
                    points = points,
                    nfcCardId = nfcCardId
                ))
            }
            
            if (response.status.isSuccess()) {
                val newPoints = response.body<Int>()
                Log.d("SupabaseApi", "Successfully awarded $points points to customer: $customerId")
                Result.success(CustomerPoints(customerId = customerId, storeId = "store", points = newPoints))
            } else {
                Log.e("SupabaseApi", "Failed to award points: ${response.status}")
                Result.failure(Exception("Failed to award points: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error awarding points to customer", e)
            Result.failure(e)
        }
    }

    /**
     * Get recent activity (points transactions) for a specific store with customer information
     */
    suspend fun getRecentActivityWithCustomerInfo(storeId: String, authToken: String): Result<List<PointsTransactionWithCustomer>> {
        return try {
            Log.d("SupabaseApi", "Fetching recent activity with customer info for store: $storeId")
            
            // First, get the transactions
            val transactionsResponse = client.get("$baseUrl/rest/v1/points_transactions") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                parameter("store_id", "eq.$storeId")
                parameter("order", "created_at.desc")
                parameter("limit", "100")
            }
            
            if (!transactionsResponse.status.isSuccess()) {
                Log.e("SupabaseApi", "Failed to fetch transactions: ${transactionsResponse.status}")
                return Result.failure(Exception("Failed to fetch transactions: ${transactionsResponse.status}"))
            }
            
            val transactions = transactionsResponse.body<List<PointsTransaction>>()
            Log.d("SupabaseApi", "Fetched ${transactions.size} transactions for store: $storeId")
            
            // Log transaction details for debugging
            transactions.forEach { transaction ->
                Log.d("SupabaseApi", "Transaction: ID=${transaction.id}, Store=${transaction.storeId}, Customer=${transaction.customerId}, Points=${transaction.pointsAwarded}")
            }
            
            // Get unique customer IDs
            val customerIds = transactions.map { it.customerId }.distinct()
            Log.d("SupabaseApi", "Found ${customerIds.size} unique customers to fetch")
            
            // Fetch customer information for all customers
            val customersMap = mutableMapOf<String, Customer>()
            if (customerIds.isNotEmpty()) {
                val customerIdsParam = customerIds.joinToString(",")
                val customersResponse = client.get("$baseUrl/rest/v1/customers") {
                    headers {
                        append("apikey", anonKey)
                        append("Authorization", "Bearer $authToken")
                    }
                    parameter("id", "in.($customerIdsParam)")
                }
                
                if (customersResponse.status.isSuccess()) {
                    val customers = customersResponse.body<List<Customer>>()
                    customers.forEach { customer ->
                        customer.id?.let { customersMap[it] = customer }
                    }
                    Log.d("SupabaseApi", "Fetched ${customers.size} customers")
                } else {
                    Log.e("SupabaseApi", "Failed to fetch customers: ${customersResponse.status}")
                }
            }
            
            // Combine transactions with customer information
            val enrichedTransactions = transactions.map { transaction ->
                val customer = customersMap[transaction.customerId]
                PointsTransactionWithCustomer(
                    id = transaction.id,
                    customerId = transaction.customerId,
                    storeId = transaction.storeId,
                    nfcCardId = transaction.nfcCardId,
                    pointsAwarded = transaction.pointsAwarded,
                    previousPoints = transaction.previousPoints,
                    newPoints = transaction.newPoints,
                    transactionType = transaction.transactionType,
                    description = transaction.description,
                    createdAt = transaction.createdAt,
                    customerName = customer?.name,
                    customerEmail = customer?.email,
                    customerPhone = customer?.phone
                )
            }
            
            Log.d("SupabaseApi", "Successfully created ${enrichedTransactions.size} enriched transactions for store: $storeId")
            Result.success(enrichedTransactions)
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error fetching recent activity with customer info", e)
            Result.failure(e)
        }
    }

    /**
     * Get recent activity (points transactions) for the current store
     */
    suspend fun getRecentActivity(authToken: String): Result<List<PointsTransactionWithCustomer>> {
        return try {
            Log.d("SupabaseApi", "Fetching recent activity for current store")
            
            // Get current store ID first
            val storeId = getCurrentStoreId(authToken)
            if (storeId == null) {
                Log.e("SupabaseApi", "No store found for current user")
                return Result.failure(Exception("No store found for current user"))
            }
            
            // Use the overloaded method
            getRecentActivityWithCustomerInfo(storeId, authToken)
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error fetching recent activity", e)
            Result.failure(e)
        }
    }

    /**
     * Get recent activity for a specific customer
     */
    suspend fun getCustomerRecentActivity(customerId: String, authToken: String): Result<List<PointsTransaction>> {
        return try {
            Log.d("SupabaseApi", "Fetching recent activity for customer: $customerId")
            
            // Get current store ID first
            val storeId = getCurrentStoreId(authToken)
            if (storeId == null) {
                Log.e("SupabaseApi", "No store found for current user")
                return Result.failure(Exception("No store found for current user"))
            }
            
            val response = client.get("$baseUrl/rest/v1/points_transactions") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                parameter("customer_id", "eq.$customerId")
                parameter("store_id", "eq.$storeId")
                parameter("order", "created_at.desc")
                parameter("limit", "20")
            }
            
            if (response.status.isSuccess()) {
                val transactions = response.body<List<PointsTransaction>>()
                Log.d("SupabaseApi", "Successfully fetched ${transactions.size} transactions for customer: $customerId in store: $storeId")
                Result.success(transactions)
            } else {
                Log.e("SupabaseApi", "Failed to fetch customer activity: ${response.status}")
                Result.failure(Exception("Failed to fetch customer activity: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error fetching customer activity", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get claimable rewards for a store based on points, including shared rewards from other stores
     */
    suspend fun getClaimableRewards(storeId: String, currentPoints: Int, authToken: String): Result<List<Reward>> {
        return try {
            Log.d("SupabaseApi", "Getting claimable rewards for store: $storeId with points: $currentPoints")
            
            // Get store-specific rewards
            val storeRewardsResponse = client.get("$baseUrl/rest/v1/rewards?store_id=eq.$storeId&points_required=lte.$currentPoints&is_active=eq.true") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            // Get shared rewards from other stores
            val sharedRewardsResponse = client.get("$baseUrl/rest/v1/rewards?store_id=neq.$storeId&is_shared=eq.true&points_required=lte.$currentPoints&is_active=eq.true") {
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
            }
            
            val allRewards = mutableListOf<Reward>()
            
            if (storeRewardsResponse.status.isSuccess()) {
                val storeRewards = storeRewardsResponse.body<List<Reward>>()
                allRewards.addAll(storeRewards)
                Log.d("SupabaseApi", "Found ${storeRewards.size} store-specific rewards")
            } else {
                Log.e("SupabaseApi", "Failed to get store rewards: ${storeRewardsResponse.status}")
            }
            
            if (sharedRewardsResponse.status.isSuccess()) {
                val sharedRewards = sharedRewardsResponse.body<List<Reward>>()
                allRewards.addAll(sharedRewards)
                Log.d("SupabaseApi", "Found ${sharedRewards.size} shared rewards from other stores")
            } else {
                Log.e("SupabaseApi", "Failed to get shared rewards: ${sharedRewardsResponse.status}")
            }
            
            Log.d("SupabaseApi", "Total claimable rewards: ${allRewards.size}")
            Result.success(allRewards)
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error getting rewards: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Claim a reward for a customer
     */
    suspend fun claimReward(customerId: String, rewardId: String, storeId: String, authToken: String): Result<RewardClaim> {
        return try {
            Log.d("SupabaseApi", "Claiming reward: customer=$customerId, reward=$rewardId, store=$storeId")
            
            val claimData = mapOf(
                "customer_id" to customerId,
                "reward_id" to rewardId,
                "store_id" to storeId,
                "claimed_at" to java.time.Instant.now().toString(),
                "is_claimed" to true
            )
            
            val response = client.post("$baseUrl/rest/v1/reward_claims") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                setBody(claimData)
            }
            
            if (response.status.isSuccess()) {
                val claim = response.body<RewardClaim>()
                Log.d("SupabaseApi", "Reward claimed successfully")
                Result.success(claim)
            } else {
                Log.e("SupabaseApi", "Failed to claim reward: ${response.status}")
                Result.failure(Exception("Failed to claim reward: ${response.status}"))
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error claiming reward: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get the current store ID for the authenticated user
     */
    private suspend fun getCurrentStoreId(authToken: String): String? {
        return try {
            Log.d("SupabaseApi", "Getting current store ID for user")
            
            val response = client.post("$baseUrl/rest/v1/rpc/get_or_create_store") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", anonKey)
                    append("Authorization", "Bearer $authToken")
                }
                setBody(mapOf("store_name" to "My Store"))
            }
            
            if (response.status.isSuccess()) {
                val storeId = response.body<String>()
                Log.d("SupabaseApi", "Current store ID: $storeId")
                storeId
            } else {
                Log.e("SupabaseApi", "Failed to get current store ID: ${response.status}")
                null
            }
        } catch (e: Exception) {
            Log.e("SupabaseApi", "Error getting current store ID: ${e.message}", e)
            null
        }
    }
} 