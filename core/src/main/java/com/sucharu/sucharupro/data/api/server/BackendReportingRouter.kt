package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.report.*

/**
 * Extension router for Module 24 — Reports, Analytics & Audit Endpoints.
 */

private val securityContextField by lazy {
    BackendRouter::class.java.getDeclaredField("securityContext").apply { isAccessible = true }
}
private val useCasesField by lazy {
    BackendRouter::class.java.getDeclaredField("useCases").apply { isAccessible = true }
}

@Suppress("UNCHECKED_CAST")
private fun parseBodyMap(body: Any?): Map<String, Any?> {
    return when (body) {
        is Map<*, *> -> body as Map<String, Any?>
        else -> emptyMap()
    }
}

private fun parseReportRequest(body: Any?): ReportRequestDto {
    if (body is ReportRequestDto) return body
    val map = parseBodyMap(body)
    if (map.isNotEmpty()) {
        val catStr = map["reportCategory"] as? String
            ?: throw ValidationException("Missing 'reportCategory' parameter.")
        val typeStr = map["reportType"] as? String
            ?: throw ValidationException("Missing 'reportType' parameter.")
        val tenantId = map["tenantId"] as? String
        val projectId = map["projectId"] as? String
        val fromDate = map["fromDate"] as? String
        val toDate = map["toDate"] as? String
        val period = map["period"] as? String ?: "MONTHLY"
        @Suppress("UNCHECKED_CAST")
        val filters = (map["filters"] as? Map<String, String>) ?: emptyMap()
        val page = (map["page"] as? Number)?.toInt() ?: 1
        val pageSize = (map["pageSize"] as? Number)?.toInt() ?: 50
        val sortBy = map["sortBy"] as? String
        val sortDir = map["sortDirection"] as? String ?: "ASC"
        @Suppress("UNCHECKED_CAST")
        val reqMetrics = (map["requestedMetrics"] as? List<String>) ?: emptyList()
        val aggLevel = map["aggregationLevel"] as? String ?: "SUMMARY"

        return ReportRequestDto(
            reportCategory = catStr,
            reportType = typeStr,
            tenantId = tenantId,
            projectId = projectId,
            fromDate = fromDate,
            toDate = toDate,
            period = period,
            filters = filters,
            page = page,
            pageSize = pageSize,
            sortBy = sortBy,
            sortDirection = sortDir,
            requestedMetrics = reqMetrics,
            aggregationLevel = aggLevel
        )
    }
    throw ValidationException("Request body must be a valid ReportRequestDto.")
}

private fun parseExportReportRequest(body: Any?): ExportReportRequestDto {
    if (body is ExportReportRequestDto) return body
    val map = parseBodyMap(body)
    if (map.isNotEmpty()) {
        val queryMap = map["queryRequest"]
            ?: throw ValidationException("Missing 'queryRequest' parameter.")
        val queryDto = parseReportRequest(queryMap)
        val format = map["format"] as? String ?: "CSV"
        val includeCharts = (map["includeCharts"] as? Boolean) ?: false

        return ExportReportRequestDto(
            queryRequest = queryDto,
            format = format,
            includeCharts = includeCharts
        )
    }
    throw ValidationException("Request body must be a valid ExportReportRequestDto.")
}

suspend fun BackendRouter.handleReportingRoutes(
    request: HttpRequest,
    correlationId: String
): HttpResponse? {
    val secContext = securityContextField.get(this) as BackendSecurityContext
    val uCases = useCasesField.get(this) as BackendUseCases
    return handleReportingRoutes(secContext, uCases, request, correlationId)
}

suspend fun handleReportingRoutes(
    securityContext: BackendSecurityContext,
    useCases: BackendUseCases,
    request: HttpRequest,
    correlationId: String
): HttpResponse? {
    return when {
        request.path == "/api/v1/reports/catalogue" && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val res = useCases.getReportCatalogue(principal)
            HttpResponse(200, ApiSuccessResponse(data = res.toDto(), correlationId = correlationId), correlationId)
        }

        request.path == "/api/v1/reports/query" && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val reqDto = parseReportRequest(request.body)
            val res = useCases.queryReport(principal, reqDto)
            HttpResponse(200, ApiSuccessResponse(data = res.toDto(), correlationId = correlationId), correlationId)
        }

        request.path == "/api/v1/reports/export" && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val reqDto = parseExportReportRequest(request.body)
            val res = useCases.exportReport(principal, reqDto)
            HttpResponse(200, ApiSuccessResponse(data = res.toDto(), correlationId = correlationId), correlationId)
        }

        else -> null
    }
}
