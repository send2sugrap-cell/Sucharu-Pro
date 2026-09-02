package com.sucharu.sucharupro.customercredit

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAllocationStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerRefundStatus
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.service.customercredit.CustomerCreditServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditServiceTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var service: CustomerCreditServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-CR-001"
    private val accountId = "CFA-CR-001"

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
        val creditDs = FakeCustomerCreditDataSource()
        creditRepo = CustomerCreditRepositoryImpl(creditDs)

        service = CustomerCreditServiceImpl(creditRepo, accountRepo, invoiceRepo, customerRepo, paymentRepo)

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-CR-01",
                    displayName = "Advance Customer",
                    primaryPhone = "01700000000",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
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
                    accountNumber = "CFA-CR-1001",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            val line = CustomerInvoiceLine(
                lineId = "LINE-1",
                invoiceId = "INV-CR-001",
                tenantId = tenantId,
                projectId = projectId,
                description = "Brochures",
                quantity = BigDecimal("100"),
                unitPrice = BigDecimal("10.0000"),
                lineTotal = BigDecimal("1000.0000")
            )

            invoice1 = CustomerInvoice(
                invoiceId = "INV-CR-001",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-2026-CR01",
                lines = listOf(line),
                subtotal = BigDecimal("1000.0000"),
                grandTotal = BigDecimal("1150.0000"),
                paidAmount = BigDecimal("0.0000"),
                dueAmount = BigDecimal("1150.0000"),
                status = CustomerInvoiceStatus.ISSUED,
                version = 1L
            )
            invoiceRepo.createInvoice(invoice1)
        }
    }

    @Test
    fun testRecordAdvance_Success() = runBlocking {
        val res = service.recordAdvance(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            amount = BigDecimal("5000.0000"),
            paymentMethod = CustomerPaymentMethod.BKASH,
            actorId = "staff1",
            actorRole = "STAFF"
        )
        assertTrue(res is DomainResult.Success)
        val advance = (res as DomainResult.Success).data
        assertEquals(CustomerAdvanceStatus.AVAILABLE, advance.status)
        assertEquals(BigDecimal("5000.0000"), advance.amount)
        assertEquals(BigDecimal("5000.0000"), advance.availableAmount)
    }

    @Test
    fun testAllocateAdvanceToInvoice_PartialAndFullAllocation() = runBlocking {
        // Record Advance of 2000
        val advRes = service.recordAdvance(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            amount = BigDecimal("2000.0000"),
            actorId = "staff1",
            actorRole = "STAFF"
        )
        val advance = (advRes as DomainResult.Success).data

        // Allocate 500 to Invoice (due 1150)
        val allocRes = service.allocateCreditToInvoice(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            invoiceId = invoice1.invoiceId,
            advanceId = advance.advanceId,
            amount = BigDecimal("500.0000"),
            actorId = "staff1",
            actorRole = "STAFF"
        )
        assertTrue(allocRes is DomainResult.Success)

        // Verify Invoice
        val invAfter1 = (invoiceRepo.getInvoiceById(tenantId, projectId, invoice1.invoiceId) as DomainResult.Success).data
        assertEquals(BigDecimal("500.0000"), invAfter1.paidAmount)
        assertEquals(BigDecimal("650.0000"), invAfter1.dueAmount)
        assertEquals(CustomerInvoiceStatus.PARTIALLY_PAID, invAfter1.status)

        // Allocate remaining due (650)
        val allocRes2 = service.allocateCreditToInvoice(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            invoiceId = invoice1.invoiceId,
            advanceId = advance.advanceId,
            amount = BigDecimal("650.0000"),
            actorId = "staff1",
            actorRole = "STAFF"
        )
        assertTrue(allocRes2 is DomainResult.Success)

        val invAfter2 = (invoiceRepo.getInvoiceById(tenantId, projectId, invoice1.invoiceId) as DomainResult.Success).data
        assertEquals(BigDecimal("1150.0000"), invAfter2.paidAmount)
        assertEquals(BigDecimal("0.0000"), invAfter2.dueAmount)
        assertEquals(CustomerInvoiceStatus.PAID, invAfter2.status)

        // Verify Advance: allocated 1150, available 850
        val advAfter = (creditRepo.getAdvanceById(tenantId, projectId, advance.advanceId) as DomainResult.Success).data
        assertEquals(BigDecimal("1150.0000"), advAfter.allocatedAmount)
        assertEquals(BigDecimal("850.0000"), advAfter.availableAmount)
        assertEquals(CustomerAdvanceStatus.ALLOCATED, advAfter.status)
    }

    @Test
    fun testReverseCreditAllocation_RestoresBalances() = runBlocking {
        // Record Advance of 1000
        val adv = (service.recordAdvance(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            amount = BigDecimal("1000.0000"),
            actorId = "staff1",
            actorRole = "STAFF"
        ) as DomainResult.Success).data

        // Allocate 500
        val alloc = (service.allocateCreditToInvoice(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            invoiceId = invoice1.invoiceId,
            advanceId = adv.advanceId,
            amount = BigDecimal("500.0000"),
            actorId = "staff1",
            actorRole = "STAFF"
        ) as DomainResult.Success).data

        // Reverse allocation
        val revRes = service.reverseCreditAllocation(
            tenantId = tenantId,
            projectId = projectId,
            allocationId = alloc.allocationId,
            reason = "Customer requested allocation reversal",
            actorId = "manager1",
            actorRole = "MANAGER",
            expectedVersion = alloc.version
        )
        assertTrue(revRes is DomainResult.Success)

        // Verify Invoice reverted to ISSUED with 0 paid
        val inv = (invoiceRepo.getInvoiceById(tenantId, projectId, invoice1.invoiceId) as DomainResult.Success).data
        assertEquals(BigDecimal("0.0000"), inv.paidAmount)
        assertEquals(BigDecimal("1150.0000"), inv.dueAmount)
        assertEquals(CustomerInvoiceStatus.ISSUED, inv.status)

        // Verify Advance restored to 1000 available
        val advRestored = (creditRepo.getAdvanceById(tenantId, projectId, adv.advanceId) as DomainResult.Success).data
        assertEquals(BigDecimal("0.0000"), advRestored.allocatedAmount)
        assertEquals(BigDecimal("1000.0000"), advRestored.availableAmount)
    }

    @Test
    fun testRefundLifecycle_FullFlow() = runBlocking {
        // Record advance of 3000
        val adv = (service.recordAdvance(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            amount = BigDecimal("3000.0000"),
            actorId = "staff1",
            actorRole = "STAFF"
        ) as DomainResult.Success).data

        // 1. Request Refund
        val reqRes = service.requestRefund(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            advanceId = adv.advanceId,
            amount = BigDecimal("1000.0000"),
            reason = "Customer cancelled future order",
            actorId = "staff1",
            actorRole = "STAFF"
        )
        assertTrue(reqRes is DomainResult.Success)
        val refund = (reqRes as DomainResult.Success).data
        assertEquals(CustomerRefundStatus.REQUESTED, refund.status)

        // 2. Approve Refund
        val appRes = service.approveRefund(
            tenantId, projectId, refund.refundId,
            "manager1", "MANAGER", refund.version
        )
        assertTrue(appRes is DomainResult.Success)
        val approved = (appRes as DomainResult.Success).data
        assertEquals(CustomerRefundStatus.APPROVED, approved.status)

        // 3. Process Refund
        val procRes = service.processRefund(
            tenantId, projectId, refund.refundId,
            "accounts1", "MANAGER", approved.version
        )
        assertTrue(procRes is DomainResult.Success)
        val processed = (procRes as DomainResult.Success).data
        assertEquals(CustomerRefundStatus.PROCESSED, processed.status)

        // 4. Complete Refund
        val compRes = service.completeRefund(
            tenantId, projectId, refund.refundId,
            "accounts1", "MANAGER", processed.version
        )
        assertTrue(compRes is DomainResult.Success)
        val completed = (compRes as DomainResult.Success).data
        assertEquals(CustomerRefundStatus.COMPLETED, completed.status)

        // Verify Advance available balance reduced by 1000 (from 3000 to 2000)
        val advAfter = (creditRepo.getAdvanceById(tenantId, projectId, adv.advanceId) as DomainResult.Success).data
        assertEquals(BigDecimal("2000.0000"), advAfter.availableAmount)
    }

    @Test
    fun testCustomerAdjustment_CreditAndDebit() = runBlocking {
        // Record Credit Adjustment
        val creditAdj = service.recordAdjustment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            adjustmentType = CustomerAdjustmentType.CREDIT,
            amount = BigDecimal("500.0000"),
            reason = "Goodwill credit adjustment",
            actorId = "manager1",
            actorRole = "MANAGER"
        )
        assertTrue(creditAdj is DomainResult.Success)

        // Summary should show 500 available credit
        val summary1 = (service.getCustomerCreditSummary(tenantId, projectId, customerId) as DomainResult.Success).data
        assertEquals(BigDecimal("500.0000"), summary1.totalAvailableCredit)

        // Record Debit Adjustment of 200
        val debitAdj = service.recordAdjustment(
            tenantId = tenantId,
            projectId = projectId,
            customerId = customerId,
            customerFinancialAccountId = accountId,
            adjustmentType = CustomerAdjustmentType.DEBIT,
            amount = BigDecimal("200.0000"),
            reason = "Fee recovery",
            actorId = "manager1",
            actorRole = "MANAGER"
        )
        assertTrue(debitAdj is DomainResult.Success)

        // Summary should now show 300 available credit
        val summary2 = (service.getCustomerCreditSummary(tenantId, projectId, customerId) as DomainResult.Success).data
        assertEquals(BigDecimal("300.0000"), summary2.totalAvailableCredit)
    }
}
