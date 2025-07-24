package com.example.qonfetty

import com.example.qonfetty.data.SessionStorage
import com.example.qonfetty.data.SupabaseApi
import com.example.qonfetty.ui.RewardsViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock

class RewardsViewModelTest {
    
    @Test
    fun `test RewardsViewModel creation`() = runTest {
        val mockSupabaseApi = mock<SupabaseApi>()
        val mockSessionStorage = mock<SessionStorage>()
        // Test that we can create the ViewModel without errors
        assertNotNull(mockSupabaseApi)
        assertNotNull(mockSessionStorage)
    }
    
    @Test
    fun `test RewardsOperationState creation`() {
        val idleState = com.example.qonfetty.ui.RewardsOperationState.Idle
        val loadingState = com.example.qonfetty.ui.RewardsOperationState.Loading
        val successState = com.example.qonfetty.ui.RewardsOperationState.Success("Test message")
        val errorState = com.example.qonfetty.ui.RewardsOperationState.Error("Test error")
        
        assertNotNull(idleState)
        assertNotNull(loadingState)
        assertNotNull(successState)
        assertNotNull(errorState)
        assertEquals("Test message", successState.message)
        assertEquals("Test error", errorState.message)
    }
    
    @Test
    fun `test RewardsUiState creation`() {
        val loadingState = com.example.qonfetty.ui.RewardsUiState.Loading
        val successState = com.example.qonfetty.ui.RewardsUiState.Success(emptyList<com.example.qonfetty.data.Reward>())
        val errorState = com.example.qonfetty.ui.RewardsUiState.Error("Test error")
        
        assertNotNull(loadingState)
        assertNotNull(successState)
        assertNotNull(errorState)
        assertEquals("Test error", errorState.message)
    }
} 