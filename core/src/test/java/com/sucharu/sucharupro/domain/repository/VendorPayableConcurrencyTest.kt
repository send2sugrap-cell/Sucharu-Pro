package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.VendorPayableRepositoryImpl
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

class VendorPayableConcurrencyTest {

    private lateinit var dataSource: FakeVendorPayableDataSource
    private lateinit var repository: VendorPayableRepository

    @Before
    fun setUp() {
        dataSource = FakeVendorPayableDataSource()
        repository = VendorPayableRepositoryImpl(dataSource)
    }

    @Test
    fun `concurrent settlements serialize safely and prevent negative outstanding`() = runBlocking {
        // Create payable of 100,000
        val createRes = repository.createPayable(
            projectId = "PRJ-01",
            vendorId = "VEND-001",
            referenceType = FinancialReferenceType.PURCHASE,
            referenceId = "PO-CONCUR-1",
            originalAmount = Money(BigDecimal("100000.00")),
            currency = "BDT",
            dueDate = System.currentTimeMillis() + 86400000L,
            description = "Concurrency test",
            notes = null,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(createRes is DomainResult.Success)
        val payableId = (createRes as DomainResult.Success).data.payableId

        // 20 concurrent coroutines attempting to record settlement of 5,000 each (Total = 100,000)
        val deferredSettlements = (1..20).map { idx ->
            async {
                repository.recordSettlement(
                    payableId = payableId,
                    settlementAmount = Money(BigDecimal("5000.00")),
                    actorId = "acct-$idx",
                    callerRole = UserRole.ACCOUNTS
                )
            }
        }

        val results = deferredSettlements.awaitAll()
        results.forEach {
            assertTrue(it is DomainResult.Success)
        }

        val finalPayable = (repository.getPayableById(payableId, UserRole.ACCOUNTS) as DomainResult.Success).data
        assertEquals(Money(BigDecimal("100000.00")), finalPayable.settledAmount)
        assertEquals(Money.ZERO, finalPayable.outstandingAmount)
    }
}
