package com.sucharu.sucharupro.domain.service.businessintegrity

import com.sucharu.sucharupro.data.api.model.businessintegrity.*
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
import com.sucharu.sucharupro.domain.model.businessintegrity.*
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

class FinancialGovernanceApiTest {

    private lateinit var integrityService: BusinessFinancialIntegrityService
    private val tenantId = "TENANT-001"
    private val projectId = "PROJ-101"
    private val periodId = "PER-2026-M08"

    @Before
    fun setUp() = runBlocking {
        val fakeIntegrityDs = FakeBusinessFinancialIntegrityDataSource()
        val fakeCostControlDs = FakeBusinessCostControlDataSource()
        val fakeLedgerDs = FakeBusinessLedgerDataSource()

        val integrityRepo = BusinessFinancialIntegrityRepositoryImpl(fakeIntegrityDs)
        val costControlRepo = BusinessCostControlRepositoryImpl(fakeCostControlDs)
        val ledgerRepo = BusinessLedgerRepositoryImpl(fakeLedgerDs)
        val expenseRepo = BusinessExpenseRepositoryImpl(FakeBusinessExpenseDataSource())
        val payableRepo = VendorPayableRepositoryImpl(FakeVendorPayableDataSource())
        val costManagementRepo = BusinessCostManagementRepositoryImpl(FakeBusinessCostManagementDataSource())
        val reconciliationRepo = BusinessFinancialReconciliationRepositoryImpl(FakeBusinessFinancialReconciliationDataSource())
        val adjustmentRepo = BusinessFinancialAdjustmentRepositoryImpl(FakeBusinessFinancialAdjustmentDataSource())
        val governanceRepo = BusinessFinancialGovernanceRepositoryImpl(FakeBusinessFinancialGovernanceDataSource())

        // Seed period & balanced postings
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
                createdBy = "admin-1"
            )
        )

        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-1",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-1",
                postingType = BusinessLedgerPostingType.EXPENSE_RECOGNITION,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-1",
                accountCategory = BusinessLedgerAccountCategory.OFFICE_EXPENSE,
                debitAmount = BigDecimal("5000.0000"),
                creditAmount = BigDecimal.ZERO,
                currency = "BDT",
                description = "Office expense",
                createdBy = "admin-1"
            )
        )
        ledgerRepo.createPosting(
            BusinessLedgerPosting(
                id = "POST-2",
                tenantId = tenantId,
                projectId = projectId,
                postingNumber = "PN-2",
                postingType = BusinessLedgerPostingType.EXPENSE_PAYMENT,
                sourceType = BusinessLedgerSourceType.BUSINESS_EXPENSE,
                sourceId = "EXP-1",
                accountCategory = BusinessLedgerAccountCategory.CASH,
                debitAmount = BigDecimal.ZERO,
                creditAmount = BigDecimal("5000.0000"),
                currency = "BDT",
                description = "Cash settlement",
                createdBy = "admin-1"
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
    fun `test execute-run DTO workflow executes all 18 assertions`() = runBlocking {
        val requestDto = ExecuteIntegrityRunRequestDto(
            periodId = periodId,
            notes = "Automated test execution",
            idempotencyKey = "KEY-001"
        )

        val runRes = integrityService.executeIntegrityRun(
            tenantId = tenantId,
            projectId = projectId,
            periodId = requestDto.periodId,
            actorId = "ADMIN-1",
            actorRole = "ADMIN",
            notes = requestDto.notes,
            idempotencyKey = requestDto.idempotencyKey
        )

        assertTrue(runRes is DomainResult.Success)
        val run = (runRes as DomainResult.Success).data
        val dto = FinancialIntegrityRunDto.fromDomain(run)

        assertEquals(18, dto.totalAssertionsCount)
        assertEquals(FinancialIntegrityStatus.PASSED.name, dto.status)
        assertEquals(18, dto.assertions.size)
        assertTrue(dto.integrityChecksum.isNotBlank())
    }

    @Test
    fun `test readiness DTO mapping returns period closure status`() = runBlocking {
        integrityService.executeIntegrityRun(tenantId, projectId, periodId, "ADMIN-1", "ADMIN")

        val readinessRes = integrityService.evaluatePeriodFinalizationReadiness(tenantId, projectId, periodId)
        assertTrue(readinessRes is DomainResult.Success)
        val readiness = (readinessRes as DomainResult.Success).data
        val dto = PeriodFinalizationReadinessDto.fromDomain(readiness)

        assertTrue(dto.isReadyForClose)
        assertEquals(PeriodClosureStatus.READY.name, dto.status)
        assertTrue(dto.blockingReasons.isEmpty())
    }

    @Test
    fun `test finalize-period-close DTO mapping generates certificate`() = runBlocking {
        integrityService.executeIntegrityRun(tenantId, projectId, periodId, "ADMIN-1", "ADMIN")

        val finalizeDto = FinalizePeriodCloseRequestDto(
            requesterId = "STAFF-1",
            notes = "Approved close"
        )

        val certRes = integrityService.finalizePeriodClose(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            actorId = "MANAGER-1",
            actorRole = "MANAGER",
            requesterId = finalizeDto.requesterId,
            notes = finalizeDto.notes
        )

        assertTrue(certRes is DomainResult.Success)
        val cert = (certRes as DomainResult.Success).data
        val dto = PeriodCloseCertificateDto.fromDomain(cert)

        assertEquals("FINALIZED", dto.status)
        assertEquals("MANAGER-1", dto.closedBy)
        assertTrue(dto.certificateChecksum.isNotBlank())
    }

    @Test
    fun `test module16-handoff DTO mapping generates verified contract`() = runBlocking {
        val handoffRes = integrityService.generateModule16HandoffContract(tenantId, projectId, periodId)
        assertTrue(handoffRes is DomainResult.Success)
        val handoff = (handoffRes as DomainResult.Success).data
        val dto = Module16FinancialHandoffContractDto.fromDomain(handoff)

        assertEquals("2026-M08", dto.periodCode)
        assertTrue(dto.isLedgerBalanced)
        assertEquals(BigDecimal("5000.0000"), dto.ledgerTotalDebit)
        assertEquals(BigDecimal("5000.0000"), dto.ledgerTotalCredit)
    }
}
