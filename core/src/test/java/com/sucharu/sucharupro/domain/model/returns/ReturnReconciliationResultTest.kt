package com.sucharu.sucharupro.domain.model.returns

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ReturnReconciliationResult] domain model (Module 11 Step 04 Chunk 04).
 */
class ReturnReconciliationResultTest {

    @Test
    fun `valid reconciliation result with inventory mutation passes`() {
        val result = ReturnReconciliationResult(
            returnId = "RET-001",
            receivingEventId = "RCV-001",
            projectId = "PRJ-001",
            acceptedQty = 10,
            stockInRecordId = "STOCKIN-001",
            ledgerEntryId = "LEDGER-001",
            inventoryMutationApplied = true,
            resultingStatus = ReturnStatus.PROCESSED,
            reconciledBy = "USER-WH-1"
        )

        assertEquals("RET-001", result.returnId)
        assertEquals("RCV-001", result.receivingEventId)
        assertEquals("PRJ-001", result.projectId)
        assertEquals(10, result.acceptedQty)
        assertEquals("STOCKIN-001", result.stockInRecordId)
        assertEquals("LEDGER-001", result.ledgerEntryId)
        assertTrue(result.inventoryMutationApplied)
        assertEquals(ReturnStatus.PROCESSED, result.resultingStatus)
        assertEquals("USER-WH-1", result.reconciledBy)
        assertTrue(result.completedAt > 0)
    }

    @Test
    fun `valid reconciliation result with zero accepted quantity and no mutation passes`() {
        val result = ReturnReconciliationResult(
            returnId = "RET-002",
            receivingEventId = "RCV-002",
            projectId = "PRJ-001",
            acceptedQty = 0,
            stockInRecordId = null,
            ledgerEntryId = null,
            inventoryMutationApplied = false,
            resultingStatus = ReturnStatus.PROCESSED,
            reconciledBy = "USER-WH-1"
        )

        assertEquals(0, result.acceptedQty)
        assertNull(result.stockInRecordId)
        assertNull(result.ledgerEntryId)
        assertFalse(result.inventoryMutationApplied)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank returnId throws exception`() {
        ReturnReconciliationResult(
            returnId = "  ",
            receivingEventId = "RCV-001",
            projectId = "PRJ-001",
            acceptedQty = 5,
            stockInRecordId = "STOCKIN-001",
            ledgerEntryId = "LEDGER-001",
            inventoryMutationApplied = true,
            resultingStatus = ReturnStatus.PROCESSED,
            reconciledBy = "USER-WH-1"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank receivingEventId throws exception`() {
        ReturnReconciliationResult(
            returnId = "RET-001",
            receivingEventId = "",
            projectId = "PRJ-001",
            acceptedQty = 5,
            stockInRecordId = "STOCKIN-001",
            ledgerEntryId = "LEDGER-001",
            inventoryMutationApplied = true,
            resultingStatus = ReturnStatus.PROCESSED,
            reconciledBy = "USER-WH-1"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank projectId throws exception`() {
        ReturnReconciliationResult(
            returnId = "RET-001",
            receivingEventId = "RCV-001",
            projectId = " ",
            acceptedQty = 5,
            stockInRecordId = "STOCKIN-001",
            ledgerEntryId = "LEDGER-001",
            inventoryMutationApplied = true,
            resultingStatus = ReturnStatus.PROCESSED,
            reconciledBy = "USER-WH-1"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative acceptedQty throws exception`() {
        ReturnReconciliationResult(
            returnId = "RET-001",
            receivingEventId = "RCV-001",
            projectId = "PRJ-001",
            acceptedQty = -1,
            stockInRecordId = "STOCKIN-001",
            ledgerEntryId = "LEDGER-001",
            inventoryMutationApplied = true,
            resultingStatus = ReturnStatus.PROCESSED,
            reconciledBy = "USER-WH-1"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank reconciledBy throws exception`() {
        ReturnReconciliationResult(
            returnId = "RET-001",
            receivingEventId = "RCV-001",
            projectId = "PRJ-001",
            acceptedQty = 5,
            stockInRecordId = "STOCKIN-001",
            ledgerEntryId = "LEDGER-001",
            inventoryMutationApplied = true,
            resultingStatus = ReturnStatus.PROCESSED,
            reconciledBy = ""
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing stockInRecordId when mutationApplied throws exception`() {
        ReturnReconciliationResult(
            returnId = "RET-001",
            receivingEventId = "RCV-001",
            projectId = "PRJ-001",
            acceptedQty = 5,
            stockInRecordId = null,
            ledgerEntryId = "LEDGER-001",
            inventoryMutationApplied = true,
            resultingStatus = ReturnStatus.PROCESSED,
            reconciledBy = "USER-WH-1"
        )
    }
}
