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

class BusinessCostPeriodClosureTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var service: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)

    @Before
    fun setup() {
        runBlocking {
            dataSource = FakeBusinessCostControlDataSource()
            repository = BusinessCostControlRepositoryImpl(dataSource)
            service = BusinessCostControlServiceImpl(repository, defaultTenantId = tenantId)
        }
    }

    @Test
    fun testPeriodEndReadinessCheckAndReport() = runBlocking {
        val p = (service.createFinancialPeriod(
            admin,
            CreateFinancialPeriodCommand("2026-08", "August 2026", 1754092800000L, 1756684799000L)
        ) as DomainResult.Success).data

        // Add a DRAFT unposted accrual to period
        val accrual = (service.createAccrual(
            admin,
            CreateCostAccrualCommand(
                costCategoryId = "CAT-LABOR",
                description = "Unposted overtime",
                accrualAmount = BigDecimal("15000.0000"),
                accountingPeriodId = p.id
            )
        ) as DomainResult.Success).data

        // Check period-end report -> should NOT be ready for closure because 1 unposted accrual remains
        val rep1 = (service.getPeriodEndReport(admin, p.id) as DomainResult.Success).data
        assertFalse(rep1.isReadyForClosure)
        assertEquals(1, rep1.pendingAccrualsCount)
        assertTrue(rep1.warnings.isNotEmpty())

        // Approve and post accrual
        service.approveAccrual(admin, accrual.id)
        service.postAccrual(admin, accrual.id)

        // Re-check period-end report -> ready for closure!
        val rep2 = (service.getPeriodEndReport(admin, p.id) as DomainResult.Success).data
        assertTrue(rep2.isReadyForClosure)
        assertEquals(0, rep2.pendingAccrualsCount)
        assertEquals(BigDecimal("15000.0000"), rep2.postedAccrualsAmount)
    }
}
