package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialAdjustmentServiceTest {

    private lateinit var adjDataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var adjRepository: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessFinancialAdjustmentServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val admin = AuthenticatedPrincipal(userId = "USR-ADMIN", username = "admin_user", role = UserRole.ADMIN, projectId = projectId)
    private val manager = AuthenticatedPrincipal(userId = "USR-MGR", username = "mgr_user", role = UserRole.MANAGER, projectId = projectId)
    private val accounts = AuthenticatedPrincipal(userId = "USR-ACC", username = "acc_user", role = UserRole.STAFF, projectId = projectId)

    @Before
    fun setup() {
        adjDataSource = FakeBusinessFinancialAdjustmentDataSource()
        adjRepository = BusinessFinancialAdjustmentRepositoryImpl(adjDataSource)
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        ledgerService = BusinessLedgerServiceImpl(ledgerRepository, defaultTenantId = tenantId)

        service = BusinessFinancialAdjustmentServiceImpl(
            repository = adjRepository,
            ledgerService = ledgerService,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testFullAdjustmentLifecycle() = runBlocking {
        // 1. Create adjustment by accounts
        val createCmd = CreateAdjustmentCommand(
            adjustmentNumber = "ADJ-2026-001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-101",
            originalAmount = BigDecimal("10000.0000"),
            adjustmentAmount = BigDecimal("-1000.0000"),
            reason = "Vendor post-invoice rebate",
            justification = "Vendor credit note #CN-998 applied to reduce effective cost",
            periodId = "PER-2026-08"
        )
        val createRes = service.createAdjustment(accounts, createCmd)
        assertTrue(createRes is DomainResult.Success)
        val adj = (createRes as DomainResult.Success).data
        assertEquals(AdjustmentStatus.DRAFT, adj.status)
        assertEquals(BigDecimal("9000.0000"), adj.effectiveAmount)

        // 2. Submit adjustment
        val subRes = service.submitAdjustment(accounts, SubmitAdjustmentCommand(adj.id))
        assertTrue(subRes is DomainResult.Success)
        assertEquals(AdjustmentStatus.SUBMITTED, (subRes as DomainResult.Success).data.status)

        // 3. Review adjustment by accounts (different user or manager)
        val revRes = service.reviewAdjustment(manager, ReviewAdjustmentCommand(adj.id))
        assertTrue(revRes is DomainResult.Success)
        assertEquals(AdjustmentStatus.UNDER_REVIEW, (revRes as DomainResult.Success).data.status)

        // 4. Approve adjustment by manager (SoD: creator is accounts, approver is manager)
        val appRes = service.approveAdjustment(manager, ApproveAdjustmentCommand(adj.id))
        assertTrue(appRes is DomainResult.Success)
        assertEquals(AdjustmentStatus.APPROVED, (appRes as DomainResult.Success).data.status)

        // 5. Post adjustment to Business Ledger
        val postRes = service.postAdjustment(manager, PostAdjustmentCommand(adj.id))
        assertTrue(postRes is DomainResult.Success)
        val postedAdj = (postRes as DomainResult.Success).data
        assertEquals(AdjustmentStatus.POSTED, postedAdj.status)
        assertNotNull(postedAdj.ledgerPostingId)

        // Verify Compensating Posting Record
        val postings = adjRepository.listPostingsByAdjustmentId(adj.id, tenantId, projectId)
        assertEquals(1, postings.size)
        assertEquals(BigDecimal("1000.0000"), postings[0].amount)
    }

    @Test
    fun testSeparationOfDutiesViolation() = runBlocking {
        // Create by manager
        val createCmd = CreateAdjustmentCommand(
            adjustmentNumber = "ADJ-2026-002",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-102",
            originalAmount = BigDecimal("5000.0000"),
            adjustmentAmount = BigDecimal("-500.0000"),
            reason = "Correction",
            justification = "Justification text longer than ten characters",
            periodId = "PER-2026-08"
        )
        val createRes = service.createAdjustment(manager, createCmd)
        val adj = (createRes as DomainResult.Success).data

        service.submitAdjustment(manager, SubmitAdjustmentCommand(adj.id))

        // Same manager tries to approve -> must fail SoD!
        val appRes = service.approveAdjustment(manager, ApproveAdjustmentCommand(adj.id))
        assertTrue(appRes is DomainResult.Error)
        assertTrue((appRes as DomainResult.Error).message.contains("Separation of Duties"))
    }
}
