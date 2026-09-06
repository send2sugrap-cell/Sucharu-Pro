package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.ValidationException
import com.sucharu.sucharupro.data.api.model.inventory.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.inventory.ProductionInventoryIntegrationService

private fun getRepositoryFactory(useCases: BackendUseCases): PostgresRepositoryFactory {
    val field = BackendUseCases::class.java.getDeclaredField("repositoryFactory")
    field.isAccessible = true
    return field.get(useCases) as PostgresRepositoryFactory
}

private fun getInventoryService(useCases: BackendUseCases, tenantId: String): ProductionInventoryIntegrationService {
    val factory = getRepositoryFactory(useCases)
    return factory.createProductionInventoryIntegrationService(tenantId)
}

suspend fun BackendUseCases.evaluateInventoryEligibility(
    principal: AuthenticatedPrincipal,
    executionJobId: String
): FinishedGoodsEligibilityResponseDto {
    val service = getInventoryService(this, principal.projectId)
    val res = service.evaluateInventoryEligibility(principal.projectId, executionJobId)
    return when (res) {
        is DomainResult.Success -> res.data.toDto()
        is DomainResult.Error -> throw ValidationException(res.message)
        DomainResult.Loading -> throw ValidationException("Evaluating eligibility.")
    }
}

suspend fun BackendUseCases.receiveFinishedGoodsFromProduction(
    principal: AuthenticatedPrincipal,
    executionJobId: String,
    reqDto: ReceiveFinishedGoodsRequestDto
): FinishedGoodsReceiptResponseDto {
    val service = getInventoryService(this, principal.projectId)
    val res = service.receiveFinishedGoodsFromProduction(
        tenantId = principal.projectId,
        executionJobId = executionJobId,
        warehouseId = reqDto.warehouseId,
        binId = reqDto.binId,
        overrideQuantity = reqDto.overrideQuantity,
        actor = principal.username
    )
    return when (res) {
        is DomainResult.Success -> res.data.toDto()
        is DomainResult.Error -> throw ValidationException(res.message)
        DomainResult.Loading -> throw ValidationException("Processing receipt.")
    }
}

suspend fun BackendUseCases.getFinishedGoodsReceiptForJob(
    principal: AuthenticatedPrincipal,
    executionJobId: String
): FinishedGoodsReceiptResponseDto? {
    val service = getInventoryService(this, principal.projectId)
    val res = service.getFinishedGoodsReceiptForJob(principal.projectId, executionJobId)
    return when (res) {
        is DomainResult.Success -> res.data?.toDto()
        is DomainResult.Error -> throw ValidationException(res.message)
        DomainResult.Loading -> null
    }
}
