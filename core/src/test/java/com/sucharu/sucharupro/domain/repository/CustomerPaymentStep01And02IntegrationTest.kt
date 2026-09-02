package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerPaymentDataSource
import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.FakeInventoryStockOutDataSource
import com.sucharu.sucharupro.data.repository.CustomerPaymentRepositoryImpl
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerPaymentMethod
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerPaymentStep01And02IntegrationTest {

    private lateinit var stockOutDataSource: FakeInventoryStockOutDataSource
    private lateinit var receivingDataSource: FakeInventoryReceivingDataSource
    private lateinit var inventoryLedgerDataSource: FakeInventoryMovementLedgerDataSource
    private lateinit var financeTransactionDataSource: FakeFinancialTransactionDataSource
    private lateinit var receivableDataSource: FakeCustomerReceivableDataSource
    private lateinit var paymentDataSource: FakeCustomerPaymentDataSource

    private lateinit var financialTransactionRepository: FinancialTransactionRepository
    private lateinit var receivableRepository: CustomerReceivableRepository
    private lateinit var paymentRepository: CustomerPaymentRepository

    @Before
    fun setUp() {
        stockOutDataSource = FakeInventoryStockOutDataSource()
        receivingDataSource = FakeInventoryReceivingDataSource()
        inventoryLedgerDataSource = FakeInventoryMovementLedgerDataSource()
        financeTransactionDataSource = FakeFinancialTransactionDataSource()
        receivableDataSource = FakeCustomerReceivableDataSource()
        paymentDataSource = FakeCustomerPaymentDataSource()

        financialTransactionRepository = FinancialTransactionRepositoryImpl(financeTransactionDataSource)
        receivableRepository = CustomerReceivableRepositoryImpl(receivableDataSource)
        paymentRepository = CustomerPaymentRepositoryImpl(
            paymentDataSource,
            receivableRepository,
            financialTransactionRepository
        )
    }

    @Test
    fun `payment posting triggers atomic financial ledger entry and receivable settlement with zero inventory mutations`() = runBlocking {
        val projectId = "PRJ-INTEG"

        // Baseline inventory checks
        val initialStockOuts = stockOutDataSource.observeStockOutRecords().first().size
        val initialStockIns = receivingDataSource.observeStockInRecords().first().size
        val initialInventoryLedger = inventoryLedgerDataSource.getEntries(projectId).size

        // Step 1 & 2: Create Receivable for 60,000
        val recRes = receivableRepository.createReceivable(
            projectId = projectId,
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-INTEG-1",
            originalAmount = Money(BigDecimal("60000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Integration Invoice",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (recRes as DomainResult.Success).data.receivableId

        // Step 3: Record and Post Payment of 60,000
        val payRes = paymentRepository.createPayment(
            projectId = projectId,
            customerId = "CUST-001",
            receivableId = recId,
            amount = Money(BigDecimal("60000.00")),
            currency = "BDT",
            paymentMethod = CustomerPaymentMethod.BANK_TRANSFER,
            paymentReference = "EFT-INTEG-99",
            paymentDate = System.currentTimeMillis(),
            idempotencyKey = null,
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val payId = (payRes as DomainResult.Success).data.paymentId

        val postRes = paymentRepository.postPayment(payId, "BANK_ACCOUNT", "acct-1", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)
        val postedPayment = (postRes as DomainResult.Success).data

        // 1. Verify Step 01 Financial Transaction created and posted
        val finTxnId = postedPayment.financialTransactionId
        assertTrue(!finTxnId.isNullOrBlank())
        val finTxnRes = financialTransactionRepository.getTransactionById(finTxnId!!, UserRole.ACCOUNTS)
        assertTrue(finTxnRes is DomainResult.Success)
        val finTxn = (finTxnRes as DomainResult.Success).data
        assertEquals(FinancialTransactionType.RECEIPT, finTxn.transactionType)
        assertEquals(FinancialEntryType.CREDIT, finTxn.entryType)
        assertEquals(FinancialTransactionStatus.POSTED, finTxn.transactionStatus)

        // 2. Verify Step 01 Financial Ledger Entry created
        val ledgerRes = financialTransactionRepository.getLedgerEntriesByTransaction(finTxnId, UserRole.ACCOUNTS)
        assertTrue(ledgerRes is DomainResult.Success)
        val ledgerEntries = (ledgerRes as DomainResult.Success).data
        assertEquals(1, ledgerEntries.size)
        assertEquals("BANK_ACCOUNT", ledgerEntries[0].accountHead)

        // 3. Verify Step 02 Receivable fully settled
        val updatedRec = (receivableRepository.getReceivableById(recId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(CustomerReceivableStatus.SETTLED, updatedRec.status)
        assertEquals(Money.ZERO, updatedRec.outstandingAmount)

        // 4. Verify Zero Inventory Mutation
        assertEquals(initialStockOuts, stockOutDataSource.observeStockOutRecords().first().size)
        assertEquals(initialStockIns, receivingDataSource.observeStockInRecords().first().size)
        assertEquals(initialInventoryLedger, inventoryLedgerDataSource.getEntries(projectId).size)
    }
}
