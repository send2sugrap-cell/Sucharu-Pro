package com.sucharu.sucharupro.data.repository.report

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.repository.report.Module24ReportRepository
import com.sucharu.sucharupro.domain.service.report.Module24ReportingService
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl

/**
 * Repository Implementation delegating to Module 24 Reporting Service.
 */
class Module24ReportRepositoryImpl(
    private val reportingService: Module24ReportingService = Module24ReportingServiceImpl()
) : Module24ReportRepository {

    override suspend fun getReportCatalogue(
        principal: AuthenticatedPrincipal
    ): DomainResult<ReportCatalogueResponse> {
        return reportingService.getReportCatalogue(principal)
    }

    override suspend fun queryReport(
        principal: AuthenticatedPrincipal,
        request: ReportRequest
    ): DomainResult<ReportResponse> {
        return reportingService.queryReport(principal, request)
    }

    override suspend fun exportReport(
        principal: AuthenticatedPrincipal,
        exportRequest: ExportReportRequest
    ): DomainResult<ReportExportDocument> {
        return reportingService.exportReport(principal, exportRequest)
    }
}
