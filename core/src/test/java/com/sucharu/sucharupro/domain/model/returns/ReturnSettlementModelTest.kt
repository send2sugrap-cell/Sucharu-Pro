package com.sucharu.sucharupro.domain.model.returns

import com.sucharu.sucharupro.domain.model.common.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Unit tests for [ReturnSettlement], [ReturnResolutionType], and [ReturnSettlementStatus]
 * domain models (Module 11 Step 05 Chunk 01).
 */
class ReturnSettlementModelTest {

    private fun createValidSettlement(
        settlementId: String = "SETTLE-001",
        returnId: String = "RET-101",
        projectId: String = "PRJ-01",
        customerId: String = "CUST-01",
        resolutionType: ReturnResolutionType = ReturnResolutionType.CREDIT_NOTE,
        amount: Money = Money(1500.0),
        status: ReturnSettlementStatus = ReturnSettlementStatus.COMPLETED,
        creditNoteId: String? = "CN-9001",
        replacementOrderId: String? = null,
        reworkId: String? = null,
        notes: String? = "Approved by accounts",
        settledBy: String = "ACTOR-ACCOUNTS",
        settledAt: Long = 1700000000000L,
        version: Long = 1L,
        idempotencyKey: String = "IDEMP-SETTLE-001"
    ) = ReturnSettlement(
        settlementId = settlementId,
        returnId = returnId,
        projectId = projectId,
        customerId = customerId,
        resolutionType = resolutionType,
        amount = amount,
        status = status,
        creditNoteId = creditNoteId,
        replacementOrderId = replacementOrderId,
        reworkId = reworkId,
        notes = notes,
        settledBy = settledBy,
        settledAt = settledAt,
        version = version,
        idempotencyKey = idempotencyKey
    )

    @Test
    fun `valid construction succeeds with all fields populated`() {
        val settlement = createValidSettlement()
        assertEquals("SETTLE-001", settlement.settlementId)
        assertEquals("RET-101", settlement.returnId)
        assertEquals("PRJ-01", settlement.projectId)
        assertEquals("CUST-01", settlement.customerId)
        assertEquals(ReturnResolutionType.CREDIT_NOTE, settlement.resolutionType)
        assertEquals(Money(1500.0), settlement.amount)
        assertEquals(ReturnSettlementStatus.COMPLETED, settlement.status)
        assertEquals("CN-9001", settlement.creditNoteId)
        assertNull(settlement.replacementOrderId)
        assertNull(settlement.reworkId)
        assertEquals("Approved by accounts", settlement.notes)
        assertEquals("ACTOR-ACCOUNTS", settlement.settledBy)
        assertEquals(1700000000000L, settlement.settledAt)
        assertEquals(1L, settlement.version)
        assertEquals("IDEMP-SETTLE-001", settlement.idempotencyKey)
    }

    @Test
    fun `all resolution types can be instantiated properly`() {
        val types = ReturnResolutionType.values()
        assertEquals(5, types.size)

        for (type in types) {
            val settlement = createValidSettlement(resolutionType = type)
            assertEquals(type, settlement.resolutionType)
            assertNotNull(type.displayName)
        }
    }

    @Test
    fun `pending and completed settlement statuses function as expected`() {
        val pending = createValidSettlement(status = ReturnSettlementStatus.PENDING)
        assertEquals(ReturnSettlementStatus.PENDING, pending.status)
        assertFalse(pending.status.isTerminal)

        val completed = createValidSettlement(status = ReturnSettlementStatus.COMPLETED)
        assertEquals(ReturnSettlementStatus.COMPLETED, completed.status)
        assertTrue(completed.status.isTerminal)

        val cancelled = createValidSettlement(status = ReturnSettlementStatus.CANCELLED)
        assertEquals(ReturnSettlementStatus.CANCELLED, cancelled.status)
        assertTrue(cancelled.status.isTerminal)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank settlementId is rejected`() {
        createValidSettlement(settlementId = "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank returnId is rejected`() {
        createValidSettlement(returnId = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank projectId is rejected`() {
        createValidSettlement(projectId = "  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank customerId is rejected`() {
        createValidSettlement(customerId = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank settledBy is rejected`() {
        createValidSettlement(settledBy = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank idempotencyKey is rejected`() {
        createValidSettlement(idempotencyKey = "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid zero version is rejected`() {
        createValidSettlement(version = 0L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid negative version is rejected`() {
        createValidSettlement(version = -1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid negative amount is rejected`() {
        createValidSettlement(amount = Money(BigDecimal("-10.00")))
    }

    @Test
    fun `zero amount is allowed for replacement or scrap write-off resolutions`() {
        val zeroSettlement = createValidSettlement(
            resolutionType = ReturnResolutionType.SCRAP_WRITE_OFF,
            amount = Money.ZERO,
            creditNoteId = null
        )
        assertEquals(Money.ZERO, zeroSettlement.amount)
    }

    @Test
    fun `immutability and copy semantics preserve data integrity`() {
        val original = createValidSettlement()
        val modified = original.copy(
            version = 2L,
            notes = "Updated notes"
        )
        assertEquals(1L, original.version)
        assertEquals(2L, modified.version)
        assertEquals("Approved by accounts", original.notes)
        assertEquals("Updated notes", modified.notes)
    }
}
