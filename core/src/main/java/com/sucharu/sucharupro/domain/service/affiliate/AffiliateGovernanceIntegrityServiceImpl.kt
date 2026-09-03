package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateGovernanceIntegrityRepository

/**
 * Production implementation of [AffiliateGovernanceIntegrityService].
 * Module 20 Step 06: Final Governance, Integrity & Cross-Module Readiness.
 *
 * Wires together Steps 01–05 service outputs to produce cross-step integrity
 * assessments and the final Module 20 handoff contract for Modules 21–24.
 *
 * SECURITY INVARIANTS:
 *  - AI_AGENT actors: all mutation paths blocked upstream by each service's
 *    assertMutationAllowed() guard. This service is READ-ONLY for all callers.
 *  - Tenant isolation: every call scoped by tenantId; no cross-tenant data access.
 *  - No Module 21/22/23/24 business logic is present in this file.
 */
class AffiliateGovernanceIntegrityServiceImpl(
    private val affiliateService: AffiliateService,
    private val profileService: AffiliateProfileService,
    private val programService: AffiliateProgramService,
    private val communicationService: AffiliateCommunicationService,
    private val commandCenterService: AffiliateCommandCenterService,
    private val integrityRepository: AffiliateGovernanceIntegrityRepository
) : AffiliateGovernanceIntegrityService {

    // ─────────────────────────────────────────────────────────────────
    // Lifecycle Integrity Assessment
    // ─────────────────────────────────────────────────────────────────

    override suspend fun assessIntegrity(
        tenantId: String,
        affiliateId: String,
        assessorId: String
    ): AffiliateLifecycleIntegrityResult {
        val (profile, eligibility, opProfile, verifications, enrollments, communications, auditRecords) =
            gatherAggregateData(tenantId, affiliateId)

        val result = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = eligibility,
            operationalProfile = opProfile,
            verifications = verifications,
            enrollments = enrollments,
            communications = communications,
            auditRecords = auditRecords,
            checkedBy = assessorId
        )

        // Persist (append-only) for governance history
        integrityRepository.appendIntegrityCheck(result)
        return result
    }

    // ─────────────────────────────────────────────────────────────────
    // Integration Readiness State
    // ─────────────────────────────────────────────────────────────────

    override suspend fun buildIntegrationReadiness(
        tenantId: String,
        affiliateId: String,
        assessorId: String
    ): AffiliateIntegrationReadinessState {
        val (profile, eligibility, opProfile, verifications, enrollments, _, _) =
            gatherAggregateData(tenantId, affiliateId)

        val workItems = runCatching {
            commandCenterService.listWorkItems(tenantId = tenantId, affiliateId = affiliateId)
        }.getOrDefault(emptyList())

        val notifPrefs = runCatching {
            communicationService.getPreferences(tenantId, affiliateId)
        }.getOrDefault(emptyList())

        val readiness = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = profile,
            eligibility = eligibility,
            operationalProfile = opProfile,
            verifications = verifications,
            enrollments = enrollments,
            workItems = workItems,
            notificationPreferences = notifPrefs,
            assessedBy = assessorId
        )

        // Persist latest snapshot (upsert)
        integrityRepository.saveIntegrationReadiness(readiness)
        return readiness
    }

    // ─────────────────────────────────────────────────────────────────
    // Final Handoff Contract
    // ─────────────────────────────────────────────────────────────────

    override suspend fun getFinalHandoffContract(
        tenantId: String,
        affiliateId: String,
        requestingUserId: String
    ): Module20Step06FinalGovernanceHandoffContract {
        val (profile, eligibility, opProfile, verifications, enrollments, _, _) =
            gatherAggregateData(tenantId, affiliateId)

        val workItems = runCatching {
            commandCenterService.listWorkItems(tenantId = tenantId, affiliateId = affiliateId)
        }.getOrDefault(emptyList())

        val notifPrefs = runCatching {
            communicationService.getPreferences(tenantId, affiliateId)
        }.getOrDefault(emptyList())

        val auditRecords = runCatching {
            affiliateService.listAuditRecords(
                tenantId = tenantId,
                affiliateId = affiliateId,
                actorPrincipal = buildReadOnlyPrincipal(tenantId)
            )
        }.getOrDefault(emptyList())

        val integrityResult = AffiliateGovernanceIntegrityEngine.checkLifecycleIntegrity(
            profile = profile,
            eligibility = eligibility,
            operationalProfile = opProfile,
            verifications = verifications,
            enrollments = enrollments,
            communications = emptyList(), // communications not factored into readiness blocking
            auditRecords = auditRecords,
            checkedBy = requestingUserId
        )

        val readiness = AffiliateGovernanceIntegrityEngine.buildIntegrationReadinessState(
            profile = profile,
            eligibility = eligibility,
            operationalProfile = opProfile,
            verifications = verifications,
            enrollments = enrollments,
            workItems = workItems,
            notificationPreferences = notifPrefs,
            assessedBy = requestingUserId
        )

        return AffiliateGovernanceIntegrityEngine.synthesizeFinalHandoffContract(
            profile = profile,
            eligibility = eligibility,
            integrationReadiness = readiness,
            integrityResult = integrityResult
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // Audit Chain Verification
    // ─────────────────────────────────────────────────────────────────

    override suspend fun verifyAuditChain(
        tenantId: String,
        affiliateId: String
    ): AuditChainVerificationResult {
        val records = runCatching {
            affiliateService.listAuditRecords(tenantId = tenantId, affiliateId = affiliateId, actorPrincipal = buildReadOnlyPrincipal(tenantId))
        }.getOrDefault(emptyList())

        return AffiliateGovernanceIntegrityEngine.verifyAuditChainIntegrity(
            tenantId = tenantId,
            affiliateId = affiliateId,
            records = records
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // Stored State Retrieval
    // ─────────────────────────────────────────────────────────────────

    override suspend fun getStoredIntegrationReadiness(
        tenantId: String,
        affiliateId: String
    ): AffiliateIntegrationReadinessState? {
        return integrityRepository.findIntegrationReadiness(tenantId, affiliateId)
    }

    override suspend fun listIntegrityChecks(
        tenantId: String,
        affiliateId: String
    ): List<AffiliateLifecycleIntegrityResult> {
        return integrityRepository.listIntegrityChecks(tenantId, affiliateId)
    }

    // ─────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────

    /**
     * Gathers all affiliate aggregate data from Steps 01–04 services.
     *
     * Each service call is wrapped in runCatching so that a failure in one step
     * (e.g. no operational profile exists yet) does not prevent integrity checks
     * from running on the data that is available.
     */
    private suspend fun gatherAggregateData(
        tenantId: String,
        affiliateId: String
    ): AffiliateAggregateData {
        val principal = buildReadOnlyPrincipal(tenantId)

        val profile = affiliateService.getAffiliateById(tenantId, affiliateId, principal)

        val eligibility = runCatching {
            affiliateService.evaluateEligibility(tenantId, affiliateId, principal)
        }.getOrNull()

        val opProfile = runCatching {
            profileService.getProfileByAffiliateId(tenantId, affiliateId)
        }.getOrNull()

        val verifications = runCatching {
            profileService.listVerifications(tenantId, affiliateId)
        }.getOrDefault(emptyList())

        val enrollments = runCatching {
            programService.findEnrollmentsByAffiliate(tenantId, affiliateId)
        }.getOrDefault(emptyList())

        val communications = runCatching {
            communicationService.listCommunications(tenantId, affiliateId)
        }.getOrDefault(emptyList())

        val auditRecords = runCatching {
            affiliateService.listAuditRecords(tenantId = tenantId, affiliateId = affiliateId, actorPrincipal = principal)
        }.getOrDefault(emptyList())

        return AffiliateAggregateData(
            profile = profile,
            eligibility = eligibility,
            opProfile = opProfile,
            verifications = verifications,
            enrollments = enrollments,
            communications = communications,
            auditRecords = auditRecords
        )
    }

    /**
     * Builds a minimal read-only [AuthenticatedPrincipal] for internal service-to-service reads.
     *
     * This principal is used exclusively for read-path calls within the integrity service.
     * No mutation can be performed with it because all mutation paths check
     * `assertMutationAllowed()` independently in each downstream service.
     */
    private fun buildReadOnlyPrincipal(tenantId: String): com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal {
        return com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal(
            userId = "GOVERNANCE_INTEGRITY_ENGINE",
            projectId = tenantId,
            username = "governance_integrity_engine",
            role = com.sucharu.sucharupro.data.api.model.UserRole.MANAGER
        )
    }

    /**
     * Destructurable tuple carrying all affiliate domain data gathered in a single pass.
     */
    private data class AffiliateAggregateData(
        val profile: AffiliateProfile,
        val eligibility: AffiliateEligibility?,
        val opProfile: AffiliateOperationalProfile?,
        val verifications: List<AffiliateVerificationRecord>,
        val enrollments: List<AffiliateEnrollment>,
        val communications: List<AffiliateCommunicationRecord>,
        val auditRecords: List<AffiliateAuditRecord>
    )
}
