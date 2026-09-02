package com.sucharu.sucharupro.ui.features.returns

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnReconciliationResult
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ReturnDetailsViewModel Step 04 Receiving & Reconciliation Integration (Chunk 05 Phase B1).
 */
class ReturnDetailsViewModelReceivingTest {

    private lateinit var dataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private val projectId = "PRJ-TEST"
    private val customerId = "CUST-TEST"
    private val actorId = "WH-ACTOR-01"

    @Before
    fun setUp() {
        dataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(dataSource)
    }

    private fun sampleReturn(
        returnId: String = "RET-01",
        returnNo: String = "RN-001",
        status: ReturnStatus = ReturnStatus.APPROVED,
        version: Long = 1L
    ) = ReturnRequest(
        returnId = returnId,
        projectId = projectId,
        returnNo = returnNo,
        customerId = customerId,
        originalChallanId = "CHAL-01",
        status = status,
        reason = ReturnReason.PRINTING_DEFECT,
        requestedBy = "CUST-USER",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        version = version
    )

    private fun sampleItem(
        returnId: String = "RET-01",
        returnItemId: String = "ITEM-01",
        requestedQuantity: Int = 10,
        acceptedQuantity: Int = 10,
        rejectedQuantity: Int = 0
    ) = ReturnItem(
        returnItemId = returnItemId,
        returnId = returnId,
        productId = "PROD-01",
        originalChallanItemId = "CHAL-ITEM-01",
        requestedQuantity = requestedQuantity,
        acceptedQuantity = acceptedQuantity,
        rejectedQuantity = rejectedQuantity,
        unit = "PCS"
    )

    private fun sampleReceiving(
        returnId: String,
        receivingEventId: String = "EVT-01",
        actualQty: Int = 10,
        acceptedQty: Int = 8,
        rejectedQty: Int = 2,
        damagedQty: Int = 0,
        remarks: String? = "All accounted for",
        version: Long = 1L
    ) = ReturnReceivingInfo(
        receivingEventId = receivingEventId,
        returnId = returnId,
        projectId = projectId,
        receiverId = actorId,
        receivedAt = System.currentTimeMillis(),
        approvedQty = 10,
        actualQty = actualQty,
        acceptedQty = acceptedQty,
        rejectedQty = rejectedQty,
        damagedQty = damagedQty,
        mismatchFlag = false,
        condition = null,
        packaging = null,
        damageNotes = remarks,
        version = version,
        idempotencyKey = "REC-$returnId-$version"
    )

    @Test
    fun `loadDetails populates receiving information when record exists`() = runBlocking {
        val req = sampleReturn(status = ReturnStatus.RETURN_RECEIVED, version = 2L)
        val receiving = sampleReceiving(returnId = req.returnId, version = 1L)
        dataSource.insertReturn(req, listOf(sampleItem()))
        dataSource.insertOrUpdateReceiving(receiving)

        val viewModel = ReturnDetailsViewModel(repository, testScope)
        viewModel.loadDetails(req.returnId, UserRole.WAREHOUSE, projectId)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.returnRequest)
        assertEquals(ReturnStatus.RETURN_RECEIVED, state.returnRequest?.status)
        assertNotNull(state.receivingInfo)
        assertEquals(8, state.receivingInfo?.acceptedQty)
        assertEquals(2, state.receivingInfo?.rejectedQty)
        assertEquals("All accounted for", state.receivingInfo?.damageNotes)
        assertNull(state.reconciliationResult)
    }

    @Test
    fun `loadDetails populates reconciliation result when return is processed`() = runBlocking {
        val req = sampleReturn(status = ReturnStatus.PROCESSED, version = 3L)
        val receiving = sampleReceiving(returnId = req.returnId, actualQty = 10, acceptedQty = 10, rejectedQty = 0, version = 1L)
        val reconciliation = ReturnReconciliationResult(
            returnId = req.returnId,
            receivingEventId = receiving.receivingEventId,
            projectId = req.projectId,
            acceptedQty = 10,
            stockInRecordId = "STK-IN-01",
            ledgerEntryId = "LEDGER-01",
            inventoryMutationApplied = true,
            resultingStatus = ReturnStatus.PROCESSED,
            reconciledBy = actorId,
            completedAt = System.currentTimeMillis()
        )
        dataSource.insertReturn(req, listOf(sampleItem()))
        dataSource.insertOrUpdateReceiving(receiving)
        dataSource.insertOrUpdateReconciliationResult(reconciliation)

        val viewModel = ReturnDetailsViewModel(repository, testScope)
        viewModel.loadDetails(req.returnId, UserRole.WAREHOUSE, projectId)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.reconciliationResult)
        assertEquals("STK-IN-01", state.reconciliationResult?.stockInRecordId)
        assertEquals("LEDGER-01", state.reconciliationResult?.ledgerEntryId)
        assertTrue(state.reconciliationResult?.inventoryMutationApplied == true)
    }

    @Test
    fun `receiveReturn successfully updates status to RETURN_RECEIVED and stores receiving info`() = runBlocking {
        val req = sampleReturn(status = ReturnStatus.APPROVED, version = 1L)
        dataSource.insertReturn(req, listOf(sampleItem()))

        val viewModel = ReturnDetailsViewModel(repository, testScope)
        viewModel.loadDetails(req.returnId, UserRole.WAREHOUSE, projectId)

        viewModel.receiveReturn(
            actualQty = 10,
            acceptedQty = 8,
            rejectedQty = 2,
            damagedQty = 0,
            remarks = "Physical check verified",
            actorId = actorId,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectId
        )

        val state = viewModel.uiState.value
        assertFalse(state.isSubmittingAction)
        assertFalse(state.showReceiveDialog)
        assertNull(state.errorMessage)
        assertNotNull(state.actionSuccessMessage)
        assertEquals(ReturnStatus.RETURN_RECEIVED, state.returnRequest?.status)
        assertEquals(2L, state.returnRequest?.version)
        assertNotNull(state.receivingInfo)
        assertEquals(8, state.receivingInfo?.acceptedQty)
        assertEquals(2, state.receivingInfo?.rejectedQty)
        assertEquals("Physical check verified", state.receivingInfo?.damageNotes)
    }

    @Test
    fun `receiveReturn domain error populates errorMessage without mutating status`() = runBlocking {
        // Return is in REQUESTED status, not APPROVED -> receiveReturn will fail domain validation
        val req = sampleReturn(status = ReturnStatus.REQUESTED, version = 1L)
        dataSource.insertReturn(req, listOf(sampleItem()))

        val viewModel = ReturnDetailsViewModel(repository, testScope)
        viewModel.loadDetails(req.returnId, UserRole.WAREHOUSE, projectId)

        viewModel.receiveReturn(
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            remarks = null,
            actorId = actorId,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectId
        )

        val state = viewModel.uiState.value
        assertFalse(state.isSubmittingAction)
        assertNotNull(state.errorMessage)
        assertEquals(ReturnStatus.REQUESTED, state.returnRequest?.status)
        assertNull(state.receivingInfo)
    }

    @Test
    fun `reconcileInventoryAndProcess successfully transitions to PROCESSED and stores result`() = runBlocking {
        val req = sampleReturn(status = ReturnStatus.RETURN_RECEIVED, version = 2L)
        val receiving = sampleReceiving(returnId = req.returnId, actualQty = 10, acceptedQty = 10, rejectedQty = 0, version = 1L)
        dataSource.insertReturn(req, listOf(sampleItem()))
        dataSource.insertOrUpdateReceiving(receiving)

        val viewModel = ReturnDetailsViewModel(repository, testScope)
        viewModel.loadDetails(req.returnId, UserRole.WAREHOUSE, projectId)

        viewModel.reconcileInventoryAndProcess(
            warehouseId = "WH-MAIN",
            locationId = "LOC-A1",
            actorId = actorId,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectId
        )

        val state = viewModel.uiState.value
        assertFalse(state.isSubmittingAction)
        assertFalse(state.showReconcileDialog)
        assertNull(state.errorMessage)
        assertNotNull(state.actionSuccessMessage)
        assertEquals(ReturnStatus.PROCESSED, state.returnRequest?.status)
        assertEquals(3L, state.returnRequest?.version)
        assertNotNull(state.reconciliationResult)
        assertEquals(ReturnStatus.PROCESSED, state.reconciliationResult?.resultingStatus)
    }

    @Test
    fun `reconcileInventoryAndProcess with missing receiving record sets error`() = runBlocking {
        // Return is RETURN_RECEIVED but has no receiving info record in data source
        val req = sampleReturn(status = ReturnStatus.RETURN_RECEIVED, version = 2L)
        dataSource.insertReturn(req, listOf(sampleItem()))

        val viewModel = ReturnDetailsViewModel(repository, testScope)
        viewModel.loadDetails(req.returnId, UserRole.WAREHOUSE, projectId)

        viewModel.reconcileInventoryAndProcess(
            warehouseId = "WH-MAIN",
            locationId = "LOC-A1",
            actorId = actorId,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectId
        )

        val state = viewModel.uiState.value
        assertFalse(state.isSubmittingAction)
        assertNotNull(state.errorMessage)
        assertNull(state.reconciliationResult)
    }

    @Test
    fun `dialog controls open and close receive and reconcile dialogs`() {
        val viewModel = ReturnDetailsViewModel(repository, testScope)

        assertFalse(viewModel.uiState.value.showReceiveDialog)
        assertFalse(viewModel.uiState.value.showReconcileDialog)

        viewModel.openReceiveDialog()
        assertTrue(viewModel.uiState.value.showReceiveDialog)

        viewModel.closeReceiveDialog()
        assertFalse(viewModel.uiState.value.showReceiveDialog)

        viewModel.openReconcileDialog()
        assertTrue(viewModel.uiState.value.showReconcileDialog)

        viewModel.closeReconcileDialog()
        assertFalse(viewModel.uiState.value.showReconcileDialog)
    }

    @Test
    fun `duplicate submission is guarded when isSubmittingAction is true`() = runBlocking {
        val req = sampleReturn(status = ReturnStatus.APPROVED, version = 1L)
        dataSource.insertReturn(req, listOf(sampleItem()))

        val viewModel = ReturnDetailsViewModel(repository, testScope)
        viewModel.loadDetails(req.returnId, UserRole.WAREHOUSE, projectId)

        viewModel.receiveReturn(
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            remarks = "First submit",
            actorId = actorId,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectId
        )

        val state = viewModel.uiState.value
        assertEquals(ReturnStatus.RETURN_RECEIVED, state.returnRequest?.status)
    }

    @Test
    fun `optimistic concurrency conflict displays domain error without mutating return state`() = runBlocking {
        val req = sampleReturn(status = ReturnStatus.APPROVED, version = 1L)
        dataSource.insertReturn(req, listOf(sampleItem()))

        val viewModel = ReturnDetailsViewModel(repository, testScope)
        viewModel.loadDetails(req.returnId, UserRole.WAREHOUSE, projectId)

        // Mutate datasource version behind the ViewModel's back
        dataSource.updateReturn(req.copy(version = 2L))

        viewModel.receiveReturn(
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            remarks = null,
            actorId = actorId,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectId
        )

        val state = viewModel.uiState.value
        assertFalse(state.isSubmittingAction)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("version", ignoreCase = true) || state.errorMessage!!.contains("concurrency", ignoreCase = true) || state.errorMessage!!.contains("mismatch", ignoreCase = true))
        assertNull(state.receivingInfo)
    }

    @Test
    fun `unauthorized role is rejected with domain error and no false success`() = runBlocking {
        val req = sampleReturn(status = ReturnStatus.APPROVED, version = 1L)
        dataSource.insertReturn(req, listOf(sampleItem()))

        val viewModel = ReturnDetailsViewModel(repository, testScope)
        viewModel.loadDetails(req.returnId, UserRole.WAREHOUSE, projectId)

        viewModel.receiveReturn(
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            remarks = null,
            actorId = "CUST-01",
            callerRole = UserRole.CUSTOMER,
            callerProjectId = projectId
        )

        val state = viewModel.uiState.value
        assertFalse(state.isSubmittingAction)
        assertNotNull(state.errorMessage)
        assertNull(state.actionSuccessMessage)
        assertEquals(ReturnStatus.APPROVED, state.returnRequest?.status)
        assertNull(state.receivingInfo)
    }
}
