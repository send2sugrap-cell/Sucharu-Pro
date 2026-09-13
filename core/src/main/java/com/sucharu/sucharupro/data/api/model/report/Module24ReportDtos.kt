package com.sucharu.sucharupro.data.api.model.report

import com.sucharu.sucharupro.data.auth.authorization.AuthorizationCapability
import com.sucharu.sucharupro.domain.model.report.*

/**
 * DTO for requesting a canonical Module 24 report.
 */
data class ReportRequestDto(
    val reportCategory: String,
    val reportType: String,
    val tenantId: String? = null,
    val projectId: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val period: String? = "MONTHLY",
    val filters: Map<String, String> = emptyMap(),
    val page: Int = 1,
    val pageSize: Int = 50,
    val sortBy: String? = null,
    val sortDirection: String = "ASC",
    val requestedMetrics: List<String> = emptyList(),
    val aggregationLevel: String = "SUMMARY"
) {
    fun toDomainModel(defaultTenantId: String, defaultProjectId: String): ReportRequest {
        val category = try {
            ReportCategory.valueOf(reportCategory.uppercase())
        } catch (_: Exception) {
            ReportCategory.EXECUTIVE_ANALYTICS
        }
        val repPeriod = try {
            if (period != null) ReportPeriod.valueOf(period.uppercase()) else ReportPeriod.MONTHLY
        } catch (_: Exception) {
            ReportPeriod.MONTHLY
        }
        val aggLevel = try {
            ReportAggregationLevel.valueOf(aggregationLevel.uppercase())
        } catch (_: Exception) {
            ReportAggregationLevel.SUMMARY
        }
        return ReportRequest(
            reportCategory = category,
            reportType = reportType,
            tenantId = tenantId?.ifBlank { null } ?: defaultTenantId,
            projectId = projectId?.ifBlank { null } ?: defaultProjectId,
            fromDate = fromDate,
            toDate = toDate,
            period = repPeriod,
            filters = filters,
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(1, 500),
            sortBy = sortBy,
            sortDirection = if (sortDirection.uppercase() == "DESC") "DESC" else "ASC",
            requestedMetrics = requestedMetrics,
            aggregationLevel = aggLevel
        )
    }
}

data class ReportMetricDto(
    val metricId: String,
    val label: String,
    val value: String,
    val numericValue: Double? = null,
    val formattedValue: String = value,
    val unit: String? = null,
    val trendPercentage: Double? = null,
    val status: String? = null
)

data class ReportDataColumnDto(
    val key: String,
    val label: String,
    val dataType: String = "STRING"
)

data class ReportDataRowDto(
    val id: String? = null,
    val values: Map<String, String> = emptyMap()
)

data class ReportChartDataPointDto(
    val label: String,
    val timestamp: Long? = null,
    val value: Double,
    val group: String? = null
)

data class ReportChartSeriesDto(
    val seriesName: String,
    val points: List<ReportChartDataPointDto> = emptyList()
)

data class ReportPaginationMetaDto(
    val currentPage: Int = 1,
    val pageSize: Int = 50,
    val totalRows: Int = 0,
    val totalPages: Int = 1
)

data class ReportExecutionMetaDto(
    val reportCategory: String,
    val reportType: String,
    val tenantId: String,
    val projectId: String,
    val generatedAt: String,
    val executionTimeMs: Long,
    val authoritativeModule: String
)

data class ReportResponseDto(
    val meta: ReportExecutionMetaDto,
    val summaryMetrics: List<ReportMetricDto> = emptyList(),
    val columns: List<ReportDataColumnDto> = emptyList(),
    val rows: List<ReportDataRowDto> = emptyList(),
    val chartSeries: List<ReportChartSeriesDto> = emptyList(),
    val pagination: ReportPaginationMetaDto = ReportPaginationMetaDto(),
    val appliedFilters: Map<String, String> = emptyMap()
)

data class ReportCatalogueItemDto(
    val category: String,
    val reportType: String,
    val displayName: String,
    val description: String,
    val authoritativeModule: String,
    val requiredCapability: String,
    val defaultMetrics: List<String> = emptyList()
)

data class ReportCategorySummaryDto(
    val category: String,
    val displayName: String,
    val reports: List<ReportCatalogueItemDto> = emptyList()
)

data class ReportCatalogueResponseDto(
    val totalCategories: Int,
    val totalReports: Int,
    val categories: List<ReportCategorySummaryDto> = emptyList()
)

data class ExportReportRequestDto(
    val queryRequest: ReportRequestDto,
    val format: String = "CSV",
    val includeCharts: Boolean = false
) {
    fun toDomainModel(defaultTenantId: String, defaultProjectId: String): ExportReportRequest {
        val domainReq = queryRequest.toDomainModel(defaultTenantId, defaultProjectId)
        val expFormat = try {
            ReportExportFormat.valueOf(format.uppercase())
        } catch (_: Exception) {
            ReportExportFormat.CSV
        }
        return ExportReportRequest(
            queryRequest = domainReq,
            format = expFormat,
            includeCharts = includeCharts
        )
    }
}

data class ReportExportDocumentDto(
    val reportType: String,
    val format: String,
    val fileName: String,
    val mimeType: String,
    val contentBase64: String? = null,
    val contentLength: Long = 0,
    val generatedAt: String
)

// Mapping Extension Functions

fun ReportMetric.toDto() = ReportMetricDto(
    metricId = metricId,
    label = label,
    value = value,
    numericValue = numericValue,
    formattedValue = formattedValue,
    unit = unit,
    trendPercentage = trendPercentage,
    status = status
)

fun ReportDataColumn.toDto() = ReportDataColumnDto(
    key = key,
    label = label,
    dataType = dataType
)

fun ReportDataRow.toDto() = ReportDataRowDto(
    id = id,
    values = values
)

fun ReportChartDataPoint.toDto() = ReportChartDataPointDto(
    label = label,
    timestamp = timestamp,
    value = value,
    group = group
)

fun ReportChartSeries.toDto() = ReportChartSeriesDto(
    seriesName = seriesName,
    points = points.map { it.toDto() }
)

fun ReportPaginationMeta.toDto() = ReportPaginationMetaDto(
    currentPage = currentPage,
    pageSize = pageSize,
    totalRows = totalRows,
    totalPages = totalPages
)

fun ReportExecutionMeta.toDto() = ReportExecutionMetaDto(
    reportCategory = reportCategory.name,
    reportType = reportType,
    tenantId = tenantId,
    projectId = projectId,
    generatedAt = generatedAt,
    executionTimeMs = executionTimeMs,
    authoritativeModule = authoritativeModule
)

fun ReportResponse.toDto() = ReportResponseDto(
    meta = meta.toDto(),
    summaryMetrics = summaryMetrics.map { it.toDto() },
    columns = columns.map { it.toDto() },
    rows = rows.map { it.toDto() },
    chartSeries = chartSeries.map { it.toDto() },
    pagination = pagination.toDto(),
    appliedFilters = appliedFilters
)

fun ReportCatalogueItem.toDto() = ReportCatalogueItemDto(
    category = category.name,
    reportType = reportType,
    displayName = displayName,
    description = description,
    authoritativeModule = authoritativeModule,
    requiredCapability = requiredCapability.name,
    defaultMetrics = defaultMetrics
)

fun ReportCategorySummary.toDto() = ReportCategorySummaryDto(
    category = category.name,
    displayName = displayName,
    reports = reports.map { it.toDto() }
)

fun ReportCatalogueResponse.toDto() = ReportCatalogueResponseDto(
    totalCategories = totalCategories,
    totalReports = totalReports,
    categories = categories.map { it.toDto() }
)

fun ReportExportDocument.toDto() = ReportExportDocumentDto(
    reportType = reportType,
    format = format.name,
    fileName = fileName,
    mimeType = mimeType,
    contentBase64 = contentBase64,
    contentLength = contentLength,
    generatedAt = generatedAt
)
