package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.businessreconciliation.BusinessFinancialReconciliationServiceImpl
import com.sucharu.sucharupro.domain.service.businessreconciliation.LinkCorrectionCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialAdjustmentReconciliationTest {

    private lateinit var adjDataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var adjRepository: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var reconDataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var reconRepository: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var reconService: BusinessFinancialReconciliationServiceImpl
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessFinancialAdjustmentServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val staff = AuthenticatedPrincipal(userId = "USR-STAFF", username = "staff_user", role = UserRole.STAFF, projectId = projectId)
    private val manager = AuthenticatedPrincipal(userId = "USR-MGR", username = "mgr_user", role = UserRole.MANAGER, projectId = projectId)
    private val admin = AuthenticatedPrincipal(userId = "USR-ADMIN", username = "admin_user", role = UserRole.ADMIN, projectId = projectId)

    @Before
    fun setup() {
        adjDataSource = FakeBusinessFinancialAdjustmentDataSource()
        adjRepository = BusinessFinancialAdjustmentRepositoryImpl(adjDataSource)
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        ledgerService = BusinessLedgerServiceImpl(ledgerRepository, defaultTenantId = tenantId)

        reconDataSource = FakeBusinessFinancialReconciliationDataSource()
        reconRepository = BusinessFinancialReconciliationRepositoryImpl(reconDataSource)
        reconService = BusinessFinancialReconciliationServiceImpl(
            repository = reconRepository,
            defaultTenantId = tenantId
        )

        service = BusinessFinancialAdjustmentServiceImpl(
            repository = adjRepository,
            ledgerService = ledgerService,
            reconciliationRepository = reconRepository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testReconciliationLinkageToFinancialAdjustment() = runBlocking {
        // 0. Seed a discrepancy in reconciliation repository
        reconRepository.createDiscrepancy(
            BusinessFinancialReconciliationDiscrepancy(
                id = "DISC-101",
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = "RUN-001",
                periodId = "PER-2026-08",
                discrepancyType = FinancialDiscrepancyType.AMOUNT_MISMATCH,
                severity = DiscrepancySeverity.WARNING,
                sourceType = "BUSINESS_EXPENSE",
                sourceId = "EXP-101",
                expectedAmount = BigDecimal("5000.0000"),
                actualAmount = BigDecimal("4800.0000"),
                differenceAmount = BigDecimal("-200.0000"),
                currency = "BDT",
                description = "Reconciliation rounding discrepancy",
                status = DiscrepancyStatus.OPEN
            )
        )

        // 1. Post an adjustment (created by staff, approved by manager, posted by admin)
        val createCmd = CreateAdjustmentCommand(
            adjustmentNumber = "ADJ-REC-001",
            adjustmentType = BusinessFinancialAdjustmentType.RECONCILIATION_CORRECTION,
            sourceType = AdjustmentSourceType.RECONCILIATION_DISCREPANCY,
            sourceId = "DISC-101",
            originalAmount = BigDecimal("5000.0000"),
            adjustmentAmount = BigDecimal("-200.0000"),
            reason = "Reconciliation rounding discrepancy fix",
            justification = "Compensating adjustment for reconciliation discrepancy #DISC-101",
            periodId = "PER-2026-08"
        )
        val createRes = service.createAdjustment(staff, createCmd)
        assertTrue(createRes is DomainResult.Success)
        val adj = (createRes as DomainResult.Success).data

        service.submitAdjustment(staff, SubmitAdjustmentCommand(adj.id))
        service.approveAdjustment(manager, ApproveAdjustmentCommand(adj.id))
        val postRes = service.postAdjustment(admin, PostAdjustmentCommand(adj.id))
        assertTrue(postRes is DomainResult.Success)

        // 2. Link correction in Reconciliation module
        val linkCmd = LinkCorrectionCommand(
            discrepancyId = "DISC-101",
            correctionType = "FINANCIAL_ADJUSTMENT",
            correctionId = adj.id,
            note = "Resolved via Adjustment ADJ-REC-001"
        )
        val linkRes = reconService.linkCorrection(admin, linkCmd)
        assertTrue(linkRes is DomainResult.Success)
        val updatedDisc = (linkRes as DomainResult.Success).data
        assertEquals(adj.id, updatedDisc.linkedCorrectionId)
        assertEquals("FINANCIAL_ADJUSTMENT", updatedDisc.linkedCorrectionType)
    }
}
