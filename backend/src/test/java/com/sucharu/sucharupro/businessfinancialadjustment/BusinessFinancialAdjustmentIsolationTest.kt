package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.AdjustmentFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.FakeBusinessFinancialAdjustmentDataSource
import com.sucharu.sucharupro.data.datasource.businessledger.FakeBusinessLedgerDataSource
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepositoryImpl
import com.sucharu.sucharupro.data.repository.businessledger.BusinessLedgerRepositoryImpl
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class BusinessFinancialAdjustmentIsolationTest {

    private lateinit var adjDataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var adjRepository: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessFinancialAdjustmentServiceImpl

    private val tenantA = "TENANT-A"
    private val projectA = "PRJ-A"
    private val projectB = "PRJ-B"

    private val adminA = AuthenticatedPrincipal(userId = "USR-A", username = "admin_a", role = UserRole.ADMIN, projectId = projectA)
    private val adminB = AuthenticatedPrincipal(userId = "USR-B", username = "admin_b", role = UserRole.ADMIN, projectId = projectB)

    @Before
    fun setup() {
        adjDataSource = FakeBusinessFinancialAdjustmentDataSource()
        adjRepository = BusinessFinancialAdjustmentRepositoryImpl(adjDataSource)
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        ledgerService = BusinessLedgerServiceImpl(ledgerRepository, defaultTenantId = tenantA)

        service = BusinessFinancialAdjustmentServiceImpl(
            repository = adjRepository,
            ledgerService = ledgerService,
            defaultTenantId = tenantA
        )
    }

    @Test
    fun testProjectIsolation() = runBlocking {
        // Project A creates an adjustment
        val cmdA = CreateAdjustmentCommand(
            adjustmentNumber = "ADJ-PRJ-A-001",
            adjustmentType = BusinessFinancialAdjustmentType.EXPENSE_CORRECTION,
            sourceType = AdjustmentSourceType.BUSINESS_EXPENSE,
            sourceId = "EXP-A-1",
            originalAmount = BigDecimal("5000.0000"),
            adjustmentAmount = BigDecimal("-500.0000"),
            reason = "Project A adjustment",
            justification = "Justification for Project A discount",
            periodId = "PER-2026-08"
        )
        val resA = service.createAdjustment(adminA, cmdA)
        assertTrue(resA is DomainResult.Success)
        val adjA = (resA as DomainResult.Success).data

        // Project B cannot see Project A's adjustment
        val listB = service.listAdjustments(adminB, AdjustmentFilter())
        assertTrue(listB is DomainResult.Success)
        assertEquals(0, (listB as DomainResult.Success).data.size)

        // Project B cannot access Project A's adjustment by ID
        val getRes = service.getAdjustmentById(adminB, adjA.id)
        assertTrue(getRes is DomainResult.Error)

        // Project A can see its own adjustment
        val listA = service.listAdjustments(adminA, AdjustmentFilter())
        assertTrue(listA is DomainResult.Success)
        assertEquals(1, (listA as DomainResult.Success).data.size)
    }
}
