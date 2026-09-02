package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.repository.DeliveryReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReconciliationInventoryBoundaryTest {

    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var ledgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var repository: DeliveryReconciliationRepository

    @Before
    fun setUp() = runBlocking {
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        ledgerDataSource = FakeInventoryMovementLedgerDataSource()

        repository = DeliveryReconciliationRepositoryImpl(reconciliationDataSource, orderDataSource)

        val order = DeliveryOrder(
            deliveryOrderId = "DO-INV",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-I",
            customerId = "CUST-1",
            sourceReferenceId = "SO-1",
            sourceReferenceType = "SO",
            deliveryType = DeliveryOrderType.CUSTOMER_DELIVERY,
            priority = DeliveryPriority.NORMAL,
            status = DeliveryOrderStatus.APPROVED,
            requestedDeliveryDate = 2000L,
            notes = null,
            createdBy = "u1",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val line = DeliveryOrderLine("DOL-1", "DO-INV", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order, listOf(line))
    }

    @Test
    fun `reconciliation operations perform zero stock or ledger mutations`() = runBlocking {
        val initialStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val initialStockIns = receivingDataSource.observeStockInRecords().first().size
        val initialLedgerEntries = ledgerDataSource.getEntries("PRJ-01").size

        val createRes = repository.createReconciliation("DO-INV", "op-1", UserRole.WAREHOUSE)
        val recId = (createRes as DomainResult.Success).data.reconciliationId

        repository.refreshCalculation(recId, "op-1", UserRole.WAREHOUSE)
        repository.startReconciliation(recId, "op-1", UserRole.WAREHOUSE)
        repository.markReconciled(recId, "op-1", "Marked", UserRole.WAREHOUSE)
        repository.closeReconciliation(recId, "mgr-1", "Closed", UserRole.MANAGER)

        val finalStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val finalStockIns = receivingDataSource.observeStockInRecords().first().size
        val finalLedgerEntries = ledgerDataSource.getEntries("PRJ-01").size

        assertEquals(initialStockOuts, finalStockOuts)
        assertEquals(initialStockIns, finalStockIns)
        assertEquals(initialLedgerEntries, finalLedgerEntries)
    }
}
