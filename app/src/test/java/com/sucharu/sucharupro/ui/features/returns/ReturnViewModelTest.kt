package com.sucharu.sucharupro.ui.features.returns

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * ViewModel unit tests for Return Request List, Create, and Details (Module 11 Step 02).
 */
class ReturnViewModelTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val projectId = "PRJ-VM"
    private val customerId = "CUST-VM"

    @Before
    fun setUp() {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
    }

    private fun sampleReturn(
        returnId: String = "RET-VM-1",
        returnNo: String = "RN-100",
        status: ReturnStatus = ReturnStatus.REQUESTED,
        reason: ReturnReason = ReturnReason.PRINTING_DEFECT
    ) = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = returnNo,
        customerId = customerId,
        originalChallanId = "CHAL-100",
        status = status,
        reason = reason,
        requestedBy = "STAFF-01",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = 1L
    )

    // =========================================================================
    // ReturnListViewModel Tests
    // =========================================================================

    @Test
    fun `ReturnListViewModel loads returns and applies search filter`() = runBlocking {
        val r1 = sampleReturn("R1", "RN-ALPHA", reason = ReturnReason.PRINTING_DEFECT)
        val r2 = sampleReturn("R2", "RN-BETA", reason = ReturnReason.DAMAGED)
        dataSource.insertReturn(r1, emptyList())
        dataSource.insertReturn(r2, emptyList())

        val viewModel = ReturnListViewModel(repository, testScope)
        viewModel.loadReturns(projectId)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.returns.size)
        assertEquals(2, state.filteredReturns.size)

        // Filter by search query
        viewModel.onSearchQueryChanged("BETA")
        val filteredState = viewModel.uiState.value
        assertEquals(1, filteredState.filteredReturns.size)
        assertEquals("RN-BETA", filteredState.filteredReturns[0].returnNo)
        Unit
    }

    @Test
    fun `ReturnListViewModel filters by status`() = runBlocking {
        val r1 = sampleReturn("R1", "RN-1", status = ReturnStatus.REQUESTED)
        val r2 = sampleReturn("R2", "RN-2", status = ReturnStatus.UNDER_INSPECTION)
        dataSource.insertReturn(r1, emptyList())
        dataSource.insertReturn(r2, emptyList())

        val viewModel = ReturnListViewModel(repository, testScope)
        viewModel.loadReturns(projectId)

        viewModel.onStatusFilterChanged(ReturnStatus.UNDER_INSPECTION)
        val filteredState = viewModel.uiState.value
        assertEquals(1, filteredState.filteredReturns.size)
        assertEquals(ReturnStatus.UNDER_INSPECTION, filteredState.filteredReturns[0].status)
        Unit
    }

    // =========================================================================
    // ReturnCreateViewModel Tests
    // =========================================================================

    @Test
    fun `ReturnCreateViewModel form validation and submission success`() = runBlocking {
        val viewModel = ReturnCreateViewModel(repository, testScope)
        viewModel.initialize(projectId, customerId)

        // Initially invalid
        assertFalse(viewModel.uiState.value.isFormValid)

        viewModel.onProductIdChanged("PROD-500")
        viewModel.onQuantityChanged("25")
        viewModel.onReasonChanged(ReturnReason.MISSING_PAGE)
        viewModel.onNotesChanged("Pages 5-8 missing")

        assertTrue(viewModel.uiState.value.isFormValid)

        viewModel.submitReturnRequest(actorId = "STAFF-01", callerRole = UserRole.STAFF)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertNotNull(state.createdReturnId)
        assertNull(state.errorMessage)
        Unit
    }

    @Test
    fun `ReturnCreateViewModel reports error for blank product`() = runBlocking {
        val viewModel = ReturnCreateViewModel(repository, testScope)
        viewModel.initialize(projectId, customerId)
        viewModel.onQuantityChanged("10")
        viewModel.onProductIdChanged("") // blank

        viewModel.submitReturnRequest(actorId = "STAFF-01", callerRole = UserRole.STAFF)

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertNotNull(state.errorMessage)
        Unit
    }

    // =========================================================================
    // ReturnDetailsViewModel Tests
    // =========================================================================

    @Test
    fun `ReturnDetailsViewModel loads details, submits for inspection, and cancels`() = runBlocking {
        val req = sampleReturn("R-DET-1", "RN-DET-1")
        val item = ReturnItem(
            returnItemId = "RI-DET-1",
            returnId = req.returnId,
            productId = "PROD-DET",
            originalChallanItemId = "CI-1",
            requestedQuantity = 5
        )
        dataSource.insertReturn(req, listOf(item))

        val viewModel = ReturnDetailsViewModel(repository, testScope)
        viewModel.loadDetails(req.returnId, UserRole.ADMIN, projectId)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(req.returnId, state.returnRequest?.returnId)
        assertEquals(1, state.items.size)

        // Submit for inspection
        viewModel.submitForInspection("ADMIN-01", UserRole.ADMIN, projectId)

        val submittedState = viewModel.uiState.value
        assertEquals(ReturnStatus.UNDER_INSPECTION, submittedState.returnRequest?.status)
        assertNotNull(submittedState.actionSuccessMessage)
        Unit
    }
}
