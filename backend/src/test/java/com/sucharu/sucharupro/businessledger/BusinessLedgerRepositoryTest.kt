package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.data.datasource.businessledger.BusinessCostAllocationFilter
import com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessledger.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessLedgerRepositoryTest {

    private lateinit var dataSource: FakeBusinessLedgerDataSource
    private lateinit var repository: BusinessLedgerRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    @Before
    fun setup() {
        dataSource = FakeBusinessLedgerDataSource()
        repository = BusinessLedgerRepositoryImpl(dataSource)
    }

    @Test
    fun testCreateAndFindPosting() = runBlocking {
        val posting = BusinessLedgerPosting(
            id = "BLP-001",
            tenantId = tenantId,
            projectId = projectId,
            postingNumber = "POST-001",
            postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
            debitAmount = BigDecimal("5000.0000"),
            creditAmount = BigDecimal.ZERO,
            createdBy = "USER-1",
            description = "Paper Stock Expense"
        )

        val created = repository.createPosting(posting)
        assertEquals("BLP-001", created.id)

        val foundById = repository.findPostingById("BLP-001", tenantId, projectId)
        assertNotNull(foundById)
        assertEquals("POST-001", foundById?.postingNumber)

        val foundByNumber = repository.findPostingByNumber("POST-001", tenantId, projectId)
        assertNotNull(foundByNumber)

        val foundBySource = repository.findPostingsBySource(
            BusinessLedgerSourceType.BUSINESS_EXPENSE,
            "EXP-101",
            tenantId,
            projectId
        )
        assertEquals(1, foundBySource.size)
    }

    @Test
    fun testListPostingsWithFilters() = runBlocking {
        val p1 = BusinessLedgerPosting(
            id = "BLP-001",
            tenantId = tenantId,
            projectId = projectId,
            postingNumber = "POST-001",
            postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
            debitAmount = BigDecimal("1000.0000"),
            createdBy = "USER-1",
            description = "Expense 1",
            jobId = "JOB-001"
        )
        val p2 = BusinessLedgerPosting(
            id = "BLP-002",
            tenantId = tenantId,
            projectId = projectId,
            postingNumber = "POST-002",
            postingType = BusinessLedgerPostingType.VENDOR_LIABILITY_RECOGNITION,
            sourceType = BusinessLedgerSourceType.VENDOR_PAYABLE,
            sourceId = "PAY-201",
            accountCategory = BusinessLedgerAccountCategory.VENDOR_COST,
            debitAmount = BigDecimal("2000.0000"),
            createdBy = "USER-1",
            description = "Vendor 1",
            vendorId = "VEND-001"
        )

        repository.createPosting(p1)
        repository.createPosting(p2)

        val all = repository.listPostings(tenantId, projectId, BusinessLedgerPostingFilter())
        assertEquals(2, all.size)

        val filteredJob = repository.listPostings(tenantId, projectId, BusinessLedgerPostingFilter(jobId = "JOB-001"))
        assertEquals(1, filteredJob.size)
        assertEquals("BLP-001", filteredJob[0].id)

        val filteredVendor = repository.listPostings(tenantId, projectId, BusinessLedgerPostingFilter(vendorId = "VEND-001"))
        assertEquals(1, filteredVendor.size)
        assertEquals("BLP-002", filteredVendor[0].id)
    }

    @Test
    fun testCostAllocationPersistenceAndReversal() = runBlocking {
        val allocation = BusinessCostAllocation(
            id = "BCA-001",
            tenantId = tenantId,
            projectId = projectId,
            allocationNumber = "ALLOC-001",
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            jobId = "JOB-1025",
            costCategory = BusinessLedgerAccountCategory.PRODUCTION_COST,
            allocatedAmount = BigDecimal("4500.0000"),
            createdBy = "USER-1"
        )

        val created = repository.createCostAllocation(allocation)
        assertEquals("BCA-001", created.id)

        val list = repository.listCostAllocations(tenantId, projectId, BusinessCostAllocationFilter(jobId = "JOB-1025"))
        assertEquals(1, list.size)
        assertFalse(list[0].isReversed)

        val reversed = repository.markCostAllocationReversed("BCA-001", "Job re-allocated", "USER-MGR-1", System.currentTimeMillis())
        assertTrue(reversed)

        val afterRev = repository.findCostAllocationById("BCA-001", tenantId, projectId)
        assertTrue(afterRev?.isReversed == true)
        assertEquals("Job re-allocated", afterRev?.reversalReason)
    }

    @Test
    fun testAuditEventPersistence() = runBlocking {
        val event = BusinessLedgerAuditEvent(
            eventId = "EVT-001",
            tenantId = tenantId,
            projectId = projectId,
            eventType = "EXPENSE_RECOGNITION_POSTED",
            actorId = "USER-1",
            actorRole = "STAFF",
            postingId = "BLP-001",
            action = "POST_EXPENSE",
            amount = BigDecimal("5000.0000")
        )

        repository.recordAuditEvent(event)

        val audits = repository.listAuditEvents(tenantId, projectId, postingId = "BLP-001")
        assertEquals(1, audits.size)
        assertEquals("EXPENSE_RECOGNITION_POSTED", audits[0].eventType)
    }
}
