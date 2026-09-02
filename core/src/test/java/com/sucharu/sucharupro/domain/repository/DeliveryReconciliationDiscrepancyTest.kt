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
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancyType
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReconciliationDiscrepancyTest {

    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReconciliationRepository

    @Before
    fun setUp() = runBlocking {
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        repository = DeliveryReconciliationRepositoryImpl(reconciliationDataSource, orderDataSource)

        val order = DeliveryOrder(
            deliveryOrderId = "DO-DISC",
            projectId = "PRJ-01",
            deliveryOrderNo = "DON-D",
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
        val line = DeliveryOrderLine("DOL-1", "DO-DISC", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order, listOf(line))
    }

    @Test
    fun `resolveDiscrepancy updates discrepancy status and advances reconciliation to RESOLVED when all resolved`() = runBlocking {
        val createRes = repository.createReconciliation("DO-DISC", "op-1", UserRole.WAREHOUSE)
        val rec = (createRes as DomainResult.Success).data

        val discrepancy = DeliveryReconciliationDiscrepancy(
            discrepancyId = "DISC-01",
            reconciliationId = rec.reconciliationId,
            projectId = "PRJ-01",
            discrepancyType = DeliveryReconciliationDiscrepancyType.POD_MISSING,
            expectedValue = 100.0,
            actualValue = 0.0,
            description = "Missing POD evidence"
        )
        reconciliationDataSource.updateReconciliation(
            reconciliation = rec.copy(reconciliationStatus = DeliveryReconciliationStatus.DISPUTED),
            discrepancies = listOf(discrepancy)
        )

        val resolveRes = repository.resolveDiscrepancy(
            reconciliationId = rec.reconciliationId,
            discrepancyId = "DISC-01",
            resolutionNotes = "Physical receipt verified offline by manager",
            actorId = "mgr-1",
            callerRole = UserRole.MANAGER
        )
        assertTrue(resolveRes is DomainResult.Success)
        assertTrue((resolveRes as DomainResult.Success).data.isResolved)

        val updatedRec = repository.getReconciliation(rec.reconciliationId, UserRole.ADMIN)
        assertEquals(DeliveryReconciliationStatus.RESOLVED, (updatedRec as DomainResult.Success).data.reconciliationStatus)
    }
}
