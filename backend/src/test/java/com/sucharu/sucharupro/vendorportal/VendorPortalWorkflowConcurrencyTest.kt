package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorPortalWorkflowDataSource
import com.sucharu.sucharupro.data.repository.VendorPortalWorkflowRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorPortalWorkflowConcurrencyTest {

    @Test
    fun testConcurrentWorkflowTimelineAppending() = runBlocking {
        val dataSource = FakeVendorPortalWorkflowDataSource()
        val repository = VendorPortalWorkflowRepositoryImpl(dataSource)

        val total = 50
        val deferredEvents = (1..total).map { i ->
            async(Dispatchers.Default) {
                repository.appendEvent(
                    VendorWorkflowTimelineEvent(
                        eventId = "EVT-CONCUR-$i",
                        workflowId = "WF-CONCUR-01",
                        tenantId = "TENANT-001",
                        projectId = "PRJ-001",
                        vendorId = "VND-001",
                        correlationId = "CORR-01",
                        stage = VendorWorkflowStage.PRODUCTION_IN_PROGRESS,
                        eventType = "PROGRESS_UPDATE",
                        title = "Progress Update #$i",
                        sourceModule = "WORKFLOW",
                        actorId = "vendor_user_$i"
                    )
                )
            }
        }

        val results = deferredEvents.awaitAll()
        assertEquals(total, results.filterIsInstance<DomainResult.Success<*>>().size)

        val listRes = repository.listEvents("TENANT-001", "PRJ-001", "VND-001", "WF-CONCUR-01")
        assertEquals(total, (listRes as DomainResult.Success).data.size)
    }
}
