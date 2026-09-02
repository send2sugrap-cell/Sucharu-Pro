package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.businessintegrity.Module16FinancialHandoffContract
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.SourceIntegrityStatus

/**
 * Validated Financial Handoff Summary for Module 16 consumption.
 */
data class ValidatedFinancialHandoff(
    val contract: Module16FinancialHandoffContract,
    val integrityStatus: SourceIntegrityStatus,
    val isLedgerBalanced: Boolean,
    val isPeriodClosed: Boolean,
    val hasValidClosureCertificate: Boolean,
    val validationNotes: List<String>
)

/**
 * Adapter interface consuming the Module 15 financial handoff contract.
 * Strictly read-only, deterministic, tenant- and project-aware.
 */
interface Module16FinancialHandoffAdapter {

    suspend fun getVerifiedFinancialHandoff(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<ValidatedFinancialHandoff>

    suspend fun verifyPeriodIntegrityStatus(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<SourceIntegrityStatus>
}
