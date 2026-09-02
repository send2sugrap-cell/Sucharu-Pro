package com.sucharu.sucharupro.data.persistence.postgres

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorCategory
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.model.vendor.VendorType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorTenantIsolationTest {

    private lateinit var fakeDataSource: FakeVendorDataSource
    private lateinit var repository: VendorRepositoryImpl

    @Before
    fun setUp() {
        fakeDataSource = FakeVendorDataSource()
        repository = VendorRepositoryImpl(fakeDataSource)
    }

    @Test
    fun testTenantIsolationCrossLookupBlocked() = runBlocking {
        // Tenant A creates Vendor
        val vendorA = Vendor(
            vendorId = "vnd_a_001",
            projectId = "TENANT-A",
            vendorCode = "VND-CODE-01",
            vendorName = "Tenant A Paper Supply",
            vendorType = VendorType.MATERIAL_SUPPLIER,
            vendorCategory = VendorCategory.PAPER_SUPPLIER,
            status = VendorStatus.ACTIVE
        )
        repository.createVendor(vendorA)

        // Tenant B creates Vendor with SAME code (allowed across different tenants)
        val vendorB = Vendor(
            vendorId = "vnd_b_001",
            projectId = "TENANT-B",
            vendorCode = "VND-CODE-01",
            vendorName = "Tenant B Logistics Provider",
            vendorType = VendorType.LOGISTICS_VENDOR,
            vendorCategory = VendorCategory.LOGISTICS_TRANSPORT,
            status = VendorStatus.ACTIVE
        )
        repository.createVendor(vendorB)

        // Tenant A finds only Tenant A's record
        val resA = repository.findById("TENANT-A", "vnd_a_001")
        assertTrue(resA is DomainResult.Success)
        assertEquals("Tenant A Paper Supply", (resA as DomainResult.Success).data.vendorName)

        // Tenant A cannot find Tenant B's ID
        val resACross = repository.findById("TENANT-A", "vnd_b_001")
        assertTrue(resACross is DomainResult.Error)

        // Tenant B finds only Tenant B's record
        val resB = repository.findById("TENANT-B", "vnd_b_001")
        assertTrue(resB is DomainResult.Success)
        assertEquals("Tenant B Logistics Provider", (resB as DomainResult.Success).data.vendorName)

        // Tenant B cannot find Tenant A's ID
        val resBCross = repository.findById("TENANT-B", "vnd_a_001")
        assertTrue(resBCross is DomainResult.Error)

        // Lookup by code in Tenant A returns Tenant A's vendor
        val codeA = repository.findByCode("TENANT-A", "VND-CODE-01")
        assertTrue(codeA is DomainResult.Success)
        assertEquals("vnd_a_001", (codeA as DomainResult.Success).data.vendorId)

        // Lookup by code in Tenant B returns Tenant B's vendor
        val codeB = repository.findByCode("TENANT-B", "VND-CODE-01")
        assertTrue(codeB is DomainResult.Success)
        assertEquals("vnd_b_001", (codeB as DomainResult.Success).data.vendorId)
    }

    @Test
    fun testTenantListIsolation() = runBlocking {
        repository.createVendor(Vendor("v1", "TENANT-ALPHA", "V-1", "Alpha Vendor 1"))
        repository.createVendor(Vendor("v2", "TENANT-ALPHA", "V-2", "Alpha Vendor 2"))
        repository.createVendor(Vendor("v3", "TENANT-BETA", "V-1", "Beta Vendor 1"))

        val alphaList = (repository.listVendors("TENANT-ALPHA") as DomainResult.Success).data
        assertEquals(2, alphaList.size)
        assertTrue(alphaList.all { it.projectId == "TENANT-ALPHA" })

        val betaList = (repository.listVendors("TENANT-BETA") as DomainResult.Success).data
        assertEquals(1, betaList.size)
        assertTrue(betaList.all { it.projectId == "TENANT-BETA" })
    }
}
