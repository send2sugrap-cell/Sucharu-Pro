package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.*
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostControlRegressionTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var ledgerDs: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepo: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)

    @Before
    fun setup() {
        runBlocking {
            dataSource = FakeBusinessCostControlDataSource()
            repository = BusinessCostControlRepositoryImpl(dataSource)
            ledgerDs = FakeBusinessLedgerDataSource()
            ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDs)
            ledgerService = BusinessLedgerServiceImpl(repository = ledgerRepo, defaultTenantId = tenantId)

            service = BusinessCostControlServiceImpl(
                repository = repository,
                ledgerService = ledgerService,
                defaultTenantId = tenantId
            )
        }
    }

    @Test
    fun testEndToEndRegressionAcrossCommitmentsAccrualsPeriodsAndReconciliation() = runBlocking {
        // 1. Create open financial period for September 2026
        val p = (service.createFinancialPeriod(
            admin,
            CreateFinancialPeriodCommand("2026-09", "September 2026", 1756684800000L, 1759276799000L)
        ) as DomainResult.Success).data

        // 2. Create commitment of 200,000
        val c = (service.createCommitment(
            admin,
            CreateCostCommitmentCommand(
                costCategoryId = "CAT-PAPER",
                description = "Annual magazine paper supply contract",
                committedAmount = BigDecimal("200000.0000"),
                periodId = p.id
            )
        ) as DomainResult.Success).data
        service.approveCommitment(admin, c.id)
        service.activateCommitment(admin, c.id)

        // 3. Consume 80,000 via shipment invoice
        service.consumeCommitment(
            admin,
            ConsumeCostCommitmentCommand(
                commitmentId = c.id,
                amount = BigDecimal("80000.0000"),
                sourceType = BusinessCostCommitmentSourceType.SERVICE_COMMITMENT,
                sourceId = "INV-2026-SEP-01"
            )
        )

        // 4. Create unbilled service accrual of 30,000
        val a = (service.createAccrual(
            admin,
            CreateCostAccrualCommand(
                costCategoryId = "CAT-MAINT",
                description = "Emergency press calibration",
                accrualAmount = BigDecimal("30000.0000"),
                accountingPeriodId = p.id,
                sourceCommitmentId = c.id
            )
        ) as DomainResult.Success).data
        service.approveAccrual(admin, a.id)
        service.postAccrual(admin, a.id)

        // 5. Check Dashboard KPIs
        val dash = (service.getControlDashboard(admin) as DomainResult.Success).data
        assertEquals(BigDecimal("200000.0000"), dash.totalCommitments)
        assertEquals(BigDecimal("80000.0000"), dash.consumedCommitments)
        assertEquals(BigDecimal("120000.0000"), dash.remainingCommitments)
        assertEquals(BigDecimal("30000.0000"), dash.accruedCosts)

        // 6. Check Ledger Postings created
        val postings = ledgerRepo.listPostings(tenantId, projectId, com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter())
        assertEquals(1, postings.size)
        assertEquals(BigDecimal("30000.0000"), postings[0].debitAmount)

        // 7. Verify Period-End Report & Closure Readiness
        val rep = (service.getPeriodEndReport(admin, p.id) as DomainResult.Success).data
        assertTrue(rep.isReadyForClosure)
        assertEquals(0, rep.pendingAccrualsCount)
        assertEquals(BigDecimal("30000.0000"), rep.postedAccrualsAmount)

        // 8. Close Period
        val closePeriodRes = service.closeFinancialPeriod(admin, p.id, "September 2026 financial closure completed")
        assertTrue(closePeriodRes is DomainResult.Success)
        val closedPeriod = (closePeriodRes as DomainResult.Success).data
        assertEquals(BusinessFinancialPeriodStatus.CLOSED, closedPeriod.status)
    }
}
