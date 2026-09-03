package com.sucharu.sucharupro.data.datasource.affiliate

import com.sucharu.sucharupro.domain.model.affiliate.AffiliateIntegrationReadinessState
import com.sucharu.sucharupro.domain.model.affiliate.AffiliateLifecycleIntegrityResult
import com.sucharu.sucharupro.domain.repository.affiliate.AffiliateGovernanceIntegrityRepository

/**
 * In-memory fake implementation of [AffiliateGovernanceIntegrityRepository].
 *
 * Used in unit tests and during local development. Never used in production.
 *
 * THREAD SAFETY: This implementation is single-threaded (no synchronization).
 * Tests must not use it from multiple coroutines concurrently.
 */
class FakeAffiliateGovernanceIntegrityDataSource : AffiliateGovernanceIntegrityRepository {

    // One snapshot per (tenantId, affiliateId)
    private val readinessStore = mutableMapOf<String, AffiliateIntegrationReadinessState>()

    // Append-only list
    private val integrityCheckStore = mutableListOf<AffiliateLifecycleIntegrityResult>()

    override suspend fun saveIntegrationReadiness(
        state: AffiliateIntegrationReadinessState
    ): AffiliateIntegrationReadinessState {
        val key = "${state.tenantId}::${state.affiliateId}"
        readinessStore[key] = state
        return state
    }

    override suspend fun findIntegrationReadiness(
        tenantId: String,
        affiliateId: String
    ): AffiliateIntegrationReadinessState? {
        return readinessStore["$tenantId::$affiliateId"]
    }

    override suspend fun appendIntegrityCheck(
        result: AffiliateLifecycleIntegrityResult
    ): AffiliateLifecycleIntegrityResult {
        integrityCheckStore.add(result)
        return result
    }

    override suspend fun listIntegrityChecks(
        tenantId: String,
        affiliateId: String
    ): List<AffiliateLifecycleIntegrityResult> {
        return integrityCheckStore
            .filter { it.tenantId == tenantId && it.affiliateId == affiliateId }
            .sortedBy { it.checkedAt }
    }

    // Test helpers

    fun clear() {
        readinessStore.clear()
        integrityCheckStore.clear()
    }

    fun allReadinessSnapshots(): List<AffiliateIntegrationReadinessState> =
        readinessStore.values.toList()

    fun allIntegrityChecks(): List<AffiliateLifecycleIntegrityResult> =
        integrityCheckStore.toList()
}
