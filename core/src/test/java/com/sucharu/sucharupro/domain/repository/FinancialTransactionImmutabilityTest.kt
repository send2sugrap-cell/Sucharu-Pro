package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialTransactionImmutabilityTest {

    private lateinit var dataSource: FakeFinancialTransactionDataSource
    private lateinit var repository: FinancialTransactionRepository

    @Before
    fun setUp() {
        dataSource = FakeFinancialTransactionDataSource()
        repository = FinancialTransactionRepositoryImpl(dataSource)
    }

    @Test
    fun `posted financial transactions cannot be modified or updated`() = runBlocking {
        val createRes = repository.createTransaction(
            projectId = "PRJ-01",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-001",
            description = "Immutability test order",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val txnId = (createRes as DomainResult.Success).data.transactionId

        repository.submitTransaction(txnId, "staff-1", UserRole.STAFF)
        repository.postTransaction(txnId, "ACCOUNTS_RECEIVABLE", "acct-1", UserRole.ACCOUNTS)

        // Attempting to update a posted transaction must fail
        val updateRes = repository.updateDraftTransaction(
            transactionId = txnId,
            amount = Money(BigDecimal("20000.00")),
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )

        assertTrue(updateRes is DomainResult.Error)
    }
}
