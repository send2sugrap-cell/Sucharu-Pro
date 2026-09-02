package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialEntryType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.finance.FinancialTransactionType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialTransactionConcurrencyTest {

    private lateinit var dataSource: FakeFinancialTransactionDataSource
    private lateinit var repository: FinancialTransactionRepository

    @Before
    fun setUp() {
        dataSource = FakeFinancialTransactionDataSource()
        repository = FinancialTransactionRepositoryImpl(dataSource)
    }

    @Test
    fun `concurrent transaction creations execute safely without race conditions`() = runBlocking {
        val total = 20
        val deferredCreations = (1..total).map { idx ->
            async {
                repository.createTransaction(
                    projectId = "PRJ-01",
                    transactionType = FinancialTransactionType.SALE,
                    entryType = FinancialEntryType.DEBIT,
                    amount = Money(BigDecimal("${idx * 100}.00")),
                    currency = "BDT",
                    referenceType = FinancialReferenceType.ORDER,
                    referenceId = "ORD-$idx",
                    description = "Concurrent Order $idx",
                    notes = null,
                    actorId = "staff-$idx",
                    callerRole = UserRole.STAFF
                )
            }
        }

        val results = deferredCreations.awaitAll()
        results.forEach {
            assertTrue(it is DomainResult.Success)
        }

        val transactions = dataSource.getTransactionsByReference("PRJ-01", "ORD-1")
        assertEquals(1, transactions.size)
    }
}
