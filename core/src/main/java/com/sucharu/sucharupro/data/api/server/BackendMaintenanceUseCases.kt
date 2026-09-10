package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.machine.maintenance.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.machine.maintenance.MachineServiceHistoryLog
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceRecord
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceSchedule
import com.sucharu.sucharupro.domain.machine.maintenance.MaintenanceStatus
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Extension Use Cases for Machine Maintenance Management (Module 21 Step 05).
 */

suspend fun BackendUseCases.createMaintenanceSchedule(
    principal: AuthenticatedPrincipal,
    machineId: String,
    request: CreateMaintenanceScheduleRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MaintenanceScheduleResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineMaintenanceService(principal.projectId)
    val now = System.currentTimeMillis()
    val schedule = MaintenanceSchedule(
        scheduleId = "SCHED-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
        tenantId = principal.projectId,
        machineId = machineId,
        title = request.title.trim(),
        description = request.description,
        maintenanceType = request.maintenanceType,
        plannedDate = request.plannedDate,
        recurrenceIntervalDays = request.recurrenceIntervalDays,
        assignedTechnicianId = request.assignedTechnicianId,
        assignedTechnicianName = request.assignedTechnicianName,
        notes = request.notes,
        createdAt = now,
        updatedAt = now,
        createdBy = principal.userId
    )
    val res = service.createSchedule(schedule)
    return when (res) {
        is DomainResult.Success -> MaintenanceScheduleResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listMaintenanceSchedules(
    principal: AuthenticatedPrincipal,
    machineId: String,
    statusStr: String? = null,
    repositoryFactory: PostgresRepositoryFactory
): List<MaintenanceScheduleResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineMaintenanceService(principal.projectId)
    val status = statusStr?.let { runCatching { MaintenanceStatus.valueOf(it.uppercase()) }.getOrNull() }
    val res = service.listSchedules(principal.projectId, machineId, status)
    return when (res) {
        is DomainResult.Success -> res.data.map { MaintenanceScheduleResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.createMaintenanceRecord(
    principal: AuthenticatedPrincipal,
    machineId: String,
    request: CreateMaintenanceRecordRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MaintenanceRecordResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineMaintenanceService(principal.projectId)
    val now = System.currentTimeMillis()
    val record = MaintenanceRecord(
        recordId = "REC-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
        tenantId = principal.projectId,
        machineId = machineId,
        scheduleId = request.scheduleId,
        title = request.title.trim(),
        problemDescription = request.problemDescription,
        maintenanceType = request.maintenanceType,
        openedAt = now,
        performedById = request.performedById ?: principal.userId,
        performedByName = request.performedByName ?: principal.username,
        notes = request.notes,
        createdAt = now,
        updatedAt = now,
        createdBy = principal.userId
    )
    val res = service.createRecord(record)
    return when (res) {
        is DomainResult.Success -> MaintenanceRecordResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.startMaintenanceRecord(
    principal: AuthenticatedPrincipal,
    machineId: String,
    recordId: String,
    repositoryFactory: PostgresRepositoryFactory
): MaintenanceRecordResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineMaintenanceService(principal.projectId)
    val res = service.startMaintenance(principal.projectId, machineId, recordId, principal.userId, principal.username)
    return when (res) {
        is DomainResult.Success -> MaintenanceRecordResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.completeMaintenanceRecord(
    principal: AuthenticatedPrincipal,
    machineId: String,
    recordId: String,
    request: CompleteMaintenanceRecordRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MaintenanceRecordResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineMaintenanceService(principal.projectId)
    val res = service.completeMaintenance(
        tenantId = principal.projectId,
        machineId = machineId,
        recordId = recordId,
        resolutionSummary = request.resolutionSummary,
        workPerformed = request.workPerformed,
        completedBy = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> MaintenanceRecordResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.cancelMaintenanceRecord(
    principal: AuthenticatedPrincipal,
    machineId: String,
    recordId: String,
    request: CancelMaintenanceRecordRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MaintenanceRecordResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineMaintenanceService(principal.projectId)
    val res = service.cancelMaintenance(principal.projectId, machineId, recordId, request.reason, principal.userId)
    return when (res) {
        is DomainResult.Success -> MaintenanceRecordResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listMaintenanceRecords(
    principal: AuthenticatedPrincipal,
    machineId: String,
    statusStr: String? = null,
    limit: Int = 100,
    repositoryFactory: PostgresRepositoryFactory
): List<MaintenanceRecordResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineMaintenanceService(principal.projectId)
    val status = statusStr?.let { runCatching { MaintenanceStatus.valueOf(it.uppercase()) }.getOrNull() }
    val res = service.listMaintenanceRecords(principal.projectId, machineId, status, limit)
    return when (res) {
        is DomainResult.Success -> res.data.map { MaintenanceRecordResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listMachineServiceHistory(
    principal: AuthenticatedPrincipal,
    machineId: String,
    limit: Int = 100,
    repositoryFactory: PostgresRepositoryFactory
): List<ServiceHistoryLogResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineMaintenanceService(principal.projectId)
    val res = service.listServiceHistory(principal.projectId, machineId, limit)
    return when (res) {
        is DomainResult.Success -> res.data.map { ServiceHistoryLogResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}
