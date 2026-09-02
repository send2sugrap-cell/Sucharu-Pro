package com.sucharu.sucharupro.domain.service.businessintegrity

import com.sucharu.sucharupro.domain.model.businessintegrity.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.businessintegrity.FinancialIntegrityRunFilter

/**
 * Service interface for Module 15 Step 10: Financial Governance, Cross-Module Reconciliation, Audit & Final Integrity Control.
 */
interface BusinessFinancialIntegrityService {

    suspend fun executeIntegrityRun(
        tenantId: String,
        projectId: String,
        periodId: String,
        actorId: String,
        actorRole: String,
        notes: String? = null,
        idempotencyKey: String? = null
    ): DomainResult<FinancialIntegrityRun>

    suspend fun getIntegrityRunById(
        tenantId: String,
        projectId: String,
        runId: String
    ): DomainResult<FinancialIntegrityRun?>

    suspend fun listIntegrityRuns(
        tenantId: String,
        projectId: String,
        filter: FinancialIntegrityRunFilter = FinancialIntegrityRunFilter()
    ): DomainResult<List<FinancialIntegrityRun>>

    suspend fun evaluatePeriodFinalizationReadiness(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<PeriodFinalizationReadiness>

    suspend fun finalizePeriodClose(
        tenantId: String,
        projectId: String,
        periodId: String,
        actorId: String,
        actorRole: String,
        requesterId: String = "",
        notes: String? = null,
        idempotencyKey: String? = null
    ): DomainResult<PeriodCloseCertificate>

    suspend fun getPeriodCloseCertificate(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<PeriodCloseCertificate?>

    suspend fun generateModule16HandoffContract(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<Module16FinancialHandoffContract>
}
