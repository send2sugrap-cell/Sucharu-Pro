package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerReceivableProjectIsolationTest {

    private lateinit var dataSource: FakeCustomerReceivableDataSource
    private lateinit var repository: CustomerReceivableRepository

    @Before
    fun setUp() {
        dataSource = FakeCustomerReceivableDataSource()
        repository = CustomerReceivableRepositoryImpl(dataSource)
    }

    @Test
    fun `receivables and due summaries strictly isolate tenant project boundaries`() = runBlocking {
        repository.createReceivable(
            projectId = "PRJ-AAA",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-A1",
            originalAmount = Money(BigDecimal("25000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Project A Obligation",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        repository.createReceivable(
            projectId = "PRJ-BBB",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-B1",
            originalAmount = Money(BigDecimal("50000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Project B Obligation",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        val prjAList = repository.observeReceivables("PRJ-AAA", UserRole.ACCOUNTS).first()
        val prjBList = repository.observeReceivables("PRJ-BBB", UserRole.ACCOUNTS).first()

        assertEquals(1, prjAList.size)
        assertEquals("PRJ-AAA", prjAList[0].projectId)
        assertEquals(Money(BigDecimal("25000.00")), prjAList[0].originalAmount)

        assertEquals(1, prjBList.size)
        assertEquals("PRJ-BBB", prjBList[0].projectId)
        assertEquals(Money(BigDecimal("50000.00")), prjBList[0].originalAmount)

        val summaryA = repository.getCustomerDueSummary("PRJ-AAA", "CUST-001", UserRole.ACCOUNTS)
        val summaryB = repository.getCustomerDueSummary("PRJ-BBB", "CUST-001", UserRole.ACCOUNTS)

        assertTrue(summaryA is DomainResult.Success)
        assertTrue(summaryB is DomainResult.Success)
        assertEquals(Money(BigDecimal("25000.00")), (summaryA as DomainResult.Success).data.totalOutstandingDue)
        assertEquals(Money(BigDecimal("50000.00")), (summaryB as DomainResult.Success).data.totalOutstandingDue)
    }
}
