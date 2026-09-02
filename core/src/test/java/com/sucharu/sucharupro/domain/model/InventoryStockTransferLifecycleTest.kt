package com.sucharu.sucharupro.domain.model

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransfer
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferStatus
import com.sucharu.sucharupro.domain.validation.InventoryStockTransferLifecycleValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * State machine transition tests for [InventoryStockTransfer] (Module 07 Step 05).
 */
class InventoryStockTransferLifecycleTest {

    @Test
    fun `valid transition from DRAFT to PENDING`() {
        val result = InventoryStockTransferLifecycleValidator.validateTransition(
            current = InventoryStockTransferStatus.DRAFT,
            target = InventoryStockTransferStatus.PENDING
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `valid transition from PENDING to APPROVED`() {
        val result = InventoryStockTransferLifecycleValidator.validateTransition(
            current = InventoryStockTransferStatus.PENDING,
            target = InventoryStockTransferStatus.APPROVED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `valid transition from APPROVED to TRANSFERRING`() {
        val result = InventoryStockTransferLifecycleValidator.validateTransition(
            current = InventoryStockTransferStatus.APPROVED,
            target = InventoryStockTransferStatus.TRANSFERRING
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `valid transition from TRANSFERRING to COMPLETED`() {
        val result = InventoryStockTransferLifecycleValidator.validateTransition(
            current = InventoryStockTransferStatus.TRANSFERRING,
            target = InventoryStockTransferStatus.COMPLETED
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `terminal state COMPLETED cannot transition`() {
        val result = InventoryStockTransferLifecycleValidator.validateTransition(
            current = InventoryStockTransferStatus.COMPLETED,
            target = InventoryStockTransferStatus.DRAFT
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("terminal state"))
    }

    @Test
    fun `invalid transition from DRAFT to COMPLETED`() {
        val result = InventoryStockTransferLifecycleValidator.validateTransition(
            current = InventoryStockTransferStatus.DRAFT,
            target = InventoryStockTransferStatus.COMPLETED
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Invalid stock transfer transition"))
    }

    @Test
    fun `can mutate returns true for non-terminal states`() {
        assertTrue(InventoryStockTransferLifecycleValidator.canMutate(InventoryStockTransferStatus.DRAFT))
        assertTrue(InventoryStockTransferLifecycleValidator.canMutate(InventoryStockTransferStatus.PENDING))
        assertTrue(InventoryStockTransferLifecycleValidator.canMutate(InventoryStockTransferStatus.APPROVED))
        assertTrue(InventoryStockTransferLifecycleValidator.canMutate(InventoryStockTransferStatus.TRANSFERRING))
    }

    @Test
    fun `can mutate returns false for terminal states`() {
        assertTrue(!InventoryStockTransferLifecycleValidator.canMutate(InventoryStockTransferStatus.COMPLETED))
        assertTrue(!InventoryStockTransferLifecycleValidator.canMutate(InventoryStockTransferStatus.CANCELLED))
    }
}
