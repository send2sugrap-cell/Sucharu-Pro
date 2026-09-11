package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.machine.events.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.machine.events.*
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Extension Use Cases for Machine Fault Events and Downtime Events (Module 21 Step 06).
 */

suspend fun BackendUseCases.recordFaultEvent(
    principal: AuthenticatedPrincipal,
    machineId: String,
    request: CreateFaultEventRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MachineFaultEventResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineFaultEventService(principal.projectId)
    val now = System.currentTimeMillis()
    val event = MachineFaultEvent(
        faultEventId = "FLT-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
        tenantId = principal.projectId,
        machineId = machineId,
        faultCode = request.faultCode,
        faultType = request.faultType,
        severity = request.severity,
        status = FaultStatus.OPEN,
        description = request.description.trim(),
        source = request.source,
        occurredAt = request.occurredAt ?: now,
        detectedAt = now,
        maintenanceRecordId = request.maintenanceRecordId,
        metadataJson = request.metadataJson,
        createdAt = now,
        updatedAt = now,
        createdBy = principal.userId
    )
    val res = service.recordFault(event)
    return when (res) {
        is DomainResult.Success -> MachineFaultEventResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.acknowledgeFaultEvent(
    principal: AuthenticatedPrincipal,
    machineId: String,
    faultEventId: String,
    repositoryFactory: PostgresRepositoryFactory
): MachineFaultEventResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineFaultEventService(principal.projectId)
    val res = service.acknowledgeFault(principal.projectId, machineId, faultEventId, principal.userId)
    return when (res) {
        is DomainResult.Success -> MachineFaultEventResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.resolveFaultEvent(
    principal: AuthenticatedPrincipal,
    machineId: String,
    faultEventId: String,
    request: ResolveFaultEventRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MachineFaultEventResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineFaultEventService(principal.projectId)
    val res = service.resolveFault(
        tenantId = principal.projectId,
        machineId = machineId,
        faultEventId = faultEventId,
        resolutionNotes = request.resolutionNotes,
        maintenanceRecordId = request.maintenanceRecordId,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> MachineFaultEventResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listFaultEvents(
    principal: AuthenticatedPrincipal,
    machineId: String,
    statusStr: String? = null,
    limit: Int = 100,
    repositoryFactory: PostgresRepositoryFactory
): List<MachineFaultEventResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineFaultEventService(principal.projectId)
    val status = statusStr?.let { runCatching { FaultStatus.valueOf(it.uppercase()) }.getOrNull() }
    val res = service.listFaultEvents(principal.projectId, machineId, status, limit)
    return when (res) {
        is DomainResult.Success -> res.data.map { MachineFaultEventResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.startDowntimeEvent(
    principal: AuthenticatedPrincipal,
    machineId: String,
    request: StartDowntimeEventRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MachineDowntimeEventResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineDowntimeService(principal.projectId)
    val now = System.currentTimeMillis()
    val event = MachineDowntimeEvent(
        downtimeId = "DOWNTIME-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
        tenantId = principal.projectId,
        machineId = machineId,
        faultEventId = request.faultEventId,
        executionJobId = request.executionJobId,
        workOrderId = request.workOrderId,
        reasonCategory = request.reasonCategory,
        reasonDetails = request.reasonDetails,
        status = DowntimeStatus.STARTED,
        startedAt = request.startedAt ?: now,
        createdAt = now,
        updatedAt = now,
        createdBy = principal.userId
    )
    val res = service.startDowntime(event)
    return when (res) {
        is DomainResult.Success -> MachineDowntimeEventResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.endDowntimeEvent(
    principal: AuthenticatedPrincipal,
    machineId: String,
    downtimeId: String,
    repositoryFactory: PostgresRepositoryFactory
): MachineDowntimeEventResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineDowntimeService(principal.projectId)
    val res = service.endDowntime(principal.projectId, machineId, downtimeId, System.currentTimeMillis(), principal.userId)
    return when (res) {
        is DomainResult.Success -> MachineDowntimeEventResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listDowntimeEvents(
    principal: AuthenticatedPrincipal,
    machineId: String,
    statusStr: String? = null,
    limit: Int = 100,
    repositoryFactory: PostgresRepositoryFactory
): List<MachineDowntimeEventResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineDowntimeService(principal.projectId)
    val status = statusStr?.let { runCatching { DowntimeStatus.valueOf(it.uppercase()) }.getOrNull() }
    val res = service.listDowntimeEvents(principal.projectId, machineId, status, limit)
    return when (res) {
        is DomainResult.Success -> res.data.map { MachineDowntimeEventResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}
