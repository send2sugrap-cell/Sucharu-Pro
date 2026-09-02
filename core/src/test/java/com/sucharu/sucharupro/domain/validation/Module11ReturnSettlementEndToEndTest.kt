package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.returns.InspectionChecklistItem
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityType
import com.sucharu.sucharupro.domain.model.returns.ReturnDecision
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnInspectionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * End-to-End lifecycle test verifying the complete Customer Return workflow:
 * REQUESTED
 * -> UNDER_INSPECTION
 * -> APPROVED
 * -> RETURN_RECEIVED
 * -> PROCESSED
 * -> SETTLED
 *
 * (Module 11 Step 01 -> Step 02 -> Step 03 -> Step 04 -> Step 05).
 */
class Module11ReturnSettlementEndToEndTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var inventoryReceivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectId = "PRJ-E2E-SETTLE"
    private val customerId = "CUST-E2E-SETTLE"
    private val returnId = "RET-E2E-SETTLE-001"
    private val returnNo = "RN-E2E-001"
    private val challanId = "CHAL-E2E-001"
    private val productId = "PROD-BOOK-001"
    private val actorStaff = "ACTOR-STAFF"
    private val actorAdmin = "ACTOR-ADMIN"
    private val actorInspector = "ACTOR-QC"
    private val actorWarehouse = "ACTOR-WH"
    private val actorAccounts = "ACTOR-ACCOUNTS"

    @Before
    fun setUp() {
        returnDataSource = FakeReturnDataSource()
        inventoryReceivingDataSource = FakeInventoryReceivingDataSource()
        inventoryLedgerDataSource = FakeInventoryMovementLedgerDataSource()

        repository = ReturnRepositoryImpl(
            dataSource = returnDataSource,
            inventoryReceivingDataSource = inventoryReceivingDataSource,
            inventoryLedgerDataSource = inventoryLedgerDataSource
        )
    }

    @Test
    fun `complete return lifecycle from creation to financial settlement`() = runBlocking {
        // -------------------------------------------------------------
        // STEP 01: Create Return Request (REQUESTED)
        // -------------------------------------------------------------
        val initialRequest = ReturnRequest(
            returnId = returnId,
            projectId = projectId,
            returnNo = returnNo,
            customerId = customerId,
            originalChallanId = challanId,
            status = ReturnStatus.REQUESTED,
            reason = ReturnReason.PRINTING_DEFECT,
            description = "Color smudge on first 50 pages of catalog",
            requestedBy = "Customer Rep",
            version = 1L
        )
        val initialItem = ReturnItem(
            returnItemId = "RI-E2E-001",
            returnId = returnId,
            productId = productId,
            originalChallanItemId = "CI-E2E-001",
            requestedQuantity = 50,
            acceptedQuantity = 0,
            rejectedQuantity = 0,
            unit = "PCS"
        )
        val createRes = repository.createReturn(
            request = initialRequest,
            items = listOf(initialItem),
            actorId = actorStaff,
            callerRole = UserRole.STAFF,
            callerProjectId = projectId
        )
        assertTrue("Step 01: Create Return must succeed", createRes is DomainResult.Success)
        var currentReturn = (createRes as DomainResult.Success).data
        assertEquals(ReturnStatus.REQUESTED, currentReturn.status)
        assertEquals(1L, currentReturn.version)

        // -------------------------------------------------------------
        // STEP 02: Submit for Inspection (UNDER_INSPECTION)
        // -------------------------------------------------------------
        val submitRes = repository.submitForInspection(
            returnId = returnId,
            actorId = actorAdmin,
            expectedVersion = currentReturn.version,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue("Step 02: Submit for Inspection must succeed", submitRes is DomainResult.Success)
        currentReturn = (submitRes as DomainResult.Success).data
        assertEquals(ReturnStatus.UNDER_INSPECTION, currentReturn.status)
        assertEquals(2L, currentReturn.version)

        // -------------------------------------------------------------
        // STEP 03: Record Inspection & Approve (APPROVED)
        // -------------------------------------------------------------
        val inspection = ReturnInspection(
            inspectionId = "INSP-E2E-001",
            returnId = returnId,
            projectId = projectId,
            inspectorId = actorInspector,
            status = ReturnInspectionStatus.COMPLETED,
            decision = ReturnDecision.APPROVE,
            decisionReason = "Full return approved for replacement/credit.",
            checklist = listOf(
                InspectionChecklistItem("c1", "Defect Type", true),
                InspectionChecklistItem("c2", "Quantity Match", true)
            ),
            findings = "Smudge verified across all 50 items. Production fault."
        )
        val inspectRes = repository.recordInspection(
            inspection = inspection,
            actorId = actorInspector,
            callerRole = UserRole.QC_INSPECTOR,
            callerProjectId = projectId
        )
        assertTrue("Step 03: Record Inspection must succeed", inspectRes is DomainResult.Success)

        val approveRes = repository.approveReturn(
            returnId = returnId,
            actorId = actorAdmin,
            expectedVersion = currentReturn.version,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectId
        )
        assertTrue("Step 03: Approve Return must succeed", approveRes is DomainResult.Success)
        currentReturn = (approveRes as DomainResult.Success).data
        assertEquals(ReturnStatus.APPROVED, currentReturn.status)
        assertEquals(3L, currentReturn.version)

        // -------------------------------------------------------------
        // STEP 04: Physical Receiving (RETURN_RECEIVED)
        // -------------------------------------------------------------
        val receiving = ReturnReceivingInfo(
            receivingEventId = "EVT-REC-${UUID.randomUUID()}",
            returnId = returnId,
            projectId = projectId,
            receiverId = actorWarehouse,
            receivedAt = System.currentTimeMillis(),
            approvedQty = 50,
            actualQty = 50,
            acceptedQty = 50,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            condition = "Good overall",
            packaging = "Original box",
            damageNotes = null,
            version = currentReturn.version,
            idempotencyKey = "IDEMP-REC-E2E-01"
        )
        val receiveRes = repository.receiveReturn(
            receivingInfo = receiving,
            actorId = actorWarehouse,
            expectedVersion = currentReturn.version,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectId
        )
        assertTrue("Step 04: Receive Return must succeed", receiveRes is DomainResult.Success)
        currentReturn = (receiveRes as DomainResult.Success).data
        assertEquals(ReturnStatus.RETURN_RECEIVED, currentReturn.status)
        assertEquals(4L, currentReturn.version)

        // -------------------------------------------------------------
        // STEP 04b: Inventory Reconciliation & Processing (PROCESSED)
        // -------------------------------------------------------------
        val reconcileRes = repository.reconcileInventoryAndProcess(
            returnId = returnId,
            warehouseId = "WH-E2E-01",
            locationId = "LOC-RETURNS",
            actorId = actorWarehouse,
            expectedVersion = currentReturn.version,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectId
        )
        assertTrue("Step 04b: Reconcile & Process must succeed", reconcileRes is DomainResult.Success)
        val recData = (reconcileRes as DomainResult.Success).data
        assertEquals(50, recData.acceptedQty)

        val updatedReturnRes = repository.getReturn(returnId, UserRole.WAREHOUSE, projectId)
        assertTrue(updatedReturnRes is DomainResult.Success)
        currentReturn = (updatedReturnRes as DomainResult.Success).data!!
        assertEquals(ReturnStatus.PROCESSED, currentReturn.status)
        assertEquals(5L, currentReturn.version)

        // -------------------------------------------------------------
        // STEP 05: Financial Settlement (SETTLED)
        // -------------------------------------------------------------
        val settlement = ReturnSettlement(
            settlementId = "SETTLE-E2E-001",
            returnId = returnId,
            projectId = projectId,
            customerId = customerId,
            resolutionType = ReturnResolutionType.CREDIT_NOTE,
            amount = Money(3750.0),
            status = ReturnSettlementStatus.COMPLETED,
            creditNoteId = "CN-E2E-5001",
            notes = "Issued credit note for 50 defect copies",
            settledBy = actorAccounts,
            version = 1L,
            idempotencyKey = "IDEMP-E2E-SETTLE-001"
        )
        val settleRes = repository.settleReturn(
            settlement = settlement,
            actorId = actorAccounts,
            expectedVersion = currentReturn.version,
            callerRole = UserRole.ACCOUNTS,
            callerProjectId = projectId
        )
        assertTrue("Step 05: Settle Return must succeed", settleRes is DomainResult.Success)
        val settledData = (settleRes as DomainResult.Success).data
        assertEquals("CN-E2E-5001", settledData.creditNoteId)
        assertEquals(Money(3750.0), settledData.amount)
        assertEquals(ReturnResolutionType.CREDIT_NOTE, settledData.resolutionType)

        // -------------------------------------------------------------
        // Verify Complete Audit Trail
        // -------------------------------------------------------------
        val auditHistory = repository.observeAuditHistory(returnId).first()
        val eventTypes = auditHistory.map { it.activityType }

        assertTrue(eventTypes.contains(ReturnActivityType.RETURN_REQUEST_CREATED))
        assertTrue(eventTypes.contains(ReturnActivityType.RETURN_REQUEST_SUBMITTED_FOR_INSPECTION))
        assertTrue(eventTypes.contains(ReturnActivityType.RETURN_INSPECTION_COMPLETED))
        assertTrue(eventTypes.contains(ReturnActivityType.RETURN_REQUEST_APPROVED))
        assertTrue(eventTypes.contains(ReturnActivityType.RETURN_RECEIVED))
        assertTrue(eventTypes.contains(ReturnActivityType.RETURN_PROCESSED))
        assertTrue(eventTypes.contains(ReturnActivityType.RETURN_SETTLED))

        // Verify Settlement Query and Live Observation
        val fetchedSettlement = repository.getSettlement(returnId, UserRole.ACCOUNTS, projectId)
        assertTrue(fetchedSettlement is DomainResult.Success)
        val fetchedData = (fetchedSettlement as DomainResult.Success).data
        assertNotNull(fetchedData)
        assertEquals("SETTLE-E2E-001", fetchedData?.settlementId)

        val observedSettlement = repository.observeSettlement(returnId).first()
        assertNotNull(observedSettlement)
        assertEquals("SETTLE-E2E-001", observedSettlement?.settlementId)
    }
}
