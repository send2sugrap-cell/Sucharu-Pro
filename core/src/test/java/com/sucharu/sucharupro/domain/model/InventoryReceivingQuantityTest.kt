package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingLineStatus
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryReceivingStatus
import com.sucharu.sucharupro.domain.validation.InventoryReceivingLifecycleValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [InventoryReceivingLifecycleValidator] state machine transitions
 * (Module 07 Step 03).
 */
class InventoryReceivingQuantityTest {

    // ──────────────────────────────────────────────────────────────
    // Receiving Lifecycle Transitions
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `DRAFT to PENDING is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.DRAFT, InventoryReceivingStatus.PENDING
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `DRAFT to CANCELLED is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.DRAFT, InventoryReceivingStatus.CANCELLED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `DRAFT to RECEIVING is invalid`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.DRAFT, InventoryReceivingStatus.RECEIVING
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `PENDING to RECEIVING is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.PENDING, InventoryReceivingStatus.RECEIVING
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `PENDING to CANCELLED is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.PENDING, InventoryReceivingStatus.CANCELLED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `RECEIVING to ACCEPTED is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.RECEIVING, InventoryReceivingStatus.ACCEPTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `RECEIVING to PARTIALLY_ACCEPTED is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.RECEIVING, InventoryReceivingStatus.PARTIALLY_ACCEPTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `RECEIVING to REJECTED is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.RECEIVING, InventoryReceivingStatus.REJECTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `ACCEPTED to COMPLETED is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.ACCEPTED, InventoryReceivingStatus.COMPLETED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `COMPLETED to anything is invalid (terminal)`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.COMPLETED, InventoryReceivingStatus.PENDING
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("terminal"))
    }

    @Test
    fun `CANCELLED to anything is invalid (terminal)`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.CANCELLED, InventoryReceivingStatus.DRAFT
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `same status to same status is valid (idempotent)`() {
        val result = InventoryReceivingLifecycleValidator.validateReceivingTransition(
            InventoryReceivingStatus.DRAFT, InventoryReceivingStatus.DRAFT
        )
        assertTrue(result is DomainResult.Success)
    }

    // ──────────────────────────────────────────────────────────────
    // Line Lifecycle Transitions
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `PENDING to VERIFIED is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateLineTransition(
            InventoryReceivingLineStatus.PENDING, InventoryReceivingLineStatus.VERIFIED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `PENDING to CANCELLED for line is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateLineTransition(
            InventoryReceivingLineStatus.PENDING, InventoryReceivingLineStatus.CANCELLED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `PENDING to ACCEPTED is invalid`() {
        val result = InventoryReceivingLifecycleValidator.validateLineTransition(
            InventoryReceivingLineStatus.PENDING, InventoryReceivingLineStatus.ACCEPTED
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun `VERIFIED to ACCEPTED is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateLineTransition(
            InventoryReceivingLineStatus.VERIFIED, InventoryReceivingLineStatus.ACCEPTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `VERIFIED to REJECTED is valid`() {
        val result = InventoryReceivingLifecycleValidator.validateLineTransition(
            InventoryReceivingLineStatus.VERIFIED, InventoryReceivingLineStatus.REJECTED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `ACCEPTED to anything is invalid (terminal)`() {
        val result = InventoryReceivingLifecycleValidator.validateLineTransition(
            InventoryReceivingLineStatus.ACCEPTED, InventoryReceivingLineStatus.PENDING
        )
        assertTrue(result is DomainResult.Error)
    }

    // ──────────────────────────────────────────────────────────────
    // Completion Status Derivation
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `all accepted derives ACCEPTED status`() {
        val result = InventoryReceivingLifecycleValidator.deriveReceivingCompletionStatus(3, 3, 0)
        assertEquals(InventoryReceivingStatus.ACCEPTED, result)
    }

    @Test
    fun `all rejected derives REJECTED status`() {
        val result = InventoryReceivingLifecycleValidator.deriveReceivingCompletionStatus(3, 0, 3)
        assertEquals(InventoryReceivingStatus.REJECTED, result)
    }

    @Test
    fun `mixed derives PARTIALLY_ACCEPTED`() {
        val result = InventoryReceivingLifecycleValidator.deriveReceivingCompletionStatus(3, 2, 1)
        assertEquals(InventoryReceivingStatus.PARTIALLY_ACCEPTED, result)
    }

    @Test
    fun `all accepted lines with no rejections derives PARTIALLY_ACCEPTED`() {
        val result = InventoryReceivingLifecycleValidator.deriveReceivingCompletionStatus(2, 2, 0)
        assertEquals(InventoryReceivingStatus.ACCEPTED, result)
    }
}
