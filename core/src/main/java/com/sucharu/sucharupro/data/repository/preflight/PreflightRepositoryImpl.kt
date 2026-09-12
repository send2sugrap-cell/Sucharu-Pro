package com.sucharu.sucharupro.data.repository.preflight

import com.sucharu.sucharupro.data.datasource.preflight.PreflightDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.*
import com.sucharu.sucharupro.domain.repository.preflight.PreflightRepository

/**
 * Production Repository implementation delegating to PreflightDataSource with validation.
 */
class PreflightRepositoryImpl(
    private val dataSource: PreflightDataSource
) : PreflightRepository {

    override suspend fun saveRun(run: PreflightRun): DomainResult<PreflightRun> {
        val validation = PreflightValidator.validateRun(run)
        if (validation is DomainResult.Error) {
            return validation
        }
        return dataSource.saveRun(run)
    }

    override suspend fun getRunById(tenantId: String, runId: String): DomainResult<PreflightRun?> {
        if (tenantId.isBlank() || runId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Run ID cannot be blank.")
        }
        return dataSource.getRunById(tenantId, runId)
    }

    override suspend fun getRunByIdempotencyKey(
        tenantId: String,
        idempotencyKey: String
    ): DomainResult<PreflightRun?> {
        if (tenantId.isBlank() || idempotencyKey.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Idempotency Key cannot be blank.")
        }
        return dataSource.getRunByIdempotencyKey(tenantId, idempotencyKey)
    }

    override suspend fun listRunsByArtwork(
        tenantId: String,
        artworkId: String,
        limit: Int
    ): DomainResult<List<PreflightRun>> {
        if (tenantId.isBlank() || artworkId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Artwork ID cannot be blank.")
        }
        return dataSource.listRunsByArtwork(tenantId, artworkId, limit)
    }

    override suspend fun saveRuleExecutions(executions: List<PreflightRuleExecution>): DomainResult<List<PreflightRuleExecution>> {
        return dataSource.saveRuleExecutions(executions)
    }

    override suspend fun listRuleExecutionsByRun(
        tenantId: String,
        runId: String
    ): DomainResult<List<PreflightRuleExecution>> {
        if (tenantId.isBlank() || runId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Run ID cannot be blank.")
        }
        return dataSource.listRuleExecutionsByRun(tenantId, runId)
    }

    override suspend fun saveFindings(findings: List<PreflightFinding>): DomainResult<List<PreflightFinding>> {
        return dataSource.saveFindings(findings)
    }

    override suspend fun listFindingsByRun(
        tenantId: String,
        runId: String
    ): DomainResult<List<PreflightFinding>> {
        if (tenantId.isBlank() || runId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Run ID cannot be blank.")
        }
        return dataSource.listFindingsByRun(tenantId, runId)
    }

    override suspend fun getFindingById(tenantId: String, findingId: String): DomainResult<PreflightFinding?> {
        if (tenantId.isBlank() || findingId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Finding ID cannot be blank.")
        }
        return dataSource.getFindingById(tenantId, findingId)
    }

    override suspend fun updateFinding(finding: PreflightFinding): DomainResult<PreflightFinding> {
        if (finding.tenantId.isBlank() || finding.findingId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Finding ID cannot be blank.")
        }
        return dataSource.updateFinding(finding)
    }

    override suspend fun saveCorrection(correction: PreflightFindingCorrection): DomainResult<PreflightFindingCorrection> {
        if (correction.tenantId.isBlank() || correction.findingId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Finding ID cannot be blank.")
        }
        val valRes = PreflightFindingValidator.validateCorrectionSubmission(correction.description, correction.submittedBy)
        if (valRes is DomainResult.Error) return DomainResult.Error(message = valRes.message)

        return dataSource.saveCorrection(correction)
    }

    override suspend fun listCorrectionsForFinding(
        tenantId: String,
        findingId: String
    ): DomainResult<List<PreflightFindingCorrection>> {
        if (tenantId.isBlank() || findingId.isBlank()) {
            return DomainResult.Error(message = "Tenant ID and Finding ID cannot be blank.")
        }
        return dataSource.listCorrectionsForFinding(tenantId, findingId)
    }
}
