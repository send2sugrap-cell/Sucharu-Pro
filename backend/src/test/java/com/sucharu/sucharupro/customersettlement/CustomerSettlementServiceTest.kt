package com.sucharu.sucharupro.customersettlement

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.customersettlement.FakeCustomerPaymentAllocationDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.customersettlement.CustomerPaymentAllocationRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.model.customersettlement.CustomerPaymentAllocationStatus
import com.sucharu.sucharupro.domain.model.customersettlement.InvoiceAllocationRequestItem
import com.sucharu.sucharupro.domain.service.customersettlement.CustomerSettlementServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerSettlementServiceTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var allocationRepo: CustomerPaymentAllocationRepositoryImpl
    private lateinit var service: CustomerSettlementServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-SET-001"
    private val accountId = "CFA-SET-001"

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
        val allocationDs = FakeCustomerPaymentAllocationDataSource()
        allocationRepo = CustomerPaymentAllocationRepositoryImpl(allocationDs)

        service = CustomerSettlementServiceImpl(
            allocationRepo, paymentRepo, invoiceRepo, accountRepo, creditRepo
        )

        runBlocking {
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-S-01",
                    displayName = "Settlement Test Customer",
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
                    accountNumber = "CFA-S-1001",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testSingleInvoicePaymentAllocation() = runBlocking {
        // Invoice 1: 10,000 due
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-01",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-1001",
                grandTotal = BigDecimal("10000.0000"),
                dueAmount = BigDecimal("10000.0000"),
                status = CustomerInvoiceStatus.ISSUED
            )
        )

        // Confirmed Payment: 6,000
        paymentRepo.createPayment(
            CustomerPayment(
                paymentId = "PAY-01",
                tenantId = tenantId,
                projectId = projectId,
                paymentNumber = "PAY-1001",
                customerId = customerId,
                customerFinancialAccountId = accountId,
                amount = BigDecimal("6000.0000"),
                status = CustomerPaymentStatus.CONFIRMED
            )
        )

        // Allocate 6,000 from PAY-01 to INV-01
        val allocRes = service.allocatePayment(
            tenantId = tenantId,
            projectId = projectId,
            paymentId = "PAY-01",
            invoiceId = "INV-01",
            amount = BigDecimal("6000.0000")
        )
        assertTrue(allocRes is DomainResult.Success)

        // Verify Invoice state
        val updatedInvRes = invoiceRepo.getInvoiceById(tenantId, projectId, "INV-01")
        val updatedInv = (updatedInvRes as DomainResult.Success).data
        assertEquals(BigDecimal("6000.0000"), updatedInv.paidAmount)
        assertEquals(BigDecimal("4000.0000"), updatedInv.dueAmount)
        assertEquals(CustomerInvoiceStatus.PARTIALLY_PAID, updatedInv.status)

        // Verify Unallocated Payment
        val unallocatedRes = service.getUnallocatedPayments(tenantId, projectId, customerId)
        assertTrue(unallocatedRes is DomainResult.Success)
        val unallocatedList = (unallocatedRes as DomainResult.Success).data
        assertTrue(unallocatedList.isEmpty()) // Fully allocated!
    }

    @Test
    fun testMultiInvoicePaymentAllocation() = runBlocking {
        // Invoice 1: 5,000 due
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-M1",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-M1",
                grandTotal = BigDecimal("5000.0000"),
                dueAmount = BigDecimal("5000.0000"),
                status = CustomerInvoiceStatus.ISSUED
            )
        )

        // Invoice 2: 3,000 due
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-M2",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-M2",
                grandTotal = BigDecimal("3000.0000"),
                dueAmount = BigDecimal("3000.0000"),
                status = CustomerInvoiceStatus.ISSUED
            )
        )

        // Payment: 10,000
        paymentRepo.createPayment(
            CustomerPayment(
                paymentId = "PAY-M1",
                tenantId = tenantId,
                projectId = projectId,
                paymentNumber = "PAY-M1",
                customerId = customerId,
                customerFinancialAccountId = accountId,
                amount = BigDecimal("10000.0000"),
                status = CustomerPaymentStatus.CONFIRMED
            )
        )

        // Multi Allocate: 5,000 to INV-M1, 3,000 to INV-M2
        val multiRes = service.allocatePaymentMulti(
            tenantId = tenantId,
            projectId = projectId,
            paymentId = "PAY-M1",
            allocations = listOf(
                InvoiceAllocationRequestItem("INV-M1", BigDecimal("5000.0000")),
                InvoiceAllocationRequestItem("INV-M2", BigDecimal("3000.0000"))
            )
        )
        assertTrue(multiRes is DomainResult.Success)
        val result = (multiRes as DomainResult.Success).data
        assertEquals(BigDecimal("8000.0000"), result.totalAllocated)
        assertEquals(BigDecimal("2000.0000"), result.remainingUnallocated)

        // Verify Invoices are PAID
        val inv1 = (invoiceRepo.getInvoiceById(tenantId, projectId, "INV-M1") as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.PAID, inv1.status)
        assertEquals(BigDecimal("0.0000"), inv1.dueAmount)

        val inv2 = (invoiceRepo.getInvoiceById(tenantId, projectId, "INV-M2") as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.PAID, inv2.status)
        assertEquals(BigDecimal("0.0000"), inv2.dueAmount)

        // Check Unallocated Payment: 2,000 remaining
        val unalloc = (service.getUnallocatedPayments(tenantId, projectId, customerId) as DomainResult.Success).data
        assertEquals(1, unalloc.size)
        assertEquals(BigDecimal("2000.0000"), unalloc[0].unallocatedAmount)
    }

    @Test
    fun testAllocationReversalRestoresInvoiceBalance() = runBlocking {
        // Invoice 10,000
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-REV",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-REV",
                grandTotal = BigDecimal("10000.0000"),
                dueAmount = BigDecimal("10000.0000"),
                status = CustomerInvoiceStatus.ISSUED
            )
        )

        // Payment 10,000
        paymentRepo.createPayment(
            CustomerPayment(
                paymentId = "PAY-REV",
                tenantId = tenantId,
                projectId = projectId,
                paymentNumber = "PAY-REV",
                customerId = customerId,
                customerFinancialAccountId = accountId,
                amount = BigDecimal("10000.0000"),
                status = CustomerPaymentStatus.CONFIRMED
            )
        )

        // Allocate 10,000
        val allocRes = service.allocatePayment(
            tenantId = tenantId,
            projectId = projectId,
            paymentId = "PAY-REV",
            invoiceId = "INV-REV",
            amount = BigDecimal("10000.0000")
        )
        val allocation = (allocRes as DomainResult.Success).data

        val paidInv = (invoiceRepo.getInvoiceById(tenantId, projectId, "INV-REV") as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.PAID, paidInv.status)

        // Reverse allocation
        val revRes = service.reverseAllocation(
            tenantId = tenantId,
            projectId = projectId,
            allocationId = allocation.allocationId,
            reason = "Payment cheque bounced",
            expectedVersion = 1L
        )
        assertTrue(revRes is DomainResult.Success)

        // Check Invoice is restored to ISSUED with 10,000 due
        val restoredInv = (invoiceRepo.getInvoiceById(tenantId, projectId, "INV-REV") as DomainResult.Success).data
        assertEquals(CustomerInvoiceStatus.ISSUED, restoredInv.status)
        assertEquals(BigDecimal("10000.0000"), restoredInv.dueAmount)
        assertEquals(BigDecimal("0.0000"), restoredInv.paidAmount)
    }
}
