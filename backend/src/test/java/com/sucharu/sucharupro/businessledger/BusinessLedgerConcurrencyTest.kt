package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerSourceType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessledger.AllocateCostCommand
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.businessledger.PostApprovedExpenseCommand
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessLedgerConcurrencyTest {

    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var expenseDataSource: FakeBusinessExpenseDataSource
    private lateinit var expenseRepository: BusinessExpenseRepositoryImpl
    private lateinit var service: BusinessLedgerServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "USER-ADMIN-1",
        projectId = projectId,
        username = "admin1",
        role = UserRole.ADMIN
    )

    @Before
    fun setup() {
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        expenseDataSource = FakeBusinessExpenseDataSource()
        expenseRepository = BusinessExpenseRepositoryImpl(expenseDataSource)

        service = BusinessLedgerServiceImpl(
            repository = ledgerRepository,
            expenseRepository = expenseRepository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testConcurrentExpensePostingsProduceSingleCanonicalRecord() = runBlocking {
        val expense = BusinessExpense(
            expenseId = "EXP-CONC-1",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-CONC-001",
            expenseCategoryId = "CAT-001",
            amount = BigDecimal("15000.0000"),
            currency = "BDT",
            status = BusinessExpenseStatus.APPROVED,
            description = "Concurrent Binding Glue Batch",
            createdBy = "USER-STAFF-1"
        )
        expenseRepository.createExpense(expense)

        val cmd = PostApprovedExpenseCommand(
            expenseId = "EXP-CONC-1",
            idempotencyKey = "IDEMP-CONC-EXP-1"
        )

        // Launch 10 concurrent requests
        val jobs = (1..10).map {
            async {
                service.postApprovedExpense(adminPrincipal, cmd)
            }
        }

        val results = jobs.awaitAll()
        val successful = results.filterIsInstance<DomainResult.Success<*>>()
        assertEquals(10, successful.size)

        // Verify that all returned the exact same posting ID
        val postingIds = successful.map { (it.data as com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPosting).id }.toSet()
        assertEquals(1, postingIds.size)

        // Total postings in ledger must be exactly 1
        val allPostings = service.listPostings(adminPrincipal, com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter())
        assertEquals(1, (allPostings as DomainResult.Success).data.size)
    }

    @Test
    fun testConcurrentCostAllocationsCannotOverAllocate() = runBlocking {
        val expense = BusinessExpense(
            expenseId = "EXP-ALLOC-CONC",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-ALLOC-001",
            expenseCategoryId = "CAT-001",
            amount = BigDecimal("10000.0000"),
            currency = "BDT",
            status = BusinessExpenseStatus.APPROVED,
            description = "Die-cutting Blades",
            createdBy = "USER-STAFF-1"
        )
        expenseRepository.createExpense(expense)

        // Try allocating 6000 twice concurrently (Total = 12,000 > 10,000)
        val cmd1 = AllocateCostCommand(
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-ALLOC-CONC",
            jobId = "JOB-1",
            allocatedAmount = BigDecimal("6000.0000"),
            idempotencyKey = "IDEMP-ALLOC-A"
        )
        val cmd2 = AllocateCostCommand(
            sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-ALLOC-CONC",
            jobId = "JOB-2",
            allocatedAmount = BigDecimal("6000.0000"),
            idempotencyKey = "IDEMP-ALLOC-B"
        )

        val j1 = async { service.allocateCost(adminPrincipal, cmd1) }
        val j2 = async { service.allocateCost(adminPrincipal, cmd2) }

        val res1 = j1.await()
        val res2 = j2.await()

        val successes = listOf(res1, res2).filterIsInstance<DomainResult.Success<*>>()
        val errors = listOf(res1, res2).filterIsInstance<DomainResult.Error>()

        // Exactly one should succeed, one must fail due to bounds constraint
        assertEquals(1, successes.size)
        assertEquals(1, errors.size)
        assertTrue(errors[0].message.contains("exceeds remaining unallocated amount"))
    }
}
