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
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.validation.businessintegrity.BusinessFinancialIntegrityValidator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FinancialGovernanceSecurityTest {

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
    fun `finalizePeriodClose rejects non-admin and non-manager roles`() = runBlocking {
        integrityService.executeIntegrityRun("TENANT-001", "PROJ-101", "PER-2026-M08", "ADMIN-1", "ADMIN")

        val res = integrityService.finalizePeriodClose(
            tenantId = "TENANT-001",
            projectId = "PROJ-101",
            periodId = "PER-2026-M08",
            actorId = "STAFF-1",
            actorRole = "STAFF", // STAFF cannot finalize period!
            requesterId = "USER-2"
        )

        assertTrue(res is DomainResult.Error)
        val error = res as DomainResult.Error
        assertTrue(error.message.contains("Only ADMIN or MANAGER roles are authorized"))
    }

    @Test
    fun `validateRunCreation rejects blank tenantId or projectId`() {
        val res1 = BusinessFinancialIntegrityValidator.validateRunCreation("", "PROJ-101", "PER-1", "RUN-1", "ACTOR-1")
        assertTrue(res1 is DomainResult.Error)

        val res2 = BusinessFinancialIntegrityValidator.validateRunCreation("TENANT-1", "", "PER-1", "RUN-1", "ACTOR-1")
        assertTrue(res2 is DomainResult.Error)
    }
}
