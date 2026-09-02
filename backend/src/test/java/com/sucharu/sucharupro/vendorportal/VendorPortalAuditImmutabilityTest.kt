package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAuditEvent
import com.sucharu.sucharupro.domain.model.vendorportal.VendorPortalAuditEventType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalAuditImmutabilityTest {

    private lateinit var dataSource: FakeVendorPortalDataSource
    private lateinit var repository: VendorPortalRepositoryImpl

    @Before
    fun setUp() {
        dataSource = FakeVendorPortalDataSource()
        repository = VendorPortalRepositoryImpl(dataSource)
    }

    @Test
    fun testAppendAndListAuditEvents() {
        runBlocking {
            val event1 = VendorPortalAuditEvent(
                eventId = "evt_001",
                tenantId = "TENANT-001",
                projectId = "PROJ-ALPHA",
                vendorId = "vnd_001",
                actorUserId = "admin_001",
                eventType = VendorPortalAuditEventType.ACCOUNT_CREATED,
                action = "CREATE_ACCOUNT",
                details = "Portal account created"
            )
            repository.recordAuditEvent(event1)

            val event2 = VendorPortalAuditEvent(
                eventId = "evt_002",
                tenantId = "TENANT-001",
                projectId = "PROJ-ALPHA",
                vendorId = "vnd_001",
                actorUserId = "admin_001",
                eventType = VendorPortalAuditEventType.ACCOUNT_ACTIVATED,
                action = "ACTIVATE_ACCOUNT",
                details = "Portal account activated"
            )
            repository.recordAuditEvent(event2)

            val listRes = repository.listAuditEvents("vnd_001", null, "TENANT-001")
            assertTrue(listRes is DomainResult.Success)
            val list = (listRes as DomainResult.Success).data
            assertEquals(2, list.size)
        }
    }
}
