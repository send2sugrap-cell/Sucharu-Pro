package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerReceivableDuplicateTest {

    private lateinit var dataSource: FakeCustomerReceivableDataSource
    private lateinit var repository: CustomerReceivableRepository

    @Before
    fun setUp() {
        dataSource = FakeCustomerReceivableDataSource()
        repository = CustomerReceivableRepositoryImpl(dataSource)
    }

    @Test
    fun `duplicate receivable creation for the same commercial reference is rejected`() = runBlocking {
        val firstRes = repository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-999",
            originalAmount = Money(BigDecimal("12000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Original Invoice Obligation",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(firstRes is DomainResult.Success)

        // Duplicate attempt with same referenceId and referenceType
        val dupRes = repository.createReceivable(
            projectId = "PRJ-01",
            customerId = "CUST-001",
            referenceType = FinancialReferenceType.INVOICE,
            referenceId = "INV-2026-999",
            originalAmount = Money(BigDecimal("12000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Duplicate attempt",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(dupRes is DomainResult.Error)
    }
}
