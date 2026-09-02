package com.sucharu.sucharupro.vendorpayable

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.vendorpayable.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableIsolationTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepositoryImpl

    private val tenantA = "TENANT-A"
    private val tenantB = "TENANT-B"
    private val projectA = "PRJ-A"
    private val projectB = "PRJ-B"

    private lateinit var serviceA: VendorPayableServiceImpl
    private lateinit var serviceB: VendorPayableServiceImpl

    private val principalA = AuthenticatedPrincipal(
        userId = "USER-A",
        projectId = projectA,
        username = "usera",
        role = UserRole.ADMIN
    )

    private val principalB = AuthenticatedPrincipal(
        userId = "USER-B",
        projectId = projectB,
        username = "userb",
        role = UserRole.ADMIN
    )

    @Before
    fun setup() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
        serviceA = VendorPayableServiceImpl(repository, tenantA)
        serviceB = VendorPayableServiceImpl(repository, tenantB)
    }

    @Test
    fun testTenantAndProjectIsolation() = runBlocking {
        // Tenant A creates a payable
        val resA = serviceA.createPayable(
            principalA,
            CreateVendorPayableCommand(
                vendorId = "VEND-A",
                originalAmount = BigDecimal("5000.00"),
                description = "Tenant A Paper Stock"
            )
        )
        assertTrue(resA is DomainResult.Success)
        val payableA = (resA as DomainResult.Success).data

        // Tenant B creates a payable
        val resB = serviceB.createPayable(
            principalB,
            CreateVendorPayableCommand(
                vendorId = "VEND-B",
                originalAmount = BigDecimal("7000.00"),
                description = "Tenant B Ink Supplies"
            )
        )
        assertTrue(resB is DomainResult.Success)
        val payableB = (resB as DomainResult.Success).data

        // 1. Cross-tenant get by ID fails
        val crossGet = serviceB.getPayableById(principalB, payableA.payableId)
        assertTrue(crossGet is DomainResult.Error)

        // 2. Tenant A list only returns Tenant A payables
        val listA = serviceA.listPayables(principalA, VendorPayableFilter())
        assertTrue(listA is DomainResult.Success)
        val dataA = (listA as DomainResult.Success).data
        assertEquals(1, dataA.size)
        assertEquals("Tenant A Paper Stock", dataA[0].description)

        // 3. Tenant B list only returns Tenant B payables
        val listB = serviceB.listPayables(principalB, VendorPayableFilter())
        assertTrue(listB is DomainResult.Success)
        val dataB = (listB as DomainResult.Success).data
        assertEquals(1, dataB.size)
        assertEquals("Tenant B Ink Supplies", dataB[0].description)
    }
}
