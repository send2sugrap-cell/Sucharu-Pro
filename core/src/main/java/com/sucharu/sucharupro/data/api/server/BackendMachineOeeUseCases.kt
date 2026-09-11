package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.machine.oee.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.machine.oee.*
import com.sucharu.sucharupro.domain.model.common.DomainResult

/**
 * Extension Use Cases for Machine Performance & OEE Foundation (Module 21 Step 08).
 */

suspend fun BackendUseCases.calculateAndSaveOee(
    principal: AuthenticatedPrincipal,
    machineId: String,
    request: CalculateOeeRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): MachineOeeResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createMachineOeeService(principal.projectId)
    val calcReq = MachineOeeCalculationRequest(
        tenantId = principal.projectId,
        machineId = machineId,
        periodStart = request.periodStart,
        periodEnd = request.periodEnd,
        customPlannedProductionSeconds = request.customPlannedProductionSeconds,
        customIdealRateUnitsPerHour = request.customIdealRateUnitsPerHour
    )
    val res = service.calculateAndSaveOee(calcReq, principal.userId)
    return when (res) {
        is DomainResult.Success -> MachineOeeResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.getLatestOeeSummary(
    principal: AuthenticatedPrincipal,
    machineId: String,
    repositoryFactory: PostgresRepositoryFactory
): MachineOeeResponseDto? {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineOeeService(principal.projectId)
    val res = service.getLatestOeeSummary(principal.projectId, machineId)
    return when (res) {
        is DomainResult.Success -> res.data?.let { MachineOeeResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listOeeHistory(
    principal: AuthenticatedPrincipal,
    machineId: String,
    limit: Int = 100,
    repositoryFactory: PostgresRepositoryFactory
): List<MachineOeeResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createMachineOeeService(principal.projectId)
    val res = service.listOeeHistory(principal.projectId, machineId, limit)
    return when (res) {
        is DomainResult.Success -> res.data.map { MachineOeeResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}
