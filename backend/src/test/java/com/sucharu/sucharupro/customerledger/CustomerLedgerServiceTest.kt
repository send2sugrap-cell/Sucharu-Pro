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
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustment
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentStatus
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdjustmentType
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvance
import com.sucharu.sucharupro.domain.model.customercredit.CustomerAdvanceStatus
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccount
import com.sucharu.sucharupro.domain.model.customerfinancial.CustomerFinancialAccountStatus
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoice
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceLine
import com.sucharu.sucharupro.domain.model.customerinvoice.CustomerInvoiceStatus
import com.sucharu.sucharupro.domain.model.customerledger.CustomerLedgerEntryType
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPayment
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.customerpayment.CustomerPaymentStatus
import com.sucharu.sucharupro.domain.service.customerledger.CustomerLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerLedgerServiceTest {

    private lateinit var customerRepo: CustomerRepositoryImpl
    private lateinit var accountRepo: CustomerFinancialAccountRepositoryImpl
    private lateinit var invoiceRepo: CustomerInvoiceRepositoryImpl
    private lateinit var paymentRepo: CustomerPaymentRepositoryImpl
    private lateinit var creditRepo: CustomerCreditRepositoryImpl
    private lateinit var ledgerRepo: CustomerLedgerRepositoryImpl
    private lateinit var service: CustomerLedgerServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val customerId = "CUS-LEDGER-001"
    private val accountId = "CFA-LEDGER-001"

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
                    customerCode = "CUS-L-01",
                    displayName = "Ledger Test Customer",
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
                    accountNumber = "CFA-L-1001",
                    status = CustomerFinancialAccountStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testEmptyLedger() = runBlocking {
        val ledgerRes = service.getCustomerLedger(tenantId, projectId, customerId)
        assertTrue(ledgerRes is DomainResult.Success)
        val entries = (ledgerRes as DomainResult.Success).data
        assertTrue(entries.isEmpty())

        val stmtRes = service.getCustomerStatement(tenantId, projectId, customerId)
        assertTrue(stmtRes is DomainResult.Success)
        val stmt = (stmtRes as DomainResult.Success).data
        assertEquals(BigDecimal("0.0000"), stmt.openingBalance)
        assertEquals(BigDecimal("0.0000"), stmt.closingBalance)
    }

    @Test
    fun testFullLedgerChronologicalFlowAndRunningBalances() = runBlocking {
        // T1: Invoice 1 of 10,000 (at t=1000)
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
                status = CustomerInvoiceStatus.ISSUED,
                issueDate = 1000L,
                createdAt = 1000L
            )
        )

        // T2: Payment 1 of 4,000 (at t=2000)
        paymentRepo.createPayment(
            CustomerPayment(
                paymentId = "PAY-01",
                tenantId = tenantId,
                projectId = projectId,
                paymentNumber = "PAY-1001",
                customerId = customerId,
                customerFinancialAccountId = accountId,
                amount = BigDecimal("4000.0000"),
                paymentDate = 2000L,
                status = CustomerPaymentStatus.CONFIRMED
            )
        )

        // T3: Advance of 2,000 (at t=3000)
        creditRepo.createAdvance(
            CustomerAdvance(
                advanceId = "ADV-01",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                advanceNumber = "ADV-1001",
                amount = BigDecimal("2000.0000"),
                availableAmount = BigDecimal("2000.0000"),
                receiptDate = 3000L,
                status = CustomerAdvanceStatus.AVAILABLE
            )
        )

        // T4: Adjustment Debit 500 (at t=4000)
        creditRepo.createAdjustment(
            CustomerAdjustment(
                adjustmentId = "ADJ-01",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                adjustmentNumber = "ADJ-1001",
                adjustmentType = CustomerAdjustmentType.DEBIT,
                amount = BigDecimal("500.0000"),
                reason = "Urgent handling fee",
                createdAt = 4000L,
                status = CustomerAdjustmentStatus.APPLIED
            )
        )

        // Fetch Full Ledger
        val ledgerRes = service.getCustomerLedger(tenantId, projectId, customerId)
        assertTrue(ledgerRes is DomainResult.Success)
        val entries = (ledgerRes as DomainResult.Success).data
        assertEquals(4, entries.size)

        // Verify Entry 1 (Invoice 10,000) -> Bal 10,000
        assertEquals(CustomerLedgerEntryType.INVOICE, entries[0].entryType)
        assertEquals(BigDecimal("10000.0000"), entries[0].balanceAfter)

        // Verify Entry 2 (Payment 4,000) -> Bal 6,000
        assertEquals(CustomerLedgerEntryType.PAYMENT, entries[1].entryType)
        assertEquals(BigDecimal("6000.0000"), entries[1].balanceAfter)

        // Verify Entry 3 (Advance 2,000) -> Bal 4,000
        assertEquals(CustomerLedgerEntryType.ADVANCE, entries[2].entryType)
        assertEquals(BigDecimal("4000.0000"), entries[2].balanceAfter)

        // Verify Entry 4 (Debit Adj 500) -> Bal 4,500
        assertEquals(CustomerLedgerEntryType.DEBIT_ADJUSTMENT, entries[3].entryType)
        assertEquals(BigDecimal("4500.0000"), entries[3].balanceAfter)
    }

    @Test
    fun testStatementWithDateRangeAndOpeningBalance() = runBlocking {
        // T1: Invoice 10,000 at t=1000
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
                status = CustomerInvoiceStatus.ISSUED,
                issueDate = 1000L,
                createdAt = 1000L
            )
        )

        // T2: Payment 4,000 at t=2000
        paymentRepo.createPayment(
            CustomerPayment(
                paymentId = "PAY-01",
                tenantId = tenantId,
                projectId = projectId,
                paymentNumber = "PAY-1001",
                customerId = customerId,
                customerFinancialAccountId = accountId,
                amount = BigDecimal("4000.0000"),
                paymentDate = 2000L,
                status = CustomerPaymentStatus.CONFIRMED
            )
        )

        // T3: Advance 3,000 at t=3000
        creditRepo.createAdvance(
            CustomerAdvance(
                advanceId = "ADV-01",
                tenantId = tenantId,
                projectId = projectId,
                customerId = customerId,
                customerFinancialAccountId = accountId,
                advanceNumber = "ADV-1001",
                amount = BigDecimal("3000.0000"),
                availableAmount = BigDecimal("3000.0000"),
                receiptDate = 3000L,
                status = CustomerAdvanceStatus.AVAILABLE
            )
        )

        // Request Statement for window [2500, 3500]
        // Transactions before 2500: Invoice 10,000 (Debit) - Payment 4,000 (Credit) = Opening Balance 6,000
        // Transaction inside [2500, 3500]: Advance 3,000 (Credit)
        // Closing Balance: 6,000 - 3,000 = 3,000
        val stmtRes = service.getCustomerStatement(tenantId, projectId, customerId, fromDate = 2500L, toDate = 3500L)
        assertTrue(stmtRes is DomainResult.Success)
        val stmt = (stmtRes as DomainResult.Success).data

        assertEquals(BigDecimal("6000.0000"), stmt.openingBalance)
        assertEquals(BigDecimal("0.0000"), stmt.totalDebit)
        assertEquals(BigDecimal("3000.0000"), stmt.totalCredit)
        assertEquals(BigDecimal("3000.0000"), stmt.closingBalance)
        assertEquals(1, stmt.entries.size)
        assertEquals(CustomerLedgerEntryType.ADVANCE, stmt.entries[0].entryType)
    }
}
