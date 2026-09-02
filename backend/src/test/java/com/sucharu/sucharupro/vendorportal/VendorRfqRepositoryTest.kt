package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.data.datasource.FakeVendorRfqDataSource
import com.sucharu.sucharupro.data.repository.VendorRfqRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorportal.VendorRfq
import com.sucharu.sucharupro.domain.model.vendorportal.VendorRfqStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VendorRfqRepositoryTest {

    private lateinit var repository: VendorRfqRepositoryImpl

    @Before
    fun setup() {
        repository = VendorRfqRepositoryImpl(FakeVendorRfqDataSource())
    }

    @Test
    fun testCreatesAndRetrievesRfq() = runBlocking {
        val rfq = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-001",
            title = "Board Supply",
            requestedBy = "user-1",
            responseDeadline = System.currentTimeMillis() + 86400000L,
            createdBy = "user-1"
        )

        val createRes = repository.createRfq(rfq)
        assertTrue(createRes is DomainResult.Success)

        val findRes = repository.findRfqById("rfq-1", "tenant-1")
        assertTrue(findRes is DomainResult.Success)
        assertEquals("Board Supply", (findRes as DomainResult.Success).data.title)
    }

    @Test
    fun testListsRfqsWithStatusFilter() = runBlocking {
        val rfq1 = VendorRfq(
            rfqId = "rfq-1",
            tenantId = "tenant-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-001",
            title = "Board Supply",
            requestedBy = "user-1",
            status = VendorRfqStatus.OPEN,
            responseDeadline = System.currentTimeMillis() + 86400000L,
            createdBy = "user-1"
        )
        val rfq2 = VendorRfq(
            rfqId = "rfq-2",
            tenantId = "tenant-1",
            projectId = "proj-1",
            rfqNumber = "RFQ-002",
            title = "Ink Supply",
            requestedBy = "user-1",
            status = VendorRfqStatus.DRAFT,
            responseDeadline = System.currentTimeMillis() + 86400000L,
            createdBy = "user-1"
        )
        repository.createRfq(rfq1)
        repository.createRfq(rfq2)

        val openRfqs = (repository.listRfqs("proj-1", VendorRfqStatus.OPEN, "tenant-1") as DomainResult.Success).data
        assertEquals(1, openRfqs.size)
        assertEquals("rfq-1", openRfqs[0].rfqId)
    }
}
