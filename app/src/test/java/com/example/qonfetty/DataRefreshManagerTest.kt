package com.example.qonfetty

import com.example.qonfetty.data.DataRefreshManager
import com.example.qonfetty.data.SessionStorage
import com.example.qonfetty.data.SupabaseApi
import com.example.qonfetty.config.EnvironmentConfig
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class DataRefreshManagerTest {
    
    @Test
    fun `test DataRefreshManager creation`() = runTest {
        // Given
        val mockSupabaseApi = mock<SupabaseApi>()
        val mockSessionStorage = mock<SessionStorage>()
        
        // When
        val dataRefreshManager = DataRefreshManager(mockSupabaseApi, mockSessionStorage)
        
        // Then
        assertNotNull(dataRefreshManager)
    }
    
    @Test
    fun `test refresh state flow`() = runTest {
        // Given
        val mockSupabaseApi = mock<SupabaseApi>()
        val mockSessionStorage = mock<SessionStorage>()
        val dataRefreshManager = DataRefreshManager(mockSupabaseApi, mockSessionStorage)
        
        // When
        val refreshState = dataRefreshManager.refreshState.value
        
        // Then
        assertEquals(DataRefreshManager.RefreshState.Idle, refreshState)
    }
    
    @Test
    fun `test customers data flow`() = runTest {
        // Given
        val mockSupabaseApi = mock<SupabaseApi>()
        val mockSessionStorage = mock<SessionStorage>()
        val dataRefreshManager = DataRefreshManager(mockSupabaseApi, mockSessionStorage)
        
        // When
        val customersData = dataRefreshManager.customersData.value
        
        // Then
        assertEquals(emptyList<com.example.qonfetty.data.CustomerWithPoints>(), customersData)
    }
    
    @Test
    fun `test recent activity data flow`() = runTest {
        // Given
        val mockSupabaseApi = mock<SupabaseApi>()
        val mockSessionStorage = mock<SessionStorage>()
        val dataRefreshManager = DataRefreshManager(mockSupabaseApi, mockSessionStorage)
        
        // When
        val recentActivityData = dataRefreshManager.recentActivityData.value
        
        // Then
        assertEquals(emptyList<com.example.qonfetty.data.PointsTransactionWithCustomer>(), recentActivityData)
    }
    
    @Test
    fun `test customer points data flow`() = runTest {
        // Given
        val mockSupabaseApi = mock<SupabaseApi>()
        val mockSessionStorage = mock<SessionStorage>()
        val dataRefreshManager = DataRefreshManager(mockSupabaseApi, mockSessionStorage)
        
        // When
        val customerPointsData = dataRefreshManager.customerPointsData.value
        
        // Then
        assertEquals(emptyMap<String, Int>(), customerPointsData)
    }
} 