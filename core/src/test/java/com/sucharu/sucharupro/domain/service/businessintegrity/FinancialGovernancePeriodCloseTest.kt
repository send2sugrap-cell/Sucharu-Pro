package com.sucharu.sucharupro.domain.service.businessintegrity

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
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpense
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpensePaymentMethod
import com.sucharu.sucharupro.domain.model.businessexpense.BusinessExpenseStatus
import com.sucharu.sucharupro.domain.model.businessintegrity.PeriodClosureStatus
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPosting
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPostingType
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerSourceType
import com.sucharu.sucharupro.domain.model.businessreconciliation.BusinessFinancialReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.businessreconciliation.DiscrepancySeverity
import com.sucharu.sucharupro.domain.model.businessreconciliation.DiscrepancyStatus
import com.sucharu.sucharupro.domain.model.businessreconciliation.FinancialDiscrepancyType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialGovernancePeriodCloseTest {

    private lateinit var integrityDataSource: FakeBusinessFinancialIntegrityDataSource
    private lateinit var expenseDataSource: FakeBusinessExpenseDataSource
    private lateinit var payableDataSource: FakeVendorPayableDataSource
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var costControlDataSource: FakeBusinessCostControlDataSource
    private lateinit var reconciliationDataSource: FakeBusinessFinancialReconciliationDataSource
    private lateinit var adjustmentDataSource: FakeBusinessFinancialAdjustmentDataSource

    private lateinit var expenseRepo: BusinessExpenseRepositoryImpl
    private lateinit var ledgerRepo: BusinessLedgerRepositoryImpl
    private lateinit var costControlRepo: BusinessCostControlRepositoryImpl
    private lateinit var reconciliationRepo: BusinessFinancialReconciliationRepositoryImpl

    private lateinit var integrityService: BusinessFinancialIntegrityService

    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"
    private val periodId = "PER-2026-M08"

    @Before
    fun setUp() = runBlocking {
        integrityDataSource = FakeBusinessFinancialIntegrityDataSource()
        expenseDataSource = FakeBusinessExpenseDataSource()
        payableDataSource = FakeVendorPayableDataSource()
        ledgerDataSource = FakeBusinessLedgerDataSource()
        val costManagementDataSource = FakeBusinessCostManagementDataSource()
        costControlDataSource = FakeBusinessCostControlDataSource()
        reconciliationDataSource = FakeBusinessFinancialReconciliationDataSource()
        adjustmentDataSource = FakeBusinessFinancialAdjustmentDataSource()
        val governanceDataSource = FakeBusinessFinancialGovernanceDataSource()

        val integrityRepo = BusinessFinancialIntegrityRepositoryImpl(integrityDataSource)
        expenseRepo = BusinessExpenseRepositoryImpl(expenseDataSource)
        val payableRepo = VendorPayableRepositoryImpl(payableDataSource)
        ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDataSource)
        val costManagementRepo = BusinessCostManagementRepositoryImpl(costManagementDataSource)
        costControlRepo = BusinessCostControlRepositoryImpl(costControlDataSource)
        reconciliationRepo = BusinessFinancialReconciliationRepositoryImpl(reconciliationDataSource)
        val adjustmentRepo = BusinessFinancialAdjustmentRepositoryImpl(adjustmentDataSource)
        val governanceRepo = BusinessFinancialGovernanceRepositoryImpl(governanceDataSource)

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
    }

    @Test
    fun `readiness is BLOCKED when approved expenses are unposted to ledger`() = runBlocking {
        expenseRepo.createExpense(
            BusinessExpense(
                expenseId = "EXP-UNPOSTED",
                tenantId = tenantId,
                projectId = projectId,
                expenseNumber = "EXP-002",
                expenseCategoryId = "CAT-GEN",
                amount = BigDecimal("1500.0000"),
                currency = "BDT",
                paymentMethod = BusinessExpensePaymentMethod.CASH,
                status = BusinessExpenseStatus.APPROVED, // Approved but not posted!
                description = "Pending Posting Expense",
                createdBy = "USER-1",
                approvedBy = "ADMIN-1"
            )
        )

        val readinessRes = integrityService.evaluatePeriodFinalizationReadiness(tenantId, projectId, periodId)
        assertTrue(readinessRes is DomainResult.Success)
        val readiness = (readinessRes as DomainResult.Success).data

        assertFalse(readiness.isReadyForClose)
        assertEquals(PeriodClosureStatus.BLOCKED, readiness.status)
        assertTrue(readiness.blockingReasons.any { it.contains("approved business expenses have not been posted") })
    }

    @Test
    fun `readiness is BLOCKED when ledger debits and credits are not equal`() = runBlocking {
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-UNBALANCED",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-UNBAL",
                postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-1",
                accountCategory = BusinessLedgerAccountCategory.OFFICE_EXPENSE,
                debitAmount = BigDecimal("3000.0000"),
                creditAmount = BigDecimal.ZERO,
                currency = "BDT",
                description = "Unbalanced Posting",
                createdBy = "USER-1"
            )
        )

        val readinessRes = integrityService.evaluatePeriodFinalizationReadiness(tenantId, projectId, periodId)
        assertTrue(readinessRes is DomainResult.Success)
        val readiness = (readinessRes as DomainResult.Success).data

        assertFalse(readiness.isReadyForClose)
        assertEquals(PeriodClosureStatus.BLOCKED, readiness.status)
        assertTrue(readiness.blockingReasons.any { it.contains("Business Ledger is not balanced") })
    }

    @Test
    fun `readiness is BLOCKED when unresolved critical reconciliation discrepancies exist`() = runBlocking {
        reconciliationRepo.createDiscrepancy(
            BusinessFinancialReconciliationDiscrepancy(
                id = "DISC-1",
                tenantId = tenantId,
                projectId = projectId,
                reconciliationRunId = "RUN-1",
                periodId = periodId,
                discrepancyType = FinancialDiscrepancyType.PAYABLE_WITHOUT_LIABILITY_POSTING,
                severity = DiscrepancySeverity.CRITICAL,
                sourceType = "PAYABLE",
                sourceId = "PAY-99",
                expectedAmount = BigDecimal("5000.0000"),
                actualAmount = BigDecimal.ZERO,
                differenceAmount = BigDecimal("5000.0000"),
                description = "Critical missing posting",
                status = DiscrepancyStatus.OPEN
            )
        )

        val readinessRes = integrityService.evaluatePeriodFinalizationReadiness(tenantId, projectId, periodId)
        assertTrue(readinessRes is DomainResult.Success)
        val readiness = (readinessRes as DomainResult.Success).data

        assertFalse(readiness.isReadyForClose)
        assertEquals(PeriodClosureStatus.BLOCKED, readiness.status)
        assertTrue(readiness.blockingReasons.any { it.contains("CRITICAL financial reconciliation discrepancies remain unresolved") })
    }

    @Test
    fun `finalizePeriodClose rejects self-approval by same requester (SoD)`() = runBlocking {
        integrityService.executeIntegrityRun(tenantId, projectId, periodId, "ADMIN-1", "ADMIN")

        val finalizeRes = integrityService.finalizePeriodClose(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            actorId = "ADMIN-1",
            actorRole = "ADMIN",
            requesterId = "ADMIN-1" // Same actor as requester -> SoD violation!
        )

        assertTrue(finalizeRes is DomainResult.Error)
        val error = finalizeRes as DomainResult.Error
        assertTrue(error.message.contains("Separation of Duties violation"))
    }
}
