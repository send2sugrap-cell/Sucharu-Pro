package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostReconciliationTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var service: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)

    private var periodId: String = ""

    @Before
    fun setup() {
        runBlocking {
            dataSource = FakeBusinessCostControlDataSource()
            repository = BusinessCostControlRepositoryImpl(dataSource)
            service = BusinessCostControlServiceImpl(repository, defaultTenantId = tenantId)

            val p = (service.createFinancialPeriod(
                admin,
                CreateFinancialPeriodCommand("2026-08", "August 2026", 1754092800000L, 1756684799000L)
            ) as DomainResult.Success).data
            periodId = p.id
        }
    }

    @Test
    fun testThreeWayReconciliationCalculations() = runBlocking {
        // Create Commitment
        val c = (service.createCommitment(
            admin,
            CreateCostCommitmentCommand(
                costCategoryId = "CAT-PAPER",
                description = "Reconciliation test commitment",
                committedAmount = BigDecimal("100000.0000")
            )
        ) as DomainResult.Success).data
        service.approveCommitment(admin, c.id)
        service.activateCommitment(admin, c.id)

        // Consume 40,000
        service.consumeCommitment(
            admin,
            ConsumeCostCommitmentCommand(
                commitmentId = c.id,
                amount = BigDecimal("40000.0000"),
                sourceId = "INV-REC-01"
            )
        )

        // Post Accrual of 25,000
        val a = (service.createAccrual(
            admin,
            CreateCostAccrualCommand(
                costCategoryId = "CAT-PAPER",
                description = "Unbilled delivery",
                accrualAmount = BigDecimal("25000.0000"),
                accountingPeriodId = periodId
            )
        ) as DomainResult.Success).data
        service.approveAccrual(admin, a.id)
        service.postAccrual(admin, a.id)

        val recon = (service.getReconciliationSummary(admin) as DomainResult.Success).data
        assertEquals(BigDecimal("100000.0000"), recon.commitmentAmount)
        assertEquals(BigDecimal("40000.0000"), recon.consumedAmount)
        assertEquals(BigDecimal("60000.0000"), recon.remainingCommitment)
        assertEquals(BigDecimal("25000.0000"), recon.accruedAmount)
    }
}
