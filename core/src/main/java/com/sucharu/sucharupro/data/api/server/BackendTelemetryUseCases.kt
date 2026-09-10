package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.machine.telemetry.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.machine.telemetry.MachineTelemetryRecord
import com.sucharu.sucharupro.domain.machine.telemetry.TelemetryMetricType
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Extension Use Cases for Machine Telemetry Ingestion (Module 21 Step 02).
 */

suspend fun BackendUseCases.ingestTelemetry(
    principal: AuthenticatedPrincipal,
    machineId: String,
    request: IngestTelemetryRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): TelemetryRecordResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineTelemetryIngestionService(principal.projectId)
    val now = System.currentTimeMillis()
    val domain = MachineTelemetryRecord(
        telemetryId = "TEL-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
        tenantId = principal.projectId,
        machineId = machineId,
        sourceDeviceId = request.sourceDeviceId,
        metricType = request.metricType,
        metricValue = request.metricValue,
        unit = request.unit,
        eventTimestamp = request.eventTimestamp ?: now,
        ingestedAt = now,
        metadataJson = request.metadataJson,
        idempotencyKey = request.idempotencyKey,
        createdBy = principal.userId
    )
    val res = service.ingestTelemetry(domain)
    return when (res) {
        is DomainResult.Success -> TelemetryRecordResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.ingestTelemetryBatch(
    principal: AuthenticatedPrincipal,
    machineId: String,
    request: TelemetryBatchIngestRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): TelemetryBatchResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineTelemetryIngestionService(principal.projectId)
    val now = System.currentTimeMillis()
    val domains = request.readings.map { req ->
        MachineTelemetryRecord(
            telemetryId = "TEL-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
            tenantId = principal.projectId,
            machineId = machineId,
            sourceDeviceId = req.sourceDeviceId,
            metricType = req.metricType,
            metricValue = req.metricValue,
            unit = req.unit,
            eventTimestamp = req.eventTimestamp ?: now,
            ingestedAt = now,
            metadataJson = req.metadataJson,
            idempotencyKey = req.idempotencyKey,
            createdBy = principal.userId
        )
    }
    val res = service.ingestTelemetryBatch(domains)
    return when (res) {
        is DomainResult.Success -> {
            val dtoList = res.data.map { TelemetryRecordResponseDto.fromDomain(it) }
            TelemetryBatchResponseDto(ingestedCount = dtoList.size, records = dtoList)
        }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listMachineTelemetry(
    principal: AuthenticatedPrincipal,
    machineId: String,
    metricTypeStr: String? = null,
    limit: Int = 100,
    repositoryFactory: PostgresRepositoryFactory
): List<TelemetryRecordResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineTelemetryIngestionService(principal.projectId)
    val metricType = metricTypeStr?.let { runCatching { TelemetryMetricType.valueOf(it.uppercase()) }.getOrNull() }

    val res = service.listMachineTelemetry(principal.projectId, machineId, metricType, limit)
    return when (res) {
        is DomainResult.Success -> res.data.map { TelemetryRecordResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}
