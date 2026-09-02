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
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.service.customercredit.CustomerCreditServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerCreditIsolationTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var service: CustomerCreditServiceImpl

    private val tenantA = "TENANT-A"
    private val projectA = "PRJ-A"
    private val customerA = "CUS-A"
    private val accountA = "CFA-A"

    private val tenantB = "TENANT-B"
    private val projectB = "PRJ-B"
    private val customerB = "CUS-B"
    private val accountB = "CFA-B"

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
            // Setup Tenant A
            customerRepo.addCustomer(
                Customer(
                    customerId = customerA,
                    customerCode = "CUS-A01",
                    displayName = "Customer A",
                    primaryPhone = "01700000001",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountA,
                    tenantId = tenantA,
                    projectId = projectA,
                    customerId = customerA,
                    accountNumber = "CFA-A01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            // Setup Tenant B
            customerRepo.addCustomer(
                Customer(
                    customerId = customerB,
                    customerCode = "CUS-B01",
                    displayName = "Customer B",
                    primaryPhone = "01700000002",
                    customerType = CustomerType.BUSINESS,
                    status = CustomerStatusType.ACTIVE,
                    createdAt = "2026-08-29T00:00:00Z",
                    updatedAt = "2026-08-29T00:00:00Z"
                )
            )
            accountRepo.createAccount(
                CustomerFinancialAccount(
                    financialAccountId = accountB,
                    tenantId = tenantB,
                    projectId = projectB,
                    customerId = customerB,
                    accountNumber = "CFA-B01",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            // Create Invoice in Tenant B
            val lineB = CustomerInvoiceLine(
                lineId = "LINE-B1",
                invoiceId = "INV-B-001",
                tenantId = tenantB,
                projectId = projectB,
                description = "Printing B",
                quantity = BigDecimal("50"),
                unitPrice = BigDecimal("20.0000"),
                lineTotal = BigDecimal("1000.0000")
            )
            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-B-001",
                    tenantId = tenantB,
                    projectId = projectB,
                    customerId = customerB,
                    customerFinancialAccountId = accountB,
                    invoiceNumber = "INV-2026-B01",
                    lines = listOf(lineB),
                    subtotal = BigDecimal("1000.0000"),
                    grandTotal = BigDecimal("1000.0000"),
                    paidAmount = BigDecimal("0.0000"),
                    dueAmount = BigDecimal("1000.0000"),
                    status = CustomerInvoiceStatus.ISSUED,
                    version = 1L
                )
            )
        }
    }

    @Test
    fun testCannotAllocateAdvanceAcrossTenantsOrCustomers() = runBlocking {
        // Record Advance in Tenant A for Customer A
        val advRes = service.recordAdvance(
            tenantId = tenantA,
            projectId = projectA,
            customerId = customerA,
            customerFinancialAccountId = accountA,
            amount = BigDecimal("1000.0000"),
            paymentMethod = CustomerPaymentMethod.CASH,
            actorId = "staffA",
            actorRole = "STAFF"
        )
        val advanceA = (advRes as DomainResult.Success).data

        // Try to allocate Advance A (Tenant A, Customer A) to Invoice B (Tenant B, Customer B)
        val crossAllocRes = service.allocateCreditToInvoice(
            tenantId = tenantA,
            projectId = projectA,
            customerId = customerA,
            invoiceId = "INV-B-001",
            advanceId = advanceA.advanceId,
            amount = BigDecimal("500.0000"),
            actorId = "staffA",
            actorRole = "STAFF"
        )
        assertTrue("Must fail cross-tenant allocation", crossAllocRes is DomainResult.Error)
    }
}
