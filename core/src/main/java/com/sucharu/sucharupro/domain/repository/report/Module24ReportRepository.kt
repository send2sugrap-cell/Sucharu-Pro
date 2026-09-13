package com.sucharu.sucharupro.domain.repository.report

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*

/**
 * Repository Contract for Module 24 — Reporting Foundation.
 */
interface Module24ReportRepository {

    suspend fun getReportCatalogue(
        principal: AuthenticatedPrincipal
    ): DomainResult<ReportCatalogueResponse>

    suspend fun queryReport(
        principal: AuthenticatedPrincipal,
        request: ReportRequest
    ): DomainResult<ReportResponse>

    suspend fun exportReport(
        principal: AuthenticatedPrincipal,
        exportRequest: ExportReportRequest
    ): DomainResult<ReportExportDocument>
}
