package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.report.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.report.*
import com.sucharu.sucharupro.domain.service.report.Module24ReportingService
import com.sucharu.sucharupro.domain.service.report.Module24ReportingServiceImpl

private val module24ReportingService: Module24ReportingService by lazy {
    Module24ReportingServiceImpl()
}

suspend fun BackendUseCases.getReportCatalogue(
    principal: AuthenticatedPrincipal
): ReportCatalogueResponse {
    return when (val res = module24ReportingService.getReportCatalogue(principal)) {
        is DomainResult.Success -> res.data
        is DomainResult.Error -> throw ValidationException(res.message)
        is DomainResult.Loading -> throw IllegalStateException("Report catalogue operation in progress.")
    }
}

suspend fun BackendUseCases.queryReport(
    principal: AuthenticatedPrincipal,
    reqDto: ReportRequestDto
): ReportResponse {
    val domainReq = reqDto.toDomainModel(
        defaultTenantId = principal.projectId,
        defaultProjectId = principal.projectId
    )
    return when (val res = module24ReportingService.queryReport(principal, domainReq)) {
        is DomainResult.Success -> res.data
        is DomainResult.Error -> {
            if (res.message.startsWith("Unauthorized") || res.message.startsWith("Tenant isolation violation")) {
                throw ForbiddenException(res.message)
            } else {
                throw ValidationException(res.message)
            }
        }
        is DomainResult.Loading -> throw IllegalStateException("Report query execution in progress.")
    }
}

suspend fun BackendUseCases.exportReport(
    principal: AuthenticatedPrincipal,
    reqDto: ExportReportRequestDto
): ReportExportDocument {
    val domainReq = reqDto.toDomainModel(
        defaultTenantId = principal.projectId,
        defaultProjectId = principal.projectId
    )
    return when (val res = module24ReportingService.exportReport(principal, domainReq)) {
        is DomainResult.Success -> res.data
        is DomainResult.Error -> {
            if (res.message.startsWith("Unauthorized") || res.message.startsWith("Tenant isolation violation")) {
                throw ForbiddenException(res.message)
            } else {
                throw ValidationException(res.message)
            }
        }
        is DomainResult.Loading -> throw IllegalStateException("Report export execution in progress.")
    }
}
