package com.sucharu.sucharupro.domain.service.preflight

import com.sucharu.sucharupro.domain.engine.preflight.PreflightEngine
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.PreflightExecutionContext
import com.sucharu.sucharupro.domain.preflight.PreflightFinding
import com.sucharu.sucharupro.domain.preflight.PreflightRun
import com.sucharu.sucharupro.domain.repository.preflight.PreflightRepository

/**
 * Domain Service implementation for Preflight operations.
 */
class PreflightServiceImpl(
    private val preflightEngine: PreflightEngine,
    private val preflightRepository: PreflightRepository
) : PreflightService {

    override suspend fun runPreflight(
        context: PreflightExecutionContext,
        requestedBy: String,
        idempotencyKey: String?
    ): DomainResult<PreflightRun> {
        return preflightEngine.runPreflight(context, requestedBy, idempotencyKey)
    }

    override suspend fun getPreflightRunDetails(
        tenantId: String,
        runId: String
    ): DomainResult<PreflightRun?> {
        return preflightRepository.getRunById(tenantId, runId)
    }

    override suspend fun listFindingsForRun(
        tenantId: String,
        runId: String
    ): DomainResult<List<PreflightFinding>> {
        return preflightRepository.listFindingsByRun(tenantId, runId)
    }

    override suspend fun listPreflightRunsForArtwork(
        tenantId: String,
        artworkId: String,
        limit: Int
    ): DomainResult<List<PreflightRun>> {
        return preflightRepository.listRunsByArtwork(tenantId, artworkId, limit)
    }
}
