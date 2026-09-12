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

class AffiliatePayoutRecoveryTest {

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

    private lateinit var recoveryDataSource: FakeAffiliatePayoutRecoveryDataSource
    private lateinit var recoveryRepository: AffiliatePayoutRecoveryRepositoryImpl
    private lateinit var recoveryService: AffiliatePayoutRecoveryServiceImpl

    private val tenantId = "TENANT-MOD23-STEP08"
    private val affiliateId = "AFF-8008"
    private val requesterId = "USER-AFF-08"
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

        recoveryDataSource = FakeAffiliatePayoutRecoveryDataSource()
        recoveryRepository = AffiliatePayoutRecoveryRepositoryImpl(recoveryDataSource)
        recoveryService = AffiliatePayoutRecoveryServiceImpl(
            payoutRequestRepository = payoutRequestRepository,
            disbursementRepository = disbursementRepository,
            recoveryRepository = recoveryRepository,
            disbursementService = disbursementService,
            ledgerService = ledgerService,
            holdService = holdService
        )

        // Seed Module 20 Affiliate Profile
        val profile = AffiliateProfile(
            affiliateId = affiliateId,
            tenantId = tenantId,
            userId = requesterId,
            displayName = "Partner Theta",
            affiliateCode = "PARTNER-THETA",
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
    fun test01_retryFailedPayout_reprocessesDisbursementSuccessfully() = runBlocking {
        // Create failing disbursement service
        val failingDisbursementService = AffiliatePayoutDisbursementServiceImpl(
            payoutRequestRepository = payoutRequestRepository,
            disbursementRepository = disbursementRepository,
            ledgerService = ledgerService,
            holdService = holdService,
            disbursementProvider = MockAffiliatePayoutDisbursementAdapter(shouldFail = true, failureReason = "Temporary gateway timeout")
        )

        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("4000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Theta",
            accountNumber = "1234567890",
            actorId = requesterId
        ) as DomainResult.Success).data

        payoutRequestService.approvePayoutRequest(tenantId, poReq.requestId, "Approved", "MGR-01")

        // First attempt fails
        failingDisbursementService.processPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")
        val failedReq = (payoutRequestService.getPayoutRequestDetails(tenantId, poReq.requestId) as DomainResult.Success).data
        assertEquals(AffiliatePayoutRequestStatus.FAILED, failedReq?.status)

        // Retry payout disbursement using working recovery service
        val retryRes = recoveryService.retryFailedPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")
        assertTrue(retryRes is DomainResult.Success)
        val record = (retryRes as DomainResult.Success).data

        assertEquals(DisbursementProviderStatus.SUCCESS, record.providerStatus)

        val completedReq = (payoutRequestService.getPayoutRequestDetails(tenantId, poReq.requestId) as DomainResult.Success).data
        assertEquals(AffiliatePayoutRequestStatus.COMPLETED, completedReq?.status)

        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("6000.00"), bal.availableBalance)
    }

    @Test
    fun test02_retryCompletedPayout_fails() = runBlocking {
        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Theta",
            accountNumber = "1234567890",
            actorId = requesterId
        ) as DomainResult.Success).data

        payoutRequestService.approvePayoutRequest(tenantId, poReq.requestId, "Approved", "MGR-01")
        disbursementService.processPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")

        val retryRes = recoveryService.retryFailedPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")
        assertTrue(retryRes is DomainResult.Error)
        val err = retryRes as DomainResult.Error
        assertTrue(err.message.contains("Cannot retry a COMPLETED payout request"))
    }

    @Test
    fun test03_reverseCompletedPayout_postsCompensatingCredit_preservesOriginalDebit() = runBlocking {
        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("4000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Theta",
            accountNumber = "1234567890",
            actorId = requesterId
        ) as DomainResult.Success).data

        payoutRequestService.approvePayoutRequest(tenantId, poReq.requestId, "Approved", "MGR-01")
        disbursementService.processPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")

        // Balance after disbursement: 6000.00 BDT
        var bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("6000.00"), bal.availableBalance)

        // Reverse completed payout
        val revRes = recoveryService.reverseCompletedPayout(
            tenantId = tenantId,
            requestId = poReq.requestId,
            reversalReason = "Beneficiary bank returned funds due to closed account",
            actorId = "MGR-01"
        )
        assertTrue(revRes is DomainResult.Success)
        val revRecord = (revRes as DomainResult.Success).data

        assertNotNull(revRecord.compensatingLedgerEntryId)
        assertEquals(Money("4000.00"), revRecord.reversedAmount)

        // Verify payout status updated to REVERSED
        val reversedReq = (payoutRequestService.getPayoutRequestDetails(tenantId, poReq.requestId) as DomainResult.Success).data
        assertEquals(AffiliatePayoutRequestStatus.REVERSED, reversedReq?.status)

        // Verify balance restored to 10,000.00 BDT via compensating credit
        bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("10000.00"), bal.currentBalance)
        assertEquals(Money("10000.00"), bal.availableBalance)
    }

    @Test
    fun test04_reverseNonCompletedPayout_fails() = runBlocking {
        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Theta",
            accountNumber = "1234567890",
            actorId = requesterId
        ) as DomainResult.Success).data

        // Request in REQUESTED state (not COMPLETED)
        val revRes = recoveryService.reverseCompletedPayout(
            tenantId = tenantId,
            requestId = poReq.requestId,
            reversalReason = "Attempted reversal before completion",
            actorId = "MGR-01"
        )
        assertTrue(revRes is DomainResult.Error)
        val err = revRes as DomainResult.Error
        assertTrue(err.message.contains("expected COMPLETED for reversal"))
    }

    @Test
    fun test05_reconcilePayoutDisbursement_consistent_createsReconciliationRecord() = runBlocking {
        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("3000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Theta",
            accountNumber = "1234567890",
            actorId = requesterId
        ) as DomainResult.Success).data

        payoutRequestService.approvePayoutRequest(tenantId, poReq.requestId, "Approved", "MGR-01")
        disbursementService.processPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")

        val recRes = recoveryService.reconcilePayoutDisbursement(tenantId, poReq.requestId, "Routine audit check", "MGR-AUDITOR")
        assertTrue(recRes is DomainResult.Success)
        val rec = (recRes as DomainResult.Success).data

        assertEquals(PayoutReconciliationStatus.CONSISTENT, rec.reconciliationStatus)
        assertEquals(AffiliatePayoutRequestStatus.COMPLETED, rec.internalStatus)
        assertEquals(DisbursementProviderStatus.SUCCESS, rec.providerStatus)
    }

    @Test
    fun test06_tenantIsolation_crossTenantRecovery_returnsError() = runBlocking {
        val poReq = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Theta",
            accountNumber = "1234567890",
            actorId = requesterId
        ) as DomainResult.Success).data

        payoutRequestService.approvePayoutRequest(tenantId, poReq.requestId, "Approved", "MGR-01")
        disbursementService.processPayoutDisbursement(tenantId, poReq.requestId, "MGR-01")

        // Tenant B manager attempting to reverse Tenant A payout
        val crossRev = recoveryService.reverseCompletedPayout("TENANT-B", poReq.requestId, "Cross tenant reversal", "MGR-TENANT-B")
        assertTrue(crossRev is DomainResult.Error)
        val err = crossRev as DomainResult.Error
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
