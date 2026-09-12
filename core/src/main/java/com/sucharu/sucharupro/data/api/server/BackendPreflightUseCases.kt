package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.preflight.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.PreflightExecutionContext

/**
 * Extension Use Cases for Automated Proofing & Preflight Engine (Module 22 Step 01).
 */

suspend fun BackendUseCases.startPreflightRun(
    principal: AuthenticatedPrincipal,
    request: StartPreflightRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): PreflightRunResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AI_AGENT)
    val service = repositoryFactory.createPreflightService(principal.projectId)
    val context = PreflightExecutionContext(
        tenantId = principal.projectId,
        jobId = request.jobId,
        artworkId = request.artworkId,
        artworkVersionId = request.artworkVersionId,
        proofId = request.proofId,
        orderSpecificationMap = request.orderSpecificationMap,
        artworkMetadataMap = request.artworkMetadataMap
    )
    val res = service.runPreflight(context, principal.userId, request.idempotencyKey)
    return when (res) {
        is DomainResult.Success -> PreflightRunResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.getPreflightRunDetails(
    principal: AuthenticatedPrincipal,
    runId: String,
    repositoryFactory: PostgresRepositoryFactory
): PreflightRunResponseDto? {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createPreflightService(principal.projectId)
    val res = service.getPreflightRunDetails(principal.projectId, runId)
    return when (res) {
        is DomainResult.Success -> res.data?.let { PreflightRunResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listPreflightFindings(
    principal: AuthenticatedPrincipal,
    runId: String,
    repositoryFactory: PostgresRepositoryFactory
): List<PreflightFindingResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createPreflightService(principal.projectId)
    val res = service.listFindingsForRun(principal.projectId, runId)
    return when (res) {
        is DomainResult.Success -> res.data.map { PreflightFindingResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listPreflightRunsForArtwork(
    principal: AuthenticatedPrincipal,
    artworkId: String,
    limit: Int = 100,
    repositoryFactory: PostgresRepositoryFactory
): List<PreflightRunResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER, UserRole.AI_AGENT)
    val service = repositoryFactory.createPreflightService(principal.projectId)
    val res = service.listPreflightRunsForArtwork(principal.projectId, artworkId, limit)
    return when (res) {
        is DomainResult.Success -> res.data.map { PreflightRunResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}
