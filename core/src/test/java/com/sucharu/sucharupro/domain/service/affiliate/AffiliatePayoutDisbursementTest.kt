package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.*
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.service.affiliate.wallet.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliatePayoutDisbursementTest {

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

    private lateinit var disbursementDataSource: FakeAffiliatePayoutDisbursementDataSource
    private lateinit var disbursementRepository: AffiliatePayoutDisbursementRepositoryImpl
    private lateinit var disbursementService: AffiliatePayoutDisbursementServiceImpl

    private val tenantId = "TENANT-MOD23-STEP07"
    private val affiliateId = "AFF-7007"
    private val requesterId = "USER-AFF-07"
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
        payoutRequestService = AffiliatePayoutRequestServiceImpl(walletRepository, payoutRequestRepository, holdService)

        disbursementDataSource = FakeAffiliatePayoutDisbursementDataSource()
        disbursementRepository = AffiliatePayoutDisbursementRepositoryImpl(disbursementDataSource)

        disbursementService = AffiliatePayoutDisbursementServiceImpl(
            payoutRequestRepository = payoutRequestRepository,
            disbursementRepository = disbursementRepository,
            ledgerService = ledgerService,
            holdService = holdService,
            disbursementProvider = MockAffiliatePayoutDisbursementAdapter()
        )

        // Seed Module 20 Affiliate Profile
        val profile = AffiliateProfile(
            affiliateId = affiliateId,
            tenantId = tenantId,
            userId = requesterId,
            displayName = "Partner Eta",
            affiliateCode = "PARTNER-ETA",
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
    fun test01_disburseApprovedPayout_success_settlesLedgerAndCompletesRequest() = runBlocking {
        // 1. Submit payout request for 4000.00 BDT
        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("4000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Eta",
            accountNumber = "1234567890",
            actorId = requesterId
        ) as DomainResult.Success).data

        // 2. Manager approves request
        payoutRequestService.approvePayoutRequest(tenantId, poReq.requestId, "Approved", "MGR-01")

        // 3. Process disbursement
        val disbRes = disbursementService.processPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")
        assertTrue(disbRes is DomainResult.Success)
        val record = (disbRes as DomainResult.Success).data

        assertEquals(DisbursementProviderStatus.SUCCESS, record.providerStatus)
        assertNotNull(record.providerTransactionRef)
        assertTrue(record.providerTransactionRef!!.startsWith("TXN-SETTLED-"))
        assertNotNull(record.ledgerEntryId)

        // Verify payout request status updated to COMPLETED
        val completedReq = (payoutRequestService.getPayoutRequestDetails(tenantId, poReq.requestId) as DomainResult.Success).data
        assertNotNull(completedReq)
        assertEquals(AffiliatePayoutRequestStatus.COMPLETED, completedReq?.status)

        // Verify balance after payout settlement: Current = 6000.00 BDT, Held = 0.00 BDT, Available = 6000.00 BDT
        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("6000.00"), bal.currentBalance)
        assertEquals(Money.ZERO, bal.heldAmount)
        assertEquals(Money("6000.00"), bal.availableBalance)
    }

    @Test
    fun test02_disburseNonApprovedPayout_fails() = runBlocking {
        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.MFS_BKASH,
            accountName = "Partner Eta",
            accountNumber = "01700000000",
            actorId = requesterId
        ) as DomainResult.Success).data

        // Request is in REQUESTED status (not APPROVED)
        val disbRes = disbursementService.processPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")
        assertTrue(disbRes is DomainResult.Error)
        val err = disbRes as DomainResult.Error
        assertTrue(err.message.contains("expected APPROVED"))
    }

    @Test
    fun test03_disbursePayout_providerFailure_transitionsToFailed() = runBlocking {
        val failingService = AffiliatePayoutDisbursementServiceImpl(
            payoutRequestRepository = payoutRequestRepository,
            disbursementRepository = disbursementRepository,
            ledgerService = ledgerService,
            holdService = holdService,
            disbursementProvider = MockAffiliatePayoutDisbursementAdapter(shouldFail = true, failureReason = "Bank account invalid")
        )

        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Eta",
            accountNumber = "0000000000",
            actorId = requesterId
        ) as DomainResult.Success).data

        payoutRequestService.approvePayoutRequest(tenantId, poReq.requestId, "Approved", "MGR-01")

        val disbRes = failingService.processPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")
        assertTrue(disbRes is DomainResult.Error)
        val err = disbRes as DomainResult.Error
        assertTrue(err.message.contains("Bank account invalid"))

        val failedReq = (payoutRequestService.getPayoutRequestDetails(tenantId, poReq.requestId) as DomainResult.Success).data
        assertNotNull(failedReq)
        assertEquals(AffiliatePayoutRequestStatus.FAILED, failedReq?.status)
    }

    @Test
    fun test04_disbursePayout_alreadyCompleted_fails() = runBlocking {
        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("1000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Eta",
            accountNumber = "1234567890",
            actorId = requesterId
        ) as DomainResult.Success).data

        payoutRequestService.approvePayoutRequest(tenantId, poReq.requestId, "Approved", "MGR-01")
        disbursementService.processPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")

        // Second disbursement attempt on completed payout
        val secondDisb = disbursementService.processPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")
        assertTrue(secondDisb is DomainResult.Error)
        val err = secondDisb as DomainResult.Error
        assertTrue(err.message.contains("already COMPLETED"))
    }

    @Test
    fun test05_tenantIsolation_crossTenantDisbursement_returnsNull() = runBlocking {
        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("1000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Eta",
            accountNumber = "1234567890",
            actorId = requesterId
        ) as DomainResult.Success).data

        payoutRequestService.approvePayoutRequest(tenantId, poReq.requestId, "Approved", "MGR-01")

        // Tenant B attempting to disburse Tenant A payout
        val crossDisb = disbursementService.processPayoutDisbursement("TENANT-B", poReq.requestId, "MGR-B")
        assertTrue(crossDisb is DomainResult.Error)
        val err = crossDisb as DomainResult.Error
        assertTrue(err.message.contains("not found"))
    }

    @Test
    fun test06_canonicalProductionWorkflow_regressionCheck() {
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
