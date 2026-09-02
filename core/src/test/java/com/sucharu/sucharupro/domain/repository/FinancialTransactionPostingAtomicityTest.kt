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

class FinancialTransactionPostingAtomicityTest {

    private lateinit var dataSource: FakeFinancialTransactionDataSource
    private lateinit var repository: FinancialTransactionRepository

    @Before
    fun setUp() {
        dataSource = FakeFinancialTransactionDataSource()
        repository = FinancialTransactionRepositoryImpl(dataSource)
    }

    @Test
    fun `posting transaction atomically produces ledger entry and updates transaction status`() = runBlocking {
        val createRes = repository.createTransaction(
            projectId = "PRJ-01",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("30000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-001",
            description = "Brochure print",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val txnId = (createRes as DomainResult.Success).data.transactionId

        repository.submitTransaction(txnId, "staff-1", UserRole.STAFF)
        val postRes = repository.postTransaction(txnId, "SALES_REVENUE", "acct-1", UserRole.ACCOUNTS)
        assertTrue(postRes is DomainResult.Success)

        val updatedTxn = (postRes as DomainResult.Success).data
        assertEquals(FinancialTransactionStatus.POSTED, updatedTxn.transactionStatus)

        val ledgerEntriesRes = repository.getLedgerEntriesByTransaction(txnId, UserRole.ACCOUNTS)
        assertTrue(ledgerEntriesRes is DomainResult.Success)
        val ledgerEntries = (ledgerEntriesRes as DomainResult.Success).data
        assertEquals(1, ledgerEntries.size)
        assertEquals(txnId, ledgerEntries[0].transactionId)
        assertEquals("SALES_REVENUE", ledgerEntries[0].accountHead)
    }
}
