package com.sucharu.sucharupro.domain.integration

import com.sucharu.sucharupro.data.datasource.*
import com.sucharu.sucharupro.data.repository.InventoryAnalyticsRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryMovementLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.InventoryReorderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.analytics.InventoryAnalyticsPeriod
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementDirection
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerEntry
import com.sucharu.sucharupro.domain.model.inventory.ledger.InventoryMovementLedgerType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.InventoryAnalyticsRepository
import com.sucharu.sucharupro.domain.repository.InventoryMovementLedgerRepository
import com.sucharu.sucharupro.domain.repository.InventoryReorderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * End-to-End integration tests for Inventory Analytics & Governance (Module 07 Step 10).
 */
class InventoryAnalyticsEndToEndTest {

    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var reorderDataSource: FakeInventoryReorderDataSource
    private lateinit var analyticsDataSource: FakeInventoryAnalyticsDataSource
    
    private lateinit var ledgerRepository: InventoryMovementLedgerRepository
    private lateinit var reorderRepository: InventoryReorderRepository
    private lateinit var analyticsRepository: InventoryAnalyticsRepository

    @Before
    fun setup() {
        ledgerDataSource = FakeInventoryMovementLedgerDataSource()
        reorderDataSource = FakeInventoryReorderDataSource()
        analyticsDataSource = FakeInventoryAnalyticsDataSource()

        val receivingDataSource = FakeInventoryReceivingDataSource()
        val stockOutDataSource = FakeInventoryStockOutDataSource()
        val transferDataSource = FakeInventoryStockTransferDataSource()
        val adjustmentDataSource = FakeInventoryStockAdjustmentDataSource()
        val productDataSource = FakeInventoryProductDataSource()
        val warehouseDataSource = FakeInventoryWarehouseDataSource()
        val locationDataSource = FakeInventoryLocationDataSource()
        val traceabilityDataSource = FakeInventoryTraceabilityDataSource()

        ledgerRepository = InventoryMovementLedgerRepositoryImpl(
            ledgerDataSource = ledgerDataSource,
            receivingDataSource = receivingDataSource,
            stockOutDataSource = stockOutDataSource,
            transferDataSource = transferDataSource,
            adjustmentDataSource = adjustmentDataSource,
            traceabilityDataSource = traceabilityDataSource
        )

        reorderRepository = InventoryReorderRepositoryImpl(
            reorderDataSource = reorderDataSource,
            receivingDataSource = receivingDataSource,
            stockOutDataSource = stockOutDataSource,
            transferDataSource = transferDataSource,
            adjustmentDataSource = adjustmentDataSource,
            productDataSource = productDataSource,
            warehouseDataSource = warehouseDataSource,
            locationDataSource = locationDataSource
        )

        analyticsRepository = InventoryAnalyticsRepositoryImpl(
            analyticsDataSource = analyticsDataSource,
            ledgerRepository = ledgerRepository,
            reorderRepository = reorderRepository
        )
    }

    @Test
    fun `basic analytics retrieval`() = runBlocking {
        val projectId = "PRJ-E2E"

        // 1. Seed Movements into Ledger (STOCK_IN 100, STOCK_OUT 120 -> Net -20)
        ledgerDataSource.insertEntries(listOf(
            createEntry(projectId, "PRD-01", "SRC-1", 100.0, "2026-08-17T08:00:00Z", InventoryMovementLedgerType.STOCK_IN, 10.0),
            createEntry(projectId, "PRD-01", "SRC-2", -120.0, "2026-08-17T09:00:00Z", InventoryMovementLedgerType.STOCK_OUT, 10.0)
        ))

        // 2. Verify Summary
        val summaryResult = analyticsRepository.getAnalyticsSummary(projectId, InventoryAnalyticsPeriod.CUSTOM, UserRole.ADMIN)
        if (summaryResult is DomainResult.Error) {
            println("ERROR: ${summaryResult.message}")
            summaryResult.exception?.printStackTrace()
        }
        assertTrue("Summary should be successful", summaryResult is DomainResult.Success)
    }

    private fun createEntry(
        projectId: String,
        productId: String,
        sourceId: String,
        quantity: Double,
        timestamp: String,
        type: InventoryMovementLedgerType,
        unitCost: Double?
    ) = InventoryMovementLedgerEntry(
        ledgerEntryId = UUID.randomUUID().toString(),
        projectId = projectId,
        productId = productId,
        locationId = "LOC-01",
        movementType = type,
        direction = if (quantity > 0) InventoryMovementDirection.IN else InventoryMovementDirection.OUT,
        quantity = quantity,
        unitCost = unitCost,
        totalCost = unitCost?.let { it * Math.abs(quantity) },
        referenceId = "REF-E2E",
        referenceType = "INTEGRATION",
        movementAt = timestamp,
        sourceMovementId = sourceId,
        createdAt = timestamp
    )
}
