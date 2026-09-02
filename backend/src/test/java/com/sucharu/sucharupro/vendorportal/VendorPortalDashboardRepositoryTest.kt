package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalDashboardRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalDashboardRepositoryTest {

    private lateinit var vendorDs: FakeVendorDataSource
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var portalDs: FakeVendorPortalDataSource
    private lateinit var portalRepo: VendorPortalRepositoryImpl
    private lateinit var dashboardRepo: VendorPortalDashboardRepositoryImpl

    @Before
    fun setUp() {
        runBlocking {
            vendorDs = FakeVendorDataSource()
            vendorRepo = VendorRepositoryImpl(vendorDs)
            portalDs = FakeVendorPortalDataSource()
            portalRepo = VendorPortalRepositoryImpl(portalDs)

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_001",
                    projectId = "PROJ-ALPHA",
                    vendorCode = "VND-001",
                    vendorName = "Global Box Makers",
                    status = VendorStatus.ACTIVE
                )
            )

            dashboardRepo = VendorPortalDashboardRepositoryImpl(
                vendorRepository = vendorRepo,
                portalRepository = portalRepo
            )
        }
    }

    @Test
    fun testGetProfileSummaryGracefullyDegradesWhenOptionalReposNull() {
        runBlocking {
            val res = dashboardRepo.getProfileSummary("vnd_001", "TENANT-001", "PROJ-ALPHA")
            assertTrue(res is DomainResult.Success)
            val profile = (res as DomainResult.Success).data
            assertEquals("vnd_001", profile.vendorId)
            assertEquals("VND-001", profile.vendorCode)
            assertEquals("Global Box Makers", profile.vendorName)
            assertEquals(0, profile.serviceCount)
            assertEquals(0, profile.capabilityCount)
        }
    }

    @Test
    fun testGetOperationalSummaryEmptyDefaults() {
        runBlocking {
            val res = dashboardRepo.getOperationalSummary("vnd_001", "TENANT-001", "PROJ-ALPHA")
            assertTrue(res is DomainResult.Success)
            val ops = (res as DomainResult.Success).data
            assertEquals(0, ops.totalPurchaseOrders)
            assertEquals(0.0, ops.onTimeDeliveryRatePercent, 0.001)
        }
    }

    @Test
    fun testGetFinancialSummaryEmptyDefaults() {
        runBlocking {
            val res = dashboardRepo.getFinancialSummary("vnd_001", "TENANT-001", "PROJ-ALPHA")
            assertTrue(res is DomainResult.Success)
            val fin = (res as DomainResult.Success).data
            assertEquals(0, fin.totalInvoices)
        }
    }
}
