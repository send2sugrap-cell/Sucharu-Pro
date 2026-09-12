package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.preflight.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.preflight.*

/**
 * Standalone Router Extension for Module 22 Preflight & Finding Governance Endpoints.
 */

private val securityContextField by lazy {
    BackendRouter::class.java.getDeclaredField("securityContext").apply { isAccessible = true }
}
private val useCasesField by lazy {
    BackendRouter::class.java.getDeclaredField("useCases").apply { isAccessible = true }
}

@Suppress("UNCHECKED_CAST")
private fun parseBodyMap(body: Any?): Map<String, Any?> {
    return when (body) {
        is Map<*, *> -> body as Map<String, Any?>
        else -> emptyMap()
    }
}

private fun parseQueryParams(path: String): Map<String, String> {
    if (!path.contains("?")) return emptyMap()
    val query = path.substringAfter("?")
    return query.split("&")
        .mapNotNull {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else null
        }
        .toMap()
}

private fun parseStartPreflightRequest(body: Any?): StartPreflightRequestDto {
    if (body is StartPreflightRequestDto) return body
    val map = parseBodyMap(body)
    if (map.isNotEmpty()) {
        val artworkId = (map["artworkId"] as? String)?.trim()?.ifBlank { null }
            ?: throw ValidationException("Missing 'artworkId' parameter.")
        val jobId = map["jobId"] as? String
        val artworkVersionId = map["artworkVersionId"] as? String
        val proofId = map["proofId"] as? String
        val idempotencyKey = map["idempotencyKey"] as? String
        @Suppress("UNCHECKED_CAST")
        val orderSpec = (map["orderSpecificationMap"] as? Map<String, Any>) ?: emptyMap()
        @Suppress("UNCHECKED_CAST")
        val artworkMeta = (map["artworkMetadataMap"] as? Map<String, Any>) ?: emptyMap()

        return StartPreflightRequestDto(
            artworkId = artworkId,
            jobId = jobId,
            artworkVersionId = artworkVersionId,
            proofId = proofId,
            idempotencyKey = idempotencyKey,
            orderSpecificationMap = orderSpec,
            artworkMetadataMap = artworkMeta
        )
    }
    throw ValidationException("Request body must be a valid StartPreflightRequestDto.")
}

private fun parseSubmitCorrectionRequest(body: Any?): SubmitCorrectionRequestDto {
    if (body is SubmitCorrectionRequestDto) return body
    val map = parseBodyMap(body)
    if (map.isNotEmpty()) {
        val desc = (map["description"] as? String)?.trim()?.ifBlank { null }
            ?: throw ValidationException("Missing 'description' parameter.")
        val typeStr = (map["correctionType"] as? String)?.uppercase()
        val corrType = try { if (typeStr != null) PreflightCorrectionType.valueOf(typeStr) else PreflightCorrectionType.OTHER } catch (_: Exception) { PreflightCorrectionType.OTHER }
        val artworkVer = map["artworkVersionId"] as? String

        return SubmitCorrectionRequestDto(
            correctionType = corrType,
            description = desc,
            artworkVersionId = artworkVer
        )
    }
    throw ValidationException("Request body must be a valid SubmitCorrectionRequestDto.")
}

private fun parseWaiveFindingRequest(body: Any?): WaiveFindingRequestDto {
    if (body is WaiveFindingRequestDto) return body
    val map = parseBodyMap(body)
    val reason = (map["waiverReason"] as? String)?.trim()?.ifBlank { null }
        ?: throw ValidationException("Missing 'waiverReason' parameter.")
    return WaiveFindingRequestDto(waiverReason = reason)
}

private fun parseEvaluateReadinessRequest(body: Any?): EvaluateProductionReadinessRequestDto {
    if (body is EvaluateProductionReadinessRequestDto) return body
    val map = parseBodyMap(body)
    if (map.isNotEmpty()) {
        val artworkId = (map["artworkId"] as? String)?.trim()?.ifBlank { null }
            ?: throw ValidationException("Missing 'artworkId' parameter.")
        val artworkVersionId = map["artworkVersionId"] as? String
        val proofId = map["proofId"] as? String
        val proofVersionId = map["proofVersionId"] as? String
        val jobId = map["jobId"] as? String
        val preflightRunId = map["preflightRunId"] as? String

        return EvaluateProductionReadinessRequestDto(
            artworkId = artworkId,
            artworkVersionId = artworkVersionId,
            proofId = proofId,
            proofVersionId = proofVersionId,
            jobId = jobId,
            preflightRunId = preflightRunId
        )
    }
    throw ValidationException("Request body must be a valid EvaluateProductionReadinessRequestDto.")
}

suspend fun BackendRouter.handlePreflightRoutes(
    request: HttpRequest,
    correlationId: String
): HttpResponse? {
    val secContext = securityContextField.get(this) as BackendSecurityContext
    val uCases = useCasesField.get(this) as BackendUseCases
    return handlePreflightRoutes(secContext, uCases, this.repositoryFactory, request, correlationId)
}

suspend fun handlePreflightRoutes(
    securityContext: BackendSecurityContext,
    useCases: BackendUseCases,
    repositoryFactory: PostgresRepositoryFactory?,
    request: HttpRequest,
    correlationId: String
): HttpResponse? {
    return when {
        request.path == "/api/v1/preflight/runs" && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val reqDto = parseStartPreflightRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.startPreflightRun(principal, reqDto, rf)
            HttpResponse(201, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/preflight/runs/[^/]+$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val runId = request.path.removePrefix("/api/v1/preflight/runs/")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.getPreflightRunDetails(principal, runId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/preflight/runs/[^/]+/findings$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val runId = request.path.removePrefix("/api/v1/preflight/runs/").removeSuffix("/findings")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.listPreflightFindings(principal, runId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/preflight/artworks/[^/]+/runs$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val artworkId = request.path.removePrefix("/api/v1/preflight/artworks/").removeSuffix("/runs")
            val queryParams = parseQueryParams(request.path)
            val limit = queryParams["limit"]?.toIntOrNull() ?: 100
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.listPreflightRunsForArtwork(principal, artworkId, limit, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/preflight/findings/[^/]+/acknowledge$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val findingId = request.path.removePrefix("/api/v1/preflight/findings/").removeSuffix("/acknowledge")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.acknowledgeFinding(principal, findingId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/preflight/findings/[^/]+/corrections$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val findingId = request.path.removePrefix("/api/v1/preflight/findings/").removeSuffix("/corrections")
            val reqDto = parseSubmitCorrectionRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.submitFindingCorrection(principal, findingId, reqDto, rf)
            HttpResponse(201, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/preflight/findings/[^/]+/revalidate$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val findingId = request.path.removePrefix("/api/v1/preflight/findings/").removeSuffix("/revalidate")
            val reqDto = parseStartPreflightRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.revalidateFinding(principal, findingId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/preflight/findings/[^/]+/waive$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val findingId = request.path.removePrefix("/api/v1/preflight/findings/").removeSuffix("/waive")
            val reqDto = parseWaiveFindingRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.waiveFinding(principal, findingId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/preflight/findings/[^/]+/corrections$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val findingId = request.path.removePrefix("/api/v1/preflight/findings/").removeSuffix("/corrections")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.listCorrectionsForFinding(principal, findingId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path == "/api/v1/preflight/readiness/evaluate" && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val reqDto = parseEvaluateReadinessRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.evaluateProductionReadiness(principal, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        else -> null
    }
}
