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
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerledger.ReceivableReconciliationStatus
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerReceivableReconciliationTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var ledgerRepo: CustomerLedgerRepositoryImpl
    private lateinit var service: CustomerLedgerServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-RECON-001"
    private val accountId = "CFA-RECON-001"

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
                    customerCode = "CUS-R-01",
                    displayName = "Reconciliation Customer",
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
                    accountNumber = "CFA-R-1001",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testConsistentAccount_ReconciliationPasses() = runBlocking {
        // Invoice 5,000, Paid 2,000, Due 3,000
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-01",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-1001",
                grandTotal = BigDecimal("5000.0000"),
                paidAmount = BigDecimal("2000.0000"),
                dueAmount = BigDecimal("3000.0000"),
                status = CustomerInvoiceStatus.PARTIALLY_PAID,
                issueDate = 1000L
            )
        )

        paymentRepo.createPayment(
            CustomerPayment(
                paymentId = "PAY-01",
                tenantId = tenantId,
                projectId = projectId,
                paymentNumber = "PAY-1001",
                customerId = customerId,
                customerFinancialAccountId = accountId,
                amount = BigDecimal("2000.0000"),
                paymentDate = 2000L,
                status = CustomerPaymentStatus.CONFIRMED
            )
        )

        val reconRes = service.reconcileCustomerReceivable(tenantId, projectId, customerId)
        assertTrue(reconRes is DomainResult.Success)
        val recon = (reconRes as DomainResult.Success).data

        assertEquals(ReceivableReconciliationStatus.CONSISTENT, recon.status)
        assertTrue(recon.isConsistent)
        assertEquals(0, recon.discrepancyCount)
        assertEquals(BigDecimal("3000.0000"), recon.invoiceTotalReceivable)
        assertEquals(BigDecimal("3000.0000"), recon.ledgerCalculatedBalance)
        assertEquals(BigDecimal("0.0000"), recon.difference)
    }

    @Test
    fun testInconsistentInvoiceDue_ReconciliationDetectsVariance() = runBlocking {
        // Intentionally corrupted invoice where due amount is wrong: grandTotal 5000, paid 2000, but due is stored as 4000 instead of 3000
        invoiceRepo.createInvoice(
            CustomerInvoice(
                invoiceId = "INV-CORRUPT-01",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                invoiceNumber = "INV-1002",
                grandTotal = BigDecimal("5000.0000"),
                paidAmount = BigDecimal("2000.0000"),
                dueAmount = BigDecimal("4000.0000"), // Mismatch!
                status = CustomerInvoiceStatus.PARTIALLY_PAID,
                issueDate = 1000L
            )
        )

        paymentRepo.createPayment(
            CustomerPayment(
                paymentId = "PAY-02",
                tenantId = tenantId,
                projectId = projectId,
                paymentNumber = "PAY-1002",
                customerId = customerId,
                customerFinancialAccountId = accountId,
                amount = BigDecimal("2000.0000"),
                paymentDate = 2000L,
                status = CustomerPaymentStatus.CONFIRMED
            )
        )

        val reconRes = service.reconcileCustomerReceivable(tenantId, projectId, customerId)
        assertTrue(reconRes is DomainResult.Success)
        val recon = (reconRes as DomainResult.Success).data

        assertEquals(ReceivableReconciliationStatus.INCONSISTENT, recon.status)
        assertFalse(recon.isConsistent)
        assertTrue(recon.discrepancyCount >= 1)
        assertTrue(recon.discrepancies.any { it.discrepancyType == "INVOICE_BALANCE_MISMATCH" })
    }
}
