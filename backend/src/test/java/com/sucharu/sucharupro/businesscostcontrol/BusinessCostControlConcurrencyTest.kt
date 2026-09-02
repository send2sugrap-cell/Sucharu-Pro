package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostControlConcurrencyTest {

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
    fun testConcurrentAccrualCreations() = runBlocking {
        val jobs = (1..20).map { i ->
            async(Dispatchers.Default) {
                val cmd = CreateCostAccrualCommand(
                    accrualNumber = "ACR-CONCUR-$i",
                    costCategoryId = "CAT-PAPER",
                    description = "Concurrent accrual $i",
                    accrualAmount = BigDecimal("1000.0000"),
                    accountingPeriodId = periodId
                )
                service.createAccrual(admin, cmd)
            }
        }

        val results = jobs.awaitAll()
        val successCount = results.count { it is DomainResult.Success }
        assertEquals(20, successCount)

        val list = (service.listAccruals(admin) as DomainResult.Success).data
        assertEquals(20, list.size)
    }
}
