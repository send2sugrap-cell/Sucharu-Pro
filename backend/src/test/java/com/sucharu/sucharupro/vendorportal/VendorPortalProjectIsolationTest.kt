package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalService
import com.sucharu.sucharupro.domain.service.vendorportal.VendorPortalServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalProjectIsolationTest {

    private lateinit var vendorDs: FakeVendorDataSource
    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var portalDs: FakeVendorPortalDataSource
    private lateinit var portalRepo: VendorPortalRepositoryImpl
    private lateinit var portalService: VendorPortalService

    @Before
    fun setUp() {
        runBlocking {
            vendorDs = FakeVendorDataSource()
            vendorRepo = VendorRepositoryImpl(vendorDs)
            portalDs = FakeVendorPortalDataSource()
            portalRepo = VendorPortalRepositoryImpl(portalDs)
            portalService = VendorPortalServiceImpl(portalRepo, vendorRepo)

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_proj_1",
                    projectId = "PROJ-1",
                    vendorCode = "VND-1",
                    vendorName = "Project 1 Vendor",
                    status = VendorStatus.ACTIVE
                )
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "vnd_proj_2",
                    projectId = "PROJ-2",
                    vendorCode = "VND-2",
                    vendorName = "Project 2 Vendor",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testListAccountsFilteredByProject() {
        runBlocking {
            portalService.createOrInviteAccount("vnd_proj_1", "PROJ1-01", null, null, "TENANT-001", "PROJ-1", "admin_001")
            portalService.createOrInviteAccount("vnd_proj_2", "PROJ2-01", null, null, "TENANT-001", "PROJ-2", "admin_001")

            val proj1List = portalService.listAccounts("PROJ-1", null, "TENANT-001")
            assertTrue(proj1List is DomainResult.Success)
            val list = (proj1List as DomainResult.Success).data
            assertEquals(1, list.size)
            assertEquals("PROJ1-01", list[0].portalCode)
        }
    }
}
