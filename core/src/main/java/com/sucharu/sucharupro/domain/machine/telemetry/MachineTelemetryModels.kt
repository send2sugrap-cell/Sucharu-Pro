package com.sucharu.sucharupro.domain.machine.telemetry

import java.math.BigDecimal

/**
 * Extensible operational metric types for machine telemetry readings.
 */
enum class TelemetryMetricType {
    SPEED,
    TEMPERATURE,
    RPM,
    OUTPUT_COUNTER,
    OPERATIONAL_STATE,
    ENERGY,
    RUNTIME,
    PRESSURE,
    OTHER
}

/**
 * Core Machine Telemetry Record entity.
 */
data class MachineTelemetryRecord(
    val telemetryId: String,
    val tenantId: String,
    val machineId: String,
    val sourceDeviceId: String? = null,
    val metricType: TelemetryMetricType,
    val metricValue: BigDecimal,
    val unit: String? = null,
    val eventTimestamp: Long,
    val ingestedAt: Long = System.currentTimeMillis(),
    val metadataJson: String? = null,
    val idempotencyKey: String? = null,
    val createdBy: String? = null
)
