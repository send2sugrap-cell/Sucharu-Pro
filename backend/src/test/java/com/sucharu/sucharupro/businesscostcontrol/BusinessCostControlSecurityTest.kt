package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.BusinessCostControlServiceImpl
import com.sucharu.sucharupro.domain.service.businesscostcontrol.CreateCostAccrualCommand
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostControlSecurityTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var service: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val manager1 = AuthenticatedPrincipal("MGR-1", projectId, "manager1", UserRole.MANAGER)
    private val manager2 = AuthenticatedPrincipal("MGR-2", projectId, "manager2", UserRole.MANAGER)
    private val staff = AuthenticatedPrincipal("STF-1", projectId, "staff", UserRole.STAFF)

    @Before
    fun setup() {
        runBlocking {
            dataSource = FakeBusinessCostControlDataSource()
            repository = BusinessCostControlRepositoryImpl(dataSource)
            service = BusinessCostControlServiceImpl(repository, defaultTenantId = tenantId)
        }
    }

    @Test
    fun testAccrualSoDEnforcement() = runBlocking {
        val p = (service.createFinancialPeriod(
            manager1,
            com.sucharu.sucharupro.domain.service.businesscostcontrol.CreateFinancialPeriodCommand("2026-08", "Aug", 1754092800000L, 1756684799000L)
        ) as DomainResult.Success).data

        // Manager 1 creates accrual
        val a = (service.createAccrual(
            manager1,
            CreateCostAccrualCommand(
                costCategoryId = "CAT-PAPER",
                description = "Self approval accrual",
                accrualAmount = BigDecimal("5000.0000"),
                accountingPeriodId = p.id
            )
        ) as DomainResult.Success).data

        // Manager 1 attempts to approve their own accrual -> SoD violation!
        val selfApprove = service.approveAccrual(manager1, a.id)
        assertTrue(selfApprove is DomainResult.Error)
        assertTrue((selfApprove as DomainResult.Error).message.contains("Separation of Duties"))

        // Manager 2 approves successfully
        val otherApprove = service.approveAccrual(manager2, a.id)
        assertTrue(otherApprove is DomainResult.Success)
    }
}
