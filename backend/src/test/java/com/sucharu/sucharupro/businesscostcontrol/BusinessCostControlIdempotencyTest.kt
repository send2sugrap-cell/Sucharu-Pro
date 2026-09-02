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

class BusinessCostControlIdempotencyTest {

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
    fun testAccrualCreationIdempotency() = runBlocking {
        val idemKey = "IDEM-ACR-KEY-555"
        val cmd = CreateCostAccrualCommand(
            costCategoryId = "CAT-PAPER",
            description = "Idempotent paper accrual",
            accrualAmount = BigDecimal("30000.0000"),
            accountingPeriodId = periodId,
            idempotencyKey = idemKey
        )

        val res1 = service.createAccrual(admin, cmd)
        assertTrue(res1 is DomainResult.Success)
        val a1 = (res1 as DomainResult.Success).data

        // Replay
        val res2 = service.createAccrual(admin, cmd)
        assertTrue(res2 is DomainResult.Success)
        val a2 = (res2 as DomainResult.Success).data

        assertEquals(a1.id, a2.id)
        val list = (service.listAccruals(admin) as DomainResult.Success).data
        assertEquals(1, list.size)
    }
}
