package com.sucharu.sucharupro.ui.features.returns

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
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
 * ViewModel integration tests for Return Settlement (Module 11 Step 05 Chunk 05).
 */
class ReturnDetailsViewModelSettlementTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl
    private lateinit var viewModel: ReturnDetailsViewModel
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val projectId = "PRJ-VM-01"
    private val customerId = "CUST-VM-01"
    private val returnId = "RET-VM-101"
    private val actorId = "ACTOR-ACCOUNTS-01"

    private val testReturn = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = "RN-VM-101",
        customerId = customerId,
        originalChallanId = "CHAL-VM-01",
        status = ReturnStatus.PROCESSED,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "CUST-USER",
        version = 1L
    )

    private val testItem = ReturnItem(
        returnItemId = "RI-VM-01",
        returnId = returnId,
        productId = "PROD-VM-01",
        originalChallanItemId = "CI-VM-01",
        requestedQuantity = 50,
        acceptedQuantity = 50,
        rejectedQuantity = 0,
        unit = "PCS"
    )

    @Before
    fun setUp() = runBlocking {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
        dataSource.insertReturn(testReturn, listOf(testItem))
        viewModel = ReturnDetailsViewModel(repository, coroutineScope = testScope)
    }

    @Test
    fun `loadDetails populates returnRequest and null settlement initially`() = runBlocking {
        viewModel.loadDetails(returnId, UserRole.ACCOUNTS, projectId)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.returnRequest)
        assertEquals(ReturnStatus.PROCESSED, state.returnRequest?.status)
        assertNull(state.settlement)
    }

    @Test
    fun `setSettleDialogVisible toggles showSettleDialog state`() {
        assertFalse(viewModel.uiState.value.showSettleDialog)
        viewModel.setSettleDialogVisible(true)
        assertTrue(viewModel.uiState.value.showSettleDialog)
        viewModel.setSettleDialogVisible(false)
        assertFalse(viewModel.uiState.value.showSettleDialog)
    }

    @Test
    fun `settleReturn executes successfully and updates uiState with settlement and audit trail`() = runBlocking {
        viewModel.loadDetails(returnId, UserRole.ACCOUNTS, projectId)

        viewModel.setSettleDialogVisible(true)
        assertTrue(viewModel.uiState.value.showSettleDialog)

        viewModel.settleReturn(
            resolutionType = ReturnResolutionType.CREDIT_NOTE,
            amount = Money(2500.0),
            creditNoteId = "CN-VM-8801",
            notes = "Credit note approved",
            actorId = actorId,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )

        val state = viewModel.uiState.value
        assertFalse("Dialog should close after settlement", state.showSettleDialog)
        assertFalse("Loading spinner should reset", state.isSubmittingAction)
        assertNull("Error message should be null", state.errorMessage)
        assertNotNull("Success message should be set", state.actionSuccessMessage)

        assertNotNull("Settlement should be populated in uiState", state.settlement)
        assertEquals(ReturnResolutionType.CREDIT_NOTE, state.settlement?.resolutionType)
        assertEquals(Money(2500.0), state.settlement?.amount)
        assertEquals("CN-VM-8801", state.settlement?.creditNoteId)

        assertEquals(2L, state.returnRequest?.version)
        assertTrue("Audit trail must contain settlement event", state.auditEvents.isNotEmpty())
    }

    @Test
    fun `settleReturn failure surfaces error message to uiState`() = runBlocking {
        viewModel.loadDetails(returnId, UserRole.ACCOUNTS, projectId)

        // Unauthorized role STAFF attempting settlement
        viewModel.settleReturn(
            resolutionType = ReturnResolutionType.REFUND,
            amount = Money(1000.0),
            actorId = "ACTOR-STAFF",
            callerRole = UserRole.STAFF,
            callerProjectId = projectId
        )

        val state = viewModel.uiState.value
        assertFalse(state.isSubmittingAction)
        assertNotNull("Error message must be set for unauthorized role", state.errorMessage)
        assertTrue(state.errorMessage!!.contains("unauthorized", ignoreCase = true) || state.errorMessage!!.contains("Requires ADMIN", ignoreCase = true))
        assertNull("Settlement should remain null on failure", state.settlement)
    }
}
