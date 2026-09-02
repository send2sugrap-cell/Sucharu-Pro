package com.sucharu.sucharupro.ui.features.returns.analytics

import com.sucharu.sucharupro.data.datasource.FakeReturnAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.returns.ReturnAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * ViewModel unit tests for [ReturnAnalyticsDashboardViewModel] (Module 11 Step 06 Chunk 04).
 */
class ReturnAnalyticsViewModelTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var analyticsDataSource: FakeReturnAnalyticsDataSource
    private lateinit var repository: ReturnAnalyticsRepositoryImpl
    private lateinit var viewModel: ReturnAnalyticsDashboardViewModel
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val projectId = "PRJ-VM-01"

    @Before
    fun setUp() {
        returnDataSource = FakeReturnDataSource()
        analyticsDataSource = FakeReturnAnalyticsDataSource()
        repository = ReturnAnalyticsRepositoryImpl(returnDataSource, analyticsDataSource)
        viewModel = ReturnAnalyticsDashboardViewModel(repository, coroutineScope = testScope)
    }

    @Test
    fun `test loadAnalytics success updates UiState`() = runBlocking {
        val now = System.currentTimeMillis()
        val ret = ReturnRequest(
            returnId = "RET-01",
            projectId = projectId,
            returnNo = "RN-01",
            customerId = "C-01",
            originalChallanId = "CH-01",
            status = ReturnStatus.PROCESSED,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "USER-1",
            createdAt = now
        )
        val items = listOf(
            ReturnItem("RI-01", "RET-01", "P-1", "CI-1", 100, 100, 0, "PCS")
        )
        returnDataSource.insertReturn(ret, items)

        viewModel.loadAnalytics(projectId, callerRole = UserRole.ADMIN)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNotNull(state.summary)
        assertEquals(1, state.summary!!.totalReturns)
        assertEquals(100, state.summary!!.totalRequestedQuantity)
    }

    @Test
    fun `test unauthorized role sets errorMessage`() = runBlocking {
        viewModel.loadAnalytics(projectId, callerRole = UserRole.CUSTOMER)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }
}
