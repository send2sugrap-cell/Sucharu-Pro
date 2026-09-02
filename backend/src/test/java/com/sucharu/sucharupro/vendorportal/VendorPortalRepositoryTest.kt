package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalRepositoryTest {

    private lateinit var dataSource: FakeVendorPortalDataSource
    private lateinit var repository: VendorPortalRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeVendorPortalDataSource()
        repository = VendorPortalRepositoryImpl(dataSource)
    }

    @Test
    fun testAccountCrudAndOptimisticLocking() {
        runBlocking {
            val account = VendorPortalAccount(
                portalAccountId = "pa_001",
                vendorId = "vnd_001",
                tenantId = "TENANT-001",
                projectId = "PROJ-ALPHA",
                status = VendorPortalAccountStatus.INVITED,
                portalCode = "PRINT-ALPHA"
            )
            val cRes = repository.createAccount(account)
            assertTrue(cRes is DomainResult.Success)

            val fRes = repository.getAccountById("pa_001", "TENANT-001")
            assertTrue(fRes is DomainResult.Success)
            assertEquals("PRINT-ALPHA", (fRes as DomainResult.Success).data?.portalCode)

            // Update with correct version
            val saved = (fRes as DomainResult.Success).data!!
            val updated = saved.copy(status = VendorPortalAccountStatus.ACTIVE, version = saved.version + 1)
            val uRes = repository.updateAccount(updated)
            assertTrue(uRes is DomainResult.Success)
            assertEquals(2L, (uRes as DomainResult.Success).data.version)

            // Update with stale version fails
            val stale = saved.copy(status = VendorPortalAccountStatus.SUSPENDED, version = 1L)
            val staleRes = repository.updateAccount(stale)
            assertTrue(staleRes is DomainResult.Error)
        }
    }

    @Test
    fun testSessionManagementAndRevocation() {
        runBlocking {
            val session = VendorPortalSession(
                sessionId = "sess_001",
                membershipId = "mem_001",
                userId = "usr_001",
                vendorId = "vnd_001",
                tenantId = "TENANT-001",
                projectId = "PROJ-ALPHA",
                sessionTokenHash = "HASH_ABC123",
                expiresAt = System.currentTimeMillis() + 3600000L
            )
            repository.createSession(session)

            val active = repository.getActiveSessionByToken("HASH_ABC123", "TENANT-001")
            assertTrue(active is DomainResult.Success)
            assertNotNull((active as DomainResult.Success).data)

            // Revoke
            repository.revokeSession("sess_001", "TENANT-001")
            val revoked = repository.getActiveSessionByToken("HASH_ABC123", "TENANT-001")
            assertTrue(revoked is DomainResult.Success)
            assertNull((revoked as DomainResult.Success).data) // Inactive session not returned
        }
    }
}
