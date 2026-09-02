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

class BusinessCostControlIsolationTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var servicePrj1: BusinessCostControlServiceImpl
    private lateinit var servicePrj2: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val prj1 = "PRJ-001"
    private val prj2 = "PRJ-002"

    private val adminPrj1 = AuthenticatedPrincipal("ADM-1", prj1, "admin1", UserRole.ADMIN)
    private val adminPrj2 = AuthenticatedPrincipal("ADM-2", prj2, "admin2", UserRole.ADMIN)

    @Before
    fun setup() {
        runBlocking {
            dataSource = FakeBusinessCostControlDataSource()
            repository = BusinessCostControlRepositoryImpl(dataSource)
            servicePrj1 = BusinessCostControlServiceImpl(repository, defaultTenantId = tenantId)
            servicePrj2 = BusinessCostControlServiceImpl(repository, defaultTenantId = tenantId)
        }
    }

    @Test
    fun testAccrualAndPeriodTenantIsolation() = runBlocking {
        val p1 = (servicePrj1.createFinancialPeriod(
            adminPrj1,
            CreateFinancialPeriodCommand("2026-08-P1", "Aug P1", 1754092800000L, 1756684799000L)
        ) as DomainResult.Success).data

        val p2 = (servicePrj2.createFinancialPeriod(
            adminPrj2,
            CreateFinancialPeriodCommand("2026-08-P2", "Aug P2", 1754092800000L, 1756684799000L)
        ) as DomainResult.Success).data

        val listP1 = (servicePrj1.listFinancialPeriods(adminPrj1) as DomainResult.Success).data
        assertEquals(1, listP1.size)
        assertEquals("2026-08-P1", listP1[0].periodCode)

        val listP2 = (servicePrj2.listFinancialPeriods(adminPrj2) as DomainResult.Success).data
        assertEquals(1, listP2.size)
        assertEquals("2026-08-P2", listP2[0].periodCode)
    }
}
