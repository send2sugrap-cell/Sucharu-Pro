package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.machine.health.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.machine.MachineType
import com.sucharu.sucharupro.domain.machine.health.MachineOperationalState
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Extension Use Cases for Machine Status & Health Monitoring (Module 21 Step 03).
 */

suspend fun BackendUseCases.evaluateMachineHealth(
    principal: AuthenticatedPrincipal,
    machineId: String,
    repositoryFactory: PostgresRepositoryFactory
): MachineHealthResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineStatusMonitoringService(principal.projectId)
    val res = service.evaluateMachineHealth(principal.projectId, machineId)
    return when (res) {
        is DomainResult.Success -> MachineHealthResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw NotFoundException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listMachinesHealth(
    principal: AuthenticatedPrincipal,
    typeStr: String? = null,
    stateStr: String? = null,
    repositoryFactory: PostgresRepositoryFactory
): MachineHealthListResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineStatusMonitoringService(principal.projectId)
    val type = typeStr?.let { runCatching { MachineType.valueOf(it.uppercase()) }.getOrNull() }
    val opState = stateStr?.let { runCatching { MachineOperationalState.valueOf(it.uppercase()) }.getOrNull() }

    val res = service.listMachinesHealth(principal.projectId, type, opState)
    return when (res) {
        is DomainResult.Success -> {
            val dtoList = res.data.map { MachineHealthResponseDto.fromDomain(it) }
            MachineHealthListResponseDto(dtoList, dtoList.size)
        }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}
