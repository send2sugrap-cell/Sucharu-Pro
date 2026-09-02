package com.sucharu.sucharupro.data.datasource.businessintegrity

import com.sucharu.sucharupro.domain.model.businessintegrity.*
import com.sucharu.sucharupro.domain.repository.businessintegrity.FinancialIntegrityRunFilter

/**
 * Datasource interface for financial integrity persistence.
 */
interface BusinessFinancialIntegrityDataSource {
    suspend fun saveIntegrityRun(run: FinancialIntegrityRun): FinancialIntegrityRun
    suspend fun getIntegrityRunById(tenantId: String, projectId: String, runId: String): FinancialIntegrityRun?
    suspend fun findRunByNumber(tenantId: String, projectId: String, runNumber: String): FinancialIntegrityRun?
    suspend fun listIntegrityRuns(tenantId: String, projectId: String, filter: FinancialIntegrityRunFilter): List<FinancialIntegrityRun>
    suspend fun getAssertionsByRunId(tenantId: String, projectId: String, runId: String): List<FinancialControlAssertion>

    suspend fun savePeriodCloseCertificate(certificate: PeriodCloseCertificate): PeriodCloseCertificate
    suspend fun getPeriodCloseCertificate(tenantId: String, projectId: String, periodId: String): PeriodCloseCertificate?
    suspend fun listPeriodCloseCertificates(tenantId: String, projectId: String): List<PeriodCloseCertificate>
}
