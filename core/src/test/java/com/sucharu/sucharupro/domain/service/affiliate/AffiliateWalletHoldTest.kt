package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletHoldDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletLedgerDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletHoldRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletHoldServiceImpl
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateWalletHoldTest {

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

    private val tenantId = "TENANT-MOD23-STEP04"
    private val affiliateId = "AFF-4004"
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

        holdService = AffiliateWalletHoldServiceImpl(
            walletRepository = walletRepository,
            holdRepository = holdRepository,
            ledgerService = ledgerService
        )

        // Seed Module 20 Affiliate Profile
        val profile = AffiliateProfile(
            affiliateId = affiliateId,
            tenantId = tenantId,
            userId = "USER-AFF-04",
            displayName = "Partner Delta",
            affiliateCode = "PARTNER-DELTA",
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
    fun test01_calculateAvailableBalance_incorporatesActiveHolds() = runBlocking {
        val holdRes = holdService.createHold(
            tenantId = tenantId,
            walletId = walletId,
            amount = Money("1500.00"),
            holdType = AffiliateWalletHoldType.COMPLIANCE_REVIEW,
            reason = "Pending compliance review for high volume referrals",
            actorId = "MGR-01"
        )
        assertTrue(holdRes is DomainResult.Success)

        val balRes = holdService.calculateAvailableBalance(tenantId, walletId)
        assertTrue(balRes is DomainResult.Success)
        val bal = (balRes as DomainResult.Success).data

        assertEquals(Money("5000.00"), bal.currentBalance)
        assertEquals(Money("1500.00"), bal.heldAmount)
        assertEquals(Money("3500.00"), bal.availableBalance)
    }

    @Test
    fun test02_releaseHold_restoresAvailableBalance() = runBlocking {
        val hold = (holdService.createHold(tenantId, walletId, Money("1500.00"), AffiliateWalletHoldType.COMPLIANCE_REVIEW, "Compliance hold", actorId = "MGR-01") as DomainResult.Success).data

        val relRes = holdService.releaseHold(
            tenantId = tenantId,
            walletId = walletId,
            holdId = hold.holdId,
            releaseReason = "Compliance verification passed",
            actorId = "MGR-01"
        )
        assertTrue(relRes is DomainResult.Success)
        val releasedHold = (relRes as DomainResult.Success).data
        assertEquals(AffiliateWalletHoldStatus.RELEASED, releasedHold.status)

        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("5000.00"), bal.currentBalance)
        assertEquals(Money.ZERO, bal.heldAmount)
        assertEquals(Money("5000.00"), bal.availableBalance)
    }

    @Test
    fun test03_createHold_exceedsAvailableBalance_fails() = runBlocking {
        val holdRes = holdService.createHold(
            tenantId = tenantId,
            walletId = walletId,
            amount = Money("6000.00"),
            holdType = AffiliateWalletHoldType.FRAUD_RISK_REVIEW,
            reason = "Exceeds balance hold",
            actorId = "MGR-01"
        )
        assertTrue(holdRes is DomainResult.Error)
        val err = holdRes as DomainResult.Error
        assertTrue(err.message.contains("Exceeds current available balance"))
    }

    @Test
    fun test04_debitWallet_respectsHeldAmount_failsWhenExceedingAvailable() = runBlocking {
        holdService.createHold(tenantId, walletId, Money("2000.00"), AffiliateWalletHoldType.DISPUTE_HOLD, "Dispute hold", actorId = "MGR-01")

        // Available balance is now 3000.00 BDT. Attempting debit of 4000.00 BDT fails.
        val debitRes = ledgerService.debitWallet(tenantId, walletId, Money("4000.00"), actorId = "STAFF-01")
        assertTrue(debitRes is DomainResult.Error)

        // Debit of 2500.00 BDT succeeds.
        val validDebit = ledgerService.debitWallet(tenantId, walletId, Money("2500.00"), actorId = "STAFF-01")
        assertTrue(validDebit is DomainResult.Success)

        val bal = (holdService.calculateAvailableBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("2500.00"), bal.currentBalance)
        assertEquals(Money("2000.00"), bal.heldAmount)
        assertEquals(Money("500.00"), bal.availableBalance)
    }

    @Test
    fun test05_evaluatePayoutEligibility_belowThreshold_returnsNotEligible() = runBlocking {
        holdService.createHold(tenantId, walletId, Money("4500.00"), AffiliateWalletHoldType.BUSINESS_POLICY_HOLD, "Policy hold", actorId = "MGR-01")

        // Available balance is 500.00 BDT. Minimum payout threshold is 1000.00 BDT.
        val evalRes = holdService.evaluatePayoutEligibility(tenantId, walletId, minimumThreshold = Money("1000.00"))
        assertTrue(evalRes is DomainResult.Success)
        val eligibility = (evalRes as DomainResult.Success).data

        assertFalse(eligibility.isEligible)
        assertFalse(eligibility.isThresholdSatisfied)
        assertEquals(1, eligibility.blockingReasons.size)
        assertTrue(eligibility.blockingReasons.first().contains("below minimum payout threshold"))
    }

    @Test
    fun test06_evaluatePayoutEligibility_aboveThreshold_returnsEligible() = runBlocking {
        val evalRes = holdService.evaluatePayoutEligibility(tenantId, walletId, minimumThreshold = Money("1000.00"))
        assertTrue(evalRes is DomainResult.Success)
        val eligibility = (evalRes as DomainResult.Success).data

        assertTrue(eligibility.isEligible)
        assertTrue(eligibility.isThresholdSatisfied)
        assertTrue(eligibility.blockingReasons.isEmpty())
    }

    @Test
    fun test07_releaseHold_alreadyReleased_fails() = runBlocking {
        val hold = (holdService.createHold(tenantId, walletId, Money("1000.00"), AffiliateWalletHoldType.DISPUTE_HOLD, "Hold 1", actorId = "MGR-01") as DomainResult.Success).data
        holdService.releaseHold(tenantId, walletId, hold.holdId, "Release 1", "MGR-01")

        val secondRelease = holdService.releaseHold(tenantId, walletId, hold.holdId, "Release 2", "MGR-01")
        assertTrue(secondRelease is DomainResult.Error)
        val err = secondRelease as DomainResult.Error
        assertTrue(err.message.contains("cannot be released"))
    }

    @Test
    fun test08_tenantIsolation_crossTenantHoldAccess_returnsNull() = runBlocking {
        val hold = (holdService.createHold(tenantId, walletId, Money("1000.00"), AffiliateWalletHoldType.DISPUTE_HOLD, "Hold 1", actorId = "MGR-01") as DomainResult.Success).data

        // Tenant B attempting to release Tenant A hold
        val crossRelease = holdService.releaseHold("TENANT-B", walletId, hold.holdId, "Cross tenant release", "MGR-B")
        assertTrue(crossRelease is DomainResult.Error)
        val err = crossRelease as DomainResult.Error
        assertTrue(err.message.contains("not found"))
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
