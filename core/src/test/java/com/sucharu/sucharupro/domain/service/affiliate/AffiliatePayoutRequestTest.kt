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

class AffiliatePayoutRequestTest {

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

    private val tenantId = "TENANT-MOD23-STEP05"
    private val affiliateId = "AFF-5005"
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
            userId = "USER-AFF-05",
            displayName = "Partner Epsilon",
            affiliateCode = "PARTNER-EPSILON",
            status = AffiliateStatus.ACTIVE,
            affiliateType = AffiliateType.INDIVIDUAL,
            joinedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        affiliateRepository.saveAffiliate(profile)

        val walletRes = walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", "STAFF-01")
        walletId = (walletRes as DomainResult.Success).data.walletId

        // Credit 5000 BDT
        ledgerService.creditWallet(tenantId, walletId, Money("5000.00"), actorId = "STAFF-01")
        Unit
    }

    @Test
    fun test01_submitPayoutRequest_validRequest_createsRequestAndReservationHold() = runBlocking {
        val reqRes = payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Epsilon Account",
            accountNumber = "1234567890",
            provider = "City Bank Ltd",
            branchRouting = "123456",
            actorId = "AFF-05"
        )
        assertTrue(reqRes is DomainResult.Success)
        val request = (reqRes as DomainResult.Success).data

        assertEquals(AffiliatePayoutRequestStatus.REQUESTED, request.status)
        assertEquals(Money("2000.00"), request.requestedAmount)
        assertNotNull(request.reservationHoldId)

        // Verify available balance decreased by reserved 2000.00 BDT
        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("5000.00"), bal.currentBalance)
        assertEquals(Money("2000.00"), bal.heldAmount)
        assertEquals(Money("3000.00"), bal.availableBalance)
    }

    @Test
    fun test02_submitPayoutRequest_insufficientAvailableBalance_fails() = runBlocking {
        val reqRes = payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("6000.00"),
            payoutMethodType = AffiliatePayoutMethodType.MFS_BKASH,
            accountName = "Partner Epsilon",
            accountNumber = "01711000000",
            actorId = "AFF-05"
        )
        assertTrue(reqRes is DomainResult.Error)
        val err = reqRes as DomainResult.Error
        assertTrue(err.message.contains("insufficient for payout"))
    }

    @Test
    fun test03_submitPayoutRequest_belowMinimumThreshold_fails() = runBlocking {
        val reqRes = payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("500.00"),
            payoutMethodType = AffiliatePayoutMethodType.MFS_BKASH,
            accountName = "Partner Epsilon",
            accountNumber = "01711000000",
            minimumThreshold = Money("1000.00"),
            actorId = "AFF-05"
        )
        assertTrue(reqRes is DomainResult.Error)
        val err = reqRes as DomainResult.Error
        assertTrue(err.message.contains("below minimum payout threshold"))
    }

    @Test
    fun test04_cancelPayoutRequest_releasesReservationHold_restoresAvailableBalance() = runBlocking {
        val request = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Epsilon",
            accountNumber = "1234567890",
            actorId = "AFF-05"
        ) as DomainResult.Success).data

        // Cancel
        val cancelRes = payoutRequestService.updatePayoutRequestStatus(
            tenantId = tenantId,
            requestId = request.requestId,
            newStatus = AffiliatePayoutRequestStatus.CANCELLED,
            reason = "Cancelled by affiliate prior to review",
            actorId = "AFF-05"
        )
        assertTrue(cancelRes is DomainResult.Success)
        val cancelled = (cancelRes as DomainResult.Success).data
        assertEquals(AffiliatePayoutRequestStatus.CANCELLED, cancelled.status)

        // Verify hold was released and available balance restored
        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("5000.00"), bal.currentBalance)
        assertEquals(Money.ZERO, bal.heldAmount)
        assertEquals(Money("5000.00"), bal.availableBalance)
    }

    @Test
    fun test05_rejectPayoutRequest_releasesReservationHold() = runBlocking {
        val request = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Epsilon",
            accountNumber = "1234567890",
            actorId = "AFF-05"
        ) as DomainResult.Success).data

        // Reject
        val rejectRes = payoutRequestService.updatePayoutRequestStatus(
            tenantId = tenantId,
            requestId = request.requestId,
            newStatus = AffiliatePayoutRequestStatus.REJECTED,
            reason = "Account holder name mismatch",
            actorId = "MGR-01"
        )
        assertTrue(rejectRes is DomainResult.Success)

        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("5000.00"), bal.availableBalance)
    }

    @Test
    fun test06_stateMachineTransitions_invalidTransition_fails() = runBlocking {
        val request = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Epsilon",
            accountNumber = "1234567890",
            actorId = "AFF-05"
        ) as DomainResult.Success).data

        // Attempt direct REQUESTED -> COMPLETED transition
        val invalidRes = payoutRequestService.updatePayoutRequestStatus(
            tenantId = tenantId,
            requestId = request.requestId,
            newStatus = AffiliatePayoutRequestStatus.COMPLETED,
            actorId = "MGR-01"
        )
        assertTrue(invalidRes is DomainResult.Error)
        val err = invalidRes as DomainResult.Error
        assertTrue(err.message.contains("Invalid payout request status transition"))
    }

    @Test
    fun test07_idempotency_duplicateRequestReturnsExisting() = runBlocking {
        val idempKey = "KEY-PAYOUT-IDEMP-001"

        val firstRes = payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Epsilon",
            accountNumber = "1234567890",
            idempotencyKey = idempKey,
            actorId = "AFF-05"
        )
        assertTrue(firstRes is DomainResult.Success)
        val firstReq = (firstRes as DomainResult.Success).data

        val secondRes = payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Epsilon",
            accountNumber = "1234567890",
            idempotencyKey = idempKey,
            actorId = "AFF-05"
        )
        assertTrue(secondRes is DomainResult.Success)
        val secondReq = (secondRes as DomainResult.Success).data

        assertEquals(firstReq.requestId, secondReq.requestId)

        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("2000.00"), bal.heldAmount)
        assertEquals(Money("3000.00"), bal.availableBalance)
    }

    @Test
    fun test08_tenantIsolation_crossTenantPayoutRequestAccess_returnsNull() = runBlocking {
        val request = (payoutRequestService.submitPayoutRequest(
            tenantId = tenantId,
            walletId = walletId,
            requestedAmount = Money("2000.00"),
            payoutMethodType = AffiliatePayoutMethodType.BANK_TRANSFER,
            accountName = "Partner Epsilon",
            accountNumber = "1234567890",
            actorId = "AFF-05"
        ) as DomainResult.Success).data

        // Tenant B querying Tenant A payout request
        val fetchRes = payoutRequestService.getPayoutRequestDetails("TENANT-B", request.requestId)
        assertTrue(fetchRes is DomainResult.Success)
        assertNull((fetchRes as DomainResult.Success).data)
    }

    @Test
    fun test09_canonicalProductionWorkflow_regressionCheck() {
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
