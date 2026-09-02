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

class BusinessCostAccrualReversalTest {

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
    fun testPartialAndFullAccrualReversalsWithCompensatingLedgerEntries() = runBlocking {
        val cmd = CreateCostAccrualCommand(
            accrualNumber = "ACR-REV-TEST",
            costCategoryId = "CAT-PAPER",
            description = "Unbilled paper shipment",
            accrualAmount = BigDecimal("50000.0000"),
            accountingPeriodId = periodId
        )
        val accrual = (service.createAccrual(admin, cmd) as DomainResult.Success).data
        service.approveAccrual(admin, accrual.id)
        service.postAccrual(admin, accrual.id)

        // 1. First Partial Reversal (e.g. 20,000 against partial vendor bill)
        val rev1Cmd = ReverseCostAccrualCommand(
            accrualId = accrual.id,
            reversalAmount = BigDecimal("20000.0000"),
            reason = "Partial invoice received from supplier",
            accountingPeriodId = periodId
        )
        val rev1Res = service.reverseAccrual(admin, rev1Cmd)
        assertTrue(rev1Res is DomainResult.Success)

        val afterRev1 = (service.getAccrualById(admin, accrual.id) as DomainResult.Success).data
        assertEquals(BigDecimal("20000.0000"), afterRev1.reversedAmount)
        assertEquals(BigDecimal("30000.0000"), afterRev1.netAccrualAmount)
        assertEquals(BusinessCostAccrualStatus.POSTED, afterRev1.status)

        // 2. Second Reversal (remaining 30,000)
        val rev2Cmd = ReverseCostAccrualCommand(
            accrualId = accrual.id,
            reversalAmount = BigDecimal("30000.0000"),
            reason = "Final invoice booked",
            accountingPeriodId = periodId
        )
        val rev2Res = service.reverseAccrual(admin, rev2Cmd)
        assertTrue(rev2Res is DomainResult.Success)

        val afterRev2 = (service.getAccrualById(admin, accrual.id) as DomainResult.Success).data
        assertEquals(BigDecimal("50000.0000"), afterRev2.reversedAmount)
        assertEquals(BigDecimal("0.0000"), afterRev2.netAccrualAmount)
        assertEquals(BusinessCostAccrualStatus.REVERSED, afterRev2.status)

        // Verify total ledger postings: 1 initial debit + 2 compensating credits
        val ledgerPostings = ledgerRepo.listPostings(tenantId, projectId, com.sucharu.sucharupro.data.datasource.businessledger.BusinessLedgerPostingFilter())
        assertEquals(3, ledgerPostings.size)
        val debits = ledgerPostings.filter { it.debitAmount > BigDecimal.ZERO }
        val credits = ledgerPostings.filter { it.creditAmount > BigDecimal.ZERO }
        assertEquals(1, debits.size)
        assertEquals(2, credits.size)
    }
}
