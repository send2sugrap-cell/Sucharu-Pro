package com.sucharu.sucharupro.ui.customer.myarea

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerMyAreaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CustomerMyAreaViewModel

    private val customerPrincipal = AuthenticatedPrincipal(
        userId = "cus-001",
        projectId = "PRJ-001",
        username = "customer_user",
        role = UserRole.CUSTOMER,
        customerId = "cus-001"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CustomerMyAreaViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadMyArea_customerPrincipal_populatesAccountSummary() = runTest {
        viewModel.loadMyArea(customerPrincipal)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State must be Success for customer principal", state is CustomerMyAreaUiState.Success)

        val successState = state as CustomerMyAreaUiState.Success
        val summary = successState.summary

        assertNotNull(summary.profile)
        assertNotNull(summary.financials)
        assertTrue(summary.activeOrders.isNotEmpty())
        assertTrue(summary.quotations.isNotEmpty())
        assertTrue(summary.documents.isNotEmpty())

        assertEquals("cus-001", summary.profile.customerId)
        assertEquals("৳50,000.00", summary.financials.totalOutstandingDueFormatted)
    }

    @Test
    fun testRefresh_updatesStateCleanly() = runTest {
        viewModel.refresh(customerPrincipal)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CustomerMyAreaUiState.Success)
        val summary = (state as CustomerMyAreaUiState.Success).summary
        assertNotNull(summary.profile)
    }
}
