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
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReconciliationConcurrencyTest {

    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReconciliationRepository

    @Before
    fun setUp() = runBlocking {
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        repository = DeliveryReconciliationRepositoryImpl(reconciliationDataSource, orderDataSource)

        val order = DeliveryOrder("DO-CONCUR", "PRJ-01", "DON-C", "CUST-1", "SO-1", "SO", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "u1", 1000L, 1000L)
        val line = DeliveryOrderLine("DOL-1", "DO-CONCUR", "PRJ-01", "P-1", 100.0, null)
        orderDataSource.insertDeliveryOrder(order, listOf(line))
    }

    @Test
    fun `concurrent discrepancy resolutions are atomic and do not deadlock`() = runBlocking {
        val createRes = repository.createReconciliation("DO-CONCUR", "op-1", UserRole.WAREHOUSE)
        val rec = (createRes as DomainResult.Success).data

        val discrepancies = (1..5).map { index ->
            DeliveryReconciliationDiscrepancy(
                discrepancyId = "DISC-$index",
                reconciliationId = rec.reconciliationId,
                projectId = "PRJ-01",
                discrepancyType = DeliveryReconciliationDiscrepancyType.QUANTITY_MISMATCH,
                expectedValue = 100.0,
                actualValue = 90.0,
                description = "Mismatch item $index"
            )
        }
        reconciliationDataSource.updateReconciliation(rec, discrepancies = discrepancies)

        val jobs = (1..5).map { index ->
            async(Dispatchers.IO) {
                repository.resolveDiscrepancy(
                    reconciliationId = rec.reconciliationId,
                    discrepancyId = "DISC-$index",
                    resolutionNotes = "Resolved concurrently $index",
                    actorId = "mgr-$index",
                    callerRole = UserRole.MANAGER
                )
            }
        }

        val results = jobs.awaitAll()
        assertTrue(results.all { it is DomainResult.Success })

        val discs = (repository.getDiscrepancies(rec.reconciliationId, UserRole.ADMIN) as DomainResult.Success).data
        assertEquals(5, discs.count { it.isResolved })
    }
}
