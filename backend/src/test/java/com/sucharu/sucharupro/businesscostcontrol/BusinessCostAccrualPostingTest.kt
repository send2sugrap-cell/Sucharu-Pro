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

class BusinessCostAccrualPostingTest {

    private lateinit var dataSource: FakeBusinessCostControlDataSource
    private lateinit var repository: BusinessCostControlRepositoryImpl
    private lateinit var ledgerDs: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepo: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
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
            ledgerDs = FakeBusinessLedgerDataSource()
            ledgerRepo = BusinessLedgerRepositoryImpl(ledgerDs)
            ledgerService = BusinessLedgerServiceImpl(repository = ledgerRepo, defaultTenantId = tenantId)

            service = BusinessCostControlServiceImpl(
                repository = repository,
                ledgerService = ledgerService,
                defaultTenantId = tenantId
            )

            val p = (service.createFinancialPeriod(
                admin,
                CreateFinancialPeriodCommand("2026-08", "August 2026", 1754092800000L, 1756684799000L)
            ) as DomainResult.Success).data
            periodId = p.id
        }
    }

    @Test
    fun testPostingAccrualCreatesCanonicalBusinessLedgerEntry() = runBlocking {
        val cmd = CreateCostAccrualCommand(
            accrualNumber = "ACR-2026-POST-01",
            costCategoryId = "CAT-MAINT",
            description = "Machine repair technician unbilled hours",
            accrualAmount = BigDecimal("28000.0000"),
            accountingPeriodId = periodId
        )
        val accrual = (service.createAccrual(admin, cmd) as DomainResult.Success).data
        service.approveAccrual(admin, accrual.id)

        // Post accrual to BusinessLedger
        val postRes = service.postAccrual(admin, accrual.id)
        assertTrue(postRes is DomainResult.Success)
        val postedAccrual = (postRes as DomainResult.Success).data
        assertEquals(BusinessCostAccrualStatus.POSTED, postedAccrual.status)
        assertNotNull(postedAccrual.ledgerPostingId)

        // Verify entry in canonical business ledger postings
        val ledgerPostings = ledgerRepo.listPostings(tenantId, projectId, com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter())
        assertEquals(1, ledgerPostings.size)
        assertEquals(BigDecimal("28000.0000"), ledgerPostings[0].debitAmount)
        assertEquals("ACR-2026-POST-01", ledgerPostings[0].reference)
    }
}
