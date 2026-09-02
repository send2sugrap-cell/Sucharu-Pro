package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalWorkflowDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalWorkflowRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorPortalWorkflowRepositoryTest {

    private lateinit var dataSource: FakeVendorPortalWorkflowDataSource
    private lateinit var repository: VendorPortalWorkflowRepositoryImpl

    @Before
    fun setup() {
        dataSource = FakeVendorPortalWorkflowDataSource()
        repository = VendorPortalWorkflowRepositoryImpl(dataSource)
    }

    @Test
    fun testWorkflowCRUD() = runBlocking {
        val wf = VendorWorkflowItem(
            workflowId = "WF-REP-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            correlationId = "CORR-01",
            workflowTitle = "Test Commercial Cycle",
            currentStage = VendorWorkflowStage.AWARDED,
            status = VendorWorkflowStatus.ACTIVE
        )

        val saveRes = repository.saveWorkflow(wf)
        assertTrue(saveRes is DomainResult.Success)

        val fetchedRes = repository.findWorkflowById("TENANT-001", "PRJ-001", "VND-001", "WF-REP-01")
        assertEquals("Test Commercial Cycle", (fetchedRes as DomainResult.Success).data?.workflowTitle)

        val updateRes = repository.updateWorkflow(wf.copy(status = VendorWorkflowStatus.COMPLETED))
        assertEquals(VendorWorkflowStatus.COMPLETED, (updateRes as DomainResult.Success).data.status)
        assertEquals(2L, updateRes.data.version)
    }

    @Test
    fun testTimelineEventsAndAudits() = runBlocking {
        val event = VendorWorkflowTimelineEvent(
            eventId = "EVT-01",
            workflowId = "WF-REP-01",
            tenantId = "TENANT-001",
            projectId = "PRJ-001",
            vendorId = "VND-001",
            correlationId = "CORR-01",
            stage = VendorWorkflowStage.PO_ACKNOWLEDGED,
            eventType = "PO_ACKNOWLEDGED",
            title = "PO #PO-101 Acknowledged",
            sourceModule = "PURCHASE_ORDER",
            actorId = "vendor_01"
        )
        val appendRes = repository.appendEvent(event)
        assertTrue(appendRes is DomainResult.Success)

        val listRes = repository.listEvents("TENANT-001", "PRJ-001", "VND-001", "WF-REP-01")
        assertEquals(1, (listRes as DomainResult.Success).data.size)
    }
}
