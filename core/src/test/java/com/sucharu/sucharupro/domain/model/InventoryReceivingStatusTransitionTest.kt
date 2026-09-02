package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLineStatus
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [InventoryReceivingStatus] and [InventoryReceivingLineStatus] state machine properties
 * (Module 07 Step 03).
 */
class InventoryReceivingStatusTransitionTest {

    // ──────────────────────────────────────────────────────────────
    // InventoryReceivingStatus
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `COMPLETED and CANCELLED and REJECTED are terminal`() {
        assertTrue(InventoryReceivingStatus.COMPLETED.isTerminal)
        assertTrue(InventoryReceivingStatus.CANCELLED.isTerminal)
        assertTrue(InventoryReceivingStatus.REJECTED.isTerminal)
    }

    @Test
    fun `DRAFT PENDING RECEIVING ACCEPTED PARTIALLY_ACCEPTED PARTIALLY_REJECTED are not terminal`() {
        assertFalse(InventoryReceivingStatus.DRAFT.isTerminal)
        assertFalse(InventoryReceivingStatus.PENDING.isTerminal)
        assertFalse(InventoryReceivingStatus.RECEIVING.isTerminal)
        assertFalse(InventoryReceivingStatus.ACCEPTED.isTerminal)
        assertFalse(InventoryReceivingStatus.PARTIALLY_ACCEPTED.isTerminal)
        assertFalse(InventoryReceivingStatus.PARTIALLY_REJECTED.isTerminal)
    }

    @Test
    fun `all expected statuses exist`() {
        val statuses = InventoryReceivingStatus.values().map { it.name }.toSet()
        assertTrue("DRAFT" in statuses)
        assertTrue("PENDING" in statuses)
        assertTrue("RECEIVING" in statuses)
        assertTrue("PARTIALLY_ACCEPTED" in statuses)
        assertTrue("ACCEPTED" in statuses)
        assertTrue("PARTIALLY_REJECTED" in statuses)
        assertTrue("REJECTED" in statuses)
        assertTrue("COMPLETED" in statuses)
        assertTrue("CANCELLED" in statuses)
    }

    // ──────────────────────────────────────────────────────────────
    // InventoryReceivingLineStatus
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `ACCEPTED PARTIALLY_ACCEPTED REJECTED CANCELLED line statuses are terminal`() {
        assertTrue(InventoryReceivingLineStatus.ACCEPTED.isTerminal)
        assertTrue(InventoryReceivingLineStatus.PARTIALLY_ACCEPTED.isTerminal)
        assertTrue(InventoryReceivingLineStatus.REJECTED.isTerminal)
        assertTrue(InventoryReceivingLineStatus.CANCELLED.isTerminal)
    }

    @Test
    fun `PENDING and VERIFIED are not terminal`() {
        assertFalse(InventoryReceivingLineStatus.PENDING.isTerminal)
        assertFalse(InventoryReceivingLineStatus.VERIFIED.isTerminal)
    }

    @Test
    fun `ACCEPTED PARTIALLY_ACCEPTED REJECTED are finalized`() {
        assertTrue(InventoryReceivingLineStatus.ACCEPTED.isFinalized)
        assertTrue(InventoryReceivingLineStatus.PARTIALLY_ACCEPTED.isFinalized)
        assertTrue(InventoryReceivingLineStatus.REJECTED.isFinalized)
    }

    @Test
    fun `PENDING VERIFIED CANCELLED are not finalized`() {
        assertFalse(InventoryReceivingLineStatus.PENDING.isFinalized)
        assertFalse(InventoryReceivingLineStatus.VERIFIED.isFinalized)
        assertFalse(InventoryReceivingLineStatus.CANCELLED.isFinalized)
    }
}
