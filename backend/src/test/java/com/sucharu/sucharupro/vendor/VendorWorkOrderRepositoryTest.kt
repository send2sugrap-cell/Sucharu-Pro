package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorWorkOrderDataSource
import com.sucharu.sucharupro.data.repository.VendorWorkOrderRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorWorkOrderRepositoryTest {

    private lateinit var repo: VendorWorkOrderRepositoryImpl
    private lateinit var fakeDs: FakeVendorWorkOrderDataSource

    @Before
    fun setup() {
        fakeDs = FakeVendorWorkOrderDataSource()
        repo = VendorWorkOrderRepositoryImpl(fakeDs)
    }

    @Test
    fun `save, retrieve by ID and number, and filter list`() = runBlocking {
        val order = VendorWorkOrder(
            workOrderId = "vwo_1",
            projectId = "p_1",
            workOrderNumber = "VWO-0001",
            vendorId = "vnd_1",
            capabilityType = CapabilityType.CTP,
            title = "CTP Job 1",
            quantity = BigDecimal("50"),
            estimatedAmount = Money(BigDecimal("500.00")),
            status = VendorWorkOrderStatus.ASSIGNED
        )

        val createRes = repo.createWorkOrder(order)
        assertTrue(createRes is DomainResult.Success)

        val findIdRes = repo.findById("p_1", "vwo_1")
        assertTrue(findIdRes is DomainResult.Success)
        assertEquals("VWO-0001", (findIdRes as DomainResult.Success).data.workOrderNumber)

        val findNumRes = repo.findByNumber("p_1", "VWO-0001")
        assertTrue(findNumRes is DomainResult.Success)

        val listRes = repo.list("p_1", vendorId = "vnd_1", status = VendorWorkOrderStatus.ASSIGNED)
        assertTrue(listRes is DomainResult.Success)
        assertEquals(1, (listRes as DomainResult.Success).data.size)
    }

    @Test
    fun `optimistic concurrency failure when updating on stale version`() = runBlocking {
        val order = VendorWorkOrder(
            workOrderId = "vwo_2",
            projectId = "p_1",
            workOrderNumber = "VWO-0002",
            vendorId = "vnd_1",
            capabilityType = CapabilityType.CTP,
            title = "Initial Title",
            quantity = BigDecimal("50"),
            estimatedAmount = Money(BigDecimal("500.00")),
            version = 1L
        )
        repo.createWorkOrder(order)

        // First update succeeds and increments version to 2
        val update1 = repo.updateWorkOrder(order.copy(title = "Updated Title 1", version = 1L))
        assertTrue(update1 is DomainResult.Success)
        assertEquals(2L, (update1 as DomainResult.Success).data.version)

        // Second update with stale version 1L fails
        val update2 = repo.updateWorkOrder(order.copy(title = "Stale Update", version = 1L))
        assertTrue(update2 is DomainResult.Error)
    }

    @Test
    fun `audit append and retrieval`() = runBlocking {
        val audit = VendorWorkOrderAuditEvent(
            auditId = "aud_1",
            projectId = "p_1",
            workOrderId = "vwo_1",
            eventType = "CREATED",
            actorId = "user_admin",
            details = "Order created"
        )
        val appendRes = repo.appendAudit(audit)
        assertTrue(appendRes is DomainResult.Success)

        val listRes = repo.listAudits("p_1", "vwo_1")
        assertTrue(listRes is DomainResult.Success)
        assertEquals(1, (listRes as DomainResult.Success).data.size)
        assertEquals("CREATED", (listRes as DomainResult.Success).data[0].eventType)
    }
}
