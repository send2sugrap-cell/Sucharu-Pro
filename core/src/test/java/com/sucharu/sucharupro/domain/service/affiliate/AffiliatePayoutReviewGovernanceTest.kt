package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliatePayoutRequestDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletHoldDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletLedgerDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliatePayoutRequestRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletHoldRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliatePayoutRequestServiceImpl
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletHoldServiceImpl
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliatePayoutReviewGovernanceTest {

    private lateinit var affiliateDataSource: FakeAffiliateDataSource
    private lateinit var affiliateRepository: AffiliateRepositoryImpl

    private lateinit var walletDataSource: FakeAffiliateWalletDataSource
    private lateinit var walletRepository: AffiliateWalletRepositoryImpl
    private lateinit var walletService: AffiliateWalletServiceImpl

    private lateinit var ledgerDataSource: FakeAffiliateWalletLedgerDataSource
    private lateinit var ledgerRepository: AffiliateWalletLedgerRepositoryImpl
    private lateinit var ledgerService: AffiliateWalletLedgerServiceImpl

    private lateinit var holdDataSource: FakeAffiliateWalletHoldDataSource
    private lateinit var holdRepository: AffiliateWalletHoldRepositoryImpl
    private lateinit var holdService: AffiliateWalletHoldServiceImpl

    private lateinit var payoutRequestDataSource: FakeAffiliatePayoutRequestDataSource
    private lateinit var payoutRequestRepository: AffiliatePayoutRequestRepositoryImpl
    private lateinit var payoutRequestService: AffiliatePayoutRequestServiceImpl

    private val tenantId = "TENANT-MOD23-STEP06"
    private val affiliateId = "AFF-6006"
    private val requesterId = "USER-AFF-06"
    private lateinit var walletId: String

    @Before
    fun setUp() = runBlocking {
        affiliateDataSource = FakeAffiliateDataSource()
        affiliateRepository = AffiliateRepositoryImpl(affiliateDataSource)

        walletDataSource = FakeAffiliateWalletDataSource()
        walletRepository = AffiliateWalletRepositoryImpl(walletDataSource)
        walletService = AffiliateWalletServiceImpl(walletRepository, affiliateRepository)

        ledgerDataSource = FakeAffiliateWalletLedgerDataSource()
        ledgerRepository = AffiliateWalletLedgerRepositoryImpl(ledgerDataSource)
        ledgerService = AffiliateWalletLedgerServiceImpl(walletRepository, ledgerRepository)

        holdDataSource = FakeAffiliateWalletHoldDataSource()
        holdRepository = AffiliateWalletHoldRepositoryImpl(holdDataSource)
        holdService = AffiliateWalletHoldServiceImpl(walletRepository, holdRepository, ledgerService)

        payoutRequestDataSource = FakeAffiliatePayoutRequestDataSource()
        payoutRequestRepository = AffiliatePayoutRequestRepositoryImpl(payoutRequestDataSource)

        payoutRequestService = AffiliatePayoutRequestServiceImpl(
            walletRepository = walletRepository,
            payoutRequestRepository = payoutRequestRepository,
            holdService = holdService
        )

        // Seed Module 20 Affiliate Profile
        val profile = AffiliateProfile(
            affiliateId = affiliateId,
            tenantId = tenantId,
            userId = requesterId,
            displayName = "Partner Zeta",
            affiliateCode = "PARTNER-ZETA",
            status = AffiliateStatus.ACTIVE,
            affiliateType = AffiliateType.INDIVIDUAL,
            joinedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        affiliateRepository.saveAffiliate(profile)

        val walletRes = walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", requesterId)
        walletId = (walletRes as DomainResult.Success).data.walletId

        // Credit 10,000 BDT
        ledgerService.creditWallet(tenantId, walletId, Money("10000.00"), actorId = "STAFF-01")
        Unit
    }

    @Test
    fun test01_reviewPayoutRequest_authorizedManager_transitionsToUnderReview() = runBlocking {
        val request = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("3000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Zeta",
            accountNumber = "9876543210",
            actorId = requesterId
        ) as DomainResult.Success).data

        val reviewRes = payoutRequestService.reviewPayoutRequest(
            tenantId = tenantId,
            requestId = request.requestId,
            notes = "Under compliance review for high volume",
            reviewerId = "MGR-REVIEWER-01"
        )
        assertTrue(reviewRes is DomainResult.Success)
        val reviewed = (reviewRes as DomainResult.Success).data

        assertEquals(AffiliatePayoutRequestStatus.UNDER_REVIEW, reviewed.status)
        assertEquals("Under compliance review for high volume", reviewed.reviewNotes)
    }

    @Test
    fun test02_approvePayoutRequest_authorizedManager_transitionsToApproved_preservesHold() = runBlocking {
        val request = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("3000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Zeta",
            accountNumber = "9876543210",
            actorId = requesterId
        ) as DomainResult.Success).data

        val approveRes = payoutRequestService.approvePayoutRequest(
            tenantId = tenantId,
            requestId = request.requestId,
            notes = "Approved after account verification",
            approverId = "MGR-APPROVER-01"
        )
        assertTrue(approveRes is DomainResult.Success)
        val approved = (approveRes as DomainResult.Success).data

        assertEquals(AffiliatePayoutRequestStatus.APPROVED, approved.status)
        assertEquals("Approved after account verification", approved.reviewNotes)

        // Payout reservation hold remains ACTIVE to protect funds for Step 07 disbursement
        val hold = (holdRepository.getHoldById(tenantId, approved.reservationHoldId!!) as DomainResult.Success).data
        assertNotNull(hold)
        assertEquals(AffiliateWalletHoldStatus.ACTIVE, hold?.status)

        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("7000.00"), bal.availableBalance)
        assertEquals(Money("3000.00"), bal.heldAmount)
    }

    @Test
    fun test03_selfApproval_requesterAttemptsApproval_fails() = runBlocking {
        val request = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("3000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Zeta",
            accountNumber = "9876543210",
            actorId = requesterId
        ) as DomainResult.Success).data

        // Requester attempting to self-approve
        val selfApproveRes = payoutRequestService.approvePayoutRequest(
            tenantId = tenantId,
            requestId = request.requestId,
            notes = "Self approval attempt",
            approverId = requesterId
        )
        assertTrue(selfApproveRes is DomainResult.Error)
        val err = selfApproveRes as DomainResult.Error
        assertTrue(err.message.contains("Separation of duties violation"))
    }

    @Test
    fun test04_rejectPayoutRequest_authorizedManager_transitionsToRejected_releasesHold() = runBlocking {
        val request = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("3000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Zeta",
            accountNumber = "9876543210",
            actorId = requesterId
        ) as DomainResult.Success).data

        val rejectRes = payoutRequestService.rejectPayoutRequest(
            tenantId = tenantId,
            requestId = request.requestId,
            rejectionReason = "Bank account details could not be verified",
            reviewerId = "MGR-REVIEWER-01"
        )
        assertTrue(rejectRes is DomainResult.Success)
        val rejected = (rejectRes as DomainResult.Success).data

        assertEquals(AffiliatePayoutRequestStatus.REJECTED, rejected.status)
        assertEquals("Bank account details could not be verified", rejected.rejectionReason)

        // Verify hold was released and available balance restored to 10,000 BDT
        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("10000.00"), bal.availableBalance)
        assertEquals(Money.ZERO, bal.heldAmount)
    }

    @Test
    fun test05_terminalState_approvedCannotBeRejectedOrApprovedAgain() = runBlocking {
        val request = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("3000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Zeta",
            accountNumber = "9876543210",
            actorId = requesterId
        ) as DomainResult.Success).data

        payoutRequestService.approvePayoutRequest(tenantId, request.requestId, "Approved", "MGR-01")

        // Attempting to reject an already APPROVED request
        val rejectRes = payoutRequestService.rejectPayoutRequest(tenantId, request.requestId, "Late rejection", "MGR-02")
        assertTrue(rejectRes is DomainResult.Error)
        val err = rejectRes as DomainResult.Error
        assertTrue(err.message.contains("Invalid payout request status transition"))
    }

    @Test
    fun test06_crossTenantReviewAttempt_returnsNull() = runBlocking {
        val request = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("3000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Zeta",
            accountNumber = "9876543210",
            actorId = requesterId
        ) as DomainResult.Success).data

        // Tenant B manager attempting to approve Tenant A request
        val crossRes = payoutRequestService.approvePayoutRequest("TENANT-B", request.requestId, "Cross tenant approval", "MGR-TENANT-B")
        assertTrue(crossRes is DomainResult.Error)
        val err = crossRes as DomainResult.Error
        assertTrue(err.message.contains("not found"))
    }

    @Test
    fun test07_canonicalProductionWorkflow_regressionCheck() {
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
