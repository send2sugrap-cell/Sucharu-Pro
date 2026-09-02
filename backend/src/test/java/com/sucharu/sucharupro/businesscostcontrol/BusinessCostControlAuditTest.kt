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

class BusinessCostControlAuditTest {

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
    fun testAuditTrailCapturesAllFinancialEvents() = runBlocking {
        // 1. Create Period
        val p = (service.createFinancialPeriod(
            admin,
            CreateFinancialPeriodCommand("2026-08", "August 2026", 1754092800000L, 1756684799000L)
        ) as DomainResult.Success).data

        // 2. Create and Approve Commitment
        val c = (service.createCommitment(
            admin,
            CreateCostCommitmentCommand(
                costCategoryId = "CAT-PAPER",
                description = "Audit paper commitment",
                committedAmount = BigDecimal("50000.0000")
            )
        ) as DomainResult.Success).data
        service.approveCommitment(admin, c.id)

        // 3. Create, Approve, Post Accrual
        val a = (service.createAccrual(
            admin,
            CreateCostAccrualCommand(
                costCategoryId = "CAT-PAPER",
                description = "Audit paper accrual",
                accrualAmount = BigDecimal("20000.0000"),
                accountingPeriodId = p.id
            )
        ) as DomainResult.Success).data
        service.approveAccrual(admin, a.id)
        service.postAccrual(admin, a.id)

        val audits = (service.listAuditEvents(admin) as DomainResult.Success).data
        assertTrue(audits.size >= 5)
        assertTrue(audits.any { it.eventType == "PERIOD_CREATED" })
        assertTrue(audits.any { it.eventType == "COMMITMENT_CREATED" })
        assertTrue(audits.any { it.eventType == "COMMITMENT_APPROVED" })
        assertTrue(audits.any { it.eventType == "ACCRUAL_CREATED" })
        assertTrue(audits.any { it.eventType == "ACCRUAL_POSTED" })
    }
}
