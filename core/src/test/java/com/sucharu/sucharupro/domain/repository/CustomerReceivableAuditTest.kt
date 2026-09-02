package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableActivityType
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerReceivableAuditTest {

    private lateinit var dataSource: FakeCustomerReceivableDataSource
    private lateinit var repository: CustomerReceivableRepository

    @Before
    fun setUp() {
        dataSource = FakeCustomerReceivableDataSource()
        repository = CustomerReceivableRepositoryImpl(dataSource)
    }

    @Test
    fun `lifecycle mutations record chronological immutable audit events`() = runBlocking {
        val createRes = repository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-AUDIT-1",
            originalAmount = Money(BigDecimal("10000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Audit Test Obligation",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (createRes as DomainResult.Success).data.receivableId

        repository.updateReceivable(
            receivableId = recId,
            description = "Updated description for audit",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        repository.recordSettlement(
            receivableId = recId,
            settlementAmount = Money(BigDecimal("4000.00")),
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        val eventsRes = repository.getActivityEvents(recId, UserRole.ACCOUNTS)
        assertTrue(eventsRes is DomainResult.Success)
        val events = (eventsRes as DomainResult.Success).data

        assertEquals(3, events.size)
        assertEquals(CustomerReceivableActivityType.RECEIVABLE_CREATED, events[0].activityType)
        assertEquals(CustomerReceivableActivityType.RECEIVABLE_UPDATED, events[1].activityType)
        assertEquals(CustomerReceivableActivityType.RECEIVABLE_SETTLEMENT_RECORDED, events[2].activityType)
    }
}
