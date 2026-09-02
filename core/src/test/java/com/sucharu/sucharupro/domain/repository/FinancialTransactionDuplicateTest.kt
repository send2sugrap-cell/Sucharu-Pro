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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialTransactionDuplicateTest {

    private lateinit var dataSource: FakeFinancialTransactionDataSource
    private lateinit var repository: FinancialTransactionRepository

    @Before
    fun setUp() {
        dataSource = FakeFinancialTransactionDataSource()
        repository = FinancialTransactionRepositoryImpl(dataSource)
    }

    @Test
    fun `multiple transactions can reference the same order if distinct events`() = runBlocking {
        // First deposit receipt
        val depositRes = repository.createTransaction(
            projectId = "PRJ-01",
            transactionType = FinancialTransactionType.RECEIPT,
            entryType = FinancialEntryType.CREDIT,
            amount = Money(BigDecimal("5000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-001",
            description = "50% Advance Deposit",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(depositRes is DomainResult.Success)

        // Final settlement receipt
        val finalRes = repository.createTransaction(
            projectId = "PRJ-01",
            transactionType = FinancialTransactionType.RECEIPT,
            entryType = FinancialEntryType.CREDIT,
            amount = Money(BigDecimal("5000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-001",
            description = "Remaining Balance Settlement",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        assertTrue(finalRes is DomainResult.Success)

        val list = repository.getTransactionsByReference("PRJ-01", "ORD-001", UserRole.ACCOUNTS)
        assertTrue(list is DomainResult.Success)
        assertEquals(2, (list as DomainResult.Success).data.size)
    }
}
