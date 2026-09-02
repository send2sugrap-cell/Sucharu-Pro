package com.sucharu.sucharupro.businessfinancialadjustment

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
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

class BusinessFinancialWriteOffTest {

    private lateinit var adjDataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var adjRepository: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessFinancialAdjustmentServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

    private val admin = AuthenticatedPrincipal(userId = "USR-ADMIN", username = "admin_user", role = UserRole.ADMIN, projectId = projectId)
    private val manager = AuthenticatedPrincipal(userId = "USR-MGR", username = "mgr_user", role = UserRole.MANAGER, projectId = projectId)
    private val accounts = AuthenticatedPrincipal(userId = "USR-ACC", username = "acc_user", role = UserRole.STAFF, projectId = projectId)

    @Before
    fun setup() {
        adjDataSource = FakeBusinessFinancialAdjustmentDataSource()
        adjRepository = BusinessFinancialAdjustmentRepositoryImpl(adjDataSource)
        ledgerDataSource = FakeBusinessLedgerDataSource()
        ledgerRepository = BusinessLedgerRepositoryImpl(ledgerDataSource)
        ledgerService = BusinessLedgerServiceImpl(ledgerRepository, defaultTenantId = tenantId)

        service = BusinessFinancialAdjustmentServiceImpl(
            repository = adjRepository,
            ledgerService = ledgerService,
            defaultTenantId = tenantId
        )
    }

    @Test
    fun testWriteOffWorkflowLifecycle() = runBlocking {
        // 1. Create Write-off
        val createCmd = CreateWriteOffCommand(
            writeOffNumber = "WO-2026-001",
            sourceType = AdjustmentSourceType.CUSTOMER_INVOICE,
            sourceId = "INV-201",
            writeOffType = BusinessFinancialWriteOffType.BAD_DEBT,
            eligibleBalance = BigDecimal("15000.0000"),
            amount = BigDecimal("15000.0000"),
            reason = "Customer insolvency and liquidation",
            justification = "Official legal bankruptcy certificate attached, full bad debt write-off approved",
            periodId = "PER-2026-08",
            customerId = "CUST-999"
        )
        val createRes = service.createWriteOff(accounts, createCmd)
        assertTrue(createRes is DomainResult.Success)
        val wo = (createRes as DomainResult.Success).data
        assertEquals(WriteOffStatus.REQUESTED, wo.status)

        // 2. Approve Write-off
        val appRes = service.approveWriteOff(manager, ApproveWriteOffCommand(wo.id))
        assertTrue(appRes is DomainResult.Success)
        val approvedWo = (appRes as DomainResult.Success).data
        assertEquals(WriteOffStatus.APPROVED, approvedWo.status)

        // 3. Post Write-off
        val postRes = service.postWriteOff(admin, PostWriteOffCommand(wo.id))
        assertTrue(postRes is DomainResult.Success)
        val postedWo = (postRes as DomainResult.Success).data
        assertEquals(WriteOffStatus.POSTED, postedWo.status)
        assertNotNull(postedWo.ledgerPostingId)
    }

    @Test
    fun testWriteOffExceedingEligibleBalanceFails() = runBlocking {
        val createCmd = CreateWriteOffCommand(
            writeOffNumber = "WO-2026-002",
            sourceType = AdjustmentSourceType.CUSTOMER_INVOICE,
            sourceId = "INV-202",
            writeOffType = BusinessFinancialWriteOffType.BAD_DEBT,
            eligibleBalance = BigDecimal("5000.0000"),
            amount = BigDecimal("8000.0000"),
            reason = "Over-write-off",
            justification = "Justification text greater than ten characters",
            periodId = "PER-2026-08"
        )
        val res = service.createWriteOff(accounts, createCmd)
        assertTrue(res is DomainResult.Error)
        assertTrue((res as DomainResult.Error).message.contains("cannot exceed eligible balance"))
    }
}
