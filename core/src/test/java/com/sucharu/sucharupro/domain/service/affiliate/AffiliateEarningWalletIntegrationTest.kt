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
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateEarningWalletIntegrationServiceImpl
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletLedgerServiceImpl
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateEarningWalletIntegrationTest {

    private lateinit var affiliateDataSource: FakeAffiliateDataSource
    private lateinit var affiliateRepository: AffiliateRepositoryImpl
    private lateinit var walletDataSource: FakeAffiliateWalletDataSource
    private lateinit var walletRepository: AffiliateWalletRepositoryImpl
    private lateinit var walletService: AffiliateWalletServiceImpl

    private lateinit var ledgerDataSource: FakeAffiliateWalletLedgerDataSource
    private lateinit var ledgerRepository: AffiliateWalletLedgerRepositoryImpl
    private lateinit var ledgerService: AffiliateWalletLedgerServiceImpl

    private lateinit var integrationService: AffiliateEarningWalletIntegrationServiceImpl

    private val tenantId = "TENANT-MOD23-STEP03"
    private val affiliateId = "AFF-3003"

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

        integrationService = AffiliateEarningWalletIntegrationServiceImpl(
            affiliateRepository = affiliateRepository,
            walletRepository = walletRepository,
            walletService = walletService,
            ledgerService = ledgerService
        )

        // Seed Module 20 Affiliate Profile
        val profile = AffiliateProfile(
            affiliateId = affiliateId,
            tenantId = tenantId,
            userId = "USER-AFF-03",
            displayName = "Partner Gamma",
            affiliateCode = "PARTNER-GAMMA",
            status = AffiliateStatus.ACTIVE,
            affiliateType = AffiliateType.INDIVIDUAL,
            joinedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        affiliateRepository.saveAffiliate(profile)
        Unit
    }

    @Test
    fun test01_approvedEarningHandoff_validAffiliate_creditsWallet() = runBlocking {
        val handoff = ApprovedEarningHandoff(
            earningReferenceId = "COMM-EARN-8801",
            tenantId = tenantId,
            affiliateId = affiliateId,
            amount = Money("1500.00"),
            currency = "BDT",
            source = "APPROVED_COMMISSION",
            sourceOrderId = "ORD-REF-01"
        )

        val creditRes = integrationService.processApprovedEarningCredit(tenantId, handoff, "STAFF-01")
        assertTrue(creditRes is DomainResult.Success)
        val entry = (creditRes as DomainResult.Success).data

        assertEquals(AffiliateWalletLedgerEntryType.CREDIT, entry.entryType)
        assertEquals(Money("1500.00"), entry.amount)
        assertEquals("COMM-EARN-8801", entry.referenceId)

        // Check derived wallet balance
        val wallet = (walletService.getWalletByAffiliate(tenantId, affiliateId, "BDT") as DomainResult.Success).data
        assertNotNull(wallet)
        val balance = (ledgerService.calculateWalletBalance(tenantId, wallet!!.walletId) as DomainResult.Success).data
        assertEquals(Money("1500.00"), balance.currentBalance)
        assertEquals(Money("1500.00"), balance.availableBalance)
    }

    @Test
    fun test02_nonExistentAffiliateInModule20_fails() = runBlocking {
        val handoff = ApprovedEarningHandoff(
            earningReferenceId = "COMM-GHOST-01",
            tenantId = tenantId,
            affiliateId = "AFF-NONEXISTENT",
            amount = Money("1000.00")
        )

        val creditRes = integrationService.processApprovedEarningCredit(tenantId, handoff, "STAFF-01")
        assertTrue(creditRes is DomainResult.Error)
        val err = creditRes as DomainResult.Error
        assertTrue(err.message.contains("not found in Module 20"))
    }

    @Test
    fun test03_crossTenantEarningHandoff_fails() = runBlocking {
        val handoff = ApprovedEarningHandoff(
            earningReferenceId = "COMM-EARN-CROSS",
            tenantId = "TENANT-B",
            affiliateId = affiliateId,
            amount = Money("1000.00")
        )

        val creditRes = integrationService.processApprovedEarningCredit(tenantId, handoff, "STAFF-01")
        assertTrue(creditRes is DomainResult.Error)
        val err = creditRes as DomainResult.Error
        assertTrue(err.message.contains("Cross-tenant earning credit rejected"))
    }

    @Test
    fun test04_currencyMismatch_failsWithoutConversion() = runBlocking {
        // First create BDT wallet
        walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", "STAFF-01")

        val usdHandoff = ApprovedEarningHandoff(
            earningReferenceId = "COMM-USD-01",
            tenantId = tenantId,
            affiliateId = affiliateId,
            amount = Money("100.00"),
            currency = "USD"
        )

        val creditRes = integrationService.processApprovedEarningCredit(tenantId, usdHandoff, "STAFF-01")
        assertTrue(creditRes is DomainResult.Error)
        val err = creditRes as DomainResult.Error
        assertTrue(err.message.contains("Currency mismatch"))
    }

    @Test
    fun test05_idempotency_duplicateEarningHandoffReturnsSameEntry() = runBlocking {
        val handoff = ApprovedEarningHandoff(
            earningReferenceId = "COMM-IDEMP-01",
            tenantId = tenantId,
            affiliateId = affiliateId,
            amount = Money("2000.00"),
            idempotencyKey = "EARN-IDEMP-KEY-01"
        )

        val firstRes = integrationService.processApprovedEarningCredit(tenantId, handoff, "STAFF-01")
        assertTrue(firstRes is DomainResult.Success)
        val firstEntry = (firstRes as DomainResult.Success).data

        val secondRes = integrationService.processApprovedEarningCredit(tenantId, handoff, "STAFF-01")
        assertTrue(secondRes is DomainResult.Success)
        val secondEntry = (secondRes as DomainResult.Success).data

        assertEquals(firstEntry.entryId, secondEntry.entryId)

        val wallet = (walletService.getWalletByAffiliate(tenantId, affiliateId, "BDT") as DomainResult.Success).data!!
        val balance = (ledgerService.calculateWalletBalance(tenantId, wallet.walletId) as DomainResult.Success).data
        assertEquals(Money("2000.00"), balance.currentBalance)
    }

    @Test
    fun test06_suspendedWallet_rejectsEarningCredit() = runBlocking {
        val wallet = (walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", "STAFF-01") as DomainResult.Success).data
        walletService.updateWalletStatus(tenantId, wallet.walletId, AffiliateWalletStatus.SUSPENDED, "MGR-01")

        val handoff = ApprovedEarningHandoff(
            earningReferenceId = "COMM-SUSPEND-01",
            tenantId = tenantId,
            affiliateId = affiliateId,
            amount = Money("500.00")
        )

        val creditRes = integrationService.processApprovedEarningCredit(tenantId, handoff, "STAFF-01")
        assertTrue(creditRes is DomainResult.Error)
        val err = creditRes as DomainResult.Error
        assertTrue(err.message.contains("SUSPENDED"))
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
