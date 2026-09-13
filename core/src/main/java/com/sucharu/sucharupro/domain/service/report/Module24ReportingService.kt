package com.sucharu.sucharupro.domain.service.report

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*

/**
 * Service Contract for Module 24 — Canonical Reports, Analytics & Audit Subsystem.
 */
interface Module24ReportingService {

    /**
     * Resolves available report catalogue filtered by principal role and capabilities.
     */
    suspend fun getReportCatalogue(
        principal: AuthenticatedPrincipal
    ): DomainResult<ReportCatalogueResponse>

    /**
     * Executes a deterministic canonical report query against authoritative domain projections.
     */
    suspend fun queryReport(
        principal: AuthenticatedPrincipal,
        request: ReportRequest
    ): DomainResult<ReportResponse>

    /**
     * Orchestrates export of a canonical report in requested document format.
     */
    suspend fun exportReport(
        principal: AuthenticatedPrincipal,
        exportRequest: ExportReportRequest
    ): DomainResult<ReportExportDocument>
}
