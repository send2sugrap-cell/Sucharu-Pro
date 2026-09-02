package com.sucharu.sucharupro.businessledger

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.businessledger.PostApprovedExpenseCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessLedgerIdempotencyTest {

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
    fun testRepeatedPostingRequestsReturnExistingRecord() = runBlocking {
        val expense = BusinessExpense(
            expenseId = "EXP-IDEMP-1",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-IDEMP-001",
            expenseCategoryId = "CAT-001",
            amount = BigDecimal("9200.0000"),
            currency = "BDT",
            status = BusinessExpenseStatus.APPROVED,
            description = "High Speed Stapler Pin Boxes",
            createdBy = "USER-STAFF-1"
        )
        expenseRepository.createExpense(expense)

        val cmd = PostApprovedExpenseCommand(
            expenseId = "EXP-IDEMP-1",
            idempotencyKey = "KEY-IDEMP-EXP-9200"
        )

        // First call -> created
        val res1 = service.postApprovedExpense(adminPrincipal, cmd)
        assertTrue(res1 is DomainResult.Success)
        val p1 = (res1 as DomainResult.Success).data

        // Second call with same idempotency key -> returns identical posting
        val res2 = service.postApprovedExpense(adminPrincipal, cmd)
        assertTrue(res2 is DomainResult.Success)
        val p2 = (res2 as DomainResult.Success).data
        assertEquals(p1.id, p2.id)

        // Third call without idempotency key but same source -> returns canonical posting
        val res3 = service.postApprovedExpense(adminPrincipal, PostApprovedExpenseCommand(expenseId = "EXP-IDEMP-1"))
        assertTrue(res3 is DomainResult.Success)
        val p3 = (res3 as DomainResult.Success).data
        assertEquals(p1.id, p3.id)

        // Count postings in repo must be exactly 1
        val all = (service.listPostings(adminPrincipal, com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter()) as DomainResult.Success).data
        assertEquals(1, all.size)
    }
}
