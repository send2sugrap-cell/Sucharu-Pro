package com.sucharu.sucharupro.domain.repository.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateIntegrationReadinessState
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateLifecycleIntegrityResult

/**
 * Domain Repository Interface for Affiliate Governance Integrity persistence.
 * Module 20 Step 06.
 *
 * All tables backed by this repository are RLS-protected and tenant-isolated.
 */
interface AffiliateGovernanceIntegrityRepository {

    /**
     * Upserts the integration readiness snapshot for an affiliate.
     * One row per (tenantId, affiliateId) — previous snapshot is replaced.
     */
    suspend fun saveIntegrationReadiness(
        state: AffiliateIntegrationReadinessState
    ): AffiliateIntegrationReadinessState

    /**
     * Retrieves the most recent integration readiness snapshot for an affiliate, or null.
     */
    suspend fun findIntegrationReadiness(
        tenantId: String,
        affiliateId: String
    ): AffiliateIntegrationReadinessState?

    /**
     * Appends an integrity check result to the append-only integrity check history.
     */
    suspend fun appendIntegrityCheck(
        result: AffiliateLifecycleIntegrityResult
    ): AffiliateLifecycleIntegrityResult

    /**
     * Lists all integrity check results for an affiliate (ascending by checkedAt).
     */
    suspend fun listIntegrityChecks(
        tenantId: String,
        affiliateId: String
    ): List<AffiliateLifecycleIntegrityResult>
}
