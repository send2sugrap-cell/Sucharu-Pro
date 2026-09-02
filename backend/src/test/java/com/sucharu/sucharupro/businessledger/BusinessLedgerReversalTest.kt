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
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPostingType
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerSourceType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.businessledger.PostApprovedExpenseCommand
import com.sucharu.sucharupro.domain.service.businessledger.ReversePostingCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessLedgerReversalTest {

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

    private val managerPrincipal = AuthenticatedPrincipal(
        userId = "USER-MGR-1",
        projectId = projectId,
        username = "manager1",
        role = UserRole.MANAGER
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
    fun testReversalImmutabilityAndZeroNetEffect() = runBlocking {
        val expense = BusinessExpense(
            expenseId = "EXP-REV-TEST",
            tenantId = tenantId,
            projectId = projectId,
            expenseNumber = "EXP-2026-REV",
            expenseCategoryId = "CAT-001",
            amount = BigDecimal("18000.0000"),
            status = BusinessExpenseStatus.APPROVED,
            description = "Plate Making Chemical Delivery",
            createdBy = "USER-STAFF-1"
        )
        expenseRepository.createExpense(expense)

        // 1. Post approved expense (Debit 18000)
        val original = (service.postApprovedExpense(
            adminPrincipal,
            PostApprovedExpenseCommand(
                expenseId = "EXP-REV-TEST",
                accountCategory = BusinessLedgerAccountCategory.PRODUCTION_COST,
                jobId = "JOB-77"
            )
        ) as DomainResult.Success).data

        assertEquals(BigDecimal("18000.0000"), original.debitAmount)
        assertEquals(BigDecimal("0.0000"), original.creditAmount)
        assertFalse(original.isReversed)

        // 2. Perform Compensating Reversal
        val revRes = service.reversePosting(
            managerPrincipal,
            ReversePostingCommand(
                postingId = original.id,
                reason = "Vendor bill reissued under different purchase order",
                correlationId = "CORR-REV-77"
            )
        )
        assertTrue(revRes is DomainResult.Success)
        val reversalPosting = (revRes as DomainResult.Success).data

        // Reversal posting has Credit 18000, Debit 0
        assertEquals(BigDecimal("0.0000"), reversalPosting.debitAmount)
        assertEquals(BigDecimal("18000.0000"), reversalPosting.creditAmount)
        assertEquals(original.id, reversalPosting.reversalOfPostingId)
        assertEquals(BusinessLedgerPostingType.REVERSAL, reversalPosting.postingType)

        // 3. Verify original posting remains immutable in history, but marked isReversed
        val reloadedOriginal = (service.getPostingById(adminPrincipal, original.id) as DomainResult.Success).data
        assertTrue(reloadedOriginal.isReversed)
        assertEquals(BigDecimal("18000.0000"), reloadedOriginal.debitAmount)
        assertEquals("USER-ADMIN-1", reloadedOriginal.createdBy)
        assertEquals("Vendor bill reissued under different purchase order", reloadedOriginal.reversalReason)
        assertEquals("USER-MGR-1", reloadedOriginal.reversedBy)

        // 4. Zero Net Effect: Debits (18,000) - Credits (18,000) = 0.0000
        val bal = (service.getBalanceSummary(adminPrincipal) as DomainResult.Success).data
        assertEquals(BigDecimal("18000.0000"), bal.totalDebit)
        assertEquals(BigDecimal("18000.0000"), bal.totalCredit)
        assertEquals(BigDecimal("0.0000"), bal.netMovement)

        // 5. Attempting duplicate reversal fails
        val dupRevRes = service.reversePosting(
            managerPrincipal,
            ReversePostingCommand(postingId = original.id, reason = "Attempt duplicate reversal")
        )
        assertTrue(dupRevRes is DomainResult.Error)
        assertTrue((dupRevRes as DomainResult.Error).message.contains("already reversed"))

        // 6. Attempting to reverse the reversal posting fails
        val revOfRevRes = service.reversePosting(
            managerPrincipal,
            ReversePostingCommand(postingId = reversalPosting.id, reason = "Attempt reversing reversal")
        )
        assertTrue(revOfRevRes is DomainResult.Error)
        assertTrue((revOfRevRes as DomainResult.Error).message.contains("already a compensating reversal"))

        // 7. Audit trail check
        val audits = (service.getAuditTrail(adminPrincipal, postingId = original.id) as DomainResult.Success).data
        assertTrue(audits.any { it.eventType == "POSTING_REVERSED" && it.actorId == "USER-MGR-1" })
    }
}
