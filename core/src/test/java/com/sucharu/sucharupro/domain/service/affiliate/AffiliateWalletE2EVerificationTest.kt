package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ForbiddenException
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.api.model.affiliate.wallet.*
import com.sucharu.sucharupro.data.api.server.*
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.data.persistence.postgres.TenantContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionContext
import com.sucharu.sucharupro.data.persistence.postgres.TransactionManager
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateRepository
import com.sucharu.sucharupro.domain.repository.affiliate.wallet.*
import com.sucharu.sucharupro.domain.service.affiliate.wallet.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

/**
 * Final End-to-End Software Verification Test Suite for Module 23 (Step 10).
 * Validates the complete lifecycle:
 * APPROVED EARNING -> WALLET CREDIT -> IMMUTABLE LEDGER -> AVAILABLE BALANCE ->
 * PAYOUT ELIGIBILITY -> PAYOUT REQUEST -> REVIEW -> APPROVAL -> DISBURSEMENT ->
 * COMPLETION -> RECONCILIATION -> REVERSAL -> AUDIT.
 */
class AffiliateWalletE2EVerificationTest {

    private lateinit var useCases: BackendUseCases
    private lateinit var customFactory: PostgresRepositoryFactory

    private lateinit var affRepo: AffiliateRepositoryImpl
    private lateinit var walletRepo: AffiliateWalletRepositoryImpl
    private lateinit var ledgerRepo: AffiliateWalletLedgerRepositoryImpl
    private lateinit var holdRepo: AffiliateWalletHoldRepositoryImpl
    private lateinit var payoutReqRepo: AffiliatePayoutRequestRepositoryImpl
    private lateinit var disbRepo: AffiliatePayoutDisbursementRepositoryImpl
    private lateinit var recoveryRepo: AffiliatePayoutRecoveryRepositoryImpl

    private lateinit var walletService: AffiliateWalletServiceImpl
    private lateinit var ledgerService: AffiliateWalletLedgerServiceImpl
    private lateinit var holdService: AffiliateWalletHoldServiceImpl
    private lateinit var earningIntegrationService: AffiliateEarningWalletIntegrationServiceImpl
    private lateinit var payoutReqService: AffiliatePayoutRequestServiceImpl
    private lateinit var disbService: AffiliatePayoutDisbursementServiceImpl
    private lateinit var recoveryService: AffiliatePayoutRecoveryServiceImpl

    private val tenantId = "TENANT-MOD23-E2E"
    private val affiliateId = "AFF-E2E-001"
    private val requesterId = "USER-AFF-E2E"

    private val affiliatePrincipal = AuthenticatedPrincipal(userId = requesterId, projectId = tenantId, username = "aff_user", role = UserRole.AFFILIATE)
    private val staffPrincipal = AuthenticatedPrincipal(userId = "USER-STAFF-E2E", projectId = tenantId, username = "staff_user", role = UserRole.STAFF)
    private val managerPrincipal = AuthenticatedPrincipal(userId = "USER-MGR-E2E", projectId = tenantId, username = "mgr_user", role = UserRole.MANAGER)

    private lateinit var walletId: String

    @Before
    fun setup() = runBlocking {
        val affDs = FakeAffiliateDataSource()
        affRepo = AffiliateRepositoryImpl(affDs)

        val walletDs = FakeAffiliateWalletDataSource()
        walletRepo = AffiliateWalletRepositoryImpl(walletDs)

        val ledgerDs = FakeAffiliateWalletLedgerDataSource()
        ledgerRepo = AffiliateWalletLedgerRepositoryImpl(ledgerDs)

        val holdDs = FakeAffiliateWalletHoldDataSource()
        holdRepo = AffiliateWalletHoldRepositoryImpl(holdDs)

        val payoutReqDs = FakeAffiliatePayoutRequestDataSource()
        payoutReqRepo = AffiliatePayoutRequestRepositoryImpl(payoutReqDs)

        val disbDs = FakeAffiliatePayoutDisbursementDataSource()
        disbRepo = AffiliatePayoutDisbursementRepositoryImpl(disbDs)

        val recoveryDs = FakeAffiliatePayoutRecoveryDataSource()
        recoveryRepo = AffiliatePayoutRecoveryRepositoryImpl(recoveryDs)

        walletService = AffiliateWalletServiceImpl(walletRepo, affRepo)
        ledgerService = AffiliateWalletLedgerServiceImpl(walletRepo, ledgerRepo)
        holdService = AffiliateWalletHoldServiceImpl(walletRepo, holdRepo, ledgerService)
        earningIntegrationService = AffiliateEarningWalletIntegrationServiceImpl(affRepo, walletRepo, walletService, ledgerService)
        payoutReqService = AffiliatePayoutRequestServiceImpl(walletRepo, payoutReqRepo, holdService)
        disbService = AffiliatePayoutDisbursementServiceImpl(payoutReqRepo, disbRepo, ledgerService, holdService, MockAffiliatePayoutDisbursementAdapter())
        recoveryService = AffiliatePayoutRecoveryServiceImpl(payoutReqRepo, disbRepo, recoveryRepo, disbService, ledgerService, holdService)

        // Seed Module 20 Affiliate Profile
        affRepo.saveAffiliate(
            AffiliateProfile(
                affiliateId = affiliateId,
                tenantId = tenantId,
                userId = requesterId,
                displayName = "E2E Affiliate Partner",
                affiliateCode = "AFF-E2E-CODE",
                status = AffiliateStatus.ACTIVE,
                affiliateType = AffiliateType.INDIVIDUAL,
                joinedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        val fakeTxManager = object : TransactionManager {
            override suspend fun <T> inTransaction(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
            override suspend fun <T> inReadOnly(tenantContext: TenantContext, block: suspend (TransactionContext) -> T): T {
                throw UnsupportedOperationException("Not required for mock tests")
            }
        }

        customFactory = object : PostgresRepositoryFactory(fakeTxManager) {
            override fun createAffiliateRepository(tenantId: String): AffiliateRepository = affRepo
            override fun createAffiliateWalletRepository(tenantId: String): AffiliateWalletRepository = walletRepo
            override fun createAffiliateWalletService(tenantId: String): AffiliateWalletService = walletService
            override fun createAffiliateWalletLedgerRepository(tenantId: String): AffiliateWalletLedgerRepository = ledgerRepo
            override fun createAffiliateWalletLedgerService(tenantId: String): AffiliateWalletLedgerService = ledgerService
            override fun createAffiliateWalletHoldRepository(tenantId: String): AffiliateWalletHoldRepository = holdRepo
            override fun createAffiliateWalletHoldService(tenantId: String): AffiliateWalletHoldService = holdService
            override fun createAffiliateEarningWalletIntegrationService(tenantId: String): AffiliateEarningWalletIntegrationService = earningIntegrationService
            override fun createAffiliatePayoutRequestRepository(tenantId: String): AffiliatePayoutRequestRepository = payoutReqRepo
            override fun createAffiliatePayoutRequestService(tenantId: String): AffiliatePayoutRequestService = payoutReqService
            override fun createAffiliatePayoutDisbursementRepository(tenantId: String): AffiliatePayoutDisbursementRepository = disbRepo
            override fun createAffiliatePayoutDisbursementService(tenantId: String): AffiliatePayoutDisbursementService = disbService
            override fun createAffiliatePayoutRecoveryRepository(tenantId: String): AffiliatePayoutRecoveryRepository = recoveryRepo
            override fun createAffiliatePayoutRecoveryService(tenantId: String): AffiliatePayoutRecoveryService = recoveryService
        }

        useCases = BackendUseCases(fakeTxManager, customFactory)

        val w = walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", requesterId)
        walletId = (w as DomainResult.Success).data.walletId
        Unit
    }

    @Test
    fun testMasterE2EWorkflow_happyPath() = runBlocking {
        // Step 03: Handoff approved commission earning of 10,000 BDT
        val handoff = ApprovedEarningHandoff(
            earningReferenceId = "EARN-REF-10001",
            tenantId = tenantId,
            affiliateId = affiliateId,
            amount = Money("10000.00"),
            currency = "BDT",
            source = "APPROVED_COMMISSION",
            sourceOrderId = "ORD-PRINT-99"
        )
        val creditRes = earningIntegrationService.processApprovedEarningCredit(tenantId, handoff, actorId = requesterId)
        assertTrue(creditRes is DomainResult.Success<*>)

        // Step 02/04: Verify wallet balance
        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("10000.00"), bal.currentBalance)
        assertEquals(Money("10000.00"), bal.availableBalance)

        // Step 05: Submit payout request for 4000 BDT
        val reqDto = SubmitPayoutRequestDto(
            requestedAmount = BigDecimal("4000.00"),
            payoutMethodAccountName = "E2E Affiliate Partner",
            payoutMethodAccountNumber = "9988776655"
        )
        val poReq = useCases.submitAffiliatePayoutRequest(affiliatePrincipal, walletId, reqDto, customFactory)
        assertEquals(AffiliatePayoutRequestStatus.REQUESTED, poReq.status)

        // Verify available balance decreased by reserved 4000 BDT
        val balAfterReq = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("10000.00"), balAfterReq.currentBalance)
        assertEquals(Money("4000.00"), balAfterReq.heldAmount)
        assertEquals(Money("6000.00"), balAfterReq.availableBalance)

        // Step 06: Review & Approve payout request
        useCases.reviewAffiliatePayoutRequest(managerPrincipal, poReq.requestId, ReviewPayoutRequestDto("Compliance verified"), customFactory)
        val approvedReq = useCases.approveAffiliatePayoutRequest(managerPrincipal, poReq.requestId, ApprovePayoutRequestDto("Approved by Manager"), customFactory)
        assertEquals(AffiliatePayoutRequestStatus.APPROVED, approvedReq.status)

        // Step 07: Process Disbursement
        val disbResp = useCases.disburseAffiliatePayout(managerPrincipal, poReq.requestId, customFactory)
        assertEquals(DisbursementProviderStatus.SUCCESS, disbResp.providerStatus)
        assertNotNull(disbResp.providerTransactionRef)

        // Verify final wallet balance: Current = 6000 BDT, Held = 0 BDT, Available = 6000 BDT
        val finalBal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("6000.00"), finalBal.currentBalance)
        assertEquals(Money.ZERO, finalBal.heldAmount)
        assertEquals(Money("6000.00"), finalBal.availableBalance)

        // Step 08: Reconcile payout
        val recResp = useCases.reconcileAffiliatePayoutDisbursement(managerPrincipal, poReq.requestId, ReconcilePayoutRequestDto("E2E Audit"), customFactory)
        assertEquals(PayoutReconciliationStatus.CONSISTENT, recResp.reconciliationStatus)

        // Step 08: Reverse completed payout (compensating credit)
        val revResp = useCases.reverseAffiliatePayout(managerPrincipal, poReq.requestId, ReversePayoutRequestDto("Bank returned payment"), customFactory)
        assertNotNull(revResp.reversalId)

        // Verify balance restored to 10,000 BDT via compensating credit
        val restoredBal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("10000.00"), restoredBal.currentBalance)
        assertEquals(Money("10000.00"), restoredBal.availableBalance)
    }

    @Test
    fun testE2E_insufficientBalance_payoutRejected() = runBlocking {
        val reqDto = SubmitPayoutRequestDto(
            requestedAmount = BigDecimal("15000.00"), // Available is 0.00
            payoutMethodAccountName = "E2E Affiliate Partner",
            payoutMethodAccountNumber = "9988776655"
        )
        try {
            useCases.submitAffiliatePayoutRequest(affiliatePrincipal, walletId, reqDto, customFactory)
            fail("Expected IllegalArgumentException for insufficient balance")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("insufficient for payout") == true)
        }
    }

    @Test
    fun testE2E_selfApproval_rejected() = runBlocking {
        // Seed 10,000 BDT
        ledgerService.creditWallet(tenantId, walletId, Money("10000.00"), actorId = "SYSTEM")

        val reqDto = SubmitPayoutRequestDto(
            requestedAmount = BigDecimal("2000.00"),
            payoutMethodAccountName = "E2E Affiliate Partner",
            payoutMethodAccountNumber = "9988776655"
        )
        // Manager submits payout
        val poReq = useCases.submitAffiliatePayoutRequest(managerPrincipal, walletId, reqDto, customFactory)

        // Manager attempts self-approval
        try {
            useCases.approveAffiliatePayoutRequest(managerPrincipal, poReq.requestId, ApprovePayoutRequestDto("Self approval"), customFactory)
            fail("Expected IllegalArgumentException for self approval")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Separation of duties violation") == true)
        }
    }

    @Test
    fun testCanonicalProductionWorkflow_regressionCheck() {
        val expectedSequence = listOf(
            ProductionStageType.DESIGN,
            ProductionStageType.APPROVAL,
            ProductionStageType.QC,
            ProductionStageType.ITEM_APPROVAL,
            ProductionStageType.CTP,
            ProductionStageType.PRINTING,
            ProductionStageType.LAMINATION,
            ProductionStageType.FOLDING,
            ProductionStageType.BINDING,
            ProductionStageType.FINAL_QC,
            ProductionStageType.PACKAGING,
            ProductionStageType.READY,
            ProductionStageType.DELIVERED
        )
        assertEquals(13, ProductionStageType.entries.size)
        assertEquals(expectedSequence, ProductionStageType.entries)
    }
}
