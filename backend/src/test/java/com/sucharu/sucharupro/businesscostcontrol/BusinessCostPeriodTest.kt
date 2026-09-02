package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriodStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BusinessCostPeriodTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var service: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)
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
    fun testCreateAndSoftClosePeriod() = runBlocking {
        val cmd = CreateFinancialPeriodCommand(
            periodCode = "2026-09",
            periodName = "September 2026",
            startDate = 1756684800000L,
            endDate = 1759276799000L
        )
        val created = (service.createFinancialPeriod(admin, cmd) as DomainResult.Success).data
        assertEquals(BusinessFinancialPeriodStatus.OPEN, created.status)

        val softCloseRes = service.softCloseFinancialPeriod(admin, created.id, "Pre-closing review")
        assertTrue(softCloseRes is DomainResult.Success)
        val softClosed = (softCloseRes as DomainResult.Success).data
        assertEquals(BusinessFinancialPeriodStatus.SOFT_CLOSED, softClosed.status)
    }

    @Test
    fun testReopenClosedPeriodRequiresAdmin() = runBlocking {
        val p = (service.createFinancialPeriod(
            admin,
            CreateFinancialPeriodCommand("2026-06", "June 2026", 1748736000000L, 1751327999000L)
        ) as DomainResult.Success).data

        service.closeFinancialPeriod(admin, p.id, "Year end audit preparation")

        // Staff cannot reopen
        val staffReopen = service.reopenFinancialPeriod(staff, p.id, "Try reopening")
        assertTrue(staffReopen is DomainResult.Error)

        // Admin reopens successfully
        val adminReopen = service.reopenFinancialPeriod(admin, p.id, "Auditor requested adjustments")
        assertTrue(adminReopen is DomainResult.Success)
        assertEquals(BusinessFinancialPeriodStatus.OPEN, (adminReopen as DomainResult.Success).data.status)
    }
}
