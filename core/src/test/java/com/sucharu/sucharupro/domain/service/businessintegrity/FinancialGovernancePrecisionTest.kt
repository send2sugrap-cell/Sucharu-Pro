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
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPosting
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerPostingType
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerSourceType
import com.sucharu.sucharupro.domain.model.common.DomainResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class FinancialGovernancePrecisionTest {

    private lateinit var integrityService: BusinessFinancialIntegrityService

    @Before
    fun setUp() = runBlocking {
        val integrityRepo = BusinessFinancialIntegrityRepositoryImpl(FakeBusinessFinancialIntegrityDataSource())
        val expenseRepo = BusinessExpenseRepositoryImpl(FakeBusinessExpenseDataSource())
        val payableRepo = VendorPayableRepositoryImpl(FakeVendorPayableDataSource())
        val ledgerRepo = BusinessLedgerRepositoryImpl(FakeBusinessLedgerDataSource())
        val costManagementRepo = BusinessCostManagementRepositoryImpl(FakeBusinessCostManagementDataSource())
        val costControlRepo = BusinessCostControlRepositoryImpl(FakeBusinessCostControlDataSource())
        val reconciliationRepo = BusinessFinancialReconciliationRepositoryImpl(FakeBusinessFinancialReconciliationDataSource())
        val adjustmentRepo = BusinessFinancialAdjustmentRepositoryImpl(FakeBusinessFinancialAdjustmentDataSource())
        val governanceRepo = BusinessFinancialGovernanceRepositoryImpl(FakeBusinessFinancialGovernanceDataSource())

        costControlRepo.createFinancialPeriod(
            BusinessFinancialPeriod(
                id = "PER-2026-M08",
                tenantId = "TENANT-001",
                projectId = "PROJ-101",
                periodCode = "2026-M08",
                periodName = "August 2026",
                startDate = 1754092800000L,
                endDate = 1756771199000L,
                status = BusinessFinancialPeriodStatus.OPEN,
                createdBy = "ADMIN-1"
            )
        )

        // Balanced fractional amounts with 4 decimal places
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-1",
                tenantId = "TENANT-001",
                projectId = "PROJ-101",
                postingNumber = "PN-001",
                postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-1",
                accountCategory = BusinessLedgerAccountCategory.OFFICE_EXPENSE,
                debitAmount = BigDecimal("1234.5678"),
                creditAmount = BigDecimal.ZERO,
                currency = "BDT",
                description = "Fractional Precision Expense",
                createdBy = "USER-1"
            )
        )
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-2",
                tenantId = "TENANT-001",
                projectId = "PROJ-101",
                postingNumber = "PN-002",
                postingType = BusinessLedgerPostingType.EXPENSE_PAYMENT,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-1",
                accountCategory = BusinessLedgerAccountCategory.CASH,
                debitAmount = BigDecimal.ZERO,
                creditAmount = BigDecimal("1234.5678"),
                currency = "BDT",
                description = "Fractional Cash",
                createdBy = "USER-1"
            )
        )

        expenseRepo.createExpense(
            BusinessExpense(
                expenseId = "EXP-1",
                tenantId = "TENANT-001",
                projectId = "PROJ-101",
                expenseNumber = "EXP-001",
                expenseCategoryId = "CAT-GEN",
                amount = BigDecimal("1234.5678"),
                currency = "BDT",
                paymentMethod = BusinessExpensePaymentMethod.CASH,
                status = BusinessExpenseStatus.POSTABLE,
                description = "Fractional Precision Expense",
                createdBy = "USER-1"
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
            defaultTenantId = "TENANT-001"
        )
    }

    @Test
    fun `precision arithmetic maintains exact 4 decimal precision without rounding drift`() = runBlocking {
        integrityService.executeIntegrityRun("TENANT-001", "PROJ-101", "PER-2026-M08", "ADMIN-1", "ADMIN")

        val certRes = integrityService.finalizePeriodClose(
            tenantId = "TENANT-001",
            projectId = "PROJ-101",
            periodId = "PER-2026-M08",
            actorId = "MANAGER-1",
            actorRole = "MANAGER",
            requesterId = "USER-1"
        )

        assertTrue(certRes is DomainResult.Success)
        val cert = (certRes as DomainResult.Success).data

        assertEquals(BigDecimal("1234.5678"), cert.totalRecognizedExpenses)
        assertEquals(BigDecimal("1234.5678"), cert.totalLedgerDebit)
        assertEquals(BigDecimal("1234.5678"), cert.totalLedgerCredit)
    }
}
