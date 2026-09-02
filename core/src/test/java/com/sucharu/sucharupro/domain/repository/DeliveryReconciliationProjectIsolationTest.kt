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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeliveryReconciliationProjectIsolationTest {

    private lateinit var reconciliationDataSource: FakeDeliveryReconciliationDataSource
    private lateinit var orderDataSource: FakeDeliveryOrderDataSource
    private lateinit var repository: DeliveryReconciliationRepository

    @Before
    fun setUp() = runBlocking {
        reconciliationDataSource = FakeDeliveryReconciliationDataSource()
        orderDataSource = FakeDeliveryOrderDataSource()
        repository = DeliveryReconciliationRepositoryImpl(reconciliationDataSource, orderDataSource)

        val o1 = DeliveryOrder("DO-PRJ1", "PRJ-01", "DON-01", "CUST-01", "SO-01", "SO", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "u1", 1000L, 1000L)
        val l1 = DeliveryOrderLine("DOL-1", "DO-PRJ1", "PRJ-01", "P-1", 50.0, null)
        orderDataSource.insertDeliveryOrder(o1, listOf(l1))

        val o2 = DeliveryOrder("DO-PRJ2", "PRJ-02", "DON-02", "CUST-02", "SO-02", "SO", DeliveryOrderType.CUSTOMER_DELIVERY, DeliveryPriority.NORMAL, DeliveryOrderStatus.APPROVED, 2000L, null, "u2", 1000L, 1000L)
        val l2 = DeliveryOrderLine("DOL-2", "DO-PRJ2", "PRJ-02", "P-2", 80.0, null)
        orderDataSource.insertDeliveryOrder(o2, listOf(l2))
    }

    @Test
    fun `reconciliation records do not leak across project boundaries`() = runBlocking {
        repository.createReconciliation("DO-PRJ1", "op-1", UserRole.ADMIN, callerProjectId = "PRJ-01")
        repository.createReconciliation("DO-PRJ2", "op-2", UserRole.ADMIN, callerProjectId = "PRJ-02")

        val prj1List = repository.observeReconciliations("PRJ-01").first()
        assertEquals(1, prj1List.size)
        assertEquals("DO-PRJ1", prj1List[0].deliveryOrderId)

        val prj2List = repository.observeReconciliations("PRJ-02").first()
        assertEquals(1, prj2List.size)
        assertEquals("DO-PRJ2", prj2List[0].deliveryOrderId)
    }

    @Test
    fun `cross project access returns isolation error`() = runBlocking {
        val createRes = repository.createReconciliation("DO-PRJ1", "op-1", UserRole.ADMIN, callerProjectId = "PRJ-01")
        val recId = (createRes as DomainResult.Success).data.reconciliationId

        val crossRes = repository.getReconciliation(recId, UserRole.MANAGER, callerProjectId = "PRJ-02")
        assertTrue(crossRes is DomainResult.Error)
    }
}
