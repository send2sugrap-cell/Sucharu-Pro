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

class VendorPayablePaymentAllocationTest {

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
    fun testPartialAndFullPaymentSettlement() = runBlocking {
        // Create & Approve Payable of 10,000.00
        val createRes = service.createPayable(
            staffPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("10000.00"),
                description = "Offset Press Chemical Supplies",
                autoSubmit = true
            )
        )
        val payable = (createRes as DomainResult.Success).data
        service.approvePayable(managerPrincipal, payable.payableId, "Approved")

        // 1. Payment 1: 4,000.00 -> PARTIALLY_PAID, Outstanding = 6,000.00
        val pay1Res = service.allocatePayment(
            managerPrincipal,
            payable.payableId,
            AllocateVendorPayablePaymentCommand(
                amount = BigDecimal("4000.00"),
                paymentMethod = VendorPayablePaymentMethod.BANK,
                paymentReference = "CHQ-88001"
            )
        )
        assertTrue(pay1Res is DomainResult.Success)
        val pay1 = (pay1Res as DomainResult.Success).data
        assertEquals(VendorPayableStatus.PARTIALLY_PAID, pay1.status)
        assertEquals(BigDecimal("4000.0000"), pay1.paidAmount)
        assertEquals(BigDecimal("6000.0000"), pay1.outstandingAmount)

        // 2. Payment 2: 6,000.00 -> PAID, Outstanding = 0.00
        val pay2Res = service.allocatePayment(
            managerPrincipal,
            payable.payableId,
            AllocateVendorPayablePaymentCommand(
                amount = BigDecimal("6000.00"),
                paymentMethod = VendorPayablePaymentMethod.BANK,
                paymentReference = "CHQ-88002"
            )
        )
        assertTrue(pay2Res is DomainResult.Success)
        val pay2 = (pay2Res as DomainResult.Success).data
        assertEquals(VendorPayableStatus.PAID, pay2.status)
        assertEquals(BigDecimal("10000.0000"), pay2.paidAmount)
        assertEquals(BigDecimal("0.0000"), pay2.outstandingAmount)

        // Verify Allocations List
        val allocsRes = service.getPayablePaymentAllocations(managerPrincipal, payable.payableId)
        assertTrue(allocsRes is DomainResult.Success)
        assertEquals(2, (allocsRes as DomainResult.Success).data.size)
    }

    @Test
    fun testOverAllocationIsStrictlyPrevented() = runBlocking {
        // Create & Approve Payable of 5,000.00
        val createRes = service.createPayable(
            staffPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = BigDecimal("5000.00"),
                description = "Lamination Rolls",
                autoSubmit = true
            )
        )
        val payable = (createRes as DomainResult.Success).data
        service.approvePayable(managerPrincipal, payable.payableId, "Approved")

        // Attempt to allocate 6,000.00 (exceeds 5,000.00)
        val overAllocRes = service.allocatePayment(
            managerPrincipal,
            payable.payableId,
            AllocateVendorPayablePaymentCommand(
                amount = BigDecimal("6000.00"),
                paymentMethod = VendorPayablePaymentMethod.BANK,
                paymentReference = "CHQ-OVER"
            )
        )
        assertTrue(overAllocRes is DomainResult.Error)
        assertTrue((overAllocRes as DomainResult.Error).message.contains("exceeds outstanding payable liability"))
    }
}
