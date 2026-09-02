package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.data.datasource.businesscost.FakeBusinessCostManagementDataSource
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.datasource.businessexpense.FakeBusinessExpenseDataSource
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessfinancialgovernance.FakeBusinessFinancialGovernanceDataSource
import com.sucharu.sucharupro.data.datasource.businessintegrity.FakeBusinessFinancialIntegrityDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.datasource.businessreconciliation.FakeBusinessFinancialReconciliationDataSource
import com.sucharu.sucharupro.data.datasource.vendorpayable.FakeVendorPayableDataSource
import com.sucharu.sucharupro.data.repository.businesscost.BusinessCostManagementRepositoryImpl
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessexpense.BusinessExpenseRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessfinancialgovernance.BusinessFinancialGovernanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessintegrity.BusinessFinancialIntegrityRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepositoryImpl
import com.sucharu.sucharupro.data.repository.vendorpayable.VendorPayableRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriod
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriodStatus
import com.sucharu.sucharupro.domain.model.businessintegrity.PeriodCloseCertificate
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPosting
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPostingType
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerSourceType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.SourceIntegrityStatus
import com.sucharu.sucharupro.domain.service.businessintegrity.BusinessFinancialIntegrityServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialHandoffAdapterTest {

    private lateinit var integrityService: BusinessFinancialIntegrityServiceImpl
    private lateinit var handoffAdapter: Module16FinancialHandoffAdapterImpl
    private lateinit var integrityRepo: BusinessFinancialIntegrityRepositoryImpl
    private lateinit var costControlRepo: BusinessCostControlRepositoryImpl
    private lateinit var ledgerRepo: BusinessLedgerRepositoryImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"
    private val periodId = "PER-2026-M08"

    @Before
    fun setUp() = runBlocking {
        val integrityDs = FakeBusinessFinancialIntegrityDataSource()
        val expenseDs = FakeBusinessExpenseDataSource()
        val payableDs = FakeVendorPayableDataSource()
        val ledgerDs = FakeBusinessLedgerDataSource()
        val costManagementDs = FakeBusinessCostManagementDataSource()
        val costControlDs = FakeBusinessCostControlDataSource()
        val reconciliationDs = FakeBusinessFinancialReconciliationDataSource()
        val adjustmentDs = FakeBusinessFinancialAdjustmentDataSource()
        val governanceDs = FakeBusinessFinancialGovernanceDataSource()

        integrityRepo = BusinessFinancialIntegrityRepositoryImpl(integrityDs)
        val expenseRepo = BusinessExpenseRepositoryImpl(expenseDs)
        val payableRepo = VendorPayableRepositoryImpl(payableDs)
        ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDs)
        val costManagementRepo = BusinessCostManagementRepositoryImpl(costManagementDs)
        costControlRepo = BusinessCostControlRepositoryImpl(costControlDs)
        val reconciliationRepo = BusinessFinancialReconciliationRepositoryImpl(reconciliationDs)
        val adjustmentRepo = BusinessFinancialAdjustmentRepositoryImpl(adjustmentDs)
        val governanceRepo = BusinessFinancialGovernanceRepositoryImpl(governanceDs)

        // Seed Open Financial Period
        costControlRepo.createFinancialPeriod(
            BusinessFinancialPeriod(
                id = periodId,
                tenantId = tenantId,
                projectId = projectId,
                periodCode = "2026-M08",
                periodName = "August 2026",
                startDate = 1754092800000L,
                endDate = 1756771199000L,
                status = BusinessFinancialPeriodStatus.OPEN,
                createdBy = "ADMIN-1"
            )
        )

        // Seed Balanced Ledger Postings
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-1",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-001",
                postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-1",
                accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
                debitAmount = BigDecimal("5000.0000"),
                creditAmount = BigDecimal("5000.0000"),
                description = "Expense Posting",
                postingDate = 1754179200000L,
                createdBy = "ADMIN-1"
            )
        )

        integrityService = BusinessFinancialIntegrityServiceImpl(
            integrityRepository = integrityRepo,
            expenseRepository = expenseRepo,
            payableRepository = payableRepo,
            ledgerRepository = ledgerRepo,
            costManagementRepository = costManagementRepo,
            costControlRepository = costControlRepo,
            reconciliationRepository = reconciliationRepo,
            adjustmentRepository = adjustmentRepo,
            governanceRepository = governanceRepo,
            defaultTenantId = tenantId
        )

        handoffAdapter = Module16FinancialHandoffAdapterImpl(integrityService)
    }

    @Test
    fun testValidFinancialHandoffConsumption() = runBlocking {
        val result = handoffAdapter.getVerifiedFinancialHandoff(tenantId, projectId, periodId)
        assertTrue(result is DomainResult.Success)
        val handoff = (result as DomainResult.Success).data

        assertEquals(tenantId, handoff.contract.tenantId)
        assertEquals(projectId, handoff.contract.projectId)
        assertEquals(periodId, handoff.contract.periodId)
        assertTrue(handoff.isLedgerBalanced)
        assertEquals(SourceIntegrityStatus.VERIFIED, handoff.integrityStatus)
        assertFalse(handoff.isPeriodClosed)
    }

    @Test
    fun testClosedPeriodWithCertificateChecksum() = runBlocking {
        // Seed certificate
        integrityRepo.savePeriodCloseCertificate(
            PeriodCloseCertificate(
                id = "CERT-1",
                periodId = periodId,
                tenantId = tenantId,
                projectId = projectId,
                periodCode = "2026-M08",
                finalRunId = "RUN-FINAL-01",
                closedBy = "ADMIN-1",
                approvedBy = "MANAGER-1",
                certificateChecksum = "sha256_mock_checksum_valid_cert",
                snapshotPayloadJson = "{}",
                createdAt = System.currentTimeMillis()
            )
        )

        val result = handoffAdapter.getVerifiedFinancialHandoff(tenantId, projectId, periodId)
        assertTrue(result is DomainResult.Success)
        val handoff = (result as DomainResult.Success).data

        assertNotNull(handoff.contract)
    }

    @Test
    fun testUnbalancedLedgerProducesConflictStatus() = runBlocking {
        // Inject an unbalanced posting
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-UNBALANCED",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-UNBAL",
                postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-99",
                accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
                debitAmount = BigDecimal("9999.0000"),
                creditAmount = BigDecimal("0.0000"), // unbalanced!
                description = "Unbalanced manual adjustment",
                postingDate = 1754179200000L,
                createdBy = "ADMIN-1"
            )
        )

        val result = handoffAdapter.getVerifiedFinancialHandoff(tenantId, projectId, periodId)
        assertTrue(result is DomainResult.Success)
        val handoff = (result as DomainResult.Success).data

        assertFalse(handoff.isLedgerBalanced)
        assertEquals(SourceIntegrityStatus.SOURCE_CONFLICT, handoff.integrityStatus)
        assertTrue(handoff.validationNotes.any { it.contains("General Business Ledger is not balanced") })
    }

    @Test
    fun testBlankParametersValidation() = runBlocking {
        val res1 = handoffAdapter.getVerifiedFinancialHandoff("", projectId, periodId)
        assertTrue(res1 is DomainResult.Error)

        val res2 = handoffAdapter.getVerifiedFinancialHandoff(tenantId, "", periodId)
        assertTrue(res2 is DomainResult.Error)

        val res3 = handoffAdapter.getVerifiedFinancialHandoff(tenantId, projectId, "")
        assertTrue(res3 is DomainResult.Error)
    }
}
