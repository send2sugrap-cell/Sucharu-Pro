package com.sucharu.sucharupro.domain.repository.businessintegrity

import com.sucharu.sucharupro.domain.model.businessintegrity.*
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Filter for querying integrity runs.
 */
data class FinancialIntegrityRunFilter(
    val periodId: String? = null,
    val status: FinancialIntegrityStatus? = null,
    val limit: Int = 100,
    val offset: Int = 0
)

/**
 * Repository interface for Business Financial Final Integrity & Period Closure persistence.
 */
interface BusinessFinancialIntegrityRepository {
    // --- Runs & Assertions ---
    suspend fun saveIntegrityRun(run: FinancialIntegrityRun): DomainResult<FinancialIntegrityRun>
    suspend fun getIntegrityRunById(tenantId: String, projectId: String, runId: String): DomainResult<FinancialIntegrityRun?>
    suspend fun findRunByNumber(tenantId: String, projectId: String, runNumber: String): DomainResult<FinancialIntegrityRun?>
    suspend fun listIntegrityRuns(tenantId: String, projectId: String, filter: FinancialIntegrityRunFilter): DomainResult<List<FinancialIntegrityRun>>
    suspend fun getAssertionsByRunId(tenantId: String, projectId: String, runId: String): DomainResult<List<FinancialControlAssertion>>

    // --- Period Close Certificates ---
    suspend fun savePeriodCloseCertificate(certificate: PeriodCloseCertificate): DomainResult<PeriodCloseCertificate>
    suspend fun getPeriodCloseCertificate(tenantId: String, projectId: String, periodId: String): DomainResult<PeriodCloseCertificate?>
    suspend fun listPeriodCloseCertificates(tenantId: String, projectId: String): DomainResult<List<PeriodCloseCertificate>>
}
