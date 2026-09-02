package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.CustomerReceivableStatus
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerReceivableBalanceTest {

    private lateinit var dataSource: FakeCustomerReceivableDataSource
    private lateinit var repository: CustomerReceivableRepository

    @Before
    fun setUp() {
        dataSource = FakeCustomerReceivableDataSource()
        repository = CustomerReceivableRepositoryImpl(dataSource)
    }

    @Test
    fun `outstanding due is computed accurately from original minus settled amount`() = runBlocking {
        val createRes = repository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-001",
            originalAmount = Money(BigDecimal("100000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Major offset print job",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(createRes is DomainResult.Success)
        val recId = (createRes as DomainResult.Success).data.receivableId

        // Partial settlement 30,000
        val settle1Res = repository.recordSettlement(
            receivableId = recId,
            settlementAmount = Money(BigDecimal("30000.00")),
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(settle1Res is DomainResult.Success)
        val rec1 = (settle1Res as DomainResult.Success).data
        assertEquals(Money(BigDecimal("30000.00")), rec1.settledAmount)
        assertEquals(Money(BigDecimal("70000.00")), rec1.outstandingAmount)
        assertEquals(CustomerReceivableStatus.PARTIALLY_SETTLED, rec1.status)

        // Remaining settlement 70,000
        val settle2Res = repository.recordSettlement(
            receivableId = recId,
            settlementAmount = Money(BigDecimal("70000.00")),
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(settle2Res is DomainResult.Success)
        val rec2 = (settle2Res as DomainResult.Success).data
        assertEquals(Money(BigDecimal("100000.00")), rec2.settledAmount)
        assertEquals(Money.ZERO, rec2.outstandingAmount)
        assertEquals(CustomerReceivableStatus.SETTLED, rec2.status)
        assertTrue(rec2.settledAt != null)
    }

    @Test
    fun `settlement exceeding outstanding balance is rejected`() = runBlocking {
        val createRes = repository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-002",
            originalAmount = Money(BigDecimal("5000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Small packaging print",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        val recId = (createRes as DomainResult.Success).data.receivableId

        val overSettleRes = repository.recordSettlement(
            receivableId = recId,
            settlementAmount = Money(BigDecimal("6000.00")),
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(overSettleRes is DomainResult.Error)
    }
}
