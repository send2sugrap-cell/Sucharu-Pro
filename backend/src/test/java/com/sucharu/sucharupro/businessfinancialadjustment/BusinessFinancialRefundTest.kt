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

class BusinessFinancialRefundTest {

    private lateinit var adjDataSource: FakeBusinessFinancialAdjustmentDataSource
    private lateinit var adjRepository: BusinessFinancialAdjustmentRepositoryImpl
    private lateinit var ledgerDataSource: FakeBusinessLedgerDataSource
    private lateinit var ledgerRepository: BusinessLedgerRepositoryImpl
    private lateinit var ledgerService: BusinessLedgerServiceImpl
    private lateinit var service: BusinessFinancialAdjustmentServiceImpl

    private val tenantId = "TENANT-001"
    private val projectId = "PRJ-001"

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
    fun testRefundWorkflowLifecycle() = runBlocking {
        // 1. Create refund request
        val createCmd = CreateRefundCommand(
            refundNumber = "REF-2026-001",
            sourceType = AdjustmentSourceType.CUSTOMER_INVOICE,
            sourceId = "INV-101",
            customerId = "CUST-001",
            eligibleBalance = BigDecimal("5000.0000"),
            requestedAmount = BigDecimal("2000.0000"),
            refundReason = "Customer overpayment refund request",
            paymentMethod = "BANK_TRANSFER",
            periodId = "PER-2026-08"
        )
        val createRes = service.createRefund(accounts, createCmd)
        assertTrue(createRes is DomainResult.Success)
        val ref = (createRes as DomainResult.Success).data
        assertEquals(RefundStatus.REQUESTED, ref.status)

        // 2. Approve refund (SoD: requester is accounts, approver is manager)
        val appRes = service.approveRefund(manager, ApproveRefundCommand(ref.id, approvedAmount = BigDecimal("2000.0000")))
        assertTrue(appRes is DomainResult.Success)
        val approvedRef = (appRes as DomainResult.Success).data
        assertEquals(RefundStatus.APPROVED, approvedRef.status)
        assertEquals(BigDecimal("2000.0000"), approvedRef.approvedAmount)

        // 3. Post refund to Business Ledger
        val postRes = service.postRefund(manager, PostRefundCommand(ref.id))
        assertTrue(postRes is DomainResult.Success)
        val postedRef = (postRes as DomainResult.Success).data
        assertEquals(RefundStatus.POSTED, postedRef.status)
        assertNotNull(postedRef.ledgerPostingId)
    }

    @Test
    fun testCannotRefundMoreThanEligibleBalance() = runBlocking {
        val createCmd = CreateRefundCommand(
            refundNumber = "REF-2026-002",
            sourceType = AdjustmentSourceType.CUSTOMER_INVOICE,
            sourceId = "INV-102",
            customerId = "CUST-001",
            eligibleBalance = BigDecimal("1000.0000"),
            requestedAmount = BigDecimal("1500.0000"),
            refundReason = "Over-refund attempt",
            periodId = "PER-2026-08"
        )
        val createRes = service.createRefund(accounts, createCmd)
        assertTrue(createRes is DomainResult.Error)
        assertTrue((createRes as DomainResult.Error).message.contains("cannot exceed eligible balance"))
    }
}
