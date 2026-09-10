package com.sucharu.sucharupro.data.api.model.machine.telemetry

import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import java.math.BigDecimal

/**
 * REST API DTOs for Machine Telemetry Ingestion.
 */
data class IngestTelemetryRequestDto(
    val metricType: TelemetryMetricType,
    val metricValue: BigDecimal,
    val unit: String? = null,
    val eventTimestamp: Long? = null,
    val sourceDeviceId: String? = null,
    val metadataJson: String? = null,
    val idempotencyKey: String? = null
)

data class TelemetryBatchIngestRequestDto(
    val readings: List<IngestTelemetryRequestDto>
)

data class TelemetryRecordResponseDto(
    val telemetryId: String,
    val tenantId: String,
    val machineId: String,
    val sourceDeviceId: String?,
    val metricType: TelemetryMetricType,
    val metricValue: BigDecimal,
    val unit: String?,
    val eventTimestamp: Long,
    val ingestedAt: Long,
    val metadataJson: String?,
    val idempotencyKey: String?,
    val createdBy: String?
) {
    companion object {
        fun fromDomain(domain: MachineTelemetryRecord): TelemetryRecordResponseDto = TelemetryRecordResponseDto(
            telemetryId = domain.telemetryId,
            tenantId = domain.tenantId,
            machineId = domain.machineId,
            sourceDeviceId = domain.sourceDeviceId,
            metricType = domain.metricType,
            metricValue = domain.metricValue,
            unit = domain.unit,
            eventTimestamp = domain.eventTimestamp,
            ingestedAt = domain.ingestedAt,
            metadataJson = domain.metadataJson,
            idempotencyKey = domain.idempotencyKey,
            createdBy = domain.createdBy
        )
    }
}

data class TelemetryBatchResponseDto(
    val ingestedCount: Int,
    val records: List<TelemetryRecordResponseDto>
)
