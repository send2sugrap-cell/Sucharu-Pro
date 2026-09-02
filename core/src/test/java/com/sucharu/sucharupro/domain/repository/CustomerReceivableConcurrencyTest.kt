package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerReceivableDataSource
import com.sucharu.sucharupro.data.repository.CustomerReceivableRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.finance.FinancialReferenceType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CustomerReceivableConcurrencyTest {

    private lateinit var dataSource: FakeCustomerReceivableDataSource
    private lateinit var repository: CustomerReceivableRepository

    @Before
    fun setUp() {
        dataSource = FakeCustomerReceivableDataSource()
        repository = CustomerReceivableRepositoryImpl(dataSource)
    }

    @Test
    fun `concurrent receivable creations execute safely without race conditions`() = runBlocking {
        val total = 20
        val deferredCreations = (1..total).map { idx ->
            async {
                repository.createReceivable(
                    projectId = "PRJ-01",
                    customerId = "CUST-$idx",
                    referenceType = FinancialReferenceType.INVOICE,
                    referenceId = "INV-CONCUR-$idx",
                    originalAmount = Money(BigDecimal("${idx * 1000}.00")),
                    currency = "BDT",
                    dueDate = System.currentTimeMillis() + 86400000L,
                    description = "Concurrent Invoice Obligation $idx",
                    notes = null,
                    actorId = "acct-$idx",
                    callerRole = UserRole.ACCOUNTS
                )
            }
        }

        val results = deferredCreations.awaitAll()
        results.forEach {
            assertTrue(it is DomainResult.Success)
        }

        val recs = repository.getReceivablesByReference("PRJ-01", "INV-CONCUR-1", UserRole.ACCOUNTS)
        assertTrue(recs is DomainResult.Success)
        assertEquals(1, (recs as DomainResult.Success).data.size)
    }
}
