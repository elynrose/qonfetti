package com.example.qonfetty

import com.example.qonfetty.data.SessionStorage
import com.example.qonfetty.data.SupabaseApi
import com.example.qonfetty.ui.AuthUiState
import com.example.qonfetty.ui.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    
    private lateinit var viewModel: AuthViewModel
    private lateinit var supabaseApi: SupabaseApi
    private lateinit var sessionStorage: SessionStorage
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        supabaseApi = mock()
        sessionStorage = mock()
        viewModel = AuthViewModel(supabaseApi, sessionStorage)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state should be Initial`() = runTest {
        val initialState = viewModel.uiState.first()
        assertEquals(AuthUiState.Initial, initialState)
    }
    
    // @Test
    // fun `initial login state should be false`() = runTest {
    //     // The initial state should be false before any async operations
    //     val isLoggedIn = viewModel.isLoggedIn.first()
    //     assertEquals(false, isLoggedIn)
    // }
} 