package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.affiliate.*
import com.sucharu.sucharupro.data.repository.affiliate.*
import com.sucharu.sucharupro.domain.model.affiliate.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Service Integration Test for [AffiliateGovernanceIntegrityServiceImpl].
 * Module 20 Step 06.
 */
class AffiliateGovernanceIntegrityServiceTest {

    private lateinit var integrityDataSource: FakeAffiliateGovernanceIntegrityDataSource
    private lateinit var affiliateDataSource: FakeAffiliateDataSource
    private lateinit var programDataSource: FakeAffiliateProgramDataSource
    private lateinit var profileDataSource: FakeAffiliateProfileDataSource
    private lateinit var commsDataSource: FakeAffiliateCommunicationDataSource
    private lateinit var commandCenterDataSource: FakeAffiliateCommandCenterDataSource

    private lateinit var affiliateService: AffiliateService
    private lateinit var profileService: AffiliateProfileService
    private lateinit var programService: AffiliateProgramService
    private lateinit var communicationService: AffiliateCommunicationService
    private lateinit var commandCenterService: AffiliateCommandCenterService
    private lateinit var integrityService: AffiliateGovernanceIntegrityService

    private val tenantId = "TENANT-INTEGRITY-SVC"

    private val adminPrincipal = AuthenticatedPrincipal(
        userId = "admin-1",
        projectId = tenantId,
        username = "admin_user",
        role = UserRole.ADMIN
    )

    private val affiliatePrincipal = AuthenticatedPrincipal(
        userId = "usr-aff-100",
        projectId = tenantId,
        username = "affiliate_100",
        role = UserRole.AFFILIATE
    )

    @Before
    fun setup() {
        integrityDataSource = FakeAffiliateGovernanceIntegrityDataSource()
        affiliateDataSource = FakeAffiliateDataSource()
        programDataSource = FakeAffiliateProgramDataSource()
        profileDataSource = FakeAffiliateProfileDataSource()
        commsDataSource = FakeAffiliateCommunicationDataSource()
        commandCenterDataSource = FakeAffiliateCommandCenterDataSource()

        val affRepo = AffiliateRepositoryImpl(affiliateDataSource)
        val progRepo = AffiliateProgramRepositoryImpl(programDataSource)
        val profRepo = AffiliateProfileRepositoryImpl(profileDataSource)
        val commsRepo = AffiliateCommunicationRepositoryImpl(commsDataSource)
        val ccRepo = AffiliateCommandCenterRepositoryImpl(commandCenterDataSource)

        affiliateService = AffiliateServiceImpl(affRepo)
        programService = AffiliateProgramServiceImpl(progRepo, affRepo)
        profileService = AffiliateProfileServiceImpl(profRepo, affRepo)
        communicationService = AffiliateCommunicationServiceImpl(commsRepo, affRepo)
        commandCenterService = AffiliateCommandCenterServiceImpl(
            commandCenterRepository = ccRepo,
            affiliateService = affiliateService,
            programService = programService,
            profileService = profileService,
            communicationService = communicationService
        )

        integrityService = AffiliateGovernanceIntegrityServiceImpl(
            affiliateService = affiliateService,
            profileService = profileService,
            programService = programService,
            communicationService = communicationService,
            commandCenterService = commandCenterService,
            integrityRepository = integrityDataSource
        )
    }

    @Test
    fun `assessIntegrity performs cross-step checks and stores snapshot`() {
        runBlocking {
            // Create affiliate
            val cmd = CreateAffiliateCommand(
                userId = "usr-aff-100",
                displayName = "Integrity Test Referral",
                affiliateCode = "INT_REF_100",
                affiliateType = AffiliateType.INDIVIDUAL,
                contactEmail = "test@sucharu.pro",
                agreementReference = "AGR-2026-TEST"
            )
            val created = affiliateService.createAffiliate(tenantId, cmd, affiliatePrincipal)

            val result = integrityService.assessIntegrity(tenantId, created.affiliateId, adminPrincipal.userId)
            assertNotNull(result)
            assertEquals(tenantId, result.tenantId)
            assertEquals(created.affiliateId, result.affiliateId)

            val storedChecks = integrityService.listIntegrityChecks(tenantId, created.affiliateId)
            assertEquals(1, storedChecks.size)
            assertEquals(result.checkId, storedChecks[0].checkId)
        }
    }

    @Test
    fun `buildIntegrationReadiness derives readiness state and persists snapshot`() {
        runBlocking {
            val cmd = CreateAffiliateCommand(
                userId = "usr-aff-100",
                displayName = "Readiness Test Referral",
                affiliateCode = "READINESS_100",
                affiliateType = AffiliateType.INDIVIDUAL,
                agreementReference = "AGR-2026-TEST"
            )
            val created = affiliateService.createAffiliate(tenantId, cmd, affiliatePrincipal)

            val readiness = integrityService.buildIntegrationReadiness(tenantId, created.affiliateId, adminPrincipal.userId)
            assertNotNull(readiness)
            assertEquals(created.affiliateId, readiness.affiliateId)

            val stored = integrityService.getStoredIntegrationReadiness(tenantId, created.affiliateId)
            assertNotNull(stored)
            assertEquals(readiness.integrityHash, stored?.integrityHash)
        }
    }

    @Test
    fun `getFinalHandoffContract synthesizes read-only sealed contract`() {
        runBlocking {
            val cmd = CreateAffiliateCommand(
                userId = "usr-aff-100",
                displayName = "Handoff Test Referral",
                affiliateCode = "HANDOFF_100",
                affiliateType = AffiliateType.INDIVIDUAL,
                agreementReference = "AGR-2026-TEST"
            )
            val created = affiliateService.createAffiliate(tenantId, cmd, affiliatePrincipal)

            val handoff = integrityService.getFinalHandoffContract(tenantId, created.affiliateId, adminPrincipal.userId)
            assertNotNull(handoff)
            assertEquals("v20.06", handoff.contractVersion)
            assertTrue(handoff.isReadOnly)
            assertEquals(created.affiliateCode, handoff.affiliateCode)
        }
    }

    @Test
    fun `verifyAuditChain confirms audit chain integrity`() {
        runBlocking {
            val cmd = CreateAffiliateCommand(
                userId = "usr-aff-100",
                displayName = "Audit Chain Test",
                affiliateCode = "AUD_CHAIN_100",
                affiliateType = AffiliateType.INDIVIDUAL,
                agreementReference = "AGR-2026-TEST"
            )
            val created = affiliateService.createAffiliate(tenantId, cmd, affiliatePrincipal)

            val chainResult = integrityService.verifyAuditChain(tenantId, created.affiliateId)
            assertTrue(chainResult.isChainIntact)
            assertEquals(1, chainResult.totalRecordsChecked)
        }
    }
}
