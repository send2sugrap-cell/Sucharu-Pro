package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAccount
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalIdempotencyTest {

    private lateinit var dataSource: FakeVendorPortalDataSource
    private lateinit var repository: VendorPortalRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeVendorPortalDataSource()
        repository = VendorPortalRepositoryImpl(dataSource)
    }

    @Test
    fun testDuplicateAccountForSameVendorIsRejected() {
        runBlocking {
            val a1 = VendorPortalAccount(
                portalAccountId = "pa_1",
                vendorId = "vnd_001",
                tenantId = "TENANT-001",
                projectId = "PROJ-ALPHA",
                portalCode = "CODE-1"
            )
            val res1 = repository.createAccount(a1)
            assertTrue(res1 is DomainResult.Success)

            // Second creation for same vendor fails
            val a2 = VendorPortalAccount(
                portalAccountId = "pa_2",
                vendorId = "vnd_001",
                tenantId = "TENANT-001",
                projectId = "PROJ-ALPHA",
                portalCode = "CODE-2"
            )
            val res2 = repository.createAccount(a2)
            assertTrue(res2 is DomainResult.Error)
            assertTrue((res2 as DomainResult.Error).exception is IllegalStateException)
        }
    }
}
