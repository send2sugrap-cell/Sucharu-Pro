package com.sucharu.sucharupro.customerledger

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerFinancialAccountDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerInvoiceDataSource
import com.sucharu.sucharupro.data.datasource.customercredit.FakeCustomerCreditDataSource
import com.sucharu.sucharupro.data.datasource.customerledger.FakeCustomerLedgerDataSource
import com.sucharu.sucharupro.data.datasource.customerpayment.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.repository.CustomerFinancialAccountRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerInvoiceRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customercredit.CustomerCreditRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerledger.CustomerLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.customerpayment.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customer.Customer
import com.sucharu.sucharupro.domain.model.customer.CustomerStatusType
import com.sucharu.sucharupro.domain.model.customer.CustomerType
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerLedgerIsolationTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var ledgerRepo: CustomerLedgerRepositoryImpl
    private lateinit var service: CustomerLedgerServiceImpl

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
        val ledgerDs = FakeCustomerLedgerDataSource()
        ledgerRepo = CustomerLedgerRepositoryImpl(ledgerDs)

        service = CustomerLedgerServiceImpl(
            ledgerRepo, accountRepo, invoiceRepo, paymentRepo, creditRepo, customerRepo
        )

        runBlocking {
            // Setup Tenant A Customer & Invoice
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
            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-A1",
                    tenantId = tenantA,
                    projectId = projectA,
                    customerId = customerA,
                    customerFinancialAccountId = accountA,
                    invoiceNumber = "INV-A-101",
                    grandTotal = BigDecimal("1000.0000"),
                    dueAmount = BigDecimal("1000.0000"),
                    status = CustomerInvoiceStatus.ISSUED
                )
            )

            // Setup Tenant B Customer & Invoice
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
            invoiceRepo.createInvoice(
                CustomerInvoice(
                    invoiceId = "INV-B1",
                    tenantId = tenantB,
                    projectId = projectB,
                    customerId = customerB,
                    customerFinancialAccountId = accountB,
                    invoiceNumber = "INV-B-101",
                    grandTotal = BigDecimal("9999.0000"),
                    dueAmount = BigDecimal("9999.0000"),
                    status = CustomerInvoiceStatus.ISSUED
                )
            )
        }
    }

    @Test
    fun testTenantIsolationOnLedgerAndStatement() = runBlocking {
        // Query Customer A ledger under Tenant A
        val ledgerA = service.getCustomerLedger(tenantA, projectA, customerA)
        assertTrue(ledgerA is DomainResult.Success)
        val entriesA = (ledgerA as DomainResult.Success).data
        assertEquals(1, entriesA.size)
        assertEquals("INV-A1", entriesA[0].referenceId)
        assertEquals(BigDecimal("1000.0000"), entriesA[0].debitAmount)

        // Query Customer A under Tenant B must fail because account is not under Tenant B
        val crossTenantQuery = service.getCustomerLedger(tenantB, projectB, customerA)
        assertTrue(crossTenantQuery is DomainResult.Error)
    }
}
