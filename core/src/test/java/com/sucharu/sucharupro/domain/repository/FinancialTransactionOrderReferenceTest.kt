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

class FinancialTransactionOrderReferenceTest {

    private lateinit var dataSource: FakeFinancialTransactionDataSource
    private lateinit var repository: FinancialTransactionRepository

    @Before
    fun setUp() {
        dataSource = FakeFinancialTransactionDataSource()
        repository = FinancialTransactionRepositoryImpl(dataSource)
    }

    @Test
    fun `financial transaction successfully links to commercial order`() = runBlocking {
        val result = repository.createTransaction(
            projectId = "PRJ-01",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("18500.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-2026-0099",
            customerId = "CUST-001",
            description = "Calendar printing order commitment",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )

        assertTrue(result is DomainResult.Success)
        val txn = (result as DomainResult.Success).data
        assertEquals(FinancialReferenceType.ORDER, txn.referenceType)
        assertEquals("ORD-2026-0099", txn.referenceId)
        assertEquals("CUST-001", txn.customerId)
    }
}
