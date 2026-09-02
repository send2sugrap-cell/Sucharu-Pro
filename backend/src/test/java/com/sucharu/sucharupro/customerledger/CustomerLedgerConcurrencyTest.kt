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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerLedgerConcurrencyTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var ledgerRepo: CustomerLedgerRepositoryImpl
    private lateinit var service: CustomerLedgerServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-CONCUR-001"
    private val accountId = "CFA-CONCUR-001"

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
            customerRepo.addCustomer(
                Customer(
                    customerId = customerId,
                    customerCode = "CUS-C-01",
                    displayName = "Concurrency Customer",
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
                    accountNumber = "CFA-C-1001",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )

            // Seed 10 invoices concurrently
            (1..10).forEach { i ->
                invoiceRepo.createInvoice(
                    CustomerInvoice(
                        invoiceId = "INV-CONCUR-$i",
                        tenantId = tenantId,
                        projectId = projectId,
                        customerId = customerId,
                        customerFinancialAccountId = accountId,
                        invoiceNumber = "INV-C-$i",
                        grandTotal = BigDecimal("100.0000"),
                        dueAmount = BigDecimal("100.0000"),
                        status = CustomerInvoiceStatus.ISSUED,
                        createdAt = 1000L + i
                    )
                )
            }
        }
    }

    @Test
    fun testConcurrentStatementQueries_ProduceIdenticalDeterministicResults() = runBlocking {
        val deferred = (1..10).map {
            async {
                service.getCustomerStatement(tenantId, projectId, customerId)
            }
        }

        val results = deferred.awaitAll()
        results.forEach { res ->
            assertTrue(res is DomainResult.Success)
            val stmt = (res as DomainResult.Success).data
            assertEquals(10, stmt.entries.size)
            assertEquals(BigDecimal("1000.0000"), stmt.closingBalance)
            assertEquals(BigDecimal("1000.0000"), stmt.totalDebit)
            assertEquals(BigDecimal("0.0000"), stmt.totalCredit)
        }
    }
}
