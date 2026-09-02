package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.*
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostAccrualConsistencyTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var ledgerDs: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepo: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"
    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)

    private var openPeriodId: String = ""
    private var closedPeriodId: String = ""

    @Before
    fun setup() {
        runBlocking {
            dataSource = FakeBusinessCostControlDataSource()
            repository = BusinessCostControlRepositoryImpl(dataSource)
            ledgerDs = FakeBusinessLedgerDataSource()
            ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDs)
            ledgerService = BusinessLedgerServiceImpl(repository = ledgerRepo, defaultTenantId = tenantId)

            service = BusinessCostControlServiceImpl(
                repository = repository,
                ledgerService = ledgerService,
                defaultTenantId = tenantId
            )

            val p1 = (service.createFinancialPeriod(
                admin,
                CreateFinancialPeriodCommand("2026-08", "August 2026", 1754092800000L, 1756684799000L)
            ) as DomainResult.Success).data
            openPeriodId = p1.id

            val p2 = (service.createFinancialPeriod(
                admin,
                CreateFinancialPeriodCommand("2026-07", "July 2026", 1751328000000L, 1754092799000L)
            ) as DomainResult.Success).data
            service.closeFinancialPeriod(admin, p2.id, "July closed")
            closedPeriodId = p2.id
        }
    }

    @Test
    fun testPostingIntoClosedPeriodIsRejected() = runBlocking {
        // Attempt to create accrual with closed period
        val cmd = CreateCostAccrualCommand(
            accrualNumber = "ACR-CLOSED-01",
            costCategoryId = "CAT-PAPER",
            description = "Late accrual",
            accrualAmount = BigDecimal("10000.0000"),
            accountingPeriodId = closedPeriodId
        )
        val res = service.createAccrual(admin, cmd)
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("closed accounting period"))
    }
}
