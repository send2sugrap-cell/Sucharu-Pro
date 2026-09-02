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

class VendorPayablePrecisionTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepositoryImpl
    private lateinit var service: VendorPayableServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val vendorId = "VEND-1001"

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
    fun testFourDecimalPrecisionPreservedExactly() = runBlocking {
        val exactAmount = BigDecimal("98765.4321")
        val createRes = service.createPayable(
            adminPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = exactAmount,
                description = "Imported Specialized Paper Rolls",
                autoSubmit = true
            )
        )
        assertTrue(createRes is DomainResult.Success)
        val payable = (createRes as DomainResult.Success).data
        assertEquals("98765.4321", payable.originalAmount.toPlainString())

        service.approvePayable(managerPrincipal, payable.payableId, "Approved")

        val allocRes = service.allocatePayment(
            managerPrincipal,
            payable.payableId,
            AllocateVendorPayablePaymentCommand(
                amount = BigDecimal("12345.1111"),
                paymentMethod = VendorPayablePaymentMethod.BANK,
                paymentReference = "TRX-PREC-01"
            )
        )
        assertTrue(allocRes is DomainResult.Success)
        val updated = (allocRes as DomainResult.Success).data

        assertEquals("12345.1111", updated.paidAmount.toPlainString())
        assertEquals("86420.3210", updated.outstandingAmount.toPlainString())
    }

    @Test
    fun testLargeFinancialAmountSupported() = runBlocking {
        val largeAmount = BigDecimal("12345678901234.5678")
        val createRes = service.createPayable(
            adminPrincipal,
            CreateVendorPayableCommand(
                vendorId = vendorId,
                originalAmount = largeAmount,
                description = "Industrial Heidelberg Printing Press Line",
                autoSubmit = true
            )
        )
        assertTrue(createRes is DomainResult.Success)
        val payable = (createRes as DomainResult.Success).data
        assertEquals(largeAmount, payable.originalAmount)
    }
}
