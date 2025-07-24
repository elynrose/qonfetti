package com.example.qonfetty.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qonfetty.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log

sealed class CustomerUiState {
    object Loading : CustomerUiState()
    data class Success(val customers: List<CustomerWithPoints>) : CustomerUiState()
    data class Error(val message: String) : CustomerUiState()
}

sealed class CustomerOperationState {
    object Idle : CustomerOperationState()
    object Loading : CustomerOperationState()
    data class Success(val message: String) : CustomerOperationState()
    data class Error(val message: String) : CustomerOperationState()
}

class CustomerViewModel(
    private val supabaseApi: SupabaseApi,
    private val sessionStorage: SessionStorage
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CustomerUiState>(CustomerUiState.Loading)
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()
    
    private val _operationState = MutableStateFlow<CustomerOperationState>(CustomerOperationState.Idle)
    val operationState: StateFlow<CustomerOperationState> = _operationState.asStateFlow()
    
    private val _customers = MutableStateFlow<List<CustomerWithPoints>>(emptyList())
    val customers: StateFlow<List<CustomerWithPoints>> = _customers.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    init {
        loadCustomers()
    }
    
    fun loadCustomers() {
        viewModelScope.launch {
            _uiState.value = CustomerUiState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _uiState.value = CustomerUiState.Error("Not authenticated")
                    return@launch
                }
                
                val result = supabaseApi.getCustomers(authToken)
                
                result.fold(
                    onSuccess = { customers ->
                        _customers.value = customers
                        _uiState.value = CustomerUiState.Success(customers)
                        Log.d("CustomerViewModel", "Loaded ${customers.size} customers")
                    },
                    onFailure = { exception ->
                        // Check if it's an authentication error
                        if (exception.message?.contains("401") == true || 
                            exception.message?.contains("Unauthorized") == true) {
                            Log.w("CustomerViewModel", "Authentication failed, clearing session")
                            sessionStorage.clearSession()
                            _uiState.value = CustomerUiState.Error("Session expired. Please login again.")
                        } else {
                            _uiState.value = CustomerUiState.Error(exception.message ?: "Failed to load customers")
                        }
                        Log.e("CustomerViewModel", "Failed to load customers: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = CustomerUiState.Error(e.message ?: "Unknown error")
                Log.e("CustomerViewModel", "Error loading customers: ${e.message}", e)
            }
        }
    }
    
    fun createCustomer(name: String, email: String, phone: String, address: String? = null) {
        viewModelScope.launch {
            _operationState.value = CustomerOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _operationState.value = CustomerOperationState.Error("Not authenticated")
                    return@launch
                }
                
                val request = CreateCustomerRequest(
                    name = name,
                    email = email,
                    phone = phone,
                    address = address
                )
                
                val result = supabaseApi.createCustomer(request, authToken)
                
                result.fold(
                    onSuccess = { customerWithPoints ->
                        // Add to current list
                        val currentCustomers = _customers.value.toMutableList()
                        currentCustomers.add(customerWithPoints)
                        _customers.value = currentCustomers
                        
                        // Update UI state to reflect the changes
                        val currentSearchQuery = _searchQuery.value
                        if (currentSearchQuery.isBlank()) {
                            _uiState.value = CustomerUiState.Success(currentCustomers)
                        } else {
                            // Re-apply search filter
                            val filteredCustomers = currentCustomers.filter { customer ->
                                customer.customer.name.contains(currentSearchQuery, ignoreCase = true) ||
                                customer.customer.email.contains(currentSearchQuery, ignoreCase = true) ||
                                customer.customer.phone.contains(currentSearchQuery, ignoreCase = true)
                            }
                            _uiState.value = CustomerUiState.Success(filteredCustomers)
                        }
                        
                        _operationState.value = CustomerOperationState.Success("Customer created successfully")
                        Log.d("CustomerViewModel", "Customer created: ${customerWithPoints.customer.name}")
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = CustomerOperationState.Idle
                    },
                    onFailure = { exception ->
                        _operationState.value = CustomerOperationState.Error(exception.message ?: "Failed to create customer")
                        Log.e("CustomerViewModel", "Failed to create customer: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _operationState.value = CustomerOperationState.Error(e.message ?: "Unknown error")
                Log.e("CustomerViewModel", "Error creating customer: ${e.message}", e)
            }
        }
    }
    
    fun updateCustomer(customerId: String, name: String?, email: String?, phone: String?, address: String?) {
        viewModelScope.launch {
            _operationState.value = CustomerOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _operationState.value = CustomerOperationState.Error("Not authenticated")
                    return@launch
                }
                
                val request = UpdateCustomerRequest(
                    name = name,
                    email = email,
                    phone = phone,
                    address = address
                )
                
                val result = supabaseApi.updateCustomer(customerId, request, authToken)
                
                result.fold(
                    onSuccess = { updatedCustomer ->
                        if (updatedCustomer != null) {
                            // Update in current list
                            val currentCustomers = _customers.value.toMutableList()
                            val index = currentCustomers.indexOfFirst { it.customer.id == customerId }
                            if (index != -1) {
                                currentCustomers[index] = updatedCustomer
                                _customers.value = currentCustomers
                                
                                // Update UI state to reflect the changes
                                val currentSearchQuery = _searchQuery.value
                                if (currentSearchQuery.isBlank()) {
                                    _uiState.value = CustomerUiState.Success(currentCustomers)
                                } else {
                                    // Re-apply search filter
                                    val filteredCustomers = currentCustomers.filter { customer ->
                                        customer.customer.name.contains(currentSearchQuery, ignoreCase = true) ||
                                        customer.customer.email.contains(currentSearchQuery, ignoreCase = true) ||
                                        customer.customer.phone.contains(currentSearchQuery, ignoreCase = true)
                                    }
                                    _uiState.value = CustomerUiState.Success(filteredCustomers)
                                }
                            }
                            
                            _operationState.value = CustomerOperationState.Success("Customer updated successfully")
                            Log.d("CustomerViewModel", "Customer updated: ${updatedCustomer.customer.name}")
                        } else {
                            _operationState.value = CustomerOperationState.Error("Customer not found")
                        }
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = CustomerOperationState.Idle
                    },
                    onFailure = { exception ->
                        _operationState.value = CustomerOperationState.Error(exception.message ?: "Failed to update customer")
                        Log.e("CustomerViewModel", "Failed to update customer: ${exception.message}", exception)
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(3000)
                        _operationState.value = CustomerOperationState.Idle
                    }
                )
            } catch (e: Exception) {
                _operationState.value = CustomerOperationState.Error(e.message ?: "Unknown error")
                Log.e("CustomerViewModel", "Error updating customer: ${e.message}", e)
                
                // Reset operation state after a delay
                kotlinx.coroutines.delay(3000)
                _operationState.value = CustomerOperationState.Idle
            }
        }
    }
    
    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            _operationState.value = CustomerOperationState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _operationState.value = CustomerOperationState.Error("Not authenticated")
                    return@launch
                }
                
                val result = supabaseApi.deleteCustomer(customerId, authToken)
                
                result.fold(
                    onSuccess = { success ->
                        if (success) {
                            // Remove from current list
                            val currentCustomers = _customers.value.toMutableList()
                            currentCustomers.removeAll { it.customer.id == customerId }
                            _customers.value = currentCustomers
                            
                            // Update UI state to reflect the changes
                            val currentSearchQuery = _searchQuery.value
                            if (currentSearchQuery.isBlank()) {
                                _uiState.value = CustomerUiState.Success(currentCustomers)
                            } else {
                                // Re-apply search filter
                                val filteredCustomers = currentCustomers.filter { customer ->
                                    customer.customer.name.contains(currentSearchQuery, ignoreCase = true) ||
                                    customer.customer.email.contains(currentSearchQuery, ignoreCase = true) ||
                                    customer.customer.phone.contains(currentSearchQuery, ignoreCase = true)
                                }
                                _uiState.value = CustomerUiState.Success(filteredCustomers)
                            }
                            
                            _operationState.value = CustomerOperationState.Success("Customer deleted successfully")
                            Log.d("CustomerViewModel", "Customer deleted: $customerId")
                        } else {
                            _operationState.value = CustomerOperationState.Error("Customer not found")
                        }
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(2000)
                        _operationState.value = CustomerOperationState.Idle
                    },
                    onFailure = { exception ->
                        _operationState.value = CustomerOperationState.Error(exception.message ?: "Failed to delete customer")
                        Log.e("CustomerViewModel", "Failed to delete customer: ${exception.message}", exception)
                        
                        // Reset operation state after a delay
                        kotlinx.coroutines.delay(3000)
                        _operationState.value = CustomerOperationState.Idle
                    }
                )
            } catch (e: Exception) {
                _operationState.value = CustomerOperationState.Error(e.message ?: "Unknown error")
                Log.e("CustomerViewModel", "Error deleting customer: ${e.message}", e)
                
                // Reset operation state after a delay
                kotlinx.coroutines.delay(3000)
                _operationState.value = CustomerOperationState.Idle
            }
        }
    }
    
    fun searchCustomers(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            // If search is empty, show all customers
            _uiState.value = CustomerUiState.Success(_customers.value)
            return
        }
        
        viewModelScope.launch {
            _uiState.value = CustomerUiState.Loading
            
            try {
                val authToken = sessionStorage.getAuthToken()
                
                if (authToken == null) {
                    _uiState.value = CustomerUiState.Error("Not authenticated")
                    return@launch
                }
                
                val result = supabaseApi.searchCustomers(query, authToken)
                
                result.fold(
                    onSuccess = { customers ->
                        _uiState.value = CustomerUiState.Success(customers)
                        Log.d("CustomerViewModel", "Search found ${customers.size} customers")
                    },
                    onFailure = { exception ->
                        _uiState.value = CustomerUiState.Error(exception.message ?: "Failed to search customers")
                        Log.e("CustomerViewModel", "Failed to search customers: ${exception.message}", exception)
                    }
                )
            } catch (e: Exception) {
                _uiState.value = CustomerUiState.Error(e.message ?: "Unknown error")
                Log.e("CustomerViewModel", "Error searching customers: ${e.message}", e)
            }
        }
    }
    
    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = CustomerUiState.Success(_customers.value)
    }
    
    fun clearOperationState() {
        _operationState.value = CustomerOperationState.Idle
    }
} 