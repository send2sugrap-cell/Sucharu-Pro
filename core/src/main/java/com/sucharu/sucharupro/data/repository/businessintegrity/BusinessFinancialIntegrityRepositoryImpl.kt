package com.sucharu.sucharupro.data.repository.businessintegrity

import com.sucharu.sucharupro.data.datasource.businessintegrity.BusinessFinancialIntegrityDataSource
import com.sucharu.sucharupro.domain.model.businessintegrity.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.repository.businessintegrity.BusinessFinancialIntegrityRepository
import com.sucharu.sucharupro.domain.repository.businessintegrity.FinancialIntegrityRunFilter

class BusinessFinancialIntegrityRepositoryImpl(
    private val dataSource: BusinessFinancialIntegrityDataSource
) : BusinessFinancialIntegrityRepository {

    override suspend fun saveIntegrityRun(run: FinancialIntegrityRun): DomainResult<FinancialIntegrityRun> {
        return try {
            val saved = dataSource.saveIntegrityRun(run)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save integrity run.")
        }
    }

    override suspend fun getIntegrityRunById(
        tenantId: String,
        projectId: String,
        runId: String
    ): DomainResult<FinancialIntegrityRun?> {
        return try {
            val run = dataSource.getIntegrityRunById(tenantId, projectId, runId)
            DomainResult.Success(run)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to get integrity run.")
        }
    }

    override suspend fun findRunByNumber(
        tenantId: String,
        projectId: String,
        runNumber: String
    ): DomainResult<FinancialIntegrityRun?> {
        return try {
            val run = dataSource.findRunByNumber(tenantId, projectId, runNumber)
            DomainResult.Success(run)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to find run by number.")
        }
    }

    override suspend fun listIntegrityRuns(
        tenantId: String,
        projectId: String,
        filter: FinancialIntegrityRunFilter
    ): DomainResult<List<FinancialIntegrityRun>> {
        return try {
            val list = dataSource.listIntegrityRuns(tenantId, projectId, filter)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list integrity runs.")
        }
    }

    override suspend fun getAssertionsByRunId(
        tenantId: String,
        projectId: String,
        runId: String
    ): DomainResult<List<FinancialControlAssertion>> {
        return try {
            val assertions = dataSource.getAssertionsByRunId(tenantId, projectId, runId)
            DomainResult.Success(assertions)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to get assertions.")
        }
    }

    override suspend fun savePeriodCloseCertificate(certificate: PeriodCloseCertificate): DomainResult<PeriodCloseCertificate> {
        return try {
            val saved = dataSource.savePeriodCloseCertificate(certificate)
            DomainResult.Success(saved)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to save period close certificate.")
        }
    }

    override suspend fun getPeriodCloseCertificate(
        tenantId: String,
        projectId: String,
        periodId: String
    ): DomainResult<PeriodCloseCertificate?> {
        return try {
            val cert = dataSource.getPeriodCloseCertificate(tenantId, projectId, periodId)
            DomainResult.Success(cert)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to get period close certificate.")
        }
    }

    override suspend fun listPeriodCloseCertificates(
        tenantId: String,
        projectId: String
    ): DomainResult<List<PeriodCloseCertificate>> {
        return try {
            val list = dataSource.listPeriodCloseCertificates(tenantId, projectId)
            DomainResult.Success(list)
        } catch (e: Exception) {
            DomainResult.Error(message = e.message ?: "Failed to list period close certificates.")
        }
    }
}
