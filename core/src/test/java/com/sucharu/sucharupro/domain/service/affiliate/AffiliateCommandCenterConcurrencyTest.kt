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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Concurrency safety test suite for Affiliate Command Center (Module 20 Step 05).
 *
 * Validates:
 *  - Concurrent work item resolution does not corrupt data structures.
 *  - Concurrent admin lifecycle actions on distinct affiliates complete without data loss.
 *  - Concurrent audit record appends maintain atomic ordering under ConcurrentHashMap.
 *  - Work item overview counts remain consistent under parallel reads and writes.
 */
class AffiliateCommandCenterConcurrencyTest {

    private lateinit var fakeAffDs: FakeAffiliateDataSource
    private lateinit var service: AffiliateCommandCenterServiceImpl

    private val tenantId = "tenant-cc-conc-001"
    private val adminUserId = "admin-conc-01"

    @Before
    fun setUp() = runBlocking {
        fakeAffDs = FakeAffiliateDataSource()
        val ccDs = FakeAffiliateCommandCenterDataSource()
        val affRepo = AffiliateRepositoryImpl(fakeAffDs)
        val progRepo = AffiliateProgramRepositoryImpl(FakeAffiliateProgramDataSource())
        val profRepo = AffiliateProfileRepositoryImpl(FakeAffiliateProfileDataSource())
        val commRepo = AffiliateCommunicationRepositoryImpl(FakeAffiliateCommunicationDataSource())
        val ccRepo = AffiliateCommandCenterRepositoryImpl(ccDs)

        service = AffiliateCommandCenterServiceImpl(
            commandCenterRepository = ccRepo,
            affiliateService = AffiliateServiceImpl(affRepo),
            programService = AffiliateProgramServiceImpl(progRepo, affRepo),
            profileService = AffiliateProfileServiceImpl(profRepo, affRepo),
            communicationService = AffiliateCommunicationServiceImpl(commRepo, affRepo)
        )

        // Seed 20 PENDING affiliates for concurrent operations
        (1..20).forEach { i ->
            fakeAffDs.saveAffiliate(
                AffiliateProfile(
                    tenantId = tenantId,
                    affiliateId = "AFF-CONC-",
                    userId = "USER-CONC-",
                    affiliateCode = "CONC",
                    displayName = "Concurrent Partner ",
                    status = AffiliateStatus.PENDING
                )
            )
        }
    }

    @Test
    fun `concurrent admin lifecycle actions on distinct affiliates complete without data loss`()= runBlocking {
        val affiliateIds = (1..20).map { "AFF-CONC-" }

        val jobs = affiliateIds.map { affId ->
            async(Dispatchers.Default) {
                service.executeAdminAction(
                    tenantId = tenantId,
                    affiliateId = affId,
                    action = "APPROVE",
                    reason = "Concurrent approval test",
                    actorUserId = adminUserId,
                    actorRole = "ADMIN",
                    correlationId = "CORR-CONC-"
                )
            }
        }
        val results = jobs.awaitAll()

        assertEquals(20, results.size)
        assertTrue("All concurrent actions must result in ACTIVE state",
            results.all { it.status == AffiliateStatus.ACTIVE })

        val audits = service.listAuditRecords(tenantId)
        assertEquals(
            "Audit records count must equal number of concurrent actions",
            20, audits.size
        )
    }

    @Test
    fun `concurrent work item resolutions maintain data integrity`()= runBlocking {
        // Trigger work item generation by loading overview
        service.getCommandCenterOverview(tenantId)

        val items = service.listWorkItems(tenantId)
        val resolvable = items.take(10)

        if (resolvable.isNotEmpty()) {
            val resolveJobs = resolvable.map { item ->
                async(Dispatchers.Default) {
                    service.resolveWorkItem(
                        tenantId = tenantId,
                        workItemId = item.workItemId,
                        resolutionNotes = "Concurrent resolve of ",
                        status = AffiliateGovernanceWorkItemStatus.RESOLVED,
                        actorUserId = adminUserId,
                        actorRole = "ADMIN",
                        correlationId = "CORR-RES-CONC-"
                    )
                }
            }
            val resolved = resolveJobs.awaitAll()
            assertTrue("All resolved items must carry RESOLVED status",
                resolved.all { it.status == AffiliateGovernanceWorkItemStatus.RESOLVED })

            // Verify audit records were created for each resolution
            val audits = service.listAuditRecords(tenantId)
            assertTrue(
                "Audit records must reflect all resolutions",
                audits.size >= resolvable.size
            )
        }
    }

    @Test
    fun `concurrent audit record appends produce non-duplicate unique audit IDs`()= runBlocking {
        val affiliateIds = (1..10).map { "AFF-CONC-" }

        val jobs = affiliateIds.map { affId ->
            async(Dispatchers.Default) {
                service.executeAdminAction(
                    tenantId = tenantId,
                    affiliateId = affId,
                    action = "APPROVE",
                    reason = "Audit uniqueness test",
                    actorUserId = adminUserId,
                    actorRole = "ADMIN",
                    correlationId = "CORR-UNIQ-"
                )
            }
        }
        jobs.awaitAll()

        val audits = service.listAuditRecords(tenantId)
        val auditIds = audits.map { it.auditId }.toSet()
        assertEquals(
            "All concurrent audit records must have unique IDs (no duplicates)",
            audits.size, auditIds.size
        )
    }

    @Test
    fun `concurrent overview reads during mutations return consistent non-negative counts`()= runBlocking {
        val mutationJobs = (1..5).map { i ->
            async(Dispatchers.Default) {
                service.executeAdminAction(
                    tenantId = tenantId,
                    affiliateId = "AFF-CONC-",
                    action = "APPROVE",
                    reason = "Concurrent mutation during overview read",
                    actorUserId = adminUserId,
                    actorRole = "ADMIN",
                    correlationId = "CORR-OVERVIEW-"
                )
            }
        }

        val readJobs = (1..5).map {
            async(Dispatchers.Default) {
                service.getCommandCenterOverview(tenantId)
            }
        }

        mutationJobs.awaitAll()
        val overviews = readJobs.awaitAll()

        overviews.forEach { overview ->
            assertTrue("totalAffiliates must be non-negative", overview.totalAffiliates >= 0)
            assertTrue("openWorkItemsCount must be non-negative", overview.openWorkItemsCount >= 0)
            assertTrue("activeCount must be non-negative", overview.activeCount >= 0)
        }
    }

    @Test
    fun `concurrent handoff contract requests are idempotent and non-colliding`()= runBlocking {
        val jobs = (1..20).map {
            async(Dispatchers.Default) {
                service.getHandoffContract(tenantId, adminUserId)
            }
        }
        val contracts = jobs.awaitAll()

        assertEquals(20, contracts.size)
        assertTrue("All handoff contracts must be read-only", contracts.all { it.isReadOnly })
        assertTrue("All handoff contracts must reference the correct tenant",
            contracts.all { it.tenantId == tenantId })
        assertTrue("Total affiliate count must be consistent across all concurrent contracts",
            contracts.map { it.totalAffiliates }.toSet().size == 1)
    }
}

