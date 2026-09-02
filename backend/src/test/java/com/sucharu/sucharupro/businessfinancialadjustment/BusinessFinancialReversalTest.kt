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

class BusinessFinancialReversalTest {

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
    fun testReversalOfPostedAdjustment() = runBlocking {
        // 1. Create, approve and post an adjustment
        val createCmd = CreateAdjustmentCommand(
            adjustmentNumber = "ADJ-REV-001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-301",
            originalAmount = BigDecimal("10000.0000"),
            adjustmentAmount = BigDecimal("-1000.0000"),
            reason = "Erroneous discount entered",
            justification = "Justification text longer than ten characters for discount",
            periodId = "PER-2026-08"
        )
        val createRes = service.createAdjustment(accounts, createCmd)
        val adj = (createRes as DomainResult.Success).data

        service.submitAdjustment(accounts, SubmitAdjustmentCommand(adj.id))
        service.approveAdjustment(manager, ApproveAdjustmentCommand(adj.id))
        service.postAdjustment(manager, PostAdjustmentCommand(adj.id))

        // 2. Reverse posted adjustment
        val revCmd = ReverseAdjustmentCommand(
            adjustmentId = adj.id,
            reason = "Adjustment was posted in error due to wrong vendor credit note"
        )
        val revRes = service.reverseAdjustment(manager, revCmd)
        assertTrue(revRes is DomainResult.Success)
        val reversedAdj = (revRes as DomainResult.Success).data
        assertEquals(AdjustmentStatus.REVERSED, reversedAdj.status)
        assertNotNull(reversedAdj.reversingPostingId)
    }
}
