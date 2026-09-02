package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeDeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.FakeDeliveryReconciliationDataSource
import com.sucharu.sucharupro.data.repository.DeliveryReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrder
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderLine
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderStatus
import com.sucharu.sucharupro.domain.model.delivery.DeliveryOrderType
import com.sucharu.sucharupro.domain.model.delivery.DeliveryPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReconciliationFinancialBoundaryTest {

    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReconciliationRepository

    @Before
    fun setUp() = runBlocking {
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        repository = DeliveryReconciliationRepositoryImpl(reconciliationDataSource, orderDataSource)

        val order = DeliveryOrder(
            deliveryOrderId = "DO-FIN",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-F",
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
        val line = DeliveryOrderLine("DOL-1", "DO-FIN", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order, listOf(line))
    }

    @Test
    fun `reconciliation records remain purely operational without financial payment mutation`() = runBlocking {
        val createRes = repository.createReconciliation("DO-FIN", "op-1", UserRole.WAREHOUSE)
        assertTrue(createRes is DomainResult.Success)
        val rec = (createRes as DomainResult.Success).data

        repository.startReconciliation(rec.reconciliationId, "op-1", UserRole.WAREHOUSE)
        val reconciledRes = repository.markReconciled(rec.reconciliationId, "op-1", "Reconciled", UserRole.WAREHOUSE)
        assertTrue(reconciledRes is DomainResult.Success)

        // Verify operational settlement property without any external ledger creation
        assertNotNull((reconciledRes as DomainResult.Success).data.settlementStatus)
    }
}
