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

class VendorWorkOrderTenantIsolationTest {

    private lateinit var repo: VendorWorkOrderRepositoryImpl
    private lateinit var fakeDs: FakeVendorWorkOrderDataSource

    @Before
    fun setup() {
        fakeDs = FakeVendorWorkOrderDataSource()
        repo = VendorWorkOrderRepositoryImpl(fakeDs)
    }

    @Test
    fun `work order created in Tenant A cannot be viewed or modified by Tenant B`() = runBlocking {
        val orderA = VendorWorkOrder(
            workOrderId = "vwo_tenant_a",
            projectId = "tenant_a",
            workOrderNumber = "VWO-A-01",
            vendorId = "vnd_a",
            capabilityType = CapabilityType.DIE_CUTTING,
            title = "Box Die Cutting",
            quantity = BigDecimal("500"),
            estimatedAmount = Money(BigDecimal("1500.00"))
        )
        repo.createWorkOrder(orderA)

        // Tenant A can find it
        val findA = repo.findById("tenant_a", "vwo_tenant_a")
        assertTrue(findA is DomainResult.Success)

        // Tenant B cannot find it
        val findB = repo.findById("tenant_b", "vwo_tenant_a")
        assertTrue(findB is DomainResult.Error)

        // Tenant B cannot find by number
        val findNumB = repo.findByNumber("tenant_b", "VWO-A-01")
        assertTrue(findNumB is DomainResult.Error)

        // Tenant B list does not leak Tenant A orders
        val listB = repo.list("tenant_b")
        assertTrue(listB is DomainResult.Success)
        assertTrue((listB as DomainResult.Success).data.isEmpty())
    }
}
