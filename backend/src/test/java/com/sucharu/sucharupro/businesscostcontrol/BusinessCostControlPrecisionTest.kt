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

class BusinessCostControlPrecisionTest {

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
    fun testAccrualAndReversalPrecisionCalculations() = runBlocking {
        val preciseAccrual = BigDecimal("98765.4321")
        val cmd = CreateCostAccrualCommand(
            costCategoryId = "CAT-PAPER",
            description = "High precision paper accrual",
            accrualAmount = preciseAccrual,
            accountingPeriodId = periodId
        )
        val accrual = (service.createAccrual(admin, cmd) as DomainResult.Success).data
        assertEquals(BigDecimal("98765.4321"), accrual.accrualAmount)
        assertEquals(4, accrual.accrualAmount.scale())

        service.approveAccrual(admin, accrual.id)
        service.postAccrual(admin, accrual.id)

        val revCmd = ReverseCostAccrualCommand(
            accrualId = accrual.id,
            reversalAmount = BigDecimal("12345.1234"),
            reason = "Precision reversal test",
            accountingPeriodId = periodId
        )
        service.reverseAccrual(admin, revCmd)

        val updated = (service.getAccrualById(admin, accrual.id) as DomainResult.Success).data
        assertEquals(BigDecimal("86420.3087"), updated.netAccrualAmount)
        assertEquals(4, updated.netAccrualAmount.scale())
    }
}
