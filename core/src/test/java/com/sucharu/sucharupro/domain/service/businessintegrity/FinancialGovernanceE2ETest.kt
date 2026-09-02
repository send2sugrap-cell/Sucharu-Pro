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
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.AdjustmentSourceType
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.AdjustmentStatus
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.BusinessFinancialAdjustment
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.BusinessFinancialAdjustmentType
import com.sucharu.sucharupro.domain.model.businessintegrity.FinancialIntegrityStatus
import com.sucharu.sucharupro.domain.model.businessintegrity.PeriodClosureStatus
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPosting
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPostingType
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerSourceType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayable
import com.sucharu.sucharupro.domain.model.vendorpayable.VendorPayableStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialGovernanceE2ETest {

    private lateinit var integrityRepo: BusinessFinancialIntegrityRepositoryImpl
    private lateinit var expenseRepo: BusinessExpenseRepositoryImpl
    private lateinit var payableRepo: VendorPayableRepositoryImpl
    private lateinit var ledgerRepo: BusinessLedgerRepositoryImpl
    private lateinit var costControlRepo: BusinessCostControlRepositoryImpl
    private lateinit var adjustmentRepo: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var integrityService: BusinessFinancialIntegrityService

    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"
    private val periodId = "PER-2026-M08"

    @Before
    fun setUp() = runBlocking {
        integrityRepo = BusinessFinancialIntegrityRepositoryImpl(FakeBusinessFinancialIntegrityDataSource())
        expenseRepo = BusinessExpenseRepositoryImpl(FakeBusinessExpenseDataSource())
        payableRepo = VendorPayableRepositoryImpl(FakeVendorPayableDataSource())
        ledgerRepo = BusinessLedgerRepositoryImpl(FakeBusinessLedgerDataSource())
        val costManagementRepo = BusinessCostManagementRepositoryImpl(FakeBusinessCostManagementDataSource())
        costControlRepo = BusinessCostControlRepositoryImpl(FakeBusinessCostControlDataSource())
        val reconciliationRepo = BusinessFinancialReconciliationRepositoryImpl(FakeBusinessFinancialReconciliationDataSource())
        adjustmentRepo = BusinessFinancialAdjustmentRepositoryImpl(FakeBusinessFinancialAdjustmentDataSource())
        val governanceRepo = BusinessFinancialGovernanceRepositoryImpl(FakeBusinessFinancialGovernanceDataSource())

        // Step 1: Initialize open financial period
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
    fun `realistic 18-step E2E financial scenario executes and achieves cryptographic period closure`() = runBlocking {
        // Step 2: Create & approve business expense
        val expense = expenseRepo.createExpense(
            BusinessExpense(
                expenseId = "EXP-101",
                tenantId = tenantId,
                projectId = projectId,
                expenseNumber = "EXP-101",
                expenseCategoryId = "CAT-IT",
                amount = BigDecimal("10000.0000"),
                currency = "BDT",
                paymentMethod = BusinessExpensePaymentMethod.BANK,
                status = BusinessExpenseStatus.POSTABLE,
                description = "Cloud Infrastructure",
                createdBy = "USER-1",
                approvedBy = "MANAGER-1"
            )
        )
        assertTrue(expense is DomainResult.Success)

        // Step 3: Create vendor payable & settle it
        val payable = payableRepo.createPayable(
            VendorPayable(
                payableId = "PAY-201",
                tenantId = tenantId,
                projectId = projectId,
                vendorId = "VEND-1",
                payableNumber = "PAY-201",
                description = "Hardware Servers",
                originalAmount = BigDecimal("5000.0000"),
                paidAmount = BigDecimal("5000.0000"),
                currency = "BDT",
                issueDate = 1000L,
                dueDate = 2000L,
                status = VendorPayableStatus.PAID,
                createdBy = "USER-1"
            )
        )
        assertTrue(payable is DomainResult.Success)

        // Step 4: Record balanced double-entry ledger postings for expense and payable
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-101",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-101",
                postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-101",
                accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
                debitAmount = BigDecimal("10000.0000"),
                creditAmount = BigDecimal.ZERO,
                currency = "BDT",
                description = "Cloud infrastructure expense",
                createdBy = "ADMIN-1"
            )
        )
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-102",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-102",
                postingType = BusinessLedgerPostingType.EXPENSE_PAYMENT,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-101",
                accountCategory = BusinessLedgerAccountCategory.BANK,
                debitAmount = BigDecimal.ZERO,
                creditAmount = BigDecimal("10000.0000"),
                currency = "BDT",
                description = "Expense settlement",
                createdBy = "ADMIN-1"
            )
        )
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-103",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-103",
                postingType = BusinessLedgerPostingType.VENDOR_LIABILITY_RECOGNITION,
                sourceType = BusinessLedgerSourceType.VENDOR_PAYABLE,
                sourceId = "PAY-201",
                accountCategory = BusinessLedgerAccountCategory.VENDOR_COST,
                debitAmount = BigDecimal("5000.0000"),
                creditAmount = BigDecimal.ZERO,
                currency = "BDT",
                description = "Vendor liability recognition",
                createdBy = "ADMIN-1"
            )
        )
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-104",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-104",
                postingType = BusinessLedgerPostingType.VENDOR_PAYMENT,
                sourceType = BusinessLedgerSourceType.VENDOR_PAYABLE,
                sourceId = "PAY-201",
                accountCategory = BusinessLedgerAccountCategory.BANK,
                debitAmount = BigDecimal.ZERO,
                creditAmount = BigDecimal("5000.0000"),
                currency = "BDT",
                description = "Vendor settlement",
                createdBy = "ADMIN-1"
            )
        )

        // Step 5: Post a financial adjustment
        adjustmentRepo.saveAdjustment(
            BusinessFinancialAdjustment(
                id = "ADJ-301",
                tenantId = tenantId,
                projectId = projectId,
                adjustmentNumber = "ADJ-301",
                adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
                sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-101",
                reason = "Vendor rebate correction",
                justification = "Rebate credit memo received",
                adjustmentAmount = BigDecimal("500.0000"),
                periodId = periodId,
                status = AdjustmentStatus.POSTED,
                createdBy = "USER-1"
            )
        )

        // Step 6: Execute Canonical Financial Integrity Control Run (18 assertions)
        val runRes = integrityService.executeIntegrityRun(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            actorId = "AUDITOR-1",
            actorRole = "AUDITOR",
            notes = "Module 15 Step 10 Comprehensive E2E Integrity Audit"
        )
        assertTrue(runRes is DomainResult.Success)
        val run = (runRes as DomainResult.Success).data
        assertEquals(FinancialIntegrityStatus.PASSED, run.status)
        assertEquals(18, run.totalAssertionsCount)
        assertEquals(18, run.passedAssertionsCount)
        assertEquals(0, run.failedAssertionsCount)

        // Step 7: Check Period Finalization Readiness
        val readinessRes = integrityService.evaluatePeriodFinalizationReadiness(tenantId, projectId, periodId)
        assertTrue(readinessRes is DomainResult.Success)
        val readiness = (readinessRes as DomainResult.Success).data
        assertTrue(readiness.isReadyForClose)
        assertEquals(PeriodClosureStatus.READY, readiness.status)

        // Step 8: Finalize Period Close with Separation of Duties
        val certRes = integrityService.finalizePeriodClose(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            actorId = "FINANCE-DIRECTOR-1",
            actorRole = "ADMIN",
            requesterId = "FINANCE-STAFF-1",
            notes = "Formal period closure certificate"
        )
        assertTrue(certRes is DomainResult.Success)
        val cert = (certRes as DomainResult.Success).data
        assertEquals("FINALIZED", cert.status)
        assertEquals(BigDecimal("10000.0000"), cert.totalRecognizedExpenses)
        assertEquals(BigDecimal("5000.0000"), cert.totalSettledPayables)
        assertEquals(BigDecimal("15000.0000"), cert.totalLedgerDebit)
        assertEquals(BigDecimal("15000.0000"), cert.totalLedgerCredit)
        assertTrue(cert.certificateChecksum.isNotBlank())

        // Step 9: Verify Period Hard-Close lock in Cost Control
        val periodAfter = costControlRepo.findFinancialPeriodById(periodId, tenantId, projectId)
        assertNotNull(periodAfter)
        assertEquals(BusinessFinancialPeriodStatus.CLOSED, periodAfter!!.status)

        // Step 10: Generate Module 16 Handoff Contract
        val handoffRes = integrityService.generateModule16HandoffContract(tenantId, projectId, periodId)
        assertTrue(handoffRes is DomainResult.Success)
        val handoff = (handoffRes as DomainResult.Success).data
        assertTrue(handoff.isPeriodClosed)
        assertEquals(cert.certificateChecksum, handoff.closureCertificateChecksum)
        assertEquals(BigDecimal("10000.0000"), handoff.totalDirectExpenses)
        assertEquals(BigDecimal("5000.0000"), handoff.totalVendorPayablesSettled)
        assertEquals(BigDecimal("500.0000"), handoff.netFinancialAdjustments)
        assertTrue(handoff.isLedgerBalanced)
    }
}
