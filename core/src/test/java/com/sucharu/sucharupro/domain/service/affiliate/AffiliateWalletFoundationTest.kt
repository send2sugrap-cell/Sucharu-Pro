package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.wallet.FakeAffiliateWalletDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.wallet.AffiliateWalletRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.production.ProductionStageType
import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus
import com.sucharu.sucharupro.domain.service.affiliate.wallet.AffiliateWalletServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AffiliateWalletFoundationTest {

    private lateinit var affiliateDataSource: FakeAffiliateDataSource
    private lateinit var affiliateRepository: AffiliateRepositoryImpl
    private lateinit var walletDataSource: FakeAffiliateWalletDataSource
    private lateinit var walletRepository: AffiliateWalletRepositoryImpl
    private lateinit var walletService: AffiliateWalletServiceImpl

    private val tenantId = "TENANT-MOD23"
    private val affiliateId = "AFF-1001"

    @Before
    fun setUp() = runBlocking {
        affiliateDataSource = FakeAffiliateDataSource()
        affiliateRepository = AffiliateRepositoryImpl(affiliateDataSource)

        walletDataSource = FakeAffiliateWalletDataSource()
        walletRepository = AffiliateWalletRepositoryImpl(walletDataSource)

        walletService = AffiliateWalletServiceImpl(
            walletRepository = walletRepository,
            affiliateRepository = affiliateRepository
        )

        // Seed canonical Module 20 Affiliate profile
        val profile = AffiliateProfile(
            affiliateId = affiliateId,
            tenantId = tenantId,
            userId = "USER-AFF-01",
            displayName = "Partner Alpha",
            affiliateCode = "PARTNER-ALPHA",
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
    fun test01_getOrCreateWallet_validAffiliate_createsNewWallet() = runBlocking {
        val res = walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", "STAFF-01")
        assertTrue(res is DomainResult.Success)
        val wallet = (res as DomainResult.Success).data

        assertNotNull(wallet.walletId)
        assertTrue(wallet.walletId.startsWith("WLT-"))
        assertEquals(tenantId, wallet.tenantId)
        assertEquals(affiliateId, wallet.affiliateId)
        assertEquals("BDT", wallet.currency)
        assertEquals(AffiliateWalletStatus.ACTIVE, wallet.status)
    }

    @Test
    fun test02_getOrCreateWallet_nonExistentAffiliate_fails() = runBlocking {
        val res = walletService.getOrCreateWallet(tenantId, "AFF-GHOST-99", "BDT", "STAFF-01")
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("not found in Module 20"))
    }

    @Test
    fun test03_getOrCreateWallet_idempotent_returnsSameWallet() = runBlocking {
        val firstRes = walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", "STAFF-01")
        assertTrue(firstRes is DomainResult.Success)
        val firstWallet = (firstRes as DomainResult.Success).data

        val secondRes = walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", "STAFF-01")
        assertTrue(secondRes is DomainResult.Success)
        val secondWallet = (secondRes as DomainResult.Success).data

        assertEquals(firstWallet.walletId, secondWallet.walletId)
        assertEquals(firstWallet.createdAt, secondWallet.createdAt)
    }

    @Test
    fun test04_updateWalletStatus_transitionsStatus() = runBlocking {
        val createRes = walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", "STAFF-01")
        val wallet = (createRes as DomainResult.Success).data

        // Suspend
        val suspendRes = walletService.updateWalletStatus(tenantId, wallet.walletId, AffiliateWalletStatus.SUSPENDED, "MGR-01")
        assertTrue(suspendRes is DomainResult.Success)
        val suspended = (suspendRes as DomainResult.Success).data
        assertEquals(AffiliateWalletStatus.SUSPENDED, suspended.status)

        // Close
        val closeRes = walletService.updateWalletStatus(tenantId, wallet.walletId, AffiliateWalletStatus.CLOSED, "MGR-01")
        assertTrue(closeRes is DomainResult.Success)
        val closed = (closeRes as DomainResult.Success).data
        assertEquals(AffiliateWalletStatus.CLOSED, closed.status)
    }

    @Test
    fun test05_tenantIsolation_crossTenantWalletAccess_returnsNull() = runBlocking {
        val createRes = walletService.getOrCreateWallet(tenantId, affiliateId, "BDT", "STAFF-01")
        val wallet = (createRes as DomainResult.Success).data

        // Tenant B trying to query Tenant A wallet
        val fetchRes = walletService.getWalletDetails("TENANT-B", wallet.walletId)
        assertTrue(fetchRes is DomainResult.Success)
        assertNull((fetchRes as DomainResult.Success).data)
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
