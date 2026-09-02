package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialTransactionRepositoryTest {

    private lateinit var dataSource: FakeFinancialTransactionDataSource
    private lateinit var repository: FinancialTransactionRepository

    @Before
    fun setUp() {
        dataSource = FakeFinancialTransactionDataSource()
        repository = FinancialTransactionRepositoryImpl(dataSource)
    }

    @Test
    fun `create, submit, and post transaction flow creates atomic ledger entry`() = runBlocking {
        val createRes = repository.createTransaction(
            projectId = "PRJ-01",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("25000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-001",
            description = "Packaging Boxes Order",
            notes = "Standard commercial term",
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )

        assertTrue(createRes is DomainResult.Success)
        val transaction = (createRes as DomainResult.Success).data
        assertEquals(FinancialTransactionStatus.DRAFT, transaction.transactionStatus)

        val submitRes = repository.submitTransaction(transaction.transactionId, "staff-1", UserRole.STAFF)
        assertTrue(submitRes is DomainResult.Success)
        assertEquals(FinancialTransactionStatus.PENDING, (submitRes as DomainResult.Success).data.transactionStatus)

        val postRes = repository.postTransaction(
            transactionId = transaction.transactionId,
            accountHead = "ACCOUNTS_RECEIVABLE",
            actorId = "accounts-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(postRes is DomainResult.Success)
        val postedTxn = (postRes as DomainResult.Success).data
        assertEquals(FinancialTransactionStatus.POSTED, postedTxn.transactionStatus)

        // Verify authoritative ledger entry created atomically
        val ledgerRes = repository.getLedgerEntriesByTransaction(transaction.transactionId, UserRole.ACCOUNTS)
        assertTrue(ledgerRes is DomainResult.Success)
        val ledgerEntries = (ledgerRes as DomainResult.Success).data
        assertEquals(1, ledgerEntries.size)
        assertEquals(Money(BigDecimal("25000.00")), ledgerEntries[0].amount)
        assertEquals("ACCOUNTS_RECEIVABLE", ledgerEntries[0].accountHead)
    }
}
