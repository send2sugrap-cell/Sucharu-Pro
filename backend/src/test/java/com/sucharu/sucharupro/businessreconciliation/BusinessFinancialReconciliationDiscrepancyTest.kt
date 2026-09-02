package com.sucharu.sucharupro.businessreconciliation

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessreconciliation.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessreconciliation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialReconciliationDiscrepancyTest {

    private lateinit var dataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var repository: BusinessFinancialReconciliationRepositoryImpl
    private lateinit var service: BusinessFinancialReconciliationServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val staff = AuthenticatedPrincipal(userId = "USR-STAFF", username = "staff_user", role = UserRole.STAFF, projectId = projectId)
    private val admin = AuthenticatedPrincipal(userId = "USR-ADMIN", username = "admin_user", role = UserRole.ADMIN, projectId = projectId)

    @Before
    fun setup() {
        dataSource = FakeBusinessFinancialReconciliationDataSource()
        repository = BusinessFinancialReconciliationRepositoryImpl(dataSource)
        service = BusinessFinancialReconciliationServiceImpl(
            repository = repository,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testDiscrepancyRejectionAndLinkingCorrection() = runBlocking {
        val run = repository.createRun(
            BusinessFinancialReconciliationRun(
                id = "RUN-DISC-01",
                tenantId = tenantId,
                projectId = projectId,
                periodId = "PER-2026-08",
                runNumber = "RUN-DISC-01",
                createdBy = "USR-STAFF",
                checksum = "chk"
            )
        )

        val disc = repository.createDiscrepancy(
            BusinessFinancialReconciliationDiscrepancy(
                id = "DISC-01",
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = run.id,
                periodId = "PER-2026-08",
                discrepancyType = FinancialDiscrepancyType.BALANCE_MISMATCH,
                severity = DiscrepancySeverity.CRITICAL,
                sourceType = "PAYABLE",
                sourceId = "PAY-01",
                expectedAmount = BigDecimal("10000.0000"),
                actualAmount = BigDecimal("8000.0000"),
                differenceAmount = BigDecimal("2000.0000"),
                description = "Payable balance mismatch"
            )
        )

        // 1. Link Correction
        val linkCmd = LinkCorrectionCommand(
            discrepancyId = disc.id,
            correctionType = "BUSINESS_LEDGER_POSTING",
            correctionId = "BLP-CORR-01",
            note = "Compensating adjusting ledger entry created"
        )
        val linkRes = service.linkCorrection(staff, linkCmd)
        assertTrue(linkRes is DomainResult.Success)
        val linkedDisc = (linkRes as DomainResult.Success).data
        assertEquals("BUSINESS_LEDGER_POSTING", linkedDisc.linkedCorrectionType)
        assertEquals("BLP-CORR-01", linkedDisc.linkedCorrectionId)

        // 2. Reject Discrepancy
        val rejCmd = RejectDiscrepancyCommand(
            discrepancyId = disc.id,
            rejectionReason = "Invalid discrepancy triggered due to concurrent test mock setup"
        )
        val rejRes = service.rejectDiscrepancy(admin, rejCmd)
        assertTrue(rejRes is DomainResult.Success)
        val rejectedDisc = (rejRes as DomainResult.Success).data
        assertEquals(DiscrepancyStatus.REJECTED, rejectedDisc.status)
    }
}
