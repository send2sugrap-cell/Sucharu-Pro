package com.sucharu.sucharupro.data.datasource.businessintegrity

import com.sucharu.sucharupro.domain.model.businessintegrity.*
import com.sucharu.sucharupro.domain.repository.businessintegrity.FinancialIntegrityRunFilter
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory thread-safe fake data source for unit and integration testing.
 */
class FakeBusinessFinancialIntegrityDataSource : BusinessFinancialIntegrityDataSource {

    private val runs = ConcurrentHashMap<String, FinancialIntegrityRun>()
    private val assertions = ConcurrentHashMap<String, MutableList<FinancialControlAssertion>>()
    private val certificates = ConcurrentHashMap<String, PeriodCloseCertificate>()

    override suspend fun saveIntegrityRun(run: FinancialIntegrityRun): FinancialIntegrityRun {
        runs[run.id] = run
        assertions[run.id] = run.assertions.toMutableList()
        return run
    }

    override suspend fun getIntegrityRunById(tenantId: String, projectId: String, runId: String): FinancialIntegrityRun? {
        val run = runs[runId] ?: return null
        if (run.tenantId != tenantId || run.projectId != projectId) return null
        val runAssertions = assertions[runId] ?: emptyList()
        return run.copy(assertions = runAssertions)
    }

    override suspend fun findRunByNumber(tenantId: String, projectId: String, runNumber: String): FinancialIntegrityRun? {
        return runs.values.firstOrNull { it.tenantId == tenantId && it.projectId == projectId && it.runNumber == runNumber }
            ?.let { run ->
                val runAssertions = assertions[run.id] ?: emptyList()
                run.copy(assertions = runAssertions)
            }
    }

    override suspend fun listIntegrityRuns(
        tenantId: String,
        projectId: String,
        filter: FinancialIntegrityRunFilter
    ): List<FinancialIntegrityRun> {
        return runs.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { filter.periodId == null || it.periodId == filter.periodId }
            .filter { filter.status == null || it.status == filter.status }
            .sortedByDescending { it.startedAt }
            .drop(filter.offset)
            .take(filter.limit)
            .map { run ->
                val runAssertions = assertions[run.id] ?: emptyList()
                run.copy(assertions = runAssertions)
            }
    }

    override suspend fun getAssertionsByRunId(tenantId: String, projectId: String, runId: String): List<FinancialControlAssertion> {
        val run = runs[runId] ?: return emptyList()
        if (run.tenantId != tenantId || run.projectId != projectId) return emptyList()
        return assertions[runId]?.toList() ?: emptyList()
    }

    override suspend fun savePeriodCloseCertificate(certificate: PeriodCloseCertificate): PeriodCloseCertificate {
        val key = "${certificate.tenantId}:${certificate.projectId}:${certificate.periodId}"
        certificates[key] = certificate
        return certificate
    }

    override suspend fun getPeriodCloseCertificate(tenantId: String, projectId: String, periodId: String): PeriodCloseCertificate? {
        val key = "$tenantId:$projectId:$periodId"
        return certificates[key]
    }

    override suspend fun listPeriodCloseCertificates(tenantId: String, projectId: String): List<PeriodCloseCertificate> {
        return certificates.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .sortedByDescending { it.closedAt }
    }

    fun clear() {
        runs.clear()
        assertions.clear()
        certificates.clear()
    }
}
