package com.sucharu.sucharupro.domain.service.preflight

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.PreflightExecutionContext
import com.sucharu.sucharupro.domain.preflight.PreflightFinding
import com.sucharu.sucharupro.domain.preflight.PreflightRun

/**
 * Domain Service interface for Preflight operations.
 */
interface PreflightService {
    suspend fun runPreflight(context: PreflightExecutionContext, requestedBy: String, idempotencyKey: String? = null): DomainResult<PreflightRun>
    suspend fun getPreflightRunDetails(tenantId: String, runId: String): DomainResult<PreflightRun?>
    suspend fun listFindingsForRun(tenantId: String, runId: String): DomainResult<List<PreflightFinding>>
    suspend fun listPreflightRunsForArtwork(tenantId: String, artworkId: String, limit: Int = 100): DomainResult<List<PreflightRun>>
}
