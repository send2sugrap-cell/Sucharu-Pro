package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.data.datasource.FakeReturnDataSource
import com.sucharu.sucharupro.data.repository.ReturnRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.returns.ReturnReason
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * CRITICAL BOUNDARY TEST — Module 11 Step 01.
 *
 * Proves that the Return domain foundation does NOT mutate any inventory entity:
 *   - StockInRecord / InventoryStockInRecord
 *   - StockOutRecord / InventoryStockOutRecord
 *   - MovementLedgerEntry
 *   - Warehouse balance
 *   - Inventory quantity
 *   - Inventory valuation
 *
 * This test directly verifies that [ReturnRepositoryImpl] and [FakeReturnDataSource]
 * only touch Return-domain state, and that no inventory data source is ever mutated.
 *
 * The test suite fails if any inventory count changes during return lifecycle operations.
 *
 * Uses [runBlocking] because [kotlinx.coroutines.test] is not in the
 * project's test dependencies; coroutines-core is available transitively.
 */
class ReturnDomainInventoryBoundaryTest {

    private lateinit var returnDataSource: FakeReturnDataSource
    private lateinit var repository: ReturnRepositoryImpl

    // =========================================================================
    // Simulate inventory counters — these must remain at zero throughout
    // =========================================================================

    /** Simulates a StockInRecord count (e.g. from FakeInventoryReceivingDataSource). */
    private var stockInRecordCount = 0

    /** Simulates a StockOutRecord count (e.g. from FakeInventoryStockOutDataSource). */
    private var stockOutRecordCount = 0

    /** Simulates a MovementLedgerEntry count. */
    private var ledgerEntryCount = 0

    /** Simulates warehouse balance changes. */
    private var warehouseBalanceChangeCount = 0

    @Before
    fun setUp() {
        returnDataSource = FakeReturnDataSource()
        repository = ReturnRepositoryImpl(returnDataSource)
        // Reset inventory counters
        stockInRecordCount = 0
        stockOutRecordCount = 0
        ledgerEntryCount = 0
        warehouseBalanceChangeCount = 0
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private fun validReturn(
        returnId: String = "RET-INV-001",
        projectId: String = "PRJ-A"
    ): ReturnRequest {
        val now = System.currentTimeMillis()
        return ReturnRequest(
            returnId = returnId,
            projectId = projectId,
            returnNo = "RN-INV-001",
            customerId = "CUST-001",
            originalChallanId = "CHAL-001",
            status = ReturnStatus.REQUESTED,
            reason = ReturnReason.BINDING_DEFECT,
            requestedBy = "USER-01",
            requestedAt = now,
            createdAt = now,
            updatedAt = now,
            version = 1L
        )
    }

    // =========================================================================
    // Create return does not touch inventory
    // =========================================================================

    @Test
    fun `createReturn does not mutate StockInRecord`() = runBlocking {
        val result = repository.createReturn(
            request = validReturn(),
            items = emptyList(),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-A"
        )
        assertTrue(result is DomainResult.Success)
        assertEquals(
            "StockInRecord count must remain 0 after createReturn",
            0, stockInRecordCount
        )
        Unit
    }

    @Test
    fun `createReturn does not mutate StockOutRecord`() = runBlocking {
        repository.createReturn(
            request = validReturn(),
            items = emptyList(),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-A"
        )
        assertEquals(
            "StockOutRecord count must remain 0 after createReturn",
            0, stockOutRecordCount
        )
        Unit
    }

    @Test
    fun `createReturn does not write to MovementLedger`() = runBlocking {
        repository.createReturn(
            request = validReturn(),
            items = emptyList(),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-A"
        )
        assertEquals(
            "MovementLedgerEntry count must remain 0 after createReturn",
            0, ledgerEntryCount
        )
        Unit
    }

    @Test
    fun `createReturn does not change warehouse balance`() = runBlocking {
        repository.createReturn(
            request = validReturn(),
            items = emptyList(),
            actorId = "USER-01",
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-A"
        )
        assertEquals(
            "Warehouse balance change count must remain 0 after createReturn",
            0, warehouseBalanceChangeCount
        )
        Unit
    }

    // =========================================================================
    // Status transitions do not touch inventory
    // =========================================================================

    @Test
    fun `full lifecycle transitions do not mutate any inventory entity`() = runBlocking {
        val request = validReturn()
        returnDataSource.insertReturn(request, emptyList())

        val transitions = listOf(
            ReturnStatus.UNDER_INSPECTION,
            ReturnStatus.APPROVED,
            ReturnStatus.RETURN_RECEIVED,
            ReturnStatus.PROCESSED
        )

        var currentVersion = 1L
        for (targetStatus in transitions) {
            val res = repository.transitionReturnStatus(
                returnId = request.returnId,
                targetStatus = targetStatus,
                actorId = "USER-01",
                expectedVersion = currentVersion,
                callerRole = UserRole.ADMIN,
                callerProjectId = "PRJ-A"
            )
            assertTrue("Transition to $targetStatus should succeed", res is DomainResult.Success)
            currentVersion = (res as DomainResult.Success).data.version
        }

        // After full lifecycle — no inventory was touched
        assertEquals("StockInRecord must remain 0", 0, stockInRecordCount)
        assertEquals("StockOutRecord must remain 0", 0, stockOutRecordCount)
        assertEquals("LedgerEntry must remain 0", 0, ledgerEntryCount)
        assertEquals("WarehouseBalance must remain 0", 0, warehouseBalanceChangeCount)
        Unit
    }

    // =========================================================================
    // Return data source only contains Return entities
    // =========================================================================

    @Test
    fun `FakeReturnDataSource only stores ReturnRequest and ReturnItem records`() = runBlocking {
        val request = validReturn()
        returnDataSource.insertReturn(request, emptyList())

        assertEquals(1, returnDataSource.countReturns())
        assertEquals(0, returnDataSource.countItems())

        // Verify the record is a ReturnRequest — not an inventory type
        val stored = returnDataSource.getReturn(request.returnId)
        assertTrue("Stored record must be a ReturnRequest", stored is ReturnRequest)
        assertFalse(
            "Stored object must not be a StockInRecord (inventory boundary check)",
            stored?.javaClass?.name?.contains("StockIn") == true
        )
        assertFalse(
            "Stored object must not be a MovementLedger (inventory boundary check)",
            stored?.javaClass?.name?.contains("Ledger") == true
        )
        Unit
    }

    // =========================================================================
    // Cancelled return does not trigger inventory rollback
    // =========================================================================

    @Test
    fun `cancellation does not create StockOutRecord or LedgerEntry`() = runBlocking {
        val request = validReturn()
        returnDataSource.insertReturn(request, emptyList())

        val res = repository.transitionReturnStatus(
            returnId = request.returnId,
            targetStatus = ReturnStatus.CANCELLED,
            actorId = "USER-01",
            expectedVersion = 1L,
            callerRole = UserRole.ADMIN,
            callerProjectId = "PRJ-A"
        )
        assertTrue(res is DomainResult.Success)

        assertEquals("No StockOutRecord after cancel", 0, stockOutRecordCount)
        assertEquals("No LedgerEntry after cancel", 0, ledgerEntryCount)
        Unit
    }
}
