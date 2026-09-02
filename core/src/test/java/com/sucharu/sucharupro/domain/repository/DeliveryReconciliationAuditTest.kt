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
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationActivityType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReconciliationAuditTest {

    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReconciliationRepository

    @Before
    fun setUp() = runBlocking {
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        repository = DeliveryReconciliationRepositoryImpl(reconciliationDataSource, orderDataSource)

        val order = DeliveryOrder(
            deliveryOrderId = "DO-AUD",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-A",
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
        val line = DeliveryOrderLine("DOL-1", "DO-AUD", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order, listOf(line))
    }

    @Test
    fun `reconciliation lifecycle generates chronological audit events`() = runBlocking {
        // 1. Create
        val createRes = repository.createReconciliation("DO-AUD", "op-1", UserRole.WAREHOUSE)
        val recId = (createRes as DomainResult.Success).data.reconciliationId

        // 2. Start
        repository.startReconciliation(recId, "op-1", UserRole.WAREHOUSE)

        // 3. Mark Reconciled
        repository.markReconciled(recId, "op-1", "Done", UserRole.WAREHOUSE)

        // 4. Close
        repository.closeReconciliation(recId, "mgr-1", "Closed", UserRole.MANAGER)

        val eventsRes = repository.getActivityEvents(recId, UserRole.ADMIN)
        assertTrue(eventsRes is DomainResult.Success)
        val types = (eventsRes as DomainResult.Success).data.map { it.activityType }

        assertTrue(types.contains(DeliveryReconciliationActivityType.CREATED))
        assertTrue(types.contains(DeliveryReconciliationActivityType.RECONCILIATION_STARTED))
        assertTrue(types.contains(DeliveryReconciliationActivityType.RECONCILED))
        assertTrue(types.contains(DeliveryReconciliationActivityType.CLOSED))
    }
}
