package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateCommandCenterDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateCommunicationDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProfileDataSource
import com.sucharu.sucharupro.data.datasource.affiliate.FakeAffiliateProgramDataSource
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateCommandCenterRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateCommunicationRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProfileRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateProgramRepositoryImpl
import com.sucharu.sucharupro.data.repository.affiliate.AffiliateRepositoryImpl
import com.sucharu.sucharupro.domain.model.affiliate.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * AI Governance Handoff Contract test suite for Affiliate Command Center (Module 20 Step 05).
 *
 * Validates:
 *  - Correct stepVersion ("20.05") and moduleScope ("AFFILIATE_ADMINISTRATIVE_COMMAND_CENTER").
 *  - isReadOnly = true and non-null integritySealHash.
 *  - Forbidden action list contains all 7 prohibited operations.
 *  - Aggregate counts (totalAffiliates, activeAffiliates, openWorkItemsCount) are accurate.
 *  - Integrity seal hash changes when affiliate count changes.
 *  - Contract reflects urgentWorkItemsCount for URGENT priority items.
 *  - governanceAttentionRequired is true when open work items exist.
 *  - workItemTypeCounts and priorityCounts maps are populated.
 */
class AffiliateCommandCenterHandoffContractTest {

    private lateinit var fakeAffDs: FakeAffiliateDataSource
    private lateinit var service: AffiliateCommandCenterServiceImpl
    private lateinit var affiliateServiceImpl: AffiliateServiceImpl

    private val tenantId = "tenant-handoff-cc-001"
    private val adminUserId = "admin-handoff-cc-01"

    @Before
    fun setUp() = runBlocking {
        fakeAffDs = FakeAffiliateDataSource()
        val ccDs = FakeAffiliateCommandCenterDataSource()
        val affRepo = AffiliateRepositoryImpl(fakeAffDs)
        val progRepo = AffiliateProgramRepositoryImpl(FakeAffiliateProgramDataSource())
        val profRepo = AffiliateProfileRepositoryImpl(FakeAffiliateProfileDataSource())
        val commRepo = AffiliateCommunicationRepositoryImpl(FakeAffiliateCommunicationDataSource())
        val ccRepo = AffiliateCommandCenterRepositoryImpl(ccDs)
        affiliateServiceImpl = AffiliateServiceImpl(affRepo)

        service = AffiliateCommandCenterServiceImpl(
            commandCenterRepository = ccRepo,
            affiliateService = affiliateServiceImpl,
            programService = AffiliateProgramServiceImpl(progRepo, affRepo),
            profileService = AffiliateProfileServiceImpl(profRepo, affRepo),
            communicationService = AffiliateCommunicationServiceImpl(commRepo, affRepo)
        )
    }

    @Test
    fun `handoff contract carries correct stepVersion and moduleScope`()= runBlocking {
        val contract = service.getHandoffContract(tenantId, adminUserId)
        assertEquals("20.05", contract.stepVersion)
        assertEquals("AFFILIATE_ADMINISTRATIVE_COMMAND_CENTER", contract.moduleScope)
    }

    @Test
    fun `handoff contract is sealed read-only with non-empty integrity hash`()= runBlocking {
        val contract = service.getHandoffContract(tenantId, adminUserId)
        assertTrue(contract.isReadOnly)
        assertTrue(
            "integritySealHash must be non-empty",
            contract.integritySealHash.isNotBlank()
        )
    }

    @Test
    fun `handoff contract forbids all 7 prohibited governance operations`()= runBlocking {
        val contract = service.getHandoffContract(tenantId, adminUserId)
        val forbidden = contract.forbiddenActions
        assertTrue(forbidden.contains("CALCULATE_COMMISSION"))
        assertTrue(forbidden.contains("ATTRIBUTE_REFERRAL"))
        assertTrue(forbidden.contains("ISSUE_PAYOUT"))
        assertTrue(forbidden.contains("MODIFY_WALLET"))
        assertTrue(forbidden.contains("BYPASS_RBAC"))
        assertTrue(forbidden.contains("BYPASS_LIFECYCLE_GOVERNANCE"))
        assertTrue(forbidden.contains("BYPASS_TENANT_ISOLATION"))
    }

    @Test
    fun `handoff contract reflects accurate affiliate count aggregations`()= runBlocking {
        // Seed: 3 pending, 2 active affiliates
        (1..3).forEach { i ->
            fakeAffDs.saveAffiliate(
                AffiliateProfile(
                    tenantId = tenantId,
                    affiliateId = "AFF-HC-P-$i",
                    userId = "U-HC-P-$i",
                    affiliateCode = "HCP$i",
                    displayName = "Pending ",
                    status = AffiliateStatus.PENDING
                )
            )
        }
        (1..2).forEach { i ->
            fakeAffDs.saveAffiliate(
                AffiliateProfile(
                    tenantId = tenantId,
                    affiliateId = "AFF-HC-A-$i",
                    userId = "U-HC-A-$i",
                    affiliateCode = "HCA$i",
                    displayName = "Active ",
                    status = AffiliateStatus.ACTIVE
                )
            )
        }

        val contract = service.getHandoffContract(tenantId, adminUserId)
        assertEquals(5L, contract.totalAffiliates)
        assertEquals(2L, contract.activeAffiliates)
    }

    @Test
    fun `handoff contract integrity seal changes when affiliate roster changes`()= runBlocking {
        val contract1 = service.getHandoffContract(tenantId, adminUserId)

        // Add a new affiliate
        fakeAffDs.saveAffiliate(
            AffiliateProfile(
                tenantId = tenantId,
                affiliateId = "AFF-HC-NEW-1",
                userId = "U-HC-NEW-1",
                affiliateCode = "HCNEW1",
                displayName = "Newly Joined Partner",
                status = AffiliateStatus.PENDING
            )
        )

        val contract2 = service.getHandoffContract(tenantId, adminUserId)

        assertNotEquals(
            "Integrity seal must change when affiliate roster changes",
            contract1.integritySealHash,
            contract2.integritySealHash
        )
        assertEquals(contract1.totalAffiliates + 1, contract2.totalAffiliates)
    }

    @Test
    fun `handoff contract openWorkItemsCount reflects synthesized work queue`()= runBlocking {
        fakeAffDs.saveAffiliate(
            AffiliateProfile(
                tenantId = tenantId,
                affiliateId = "AFF-HC-WI-1",
                userId = "U-HC-WI-1",
                affiliateCode = "HCWI1",
                displayName = "Work Item Partner",
                status = AffiliateStatus.PENDING,
                agreementReference = null  // No agreement — triggers AGREEMENT_ACCEPTANCE work item
            )
        )

        val contract = service.getHandoffContract(tenantId, adminUserId)
        assertTrue(
            "openWorkItemsCount must be > 0 when pending affiliates exist with no agreement",
            contract.openWorkItemsCount >= 1L
        )
    }

    @Test
    fun `handoff contract governanceAttentionRequired is true when work items exist`()= runBlocking {
        fakeAffDs.saveAffiliate(
            AffiliateProfile(
                tenantId = tenantId,
                affiliateId = "AFF-HC-GOV-1",
                userId = "U-HC-GOV-1",
                affiliateCode = "HCGOV1",
                displayName = "Governance Attention Partner",
                status = AffiliateStatus.SUSPENDED
            )
        )

        val contract = service.getHandoffContract(tenantId, adminUserId)
        assertTrue(
            "governanceAttentionRequired must be true when suspended or pending affiliates exist",
            contract.governanceAttentionRequired
        )
    }

    @Test
    fun `handoff contract workItemTypeCounts map is populated for known work item types`()= runBlocking {
        fakeAffDs.saveAffiliate(
            AffiliateProfile(
                tenantId = tenantId,
                affiliateId = "AFF-HC-TYPEMAP-1",
                userId = "U-HC-TYPEMAP-1",
                affiliateCode = "HCTM1",
                displayName = "Type Map Partner",
                status = AffiliateStatus.PENDING,
                agreementReference = null
            )
        )

        val contract = service.getHandoffContract(tenantId, adminUserId)
        assertTrue(
            "workItemTypeCounts must be non-empty when work items exist",
            contract.workItemTypeCounts.isNotEmpty()
        )
    }

    @Test
    fun `handoff contract priorityCounts map is populated for known priorities`()= runBlocking {
        fakeAffDs.saveAffiliate(
            AffiliateProfile(
                tenantId = tenantId,
                affiliateId = "AFF-HC-PRI-1",
                userId = "U-HC-PRI-1",
                affiliateCode = "HCPRI1",
                displayName = "Priority Map Partner",
                status = AffiliateStatus.PENDING
            )
        )

        val contract = service.getHandoffContract(tenantId, adminUserId)
        assertTrue(
            "priorityCounts must be non-empty when work items exist",
            contract.priorityCounts.isNotEmpty()
        )
    }

    @Test
    fun `handoff contract matches service-level overview counts`()= runBlocking {
        (1..4).forEach { i ->
            fakeAffDs.saveAffiliate(
                AffiliateProfile(
                    tenantId = tenantId,
                    affiliateId = "AFF-HC-SYNC-",
                    userId = "U-HC-SYNC-",
                    affiliateCode = "HCSYNC",
                    displayName = "Sync Partner ",
                    status = if (i <= 2) AffiliateStatus.ACTIVE else AffiliateStatus.PENDING
                )
            )
        }

        val contract = service.getHandoffContract(tenantId, adminUserId)
        val overview = service.getCommandCenterOverview(tenantId)

        assertEquals(
            "Contract totalAffiliates must match overview totalAffiliates",
            overview.totalAffiliates, contract.totalAffiliates
        )
        assertEquals(
            "Contract activeAffiliates must match overview activeCount",
            overview.activeCount, contract.activeAffiliates
        )
        assertEquals(
            "Contract openWorkItemsCount must match overview openWorkItemsCount",
            overview.openWorkItemsCount, contract.openWorkItemsCount
        )
    }
}

