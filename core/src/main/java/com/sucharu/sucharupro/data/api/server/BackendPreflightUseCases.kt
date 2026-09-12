package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.preflight.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.preflight.PreflightExecutionContext

/**
 * Extension Use Cases for Automated Proofing & Preflight Engine & Finding Governance (Module 22 Step 08).
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

// Step 08 Finding Governance Use Cases

suspend fun BackendUseCases.acknowledgeFinding(
    principal: AuthenticatedPrincipal,
    findingId: String,
    repositoryFactory: PostgresRepositoryFactory
): PreflightFindingResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createPreflightService(principal.projectId)
    val res = service.acknowledgeFinding(principal.projectId, findingId, principal.userId)
    return when (res) {
        is DomainResult.Success -> PreflightFindingResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.submitFindingCorrection(
    principal: AuthenticatedPrincipal,
    findingId: String,
    request: SubmitCorrectionRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): PreflightFindingCorrectionResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createPreflightService(principal.projectId)
    val res = service.submitCorrection(
        tenantId = principal.projectId,
        findingId = findingId,
        correctionType = request.correctionType,
        description = request.description,
        artworkVersionId = request.artworkVersionId,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> PreflightFindingCorrectionResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.revalidateFinding(
    principal: AuthenticatedPrincipal,
    findingId: String,
    request: StartPreflightRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): PreflightFindingResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
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
    val res = service.revalidateFinding(principal.projectId, findingId, context, principal.userId)
    return when (res) {
        is DomainResult.Success -> PreflightFindingResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.waiveFinding(
    principal: AuthenticatedPrincipal,
    findingId: String,
    request: WaiveFindingRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): PreflightFindingResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createPreflightService(principal.projectId)
    val res = service.waiveFinding(principal.projectId, findingId, request.waiverReason, principal.userId)
    return when (res) {
        is DomainResult.Success -> PreflightFindingResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listCorrectionsForFinding(
    principal: AuthenticatedPrincipal,
    findingId: String,
    repositoryFactory: PostgresRepositoryFactory
): List<PreflightFindingCorrectionResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.CUSTOMER)
    val service = repositoryFactory.createPreflightService(principal.projectId)
    val res = service.listCorrectionsForFinding(principal.projectId, findingId)
    return when (res) {
        is DomainResult.Success -> res.data.map { PreflightFindingCorrectionResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

// Step 09 Production Readiness Use Cases

suspend fun BackendUseCases.evaluateProductionReadiness(
    principal: AuthenticatedPrincipal,
    request: EvaluateProductionReadinessRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): PreflightProductionReadinessResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AI_AGENT)
    val service = repositoryFactory.createPreflightService(principal.projectId)
    val res = service.evaluateProductionReadiness(
        tenantId = principal.projectId,
        artworkId = request.artworkId,
        artworkVersionId = request.artworkVersionId,
        proofId = request.proofId,
        proofVersionId = request.proofVersionId,
        jobId = request.jobId,
        preflightRunId = request.preflightRunId,
        evaluatorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> PreflightProductionReadinessResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}
