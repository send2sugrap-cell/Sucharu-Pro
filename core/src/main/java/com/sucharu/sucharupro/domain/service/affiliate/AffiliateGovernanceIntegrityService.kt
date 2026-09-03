package com.sucharu.sucharupro.domain.service.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.*

/**
 * Domain Service Interface: Final Governance Integrity for Affiliate Management (Module 20 Step 06).
 *
 * This service coordinates cross-step consistency validation and provides the
 * authoritative integration readiness contract for Modules 21–24.
 *
 * SECURITY INVARIANTS:
 *  - AI_AGENT actors: READ-ONLY. All mutation attempts throw ForbiddenException upstream.
 *  - Tenant isolation: All queries scoped by tenantId (enforced via RLS at the DB layer).
 *  - No Module 21/22/23/24 business logic is present or invoked here.
 */
interface AffiliateGovernanceIntegrityService {

    /**
     * Performs a full cross-step lifecycle integrity assessment for a single affiliate.
     *
     * Detects CRITICAL and HIGH severity violations across Steps 01–05.
     * Returns a deterministic, hash-sealed [AffiliateLifecycleIntegrityResult].
     */
    suspend fun assessIntegrity(
        tenantId: String,
        affiliateId: String,
        assessorId: String
    ): AffiliateLifecycleIntegrityResult

    /**
     * Builds the integration readiness state for an affiliate.
     *
     * Computes step-wise readiness gates and derives the four downstream readiness flags:
     *  - isReadyForAttribution  (Module 21)
     *  - isReadyForCommission   (Module 22)
     *  - isReadyForPayout       (Module 23)
     *  - isReadyForAnalytics    (Module 24)
     */
    suspend fun buildIntegrationReadiness(
        tenantId: String,
        affiliateId: String,
        assessorId: String
    ): AffiliateIntegrationReadinessState

    /**
     * Returns the master Module 20 Step 06 final governance handoff contract.
     *
     * This is the single authoritative integration handshake between Module 20
     * and Modules 21–24. The contract is cryptographically sealed and read-only.
     */
    suspend fun getFinalHandoffContract(
        tenantId: String,
        affiliateId: String,
        requestingUserId: String
    ): Module20Step06FinalGovernanceHandoffContract

    /**
     * Verifies the SHA-256 audit chain integrity for an affiliate's audit records.
     *
     * Detects tamper attempts by recomputing and comparing chain hashes.
     */
    suspend fun verifyAuditChain(
        tenantId: String,
        affiliateId: String
    ): AuditChainVerificationResult

    /**
     * Retrieves the most recently stored [AffiliateIntegrationReadinessState] for an affiliate,
     * or null if none has been computed yet.
     */
    suspend fun getStoredIntegrationReadiness(
        tenantId: String,
        affiliateId: String
    ): AffiliateIntegrationReadinessState?

    /**
     * Lists all integrity check results recorded for an affiliate (audit history).
     */
    suspend fun listIntegrityChecks(
        tenantId: String,
        affiliateId: String
    ): List<AffiliateLifecycleIntegrityResult>
}
