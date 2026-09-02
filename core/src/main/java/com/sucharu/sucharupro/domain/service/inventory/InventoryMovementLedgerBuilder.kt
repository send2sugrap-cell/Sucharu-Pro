package com.sucharu.sucharupro.domain.service.inventory

import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryAdjustmentType
import com.sucharu.sucharupro.domain.model.inventory.adjustment.InventoryStockAdjustmentRecord
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.inventory.stockout.InventoryStockOutRecord
import com.sucharu.sucharupro.domain.model.inventory.stocktransfer.InventoryStockTransferRecord
import java.time.Instant
import java.util.UUID

/**
 * Domain service to map physical and logical stock records into the immutable movement ledger
 * (Module 07 Step 09).
 */
object InventoryMovementLedgerBuilder {

    /**
     * Maps an [InventoryStockInRecord] to an [InventoryMovementLedgerEntry] (IN).
     */
    fun buildFromStockIn(
        record: InventoryStockInRecord,
        unitCost: Double? = null
    ): InventoryMovementLedgerEntry {
        val now = Instant.now().toString()
        return InventoryMovementLedgerEntry(
            ledgerEntryId = UUID.randomUUID().toString(),
            projectId = record.projectId,
            productId = record.inventoryProductId,
            locationId = record.locationId,
            movementType = InventoryMovementLedgerType.STOCK_IN,
            direction = InventoryMovementDirection.IN,
            quantity = record.quantity.toDouble(),
            unitCost = unitCost,
            totalCost = unitCost?.let { it * record.quantity },
            referenceId = record.receivingId,
            referenceType = "RECEIVING",
            movementAt = record.createdAt,
            sourceMovementId = record.stockInId,
            createdAt = now
        )
    }

    /**
     * Maps an [InventoryStockOutRecord] to an [InventoryMovementLedgerEntry] (OUT).
     */
    fun buildFromStockOut(
        record: InventoryStockOutRecord,
        unitCost: Double? = null
    ): InventoryMovementLedgerEntry {
        val now = Instant.now().toString()
        return InventoryMovementLedgerEntry(
            ledgerEntryId = UUID.randomUUID().toString(),
            projectId = record.projectId,
            productId = record.inventoryProductId,
            locationId = record.locationId,
            movementType = InventoryMovementLedgerType.STOCK_OUT,
            direction = InventoryMovementDirection.OUT,
            quantity = -record.quantity.toDouble(),
            unitCost = unitCost,
            totalCost = unitCost?.let { it * record.quantity },
            referenceId = record.stockOutId,
            referenceType = "STOCK_OUT",
            movementAt = record.createdAt,
            sourceMovementId = record.stockOutRecordId,
            createdAt = now
        )
    }

    /**
     * Maps an [InventoryStockTransferRecord] to a pair of [InventoryMovementLedgerEntry] (OUT + IN).
     */
    fun buildFromStockTransfer(
        record: InventoryStockTransferRecord,
        unitCost: Double? = null
    ): List<InventoryMovementLedgerEntry> {
        val now = Instant.now().toString()
        
        val outEntry = InventoryMovementLedgerEntry(
            ledgerEntryId = UUID.randomUUID().toString(),
            projectId = record.projectId,
            productId = record.inventoryProductId,
            locationId = record.fromLocationId,
            movementType = InventoryMovementLedgerType.TRANSFER_OUT,
            direction = InventoryMovementDirection.OUT,
            quantity = -record.quantity.toDouble(),
            unitCost = unitCost,
            totalCost = unitCost?.let { it * record.quantity },
            referenceId = record.transferId,
            referenceType = "TRANSFER",
            movementAt = record.createdAt,
            sourceMovementId = record.transferRecordId,
            createdAt = now
        )

        val inEntry = InventoryMovementLedgerEntry(
            ledgerEntryId = UUID.randomUUID().toString(),
            projectId = record.projectId,
            productId = record.inventoryProductId,
            locationId = record.toLocationId,
            movementType = InventoryMovementLedgerType.TRANSFER_IN,
            direction = InventoryMovementDirection.IN,
            quantity = record.quantity.toDouble(),
            unitCost = unitCost,
            totalCost = unitCost?.let { it * record.quantity },
            referenceId = record.transferId,
            referenceType = "TRANSFER",
            movementAt = record.createdAt,
            sourceMovementId = record.transferRecordId,
            createdAt = now
        )

        return listOf(outEntry, inEntry)
    }

    /**
     * Maps an [InventoryStockAdjustmentRecord] to an [InventoryMovementLedgerEntry].
     */
    fun buildFromStockAdjustment(
        record: InventoryStockAdjustmentRecord,
        unitCost: Double? = null
    ): InventoryMovementLedgerEntry {
        val now = Instant.now().toString()
        val isIncrease = record.adjustmentType == InventoryAdjustmentType.INCREASE
        
        return InventoryMovementLedgerEntry(
            ledgerEntryId = UUID.randomUUID().toString(),
            projectId = record.projectId,
            productId = record.inventoryProductId,
            locationId = record.locationId,
            movementType = if (isIncrease) InventoryMovementLedgerType.ADJUSTMENT_IN else InventoryMovementLedgerType.ADJUSTMENT_OUT,
            direction = if (isIncrease) InventoryMovementDirection.IN else InventoryMovementDirection.OUT,
            quantity = if (isIncrease) record.quantity.toDouble() else -record.quantity.toDouble(),
            unitCost = unitCost,
            totalCost = unitCost?.let { it * record.quantity },
            referenceId = record.adjustmentId,
            referenceType = "ADJUSTMENT",
            movementAt = record.createdAt,
            sourceMovementId = record.adjustmentRecordId,
            createdAt = now
        )
    }
}
