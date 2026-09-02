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

class BusinessFinancialAdjustmentAuditTest {

    private lateinit var adjDataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var adjRepository: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessFinancialAdjustmentServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val admin = AuthenticatedPrincipal(userId = "USR-ADMIN", username = "admin_user", role = UserRole.ADMIN, projectId = projectId)

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
    fun testEveryLifecycleTransitionGeneratesAuditEvent() = runBlocking {
        // 1. Create
        val createCmd = CreateAdjustmentCommand(
            adjustmentNumber = "ADJ-AUD-001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-999",
            originalAmount = BigDecimal("5000.0000"),
            adjustmentAmount = BigDecimal("-500.0000"),
            reason = "Audit test",
            justification = "Justification for audit event verification",
            periodId = "PER-2026-08"
        )
        val createRes = service.createAdjustment(admin, createCmd)
        val adj = (createRes as DomainResult.Success).data

        // 2. Submit
        service.submitAdjustment(admin, SubmitAdjustmentCommand(adj.id))

        // 3. Review (different user)
        val mgr = AuthenticatedPrincipal(userId = "USR-MGR", username = "mgr", role = UserRole.MANAGER, projectId = projectId)
        service.reviewAdjustment(mgr, ReviewAdjustmentCommand(adj.id))

        // 4. Approve
        service.approveAdjustment(mgr, ApproveAdjustmentCommand(adj.id))

        // 5. Post
        service.postAdjustment(admin, PostAdjustmentCommand(adj.id))

        // Verify audit trail
        val auditRes = service.listAuditEvents(admin, entityId = adj.id)
        assertTrue(auditRes is DomainResult.Success)
        val events = (auditRes as DomainResult.Success).data
        assertEquals(5, events.size)

        val eventTypes = events.map { it.eventType }
        assertEquals(listOf("CREATED", "SUBMITTED", "REVIEW_STARTED", "APPROVED", "POSTED"), eventTypes)
    }
}
