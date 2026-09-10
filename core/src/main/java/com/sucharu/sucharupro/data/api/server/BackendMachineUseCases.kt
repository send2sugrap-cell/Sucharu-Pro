package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.machine.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.machine.MachineEquipment
import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Extension Use Cases for Machine Registry & Equipment Foundation (Module 21 Step 01).
 */

suspend fun BackendUseCases.registerMachine(
    principal: AuthenticatedPrincipal,
    request: CreateMachineRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MachineResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineRegistryService(principal.projectId)
    val domain = MachineEquipment(
        machineId = "MAC-" + java.util.UUID.randomUUID().toString().take(8).uppercase(),
        tenantId = principal.projectId,
        assetCode = request.assetCode.trim(),
        name = request.name.trim(),
        type = request.type,
        category = request.category,
        manufacturer = request.manufacturer,
        model = request.model,
        serialNumber = request.serialNumber,
        description = request.description,
        status = request.status,
        ownershipType = request.ownershipType,
        locationReference = request.locationReference,
        department = request.department,
        configurationMetadata = request.configurationMetadata,
        createdBy = principal.userId,
        updatedBy = principal.userId
    )
    return when (val res = service.registerMachine(domain)) {
        is DomainResult.Success -> MachineResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw ConflictException(message = res.message)
        DomainResult.Loading -> throw IllegalStateException("Unexpected loading state")
    }
}

suspend fun BackendUseCases.updateMachine(
    principal: AuthenticatedPrincipal,
    machineId: String,
    request: UpdateMachineRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MachineResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineRegistryService(principal.projectId)
    val existingResult = service.getMachineDetails(principal.projectId, machineId)
    val existing = (existingResult as? DomainResult.Success)?.data
        ?: throw NotFoundException("Machine '$machineId' not found.")

    val updatedDomain = existing.copy(
        assetCode = request.assetCode?.trim() ?: existing.assetCode,
        name = request.name?.trim() ?: existing.name,
        type = request.type ?: existing.type,
        category = request.category ?: existing.category,
        manufacturer = request.manufacturer ?: existing.manufacturer,
        model = request.model ?: existing.model,
        serialNumber = request.serialNumber ?: existing.serialNumber,
        description = request.description ?: existing.description,
        status = request.status ?: existing.status,
        ownershipType = request.ownershipType ?: existing.ownershipType,
        locationReference = request.locationReference ?: existing.locationReference,
        department = request.department ?: existing.department,
        configurationMetadata = request.configurationMetadata ?: existing.configurationMetadata,
        updatedAt = System.currentTimeMillis(),
        updatedBy = principal.userId
    )

    return when (val res = service.updateMachine(updatedDomain)) {
        is DomainResult.Success -> MachineResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        DomainResult.Loading -> throw IllegalStateException("Unexpected loading state")
    }
}

suspend fun BackendUseCases.getMachineDetails(
    principal: AuthenticatedPrincipal,
    machineId: String,
    repositoryFactory: PostgresRepositoryFactory
): MachineResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineRegistryService(principal.projectId)
    return when (val res = service.getMachineDetails(principal.projectId, machineId)) {
        is DomainResult.Success -> {
            val machine = res.data ?: throw NotFoundException("Machine '$machineId' not found.")
            MachineResponseDto.fromDomain(machine)
        }
        is DomainResult.Error -> throw NotFoundException(res.message)
        DomainResult.Loading -> throw IllegalStateException("Unexpected loading state")
    }
}

suspend fun BackendUseCases.listMachines(
    principal: AuthenticatedPrincipal,
    typeStr: String? = null,
    statusStr: String? = null,
    repositoryFactory: PostgresRepositoryFactory
): MachineListResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineRegistryService(principal.projectId)
    val type = typeStr?.let { runCatching { MachineType.valueOf(it.uppercase()) }.getOrNull() }
    val status = statusStr?.let { runCatching { MachineStatus.valueOf(it.uppercase()) }.getOrNull() }

    return when (val res = service.listMachines(principal.projectId, type, status)) {
        is DomainResult.Success -> {
            val dtoList = res.data.map { MachineResponseDto.fromDomain(it) }
            MachineListResponseDto(dtoList, dtoList.size)
        }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        DomainResult.Loading -> throw IllegalStateException("Unexpected loading state")
    }
}

suspend fun BackendUseCases.updateMachineStatus(
    principal: AuthenticatedPrincipal,
    machineId: String,
    request: UpdateMachineStatusRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MachineResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineRegistryService(principal.projectId)
    return when (val res = service.changeMachineStatus(principal.projectId, machineId, request.status, principal.userId)) {
        is DomainResult.Success -> MachineResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        DomainResult.Loading -> throw IllegalStateException("Unexpected loading state")
    }
}
