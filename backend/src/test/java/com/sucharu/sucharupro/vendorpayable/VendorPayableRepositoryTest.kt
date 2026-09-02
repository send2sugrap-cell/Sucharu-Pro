package com.sucharu.sucharupro.vendorpayable

import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableRepositoryTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VEND-1001"

    @Before
    fun setup() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
    }

    @Test
    fun testCreateAndRetrievePayable() = runBlocking {
        val payable = VendorPayable(
            payableId = "PAY-001",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "PAYABLE-20261016-0001",
            vendorId = vendorId,
            description = "Subcontracted CTP Output",
            originalAmount = BigDecimal("4500.00"),
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            createdBy = "USER-01"
        )

        val createRes = repository.createPayable(payable)
        assertTrue(createRes is DomainResult.Success)

        val getRes = repository.getPayableById(tenantId, projectId, "PAY-001")
        assertTrue(getRes is DomainResult.Success)
        val retrieved = (getRes as DomainResult.Success).data
        assertNotNull(retrieved)
        assertEquals("PAYABLE-20261016-0001", retrieved?.payableNumber)
        assertEquals(BigDecimal("4500.00"), retrieved?.originalAmount)
    }

    @Test
    fun testUpdatePayable() = runBlocking {
        val payable = VendorPayable(
            payableId = "PAY-002",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "PAYABLE-20261016-0002",
            vendorId = vendorId,
            description = "Initial Description",
            originalAmount = BigDecimal("1000.00"),
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            createdBy = "USER-01"
        )
        repository.createPayable(payable)

        val updated = payable.copy(
            description = "Updated Description",
            originalAmount = BigDecimal("1200.00"),
            status = VendorPayableStatus.APPROVED
        )
        val updateRes = repository.updatePayable(updated)
        assertTrue(updateRes is DomainResult.Success)

        val getRes = repository.getPayableById(tenantId, projectId, "PAY-002")
        val retrieved = (getRes as DomainResult.Success).data
        assertEquals("Updated Description", retrieved?.description)
        assertEquals(BigDecimal("1200.00"), retrieved?.originalAmount)
        assertEquals(VendorPayableStatus.APPROVED, retrieved?.status)
    }

    @Test
    fun testListAndCountWithFilters() = runBlocking {
        val p1 = VendorPayable(
            payableId = "PAY-A",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "PAYABLE-A",
            vendorId = "VEND-A",
            jobId = "JOB-101",
            description = "Lamination Job 101",
            originalAmount = BigDecimal("800.00"),
            issueDate = 1000L,
            dueDate = 2000L,
            status = VendorPayableStatus.APPROVED,
            createdBy = "USER-01"
        )
        val p2 = VendorPayable(
            payableId = "PAY-B",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "PAYABLE-B",
            vendorId = "VEND-B",
            jobId = "JOB-102",
            description = "Binding Job 102",
            originalAmount = BigDecimal("1500.00"),
            issueDate = 3000L,
            dueDate = 4000L,
            status = VendorPayableStatus.DRAFT,
            createdBy = "USER-01"
        )
        repository.createPayable(p1)
        repository.createPayable(p2)

        // Filter by vendor
        val vendAList = repository.listPayables(tenantId, projectId, vendorId = "VEND-A")
        assertTrue(vendAList is DomainResult.Success)
        assertEquals(1, (vendAList as DomainResult.Success).data.size)
        assertEquals("PAY-A", vendAList.data[0].payableId)

        // Filter by status
        val draftList = repository.listPayables(tenantId, projectId, status = VendorPayableStatus.DRAFT)
        assertTrue(draftList is DomainResult.Success)
        assertEquals(1, (draftList as DomainResult.Success).data.size)
        assertEquals("PAY-B", draftList.data[0].payableId)

        // Count total
        val countTotal = repository.countPayables(tenantId, projectId)
        assertTrue(countTotal is DomainResult.Success)
        assertEquals(2L, (countTotal as DomainResult.Success).data)
    }

    @Test
    fun testPaymentAllocationAndAuditTrailPersistence() = runBlocking {
        val payable = VendorPayable(
            payableId = "PAY-003",
            tenantId = tenantId,
            projectId = projectId,
            payableNumber = "PAYABLE-20261016-0003",
            vendorId = vendorId,
            description = "Foil Stamping Services",
            originalAmount = BigDecimal("6000.00"),
            issueDate = System.currentTimeMillis(),
            dueDate = System.currentTimeMillis() + 86400000L,
            status = VendorPayableStatus.APPROVED,
            createdBy = "USER-01"
        )
        repository.createPayable(payable)

        val allocation = VendorPayablePaymentAllocation(
            allocationId = "ALLOC-001",
            tenantId = tenantId,
            projectId = projectId,
            payableId = "PAY-003",
            vendorId = vendorId,
            amount = BigDecimal("2500.00"),
            paymentMethod = VendorPayablePaymentMethod.BANK,
            paymentReference = "TRX-99881",
            paymentDate = System.currentTimeMillis(),
            allocatedBy = "USER-MANAGER"
        )
        val allocRes = repository.recordPaymentAllocation(allocation)
        assertTrue(allocRes is DomainResult.Success)

        val allocsRes = repository.getPaymentAllocations(tenantId, projectId, "PAY-003")
        assertTrue(allocsRes is DomainResult.Success)
        val allocList = (allocsRes as DomainResult.Success).data
        assertEquals(1, allocList.size)
        assertEquals(BigDecimal("2500.00"), allocList[0].amount)

        // Audit Event
        val auditEvent = VendorPayableAuditEvent(
            eventId = "EVT-001",
            tenantId = tenantId,
            projectId = projectId,
            payableId = "PAY-003",
            vendorId = vendorId,
            eventType = "PAYMENT_ALLOCATED",
            actorId = "USER-MANAGER",
            actorRole = "MANAGER",
            newStatus = VendorPayableStatus.PARTIALLY_PAID,
            amount = BigDecimal("2500.00")
        )
        val auditRes = repository.recordAuditEvent(auditEvent)
        assertTrue(auditRes is DomainResult.Success)

        val auditsRes = repository.getAuditEvents(tenantId, projectId, "PAY-003")
        assertTrue(auditsRes is DomainResult.Success)
        assertEquals(1, (auditsRes as DomainResult.Success).data.size)
    }
}
