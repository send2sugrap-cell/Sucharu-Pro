package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import com.sucharu.sucharupro.domain.model.returns.InspectionChecklistItem
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityType
import com.sucharu.sucharupro.domain.model.returns.ReturnDecision
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnInspectionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * End-to-End Domain Lifecycle & Integration Test Suite for Module 11 (Return Management).
 *
 * Validates the complete canonical lifecycle:
 * REQUESTED -> UNDER_INSPECTION -> APPROVED -> RETURN_RECEIVED -> PROCESSED
 *
 * Invariants verified:
 * - Receiving info persistence
 * - Only accepted quantity stocked into canonical finished-product inventory
 * - Canonical Stock-In record creation with sourceReference 'RETURN:<returnNo>'
 * - Canonical Movement Ledger entry creation (STOCK_IN, IN)
 * - Reconciliation result persistence
 * - Zero accepted quantity scenario (record-only reconciliation)
 * - Idempotency
 * - Project isolation
 * - RBAC boundaries
 * - Optimistic concurrency control
 * - Append-only audit trail
 */
class Module11ReturnEndToEndLifecycleTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var inventoryReceivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var repository: ReturnRepositoryImpl

    private val projectIdA = "PRJ-ALPHA"
    private val projectIdB = "PRJ-BETA"
    private val customerIdA = "CUST-A"
    private val customerIdB = "CUST-B"

    private val actorAdmin = "ADMIN-01"
    private val actorInspector = "QC-INSP-01"
    private val actorWarehouse = "WH-RECEIVER-01"

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
    fun `complete canonical lifecycle from REQUESTED to PROCESSED`() = runBlocking {
        // STEP A: Creation -> REQUESTED
        val returnId = "RET-E2E-001"
        val returnNo = "RN-E2E-001"
        val initialRequest = ReturnRequest(
            returnId = returnId,
            projectId = projectIdA,
            returnNo = returnNo,
            customerId = customerIdA,
            originalChallanId = "CHAL-101",
            status = ReturnStatus.REQUESTED,
            reason = ReturnReason.PRINTING_DEFECT,
            description = "Color mismatch on batch",
            requestedBy = "CUST-USER",
            requestedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            version = 1L
        )
        val returnItem = ReturnItem(
            returnItemId = "RITEM-01",
            returnId = returnId,
            productId = "PROD-BOOK-01",
            originalChallanItemId = "CHAL-ITEM-01",
            requestedQuantity = 50,
            acceptedQuantity = 0,
            rejectedQuantity = 0,
            unit = "PCS"
        )
        val createResult = repository.createReturn(
            request = initialRequest,
            items = listOf(returnItem),
            actorId = "STAFF-01",
            callerRole = UserRole.STAFF,
            callerProjectId = projectIdA
        )
        assertTrue(createResult is DomainResult.Success)
        val created = (createResult as DomainResult.Success<ReturnRequest>).data
        assertEquals(ReturnStatus.REQUESTED, created.status)
        assertEquals(1L, created.version)

        // STEP B: Submit for Inspection -> UNDER_INSPECTION
        val submitResult = repository.submitForInspection(
            returnId = returnId,
            actorId = actorAdmin,
            expectedVersion = 1L,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectIdA
        )
        assertTrue(submitResult is DomainResult.Success)
        val underInspection = (submitResult as DomainResult.Success<ReturnRequest>).data
        assertEquals(ReturnStatus.UNDER_INSPECTION, underInspection.status)
        assertEquals(2L, underInspection.version)

        // STEP C: Record Inspection & Approval -> APPROVED
        val inspection = ReturnInspection(
            inspectionId = "INSP-01",
            returnId = returnId,
            projectId = projectIdA,
            inspectorId = actorInspector,
            status = ReturnInspectionStatus.COMPLETED,
            decision = ReturnDecision.APPROVE,
            decisionReason = "Physical defect verified, return authorized",
            checklist = listOf(
                InspectionChecklistItem("CHK-01", "Visual damage check", true),
                InspectionChecklistItem("CHK-02", "Quantity matches challan", true)
            ),
            findings = "5 units water damaged, 45 acceptable"
        )
        val recordInspRes = repository.recordInspection(
            inspection = inspection,
            actorId = actorInspector,
            callerRole = UserRole.QC_INSPECTOR,
            callerProjectId = projectIdA
        )
        assertTrue(recordInspRes is DomainResult.Success)

        val approveResult = repository.approveReturn(
            returnId = returnId,
            actorId = actorAdmin,
            expectedVersion = 2L,
            callerRole = UserRole.ADMIN,
            callerProjectId = projectIdA
        )
        assertTrue(approveResult is DomainResult.Success)
        val approved = (approveResult as DomainResult.Success<ReturnRequest>).data
        assertEquals(ReturnStatus.APPROVED, approved.status)
        assertEquals(3L, approved.version)

        // STEP D: Physical Receiving -> RETURN_RECEIVED
        val receivingEventId = "EVT-REC-${UUID.randomUUID()}"
        val receivingInfo = ReturnReceivingInfo(
            receivingEventId = receivingEventId,
            returnId = returnId,
            projectId = projectIdA,
            receiverId = actorWarehouse,
            receivedAt = System.currentTimeMillis(),
            approvedQty = 50,
            actualQty = 50,
            acceptedQty = 45,
            rejectedQty = 5,
            damagedQty = 0,
            mismatchFlag = false,
            condition = "Good overall",
            packaging = "Original box",
            damageNotes = "5 units rejected due to water stain",
            version = 3L,
            idempotencyKey = "IDEMP-REC-E2E-01"
        )
        val receiveResult = repository.receiveReturn(
            receivingInfo = receivingInfo,
            actorId = actorWarehouse,
            expectedVersion = 3L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue(receiveResult is DomainResult.Success)
        val received = (receiveResult as DomainResult.Success<ReturnRequest>).data
        assertEquals(ReturnStatus.RETURN_RECEIVED, received.status)
        assertEquals(4L, received.version)

        // Verify receiving record persisted
        val persistedReceivingRes = repository.getReceiving(returnId, UserRole.WAREHOUSE, projectIdA)
        assertTrue(persistedReceivingRes is DomainResult.Success)
        val persistedReceiving = (persistedReceivingRes as DomainResult.Success<ReturnReceivingInfo?>).data
        assertNotNull(persistedReceiving)
        assertEquals(45, persistedReceiving?.acceptedQty)
        assertEquals(5, persistedReceiving?.rejectedQty)
        assertEquals(actorWarehouse, persistedReceiving?.receiverId)

        // STEP E: Inventory Reconciliation -> PROCESSED
        val reconcileResult = repository.reconcileInventoryAndProcess(
            returnId = returnId,
            warehouseId = "WH-CENTRAL",
            locationId = "LOC-FIN-A1",
            actorId = actorWarehouse,
            expectedVersion = 4L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue(reconcileResult is DomainResult.Success)
        val reconciliation = (reconcileResult as DomainResult.Success).data
        assertEquals(ReturnStatus.PROCESSED, reconciliation.resultingStatus)
        assertEquals(45, reconciliation.acceptedQty)
        assertTrue(reconciliation.inventoryMutationApplied)
        assertNotNull(reconciliation.stockInRecordId)
        assertNotNull(reconciliation.ledgerEntryId)

        // Verify final ReturnRequest status
        val finalReturnRes = repository.getReturn(returnId, UserRole.WAREHOUSE, projectIdA)
        assertTrue(finalReturnRes is DomainResult.Success)
        val finalReturn = (finalReturnRes as DomainResult.Success<ReturnRequest>).data
        assertEquals(ReturnStatus.PROCESSED, finalReturn.status)
        assertEquals(5L, finalReturn.version)

        // VERIFY STOCK-IN: Canonical Inventory Receiving Data Source
        val stockInRecords = inventoryReceivingDataSource.observeStockInRecords().first()
        val stockIn = stockInRecords.find { it.stockInId == reconciliation.stockInRecordId }
        assertNotNull("Stock-In record must exist in canonical inventory", stockIn)
        assertEquals("RETURN:$returnNo", stockIn?.sourceReference)
        assertEquals("WH-CENTRAL", stockIn?.warehouseId)
        assertEquals("LOC-FIN-A1", stockIn?.locationId)
        assertEquals(45, stockIn?.quantity)
        assertEquals(InventoryUnit.PCS, stockIn?.unit)

        // VERIFY MOVEMENT LEDGER: Canonical Inventory Movement Ledger Data Source
        val ledgerEntries = inventoryLedgerDataSource.getEntries(projectIdA)
        val ledgerEntry = ledgerEntries.find { it.ledgerEntryId == reconciliation.ledgerEntryId }
        assertNotNull("Ledger entry must exist in canonical inventory ledger", ledgerEntry)
        assertEquals(InventoryMovementLedgerType.STOCK_IN, ledgerEntry?.movementType)
        assertEquals(45.0, ledgerEntry?.quantity ?: 0.0, 0.001)
        assertEquals("LOC-FIN-A1", ledgerEntry?.locationId)
        assertEquals(returnId, ledgerEntry?.referenceId)
        assertEquals("RECEIVING", ledgerEntry?.referenceType)

        // VERIFY AUDIT TRAIL: Complete chronological sequence
        val auditRes = repository.getAuditHistory(returnId, UserRole.WAREHOUSE, projectIdA)
        assertTrue(auditRes is DomainResult.Success)
        val auditTrail = (auditRes as DomainResult.Success).data
        val activityTypes = auditTrail.map { it.activityType }
        assertTrue(activityTypes.contains(ReturnActivityType.RETURN_REQUEST_CREATED))
        assertTrue(activityTypes.contains(ReturnActivityType.RETURN_REQUEST_SUBMITTED_FOR_INSPECTION))
        assertTrue(activityTypes.contains(ReturnActivityType.RETURN_REQUEST_APPROVED))
        assertTrue(activityTypes.contains(ReturnActivityType.RETURN_RECEIVED))
        assertTrue(activityTypes.contains(ReturnActivityType.RETURN_PROCESSED))
    }

    @Test
    fun `zero accepted quantity scenario results in record-only reconciliation without stock-in`() = runBlocking {
        val returnId = "RET-ZERO-01"
        val returnNo = "RN-ZERO-01"
        val request = ReturnRequest(
            returnId = returnId,
            projectId = projectIdA,
            returnNo = returnNo,
            customerId = customerIdA,
            originalChallanId = "CHAL-01",
            status = ReturnStatus.APPROVED,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "CUST-USER",
            version = 1L
        )
        val item = ReturnItem(
            returnItemId = "ITEM-Z01",
            returnId = returnId,
            productId = "PROD-01",
            originalChallanItemId = "CHAL-01",
            requestedQuantity = 10,
            acceptedQuantity = 10,
            rejectedQuantity = 0,
            unit = "PCS"
        )
        returnDataSource.insertReturn(request, listOf(item))

        // Physical receiving with 0 accepted quantity (all 10 damaged/rejected)
        val receivingInfo = ReturnReceivingInfo(
            receivingEventId = "EVT-REC-Z01",
            returnId = returnId,
            projectId = projectIdA,
            receiverId = actorWarehouse,
            receivedAt = System.currentTimeMillis(),
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 0,
            rejectedQty = 8,
            damagedQty = 2,
            mismatchFlag = false,
            condition = "Total loss",
            packaging = "Crushed",
            damageNotes = "All units destroyed",
            version = 1L,
            idempotencyKey = "IDEMP-REC-ZERO-01"
        )
        val receiveResult = repository.receiveReturn(
            receivingInfo = receivingInfo,
            actorId = actorWarehouse,
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue(receiveResult is DomainResult.Success)

        // Reconcile
        val reconcileResult = repository.reconcileInventoryAndProcess(
            returnId = returnId,
            warehouseId = "WH-CENTRAL",
            locationId = "LOC-SCRAP",
            actorId = actorWarehouse,
            expectedVersion = 2L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue(reconcileResult is DomainResult.Success)
        val reconciliation = (reconcileResult as DomainResult.Success).data
        assertEquals(ReturnStatus.PROCESSED, reconciliation.resultingStatus)
        assertEquals(0, reconciliation.acceptedQty)
        assertFalse(reconciliation.inventoryMutationApplied)
        assertNull(reconciliation.stockInRecordId)
        assertNull(reconciliation.ledgerEntryId)

        // Verify NO stock-in or ledger entries were created
        val stockInRecords = inventoryReceivingDataSource.observeStockInRecords().first()
        assertTrue("No stock-in records should be created for zero accepted quantity", stockInRecords.isEmpty())

        val ledgerEntries = inventoryLedgerDataSource.getEntries(projectIdA)
        assertTrue("No movement ledger entries should be created for zero accepted quantity", ledgerEntries.isEmpty())
    }

    @Test
    fun `idempotency prevents duplicate stock-in and ledger entries on repeated reconciliation`() = runBlocking {
        val returnId = "RET-IDEMP-01"
        val returnNo = "RN-IDEMP-01"
        val request = ReturnRequest(
            returnId = returnId,
            projectId = projectIdA,
            returnNo = returnNo,
            customerId = customerIdA,
            originalChallanId = "CHAL-01",
            status = ReturnStatus.APPROVED,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "CUST-USER",
            version = 1L
        )
        val item = ReturnItem(
            returnItemId = "ITEM-I01",
            returnId = returnId,
            productId = "PROD-01",
            originalChallanItemId = "CHAL-01",
            requestedQuantity = 20,
            acceptedQuantity = 20,
            rejectedQuantity = 0,
            unit = "PCS"
        )
        returnDataSource.insertReturn(request, listOf(item))

        val receivingInfo = ReturnReceivingInfo(
            receivingEventId = "EVT-REC-I01",
            returnId = returnId,
            projectId = projectIdA,
            receiverId = actorWarehouse,
            receivedAt = System.currentTimeMillis(),
            approvedQty = 20,
            actualQty = 20,
            acceptedQty = 20,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            version = 1L,
            idempotencyKey = "IDEMP-REC-I01"
        )
        repository.receiveReturn(receivingInfo, actorWarehouse, 1L, null, UserRole.WAREHOUSE, projectIdA)

        // First reconciliation
        val result1 = repository.reconcileInventoryAndProcess(
            returnId = returnId,
            warehouseId = "WH-CENTRAL",
            locationId = "LOC-01",
            actorId = actorWarehouse,
            expectedVersion = 2L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue(result1 is DomainResult.Success)
        val rec1 = (result1 as DomainResult.Success).data

        // Repeated reconciliation with PROCESSED version 3L
        val result2 = repository.reconcileInventoryAndProcess(
            returnId = returnId,
            warehouseId = "WH-CENTRAL",
            locationId = "LOC-01",
            actorId = actorWarehouse,
            expectedVersion = 3L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue(result2 is DomainResult.Success)
        val rec2 = (result2 as DomainResult.Success).data

        assertEquals(rec1.stockInRecordId, rec2.stockInRecordId)
        assertEquals(rec1.ledgerEntryId, rec2.ledgerEntryId)

        // Total inventory stock-in records count must be exactly 1
        val stockIns = inventoryReceivingDataSource.observeStockInRecords().first()
        assertEquals(1, stockIns.size)

        // Total ledger entries count must be exactly 1
        val ledgerEntries = inventoryLedgerDataSource.getEntries(projectIdA)
        assertEquals(1, ledgerEntries.size)
    }

    @Test
    fun `project isolation blocks cross-project receiving and reconciliation`() = runBlocking {
        val returnId = "RET-ISOLATION-01"
        val requestB = ReturnRequest(
            returnId = returnId,
            projectId = projectIdB,
            returnNo = "RN-ISOL-B",
            customerId = customerIdB,
            originalChallanId = "CHAL-B",
            status = ReturnStatus.APPROVED,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "CUST-B",
            version = 1L
        )
        val itemB = ReturnItem(
            returnItemId = "ITEM-B01",
            returnId = returnId,
            productId = "PROD-B",
            originalChallanItemId = "CHAL-B",
            requestedQuantity = 10,
            acceptedQuantity = 10,
            rejectedQuantity = 0,
            unit = "PCS"
        )
        returnDataSource.insertReturn(requestB, listOf(itemB))

        // Actor from Project A attempts to receive Project B's return
        val receivingInfo = ReturnReceivingInfo(
            receivingEventId = "EVT-REC-CROSS",
            returnId = returnId,
            projectId = projectIdA, // spoofed project
            receiverId = actorWarehouse,
            receivedAt = System.currentTimeMillis(),
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            version = 1L,
            idempotencyKey = "CROSS-KEY"
        )
        val crossReceive = repository.receiveReturn(
            receivingInfo = receivingInfo,
            actorId = actorWarehouse,
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA // caller belongs to Project A
        )
        assertTrue("Cross-project receiving must be rejected", crossReceive is DomainResult.Error)

        // Legitimate Project B receiving
        val validReceiving = receivingInfo.copy(projectId = projectIdB)
        val validReceiveResult = repository.receiveReturn(
            receivingInfo = validReceiving,
            actorId = actorWarehouse,
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdB
        )
        assertTrue(validReceiveResult is DomainResult.Success)

        // Actor from Project A attempts to reconcile Project B's return
        val crossReconcile = repository.reconcileInventoryAndProcess(
            returnId = returnId,
            warehouseId = "WH-CENTRAL",
            locationId = "LOC-01",
            actorId = actorWarehouse,
            expectedVersion = 2L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue("Cross-project reconciliation must be rejected", crossReconcile is DomainResult.Error)

        // Query isolation
        val crossQueryReceiving = repository.getReceiving(returnId, UserRole.WAREHOUSE, projectIdA)
        assertTrue(crossQueryReceiving is DomainResult.Error)
    }

    @Test
    fun `RBAC enforces authorized mutation roles and rejects unauthorized roles`() = runBlocking {
        val returnId = "RET-RBAC-01"
        val request = ReturnRequest(
            returnId = returnId,
            projectId = projectIdA,
            returnNo = "RN-RBAC-01",
            customerId = customerIdA,
            originalChallanId = "CHAL-01",
            status = ReturnStatus.APPROVED,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "CUST-A",
            version = 1L
        )
        val item = ReturnItem(
            returnItemId = "ITEM-RBAC-01",
            returnId = returnId,
            productId = "PROD-01",
            originalChallanItemId = "CHAL-01",
            requestedQuantity = 10,
            acceptedQuantity = 10,
            rejectedQuantity = 0,
            unit = "PCS"
        )
        returnDataSource.insertReturn(request, listOf(item))

        val receivingInfo = ReturnReceivingInfo(
            receivingEventId = "EVT-REC-RBAC",
            returnId = returnId,
            projectId = projectIdA,
            receiverId = "USER-01",
            receivedAt = System.currentTimeMillis(),
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            version = 1L,
            idempotencyKey = "RBAC-KEY-01"
        )

        // Unauthorized roles attempting receiving mutation
        val unauthorizedRoles = listOf(
            UserRole.CUSTOMER,
            UserRole.STAFF,
            UserRole.QC_INSPECTOR,
            UserRole.ACCOUNTS,
            UserRole.DESIGNER,
            UserRole.VENDOR,
            UserRole.AFFILIATE
        )

        for (role in unauthorizedRoles) {
            val result = repository.receiveReturn(
                receivingInfo = receivingInfo,
                actorId = "UNAUTH-USER",
                expectedVersion = 1L,
                callerRole = role,
                callerProjectId = projectIdA
            )
            assertTrue("Role $role must be rejected from physical receiving", result is DomainResult.Error)
        }

        // Authorized role performs receiving
        val authReceive = repository.receiveReturn(
            receivingInfo = receivingInfo,
            actorId = actorWarehouse,
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue(authReceive is DomainResult.Success)

        // Unauthorized roles attempting reconciliation mutation
        for (role in unauthorizedRoles) {
            val result = repository.reconcileInventoryAndProcess(
                returnId = returnId,
                warehouseId = "WH-CENTRAL",
                locationId = "LOC-01",
                actorId = "UNAUTH-USER",
                expectedVersion = 2L,
                callerRole = role,
                callerProjectId = projectIdA
            )
            assertTrue("Role $role must be rejected from inventory reconciliation", result is DomainResult.Error)
        }
    }

    @Test
    fun `optimistic concurrency rejects stale versions during receiving and reconciliation`() = runBlocking {
        val returnId = "RET-CONC-01"
        val request = ReturnRequest(
            returnId = returnId,
            projectId = projectIdA,
            returnNo = "RN-CONC-01",
            customerId = customerIdA,
            originalChallanId = "CHAL-01",
            status = ReturnStatus.APPROVED,
            reason = ReturnReason.PRINTING_DEFECT,
            requestedBy = "CUST-A",
            version = 1L
        )
        val item = ReturnItem(
            returnItemId = "ITEM-C01",
            returnId = returnId,
            productId = "PROD-01",
            originalChallanItemId = "CHAL-01",
            requestedQuantity = 10,
            acceptedQuantity = 10,
            rejectedQuantity = 0,
            unit = "PCS"
        )
        returnDataSource.insertReturn(request, listOf(item))

        val receivingInfo = ReturnReceivingInfo(
            receivingEventId = "EVT-REC-CONC",
            returnId = returnId,
            projectId = projectIdA,
            receiverId = actorWarehouse,
            receivedAt = System.currentTimeMillis(),
            approvedQty = 10,
            actualQty = 10,
            acceptedQty = 10,
            rejectedQty = 0,
            damagedQty = 0,
            mismatchFlag = false,
            version = 1L,
            idempotencyKey = "CONC-KEY-01"
        )

        // Stale expectedVersion on receiveReturn
        val staleReceive = repository.receiveReturn(
            receivingInfo = receivingInfo,
            actorId = actorWarehouse,
            expectedVersion = 99L, // stale
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue("Stale version on receiveReturn must fail", staleReceive is DomainResult.Error)

        // Successful receiving
        val validReceive = repository.receiveReturn(
            receivingInfo = receivingInfo,
            actorId = actorWarehouse,
            expectedVersion = 1L,
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue(validReceive is DomainResult.Success)

        // Stale expectedVersion on reconcileInventoryAndProcess
        val staleReconcile = repository.reconcileInventoryAndProcess(
            returnId = returnId,
            warehouseId = "WH-CENTRAL",
            locationId = "LOC-01",
            actorId = actorWarehouse,
            expectedVersion = 1L, // stale, version is now 2L
            callerRole = UserRole.WAREHOUSE,
            callerProjectId = projectIdA
        )
        assertTrue("Stale version on reconciliation must fail", staleReconcile is DomainResult.Error)
    }
}
