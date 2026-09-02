package com.sucharu.sucharupro.businesscostcontrol

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businesscostcontrol.FakeBusinessCostControlDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businesscostcontrol.BusinessCostControlRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessCostAccrualStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.*
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessCostAccrualServiceTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var ledgerDs: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepo: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessCostControlServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val admin = AuthenticatedPrincipal("ADM-1", projectId, "admin", UserRole.ADMIN)
    private val manager = AuthenticatedPrincipal("MGR-1", projectId, "manager", UserRole.MANAGER)
    private val staff = AuthenticatedPrincipal("STF-1", projectId, "staff", UserRole.STAFF)

    private var periodId: String = ""

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

            // Create open period
            val p = service.createFinancialPeriod(
                admin,
                CreateFinancialPeriodCommand(
                    periodCode = "2026-08",
                    periodName = "August 2026",
                    startDate = 1754092800000L,
                    endDate = 1756684799000L
                )
            )
            periodId = (p as DomainResult.Success).data.id
        }
    }

    @Test
    fun testAccrualCreationReviewAndApprovalWorkflow() = runBlocking {
        // Staff creates DRAFT accrual
        val cmd = CreateCostAccrualCommand(
            accrualNumber = "ACR-2026-001",
            costCategoryId = "CAT-LABOR",
            description = "Overtime printing labor",
            accrualAmount = BigDecimal("35000.0000"),
            accountingPeriodId = periodId
        )
        val createRes = service.createAccrual(staff, cmd)
        assertTrue(createRes is DomainResult.Success)
        val accrual = (createRes as DomainResult.Success).data
        assertEquals(BusinessCostAccrualStatus.DRAFT, accrual.status)

        // Review accrual
        val revRes = service.reviewAccrual(staff, accrual.id)
        assertTrue(revRes is DomainResult.Success)
        assertEquals(BusinessCostAccrualStatus.REVIEWED, (revRes as DomainResult.Success).data.status)

        // Manager approves accrual
        val appRes = service.approveAccrual(manager, accrual.id)
        assertTrue(appRes is DomainResult.Success)
        assertEquals(BusinessCostAccrualStatus.APPROVED, (appRes as DomainResult.Success).data.status)
    }

    @Test
    fun testAccrualCancellationBeforePosting() = runBlocking {
        val cmd = CreateCostAccrualCommand(
            costCategoryId = "CAT-UTIL",
            description = "Wrong utility estimate",
            accrualAmount = BigDecimal("10000.0000"),
            accountingPeriodId = periodId
        )
        val accrual = (service.createAccrual(admin, cmd) as DomainResult.Success).data
        val cancelRes = service.cancelAccrual(admin, accrual.id, "Incorrect estimate entered")
        assertTrue(cancelRes is DomainResult.Success)
        assertEquals(BusinessCostAccrualStatus.CANCELLED, (cancelRes as DomainResult.Success).data.status)
    }
}
