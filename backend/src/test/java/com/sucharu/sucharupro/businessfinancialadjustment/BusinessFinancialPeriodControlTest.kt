package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriod
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriodStatus
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.BusinessCostControlServiceImpl
import com.sucharu.sucharupro.domain.service.businesscostcontrol.CreateFinancialPeriodCommand
import com.sucharu.sucharupro.domain.service.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialPeriodControlTest {

    private lateinit var adjDataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var adjRepository: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var costControlDataSource: FakeBusinessCostControlDataSource
    private lateinit var costControlRepository: BusinessCostControlRepositoryImpl
    private lateinit var costControlService: BusinessCostControlServiceImpl
    private lateinit var service: BusinessFinancialAdjustmentServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val admin = AuthenticatedPrincipal(userId = "USR-ADMIN", username = "admin_user", role = UserRole.ADMIN, projectId = projectId)
    private val staff = AuthenticatedPrincipal(userId = "USR-STAFF", username = "staff_user", role = UserRole.STAFF, projectId = projectId)

    @Before
    fun setup() {
        adjDataSource = FakeBusinessFinancialAdjustmentDataSource()
        adjRepository = BusinessFinancialAdjustmentRepositoryImpl(adjDataSource)
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        ledgerService = BusinessLedgerServiceImpl(ledgerRepository, defaultTenantId = tenantId)

        costControlDataSource = FakeBusinessCostControlDataSource()
        costControlRepository = BusinessCostControlRepositoryImpl(costControlDataSource)
        costControlService = BusinessCostControlServiceImpl(
            repository = costControlRepository,
            ledgerService = ledgerService,
            defaultTenantId = tenantId
        )

        service = BusinessFinancialAdjustmentServiceImpl(
            repository = adjRepository,
            ledgerService = ledgerService,
            costControlService = costControlService,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testHardClosedPeriodRejectsPosting() = runBlocking {
        // Create hard-closed period
        val period = BusinessFinancialPeriod(
            id = "PER-CLOSED",
            tenantId = tenantId,
            projectId = projectId,
            periodCode = "2026-01",
            periodName = "January 2026",
            startDate = 1000L,
            endDate = 2000L,
            status = BusinessFinancialPeriodStatus.CLOSED
        )
        costControlRepository.createFinancialPeriod(period)

        val cmd = CreateAdjustmentCommand(
            adjustmentNumber = "ADJ-PER-001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-900",
            originalAmount = BigDecimal("5000.0000"),
            adjustmentAmount = BigDecimal("-500.0000"),
            reason = "Discount",
            justification = "Justification for closed period test",
            periodId = "PER-CLOSED"
        )
        val res = service.createAdjustment(admin, cmd)
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("hard-closed"))
    }
}
