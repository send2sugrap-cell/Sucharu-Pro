package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletLedgerDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletLedgerRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.wallet.*
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateWalletLedgerTest {

    private lateinit var affiliateDataSource: FakeAffiliateDataSource
    private lateinit var affiliateRepository: AffiliateRepositoryImpl
    private lateinit var walletDataSource: FakeAffiliateWalletDataSource
    private lateinit var walletRepository: AffiliateWalletRepositoryImpl
    private lateinit var walletService: AffiliateWalletServiceImpl

    private lateinit var ledgerDataSource: FakeAffiliateWalletLedgerDataSource
    private lateinit var ledgerRepository: AffiliateWalletLedgerRepositoryImpl
    private lateinit var ledgerService: AffiliateWalletLedgerServiceImpl

    private val tenantId = "TENANT-MOD23-STEP02"
    private val affiliateId = "AFF-2002"
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

        // Seed Module 20 Affiliate Profile
        val profile = AffiliateProfile(
            affiliateId = affiliateId,
            tenantId = tenantId,
            userId = "USER-AFF-02",
            displayName = "Partner Beta",
            affiliateCode = "PARTNER-BETA",
            status = AffiliateStatus.ACTIVE,
            affiliateType = AffiliateType.INDIVIDUAL,
            joinedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        affiliateRepository.saveAffiliate(profile)

        // Create Wallet
        val walletRes = walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", "STAFF-01")
        val wallet = (walletRes as DomainResult.Success).data
        walletId = wallet.walletId
        Unit
    }

    @Test
    fun test01_creditWallet_increasesCurrentAndAvailableBalance() = runBlocking {
        val creditRes = ledgerService.creditWallet(
            tenantId = tenantId,
            walletId = walletId,
            amount = Money("5000.00"),
            referenceId = "REF-COMM-101",
            reason = "Monthly referral commission credit",
            actorId = "STAFF-01"
        )
        assertTrue(creditRes is DomainResult.Success)
        val entry = (creditRes as DomainResult.Success).data
        assertEquals(AffiliateWalletLedgerEntryType.CREDIT, entry.entryType)
        assertEquals(Money("5000.00"), entry.amount)

        // Calculate balance
        val balRes = ledgerService.calculateWalletBalance(tenantId, walletId)
        assertTrue(balRes is DomainResult.Success)
        val bal = (balRes as DomainResult.Success).data

        assertEquals(Money("5000.00"), bal.currentBalance)
        assertEquals(Money("5000.00"), bal.availableBalance)
        assertEquals(Money("5000.00"), bal.totalCredits)
        assertEquals(Money.ZERO, bal.totalDebits)
    }

    @Test
    fun test02_debitWallet_withinAvailableBalance_decreasesBalance() = runBlocking {
        ledgerService.creditWallet(tenantId, walletId, Money("5000.00"), actorId = "STAFF-01")

        val debitRes = ledgerService.debitWallet(
            tenantId = tenantId,
            walletId = walletId,
            amount = Money("2000.00"),
            referenceId = "REF-PO-99",
            reason = "Partial payout debit",
            actorId = "STAFF-01"
        )
        assertTrue(debitRes is DomainResult.Success)

        val bal = (ledgerService.calculateWalletBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("3000.00"), bal.currentBalance)
        assertEquals(Money("3000.00"), bal.availableBalance)
        assertEquals(Money("5000.00"), bal.totalCredits)
        assertEquals(Money("2000.00"), bal.totalDebits)
    }

    @Test
    fun test03_debitWallet_exceedsAvailableBalance_fails() = runBlocking {
        ledgerService.creditWallet(tenantId, walletId, Money("1000.00"), actorId = "STAFF-01")

        val debitRes = ledgerService.debitWallet(
            tenantId = tenantId,
            walletId = walletId,
            amount = Money("1500.00"),
            actorId = "STAFF-01"
        )
        assertTrue(debitRes is DomainResult.Error)
        val err = debitRes as DomainResult.Error
        assertTrue(err.message.contains("Insufficient available balance"))
    }

    @Test
    fun test04_reverseCreditEntry_compensatingDebit_restoresBalance() = runBlocking {
        val credit = (ledgerService.creditWallet(tenantId, walletId, Money("5000.00"), actorId = "STAFF-01") as DomainResult.Success).data

        val revRes = ledgerService.reverseLedgerEntry(
            tenantId = tenantId,
            walletId = walletId,
            originalEntryId = credit.entryId,
            reason = "Correction of duplicate credit",
            actorId = "MGR-01"
        )
        assertTrue(revRes is DomainResult.Success)
        val revEntry = (revRes as DomainResult.Success).data
        assertEquals(AffiliateWalletLedgerEntryType.REVERSAL, revEntry.entryType)
        assertEquals(AffiliateWalletLedgerDirection.DEBIT, revEntry.direction)
        assertEquals(credit.entryId, revEntry.reversalOfEntryId)

        val bal = (ledgerService.calculateWalletBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money.ZERO, bal.currentBalance)
        assertEquals(Money.ZERO, bal.availableBalance)
    }

    @Test
    fun test05_duplicateReversalAttempt_fails() = runBlocking {
        val credit = (ledgerService.creditWallet(tenantId, walletId, Money("5000.00"), actorId = "STAFF-01") as DomainResult.Success).data
        ledgerService.reverseLedgerEntry(tenantId, walletId, credit.entryId, "First reversal", "MGR-01")

        val secondRevRes = ledgerService.reverseLedgerEntry(tenantId, walletId, credit.entryId, "Second reversal", "MGR-01")
        assertTrue(secondRevRes is DomainResult.Error)
        val err = secondRevRes as DomainResult.Error
        assertTrue(err.message.contains("already been reversed"))
    }

    @Test
    fun test06_adjustWalletBalance_governedAdjustment_updatesBalance() = runBlocking {
        // Admin positive adjustment
        val posAdj = ledgerService.adjustWalletBalance(
            tenantId = tenantId,
            walletId = walletId,
            amount = Money("1000.00"),
            direction = AffiliateWalletLedgerDirection.CREDIT,
            reason = "Dispute resolution credit adjustment",
            actorId = "MGR-01"
        )
        assertTrue(posAdj is DomainResult.Success)

        var bal = (ledgerService.calculateWalletBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("1000.00"), bal.currentBalance)

        // Admin negative adjustment
        val negAdj = ledgerService.adjustWalletBalance(
            tenantId = tenantId,
            walletId = walletId,
            amount = Money("300.00"),
            direction = AffiliateWalletLedgerDirection.DEBIT,
            reason = "Fee recovery adjustment",
            actorId = "MGR-01"
        )
        assertTrue(negAdj is DomainResult.Success)

        bal = (ledgerService.calculateWalletBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("700.00"), bal.currentBalance)
    }

    @Test
    fun test07_idempotency_duplicatePostingReturnsOriginalEntry() = runBlocking {
        val idempKey = "KEY-IDEMP-999"

        val firstRes = ledgerService.creditWallet(
            tenantId = tenantId,
            walletId = walletId,
            amount = Money("2500.00"),
            idempotencyKey = idempKey,
            actorId = "STAFF-01"
        )
        assertTrue(firstRes is DomainResult.Success)
        val firstEntry = (firstRes as DomainResult.Success).data

        val secondRes = ledgerService.creditWallet(
            tenantId = tenantId,
            walletId = walletId,
            amount = Money("2500.00"),
            idempotencyKey = idempKey,
            actorId = "STAFF-01"
        )
        assertTrue(secondRes is DomainResult.Success)
        val secondEntry = (secondRes as DomainResult.Success).data

        assertEquals(firstEntry.entryId, secondEntry.entryId)

        val bal = (ledgerService.calculateWalletBalance(tenantId, walletId) as DomainResult.Success).data
        assertEquals(Money("2500.00"), bal.currentBalance)
    }

    @Test
    fun test08_tenantIsolation_crossTenantLedgerAccess_returnsEmpty() = runBlocking {
        ledgerService.creditWallet(tenantId, walletId, Money("5000.00"), actorId = "STAFF-01")

        // Tenant B querying Tenant A wallet ledger
        val fetchRes = ledgerService.listLedgerEntries("TENANT-B", walletId)
        assertTrue(fetchRes is DomainResult.Success)
        assertTrue((fetchRes as DomainResult.Success).data.isEmpty())
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
