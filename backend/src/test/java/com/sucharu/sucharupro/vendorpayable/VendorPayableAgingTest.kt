package com.sucharu.sucharupro.vendorpayable

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.*
import com.sucharu.sucharupro.domain.service.vendorpayable.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class VendorPayableAgingTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepositoryImpl
    private lateinit var service: VendorPayableServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VEND-AGING-1"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "USER-ADMIN",
        projectId = projectId,
        username = "admin",
        role = UserRole.ADMIN
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USER-MGR",
        projectId = projectId,
        username = "manager",
        role = UserRole.MANAGER
    )

    @Before
    fun setup() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
        service = VendorPayableServiceImpl(repository, tenantId)
    }

    @Test
    fun testAllAgingBucketsCalculatedCorrectly() = runBlocking {
        val now = 1761868800000L // Reference anchor
        val oneDayMillis = 24L * 60L * 60L * 1000L

        // Helper to create and approve payable with specific due date
        suspend fun createPayableWithDueDate(name: String, dueDate: Long, amount: BigDecimal) {
            val res = service.createPayable(
                adminPrincipal,
                CreateVendorPayableCommand(
                    vendorId = vendorId,
                    originalAmount = amount,
                    description = name,
                    autoSubmit = true
                )
            )
            val p = (res as DomainResult.Success).data
            repository.updatePayable(p.copy(dueDate = dueDate))
            service.approvePayable(managerPrincipal, p.payableId, "Approved")
        }

        // 1. Current (Due 5 days in future)
        createPayableWithDueDate("Current Bill", now + (5L * oneDayMillis), BigDecimal("1000.00"))

        // 2. 1-7 Days Overdue (Due 3 days ago)
        createPayableWithDueDate("1-7 Days Overdue", now - (3L * oneDayMillis), BigDecimal("2000.00"))

        // 3. 8-30 Days Overdue (Due 15 days ago)
        createPayableWithDueDate("8-30 Days Overdue", now - (15L * oneDayMillis), BigDecimal("3000.00"))

        // 4. 31-60 Days Overdue (Due 45 days ago)
        createPayableWithDueDate("31-60 Days Overdue", now - (45L * oneDayMillis), BigDecimal("4000.00"))

        // 5. 61-90 Days Overdue (Due 75 days ago)
        createPayableWithDueDate("61-90 Days Overdue", now - (75L * oneDayMillis), BigDecimal("5000.00"))

        // 6. 90+ Days Overdue (Due 120 days ago)
        createPayableWithDueDate("90+ Days Overdue", now - (120L * oneDayMillis), BigDecimal("6000.00"))

        val agingRes = service.getVendorPayableAging(adminPrincipal, vendorId, asOfDate = now)
        assertTrue(agingRes is DomainResult.Success)
        val report = (agingRes as DomainResult.Success).data

        assertEquals(BigDecimal("21000.0000"), report.totalOutstanding)

        val bucketMap = report.buckets.associateBy { it.bucket }
        assertEquals(BigDecimal("1000.0000"), bucketMap[VendorPayableAgingBucket.CURRENT]?.outstandingAmount)
        assertEquals(BigDecimal("2000.0000"), bucketMap[VendorPayableAgingBucket.DAYS_1_7]?.outstandingAmount)
        assertEquals(BigDecimal("3000.0000"), bucketMap[VendorPayableAgingBucket.DAYS_8_30]?.outstandingAmount)
        assertEquals(BigDecimal("4000.0000"), bucketMap[VendorPayableAgingBucket.DAYS_31_60]?.outstandingAmount)
        assertEquals(BigDecimal("5000.0000"), bucketMap[VendorPayableAgingBucket.DAYS_61_90]?.outstandingAmount)
        assertEquals(BigDecimal("6000.0000"), bucketMap[VendorPayableAgingBucket.DAYS_90_PLUS]?.outstandingAmount)
    }
}
