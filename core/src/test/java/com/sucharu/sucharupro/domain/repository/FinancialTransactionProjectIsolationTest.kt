package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialTransactionProjectIsolationTest {

    private lateinit var dataSource: FakeFinancialTransactionDataSource
    private lateinit var repository: FinancialTransactionRepository

    @Before
    fun setUp() {
        dataSource = FakeFinancialTransactionDataSource()
        repository = FinancialTransactionRepositoryImpl(dataSource)
    }

    @Test
    fun `transactions and ledger queries strictly isolate project boundaries`() = runBlocking {
        repository.createTransaction(
            projectId = "PRJ-A",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("1000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-A",
            description = "Project A Order",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )

        repository.createTransaction(
            projectId = "PRJ-B",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("2000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-B",
            description = "Project B Order",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )

        val prjAList = repository.observeTransactions("PRJ-A", UserRole.ACCOUNTS).first()
        val prjBList = repository.observeTransactions("PRJ-B", UserRole.ACCOUNTS).first()

        assertEquals(1, prjAList.size)
        assertEquals("PRJ-A", prjAList[0].projectId)

        assertEquals(1, prjBList.size)
        assertEquals("PRJ-B", prjBList[0].projectId)
    }
}
