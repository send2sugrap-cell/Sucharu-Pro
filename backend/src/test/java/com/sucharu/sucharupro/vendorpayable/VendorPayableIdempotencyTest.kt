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

class VendorPayableIdempotencyTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepositoryImpl
    private lateinit var service: VendorPayableServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VEND-1001"

    private val staffPrincipal = AuthenticatedPrincipal(
        userId = "USER-STAFF-1",
        projectId = projectId,
        username = "staff1",
        role = UserRole.STAFF
    )

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USER-MGR-1",
        projectId = projectId,
        username = "manager1",
        role = UserRole.MANAGER
    )

    @Before
    fun setup() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
        service = VendorPayableServiceImpl(repository, tenantId)
    }

    @Test
    fun testIdempotentPayableCreationReturnsSameRecord() = runBlocking {
        val idemKey = "IDEM-PAY-KEY-101"
        val cmd = CreateVendorPayableCommand(
            vendorId = vendorId,
            originalAmount = BigDecimal("3500.00"),
            description = "Specialty Paper Batch",
            idempotencyKey = idemKey
        )

        val firstRes = service.createPayable(staffPrincipal, cmd)
        assertTrue(firstRes is DomainResult.Success)
        val firstPayable = (firstRes as DomainResult.Success).data

        val secondRes = service.createPayable(staffPrincipal, cmd)
        assertTrue(secondRes is DomainResult.Success)
        val secondPayable = (secondRes as DomainResult.Success).data

        assertEquals(firstPayable.payableId, secondPayable.payableId)
        assertEquals(firstPayable.payableNumber, secondPayable.payableNumber)

        val countRes = service.countPayables(staffPrincipal)
        assertTrue(countRes is DomainResult.Success)
        assertEquals(1L, (countRes as DomainResult.Success).data)
    }

    @Test
    fun testIdempotentPaymentAllocationReturnsSameRecord() = runBlocking {
        val createRes = service.createPayable(
            staffPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("6000.00"),
                description = "Printing Plates",
                autoSubmit = true
            )
        )
        val payable = (createRes as DomainResult.Success).data
        service.approvePayable(managerPrincipal, payable.payableId, "Approved")

        val allocCmd = AllocateVendorPayablePaymentCommand(
            amount = BigDecimal("2000.00"),
            paymentMethod = VendorPayablePaymentMethod.BANK,
            paymentReference = "TRX-IDEM-01",
            idempotencyKey = "IDEM-ALLOC-KEY-202"
        )

        val firstAlloc = service.allocatePayment(managerPrincipal, payable.payableId, allocCmd)
        assertTrue(firstAlloc is DomainResult.Success)

        val secondAlloc = service.allocatePayment(managerPrincipal, payable.payableId, allocCmd)
        assertTrue(secondAlloc is DomainResult.Success)

        val allocsRes = service.getPayablePaymentAllocations(managerPrincipal, payable.payableId)
        assertTrue(allocsRes is DomainResult.Success)
        // Should only have 1 recorded allocation due to idempotency deduplication
        assertEquals(1, (allocsRes as DomainResult.Success).data.size)
    }
}
