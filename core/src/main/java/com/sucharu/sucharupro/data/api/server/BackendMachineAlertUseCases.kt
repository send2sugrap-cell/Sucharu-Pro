package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.machine.alerts.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.machine.alerts.*
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Extension Use Cases for Machine Operational Alerts & Notifications (Module 21 Step 07).
 */

suspend fun BackendUseCases.raiseMachineAlert(
    principal: AuthenticatedPrincipal,
    machineId: String,
    request: CreateMachineAlertRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MachineOperationalAlertResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineAlertService(principal.projectId)
    val now = System.currentTimeMillis()
    val alert = MachineOperationalAlert(
        alertId = "ALT-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
        tenantId = principal.projectId,
        machineId = machineId,
        alertType = request.alertType,
        severity = request.severity,
        status = MachineAlertStatus.ACTIVE,
        source = request.source,
        telemetryRecordId = request.telemetryRecordId,
        faultEventId = request.faultEventId,
        maintenanceScheduleId = request.maintenanceScheduleId,
        maintenanceRecordId = request.maintenanceRecordId,
        title = request.title.trim(),
        description = request.description.trim(),
        correlationKey = request.correlationKey ?: "$machineId:${request.alertType}:${request.title}",
        occurredAt = now,
        createdAt = now,
        updatedAt = now,
        createdBy = principal.userId
    )
    val res = service.raiseAlert(alert)
    return when (res) {
        is DomainResult.Success -> MachineOperationalAlertResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.acknowledgeMachineAlert(
    principal: AuthenticatedPrincipal,
    machineId: String,
    alertId: String,
    repositoryFactory: PostgresRepositoryFactory
): MachineOperationalAlertResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineAlertService(principal.projectId)
    val res = service.acknowledgeAlert(principal.projectId, machineId, alertId, principal.userId)
    return when (res) {
        is DomainResult.Success -> MachineOperationalAlertResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.resolveMachineAlert(
    principal: AuthenticatedPrincipal,
    machineId: String,
    alertId: String,
    request: ResolveMachineAlertRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MachineOperationalAlertResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineAlertService(principal.projectId)
    val res = service.resolveAlert(principal.projectId, machineId, alertId, request.resolutionNotes, principal.userId)
    return when (res) {
        is DomainResult.Success -> MachineOperationalAlertResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.dismissMachineAlert(
    principal: AuthenticatedPrincipal,
    machineId: String,
    alertId: String,
    request: DismissMachineAlertRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MachineOperationalAlertResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineAlertService(principal.projectId)
    val res = service.dismissAlert(principal.projectId, machineId, alertId, request.reason, principal.userId)
    return when (res) {
        is DomainResult.Success -> MachineOperationalAlertResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listMachineAlerts(
    principal: AuthenticatedPrincipal,
    machineId: String,
    statusStr: String? = null,
    limit: Int = 100,
    repositoryFactory: PostgresRepositoryFactory
): List<MachineOperationalAlertResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineAlertService(principal.projectId)
    val status = statusStr?.let { runCatching { MachineAlertStatus.valueOf(it.uppercase()) }.getOrNull() }
    val res = service.listAlerts(principal.projectId, machineId, status, limit)
    return when (res) {
        is DomainResult.Success -> res.data.map { MachineOperationalAlertResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}
