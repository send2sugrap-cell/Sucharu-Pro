package com.sucharu.sucharupro.customerpayment

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.service.customerpayment.CustomerPaymentServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * MODULE 14 STEP 03: Customer Payment Service & Invoice Receivable Settlement Tests.
 */
class CustomerPaymentServiceTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var service: CustomerPaymentServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-PAY-001"
    private val accountId = "CFA-PAY-001"

    private lateinit var invoice1: CustomerInvoice

    @Before
    fun setup() {
        val customerDs = FakeCustomerDataSource()
        customerRepo = CustomerRepositoryImpl(customerDs)
        val accountDs = FakeCustomerFinancialAccountDataSource()
        accountRepo = CustomerFinancialAccountRepositoryImpl(accountDs)
        val invoiceDs = FakeCustomerInvoiceDataSource()
        invoiceRepo = CustomerInvoiceRepositoryImpl(invoiceDs)
        val paymentDs = FakeCustomerPaymentDataSource()
        paymentRepo = CustomerPaymentRepositoryImpl(paymentDs)

        service = CustomerPaymentServiceImpl(paymentRepo, invoiceRepo, customerRepo, accountRepo)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-P-01",
                    displayName = "Payment Test Client",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    primaryPhone = "+8801711111111",
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )

            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountId,
                    tenantId = tenantId,
                    projectId = projectId,
                    customerId = customerId,
                    accountNumber = "ACC-P-01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            val line = CustomerInvoiceLine(
                lineId = "L1", invoiceId = "INV-P-01", tenantId = tenantId, projectId = projectId,
                description = "Brochures", quantity = BigDecimal("100"), unitPrice = BigDecimal("100"),
                lineTotal = BigDecimal("10000.0000")
            )
            invoice1 = CustomerInvoice(
                invoiceId = "INV-P-01",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-P-1001",
                subtotal = BigDecimal("10000.0000"),
                grandTotal = BigDecimal("10000.0000"),
                paidAmount = BigDecimal.ZERO,
                dueAmount = BigDecimal("10000.0000"),
                status = CustomerInvoiceStatus.ISSUED,
                lines = listOf(line),
                version = 1L
            )
            invoiceRepo.createInvoice(invoice1)
        }
    }

    @Test
    fun testPartialPaymentThenFullSettlement() = runBlocking {
        // 1. Partial Payment of 4,000
        val p1Res = service.recordPayment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceId = invoice1.invoiceId,
            amount = BigDecimal("4000.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BKASH,
            referenceNumber = "TRX-P1",
            actorId = "cashier",
            actorRole = "STAFF"
        )
        assertTrue(p1Res is DomainResult.Success)
        val p1 = (p1Res as DomainResult.Success).data
        assertEquals(CustomerPaymentStatus.RECORDED, p1.status)
        assertEquals(0, BigDecimal("4000.0000").compareTo(p1.amount))

        // Verify invoice balance: Paid = 4,000, Due = 6,000, Status = PARTIALLY_PAID
        val invAfterP1 = (invoiceRepo.getInvoiceById(tenantId, projectId, invoice1.invoiceId) as DomainResult.Success).data
        assertEquals(0, BigDecimal("4000.0000").compareTo(invAfterP1.paidAmount))
        assertEquals(0, BigDecimal("6000.0000").compareTo(invAfterP1.dueAmount))
        assertEquals(CustomerInvoiceStatus.PARTIALLY_PAID, invAfterP1.status)

        // 2. Final Payment of 6,000
        val p2Res = service.recordPayment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceId = invoice1.invoiceId,
            amount = BigDecimal("6000.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK,
            referenceNumber = "CHQ-7788",
            actorId = "cashier",
            actorRole = "STAFF"
        )
        assertTrue(p2Res is DomainResult.Success)

        // Verify invoice balance: Paid = 10,000, Due = 0, Status = PAID
        val invAfterP2 = (invoiceRepo.getInvoiceById(tenantId, projectId, invoice1.invoiceId) as DomainResult.Success).data
        assertEquals(0, BigDecimal("10000.0000").compareTo(invAfterP2.paidAmount))
        assertEquals(0, BigDecimal.ZERO.compareTo(invAfterP2.dueAmount))
        assertEquals(CustomerInvoiceStatus.PAID, invAfterP2.status)
    }

    @Test
    fun testAdvancePaymentWithoutInvoice() = runBlocking {
        val advRes = service.recordPayment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceId = null, // Advance payment
            amount = BigDecimal("5000.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.CASH,
            actorId = "cashier",
            actorRole = "STAFF"
        )
        assertTrue(advRes is DomainResult.Success)
        val adv = (advRes as DomainResult.Success).data
        assertNull(adv.invoiceId)
        assertEquals(CustomerPaymentStatus.RECORDED, adv.status)
    }

    @Test
    fun testPaymentCancellationReversesInvoiceBalance() = runBlocking {
        // Record payment of 3,000
        val pay = (service.recordPayment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            invoiceId = invoice1.invoiceId,
            amount = BigDecimal("3000.00"),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.NAGAD,
            referenceNumber = "TXN-CANCEL-ME",
            actorId = "cashier",
            actorRole = "STAFF"
        ) as DomainResult.Success).data

        // Cancel payment with reason
        val cancelRes = service.cancelPayment(
            tenantId = tenantId,
            projectId = projectId,
            paymentId = pay.paymentId,
            reason = "Customer cancelled transaction due to reversal",
            actorId = "manager_1",
            actorRole = "MANAGER",
            expectedVersion = 1L
        )
        assertTrue(cancelRes is DomainResult.Success)
        val cancelled = (cancelRes as DomainResult.Success).data
        assertEquals(CustomerPaymentStatus.CANCELLED, cancelled.status)

        // Verify invoice balance is restored: Paid = 0, Due = 10,000, Status = ISSUED
        val inv = (invoiceRepo.getInvoiceById(tenantId, projectId, invoice1.invoiceId) as DomainResult.Success).data
        assertEquals(0, BigDecimal.ZERO.compareTo(inv.paidAmount))
        assertEquals(0, BigDecimal("10000.0000").compareTo(inv.dueAmount))
        assertEquals(CustomerInvoiceStatus.ISSUED, inv.status)
    }
}
