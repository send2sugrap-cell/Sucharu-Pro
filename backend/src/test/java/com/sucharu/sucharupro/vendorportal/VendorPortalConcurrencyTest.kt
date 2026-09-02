package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAccount
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAccountStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalConcurrencyTest {

    private lateinit var dataSource: FakeVendorPortalDataSource
    private lateinit var repository: VendorPortalRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeVendorPortalDataSource()
        repository = VendorPortalRepositoryImpl(dataSource)
    }

    @Test
    fun testConcurrentAccountCreationWithSameCodeIsBlocked() {
        runBlocking {
            val a1 = async {
                repository.createAccount(
                    VendorPortalAccount(
                        portalAccountId = "pa_conc_1",
                        vendorId = "vnd_1",
                        tenantId = "TENANT-001",
                        projectId = "PROJ-ALPHA",
                        portalCode = "CONCURRENT-CODE"
                    )
                )
            }
            val a2 = async {
                repository.createAccount(
                    VendorPortalAccount(
                        portalAccountId = "pa_conc_2",
                        vendorId = "vnd_2",
                        tenantId = "TENANT-001",
                        projectId = "PROJ-ALPHA",
                        portalCode = "CONCURRENT-CODE"
                    )
                )
            }

            val results = awaitAll(a1, a2)
            val successCount = results.count { it is DomainResult.Success }
            val errorCount = results.count { it is DomainResult.Error }

            assertEquals(1, successCount)
            assertEquals(1, errorCount)
        }
    }
}
