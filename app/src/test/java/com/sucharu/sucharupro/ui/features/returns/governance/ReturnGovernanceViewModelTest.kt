package com.sucharu.sucharupro.ui.features.returns.governance

import com.sucharu.sucharupro.data.datasource.FakeReturnAnalyticsDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnAnalyticsRepositoryImpl
import com.sucharu.sucharupro.domain.model.returns.ReturnException
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnExceptionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ViewModel unit tests for [ReturnGovernanceViewModel] (Module 11 Step 06 Chunk 04).
 */
class ReturnGovernanceViewModelTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var analyticsDataSource: FakeReturnAnalyticsDataSource
    private lateinit var repository: ReturnAnalyticsRepositoryImpl
    private lateinit var viewModel: ReturnGovernanceViewModel
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val projectId = "PRJ-GOV-VM-01"

    @Before
    fun setUp() {
        returnDataSource = FakeReturnDataSource()
        analyticsDataSource = FakeReturnAnalyticsDataSource()
        repository = ReturnAnalyticsRepositoryImpl(returnDataSource, analyticsDataSource)
        viewModel = ReturnGovernanceViewModel(repository, coroutineScope = testScope)
    }

    @Test
    fun `test loadExceptions updates UiState`() = runBlocking {
        val ex = ReturnException(
            exceptionId = "EX-VM-01",
            projectId = projectId,
            exceptionType = ReturnExceptionType.AGING_UNINSPECTED,
            description = "Aging alert",
            idempotencyKey = "K-01"
        )
        analyticsDataSource.insertOrUpdateException(ex)

        viewModel.loadExceptions(projectId, callerRole = UserRole.ADMIN)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(1, state.exceptions.size)
        assertEquals("EX-VM-01", state.exceptions.first().exceptionId)
    }

    @Test
    fun `test acknowledgeException updates exception state in UiState`() = runBlocking {
        val ex = ReturnException(
            exceptionId = "EX-VM-02",
            projectId = projectId,
            exceptionType = ReturnExceptionType.UNSETTLED_PROCESSED,
            description = "Unsettled alert",
            version = 1L,
            idempotencyKey = "K-02"
        )
        analyticsDataSource.insertOrUpdateException(ex)

        viewModel.loadExceptions(projectId, callerRole = UserRole.MANAGER)

        viewModel.acknowledgeException("EX-VM-02", "MANAGER-1", callerRole = UserRole.MANAGER)

        val state = viewModel.uiState.value
        assertFalse(state.isActionInProgress)
        val updated = state.exceptions.find { it.exceptionId == "EX-VM-02" }
        assertNotNull(updated)
        assertEquals(ReturnExceptionStatus.ACKNOWLEDGED, updated!!.status)
        assertEquals("MANAGER-1", updated.acknowledgedBy)
    }
}
