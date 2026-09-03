package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*

/**
 * Service interface for Substrate Release & Revision Governance.
 * Module 19 Step 05.
 */
interface SubstrateReleaseGovernanceService {

    suspend fun evaluateCancellation(
        tenantId: String,
        input: SubstrateReleaseGovernanceEngine.EvaluationInput
    ): SubstrateReleaseGovernanceRecord

    suspend fun evaluateRevision(
        tenantId: String,
        input: SubstrateReleaseGovernanceEngine.EvaluationInput
    ): SubstrateReleaseGovernanceRecord

    suspend fun approveRelease(
        tenantId: String,
        governanceId: String,
        actor: String,
        notes: String? = null
    ): SubstrateReleaseGovernanceRecord

    suspend fun executeRelease(
        tenantId: String,
        governanceId: String,
        actor: String
    ): SubstrateReleaseGovernanceRecord

    suspend fun rejectRelease(
        tenantId: String,
        governanceId: String,
        actor: String,
        reason: String
    ): SubstrateReleaseGovernanceRecord

    suspend fun getGovernanceRecord(tenantId: String, governanceId: String): SubstrateReleaseGovernanceRecord?

    suspend fun listGovernanceRecords(tenantId: String, limit: Int = 50): List<SubstrateReleaseGovernanceRecord>

    suspend fun exportHandoffContract(
        tenantId: String,
        governanceId: String
    ): Module19Step05SubstrateReleaseGovernanceHandoffContract
}
