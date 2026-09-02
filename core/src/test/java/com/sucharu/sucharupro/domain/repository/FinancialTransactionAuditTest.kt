package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeFinancialTransactionDataSource
import com.sucharu.sucharupro.data.repository.FinancialTransactionRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialActivityType
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

class FinancialTransactionAuditTest {

    private lateinit var dataSource: FakeFinancialTransactionDataSource
    private lateinit var repository: FinancialTransactionRepository

    @Before
    fun setUp() {
        dataSource = FakeFinancialTransactionDataSource()
        repository = FinancialTransactionRepositoryImpl(dataSource)
    }

    @Test
    fun `lifecycle steps record chronological immutable audit events`() = runBlocking {
        val createRes = repository.createTransaction(
            projectId = "PRJ-01",
            transactionType = FinancialTransactionType.SALE,
            entryType = FinancialEntryType.DEBIT,
            amount = Money(BigDecimal("1000.00")),
            currency = "BDT",
            referenceType = FinancialReferenceType.ORDER,
            referenceId = "ORD-01",
            description = "Audit Test",
            notes = null,
            actorId = "staff-1",
            callerRole = UserRole.STAFF
        )
        val txnId = (createRes as DomainResult.Success).data.transactionId

        repository.updateDraftTransaction(txnId, description = "Updated Description", actorId = "staff-1", callerRole = UserRole.STAFF)
        repository.submitTransaction(txnId, actorId = "staff-1", callerRole = UserRole.STAFF)
        repository.postTransaction(txnId, accountHead = "ACCOUNTS_RECEIVABLE", actorId = "acct-1", callerRole = UserRole.ACCOUNTS)

        val eventsRes = repository.getActivityEvents(txnId, UserRole.ACCOUNTS)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data

        assertEquals(5, events.size)
        assertEquals(FinancialActivityType.TRANSACTION_CREATED, events[0].activityType)
        assertEquals(FinancialActivityType.TRANSACTION_UPDATED, events[1].activityType)
        assertEquals(FinancialActivityType.TRANSACTION_SUBMITTED, events[2].activityType)
        assertEquals(FinancialActivityType.TRANSACTION_POSTED, events[3].activityType)
        assertEquals(FinancialActivityType.LEDGER_ENTRY_POSTED, events[4].activityType)
    }
}
