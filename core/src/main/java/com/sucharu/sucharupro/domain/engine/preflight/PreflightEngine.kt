package com.sucharu.sucharupro.domain.engine.preflight

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.PreflightExecutionContext
import com.sucharu.sucharupro.domain.preflight.PreflightRun

/**
 * Technical execution engine contract for Automated Proofing & Preflight.
 */
interface PreflightEngine {
    suspend fun runPreflight(
        context: PreflightExecutionContext,
        requestedBy: String,
        idempotencyKey: String? = null
    ): DomainResult<PreflightRun>
}
