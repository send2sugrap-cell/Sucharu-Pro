package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.copilot.*
import com.sucharu.sucharupro.domain.service.ai.SucharuAiContextOrchestrator
import com.sucharu.sucharupro.domain.service.copilot.BusinessCopilotService

/**
 * Extension router for BI-12 — Sucharu AI Gateway & Business Copilot Endpoints.
 *
 * Routes requests to the server-authoritative SucharuAiContextOrchestrator
 * and BusinessCopilotService for AI conversation orchestration.
 */

private val securityContextField by lazy {
    BackendRouter::class.java.getDeclaredField("securityContext").apply { isAccessible = true }
}

private val copilotService = BusinessCopilotService()
private val orchestrator = SucharuAiContextOrchestrator()

@Suppress("UNCHECKED_CAST")
private fun parseBodyMap(body: Any?): Map<String, Any?> {
    return when (body) {
        is Map<*, *> -> body as Map<String, Any?>
        else -> emptyMap()
    }
}

private fun parseProcessCopilotQueryRequest(body: Any?): ProcessCopilotQueryRequestDto {
    if (body is ProcessCopilotQueryRequestDto) return body
    val map = parseBodyMap(body)
    if (map.isNotEmpty()) {
        val queryStr = (map["prompt"] as? String) ?: (map["query"] as? String)
            ?: throw ValidationException("Missing 'prompt' or 'query' parameter.")
        val custId = map["customerId"] as? String
        val lang = map["language"] as? String ?: "bn-BD"

        return ProcessCopilotQueryRequestDto(
            prompt = queryStr,
            query = queryStr,
            customerId = custId,
            language = lang
        )
    }
    throw ValidationException("Request body must be a valid ProcessCopilotQueryRequestDto.")
}

suspend fun BackendRouter.handleCopilotRoutes(
    request: HttpRequest,
    correlationId: String
): HttpResponse? {
    val secContext = securityContextField.get(this) as BackendSecurityContext
    return handleCopilotRoutes(secContext, request, correlationId)
}

fun handleCopilotRoutes(
    securityContext: BackendSecurityContext,
    request: HttpRequest,
    correlationId: String
): HttpResponse? {
    return when {
        request.path == "/api/v1/copilot/query" && request.method == "POST" -> {
            val principal = try {
                securityContext.authenticate(request.authorizationHeader)
            } catch (_: Exception) {
                null
            }

            val reqDto = parseProcessCopilotQueryRequest(request.body)
            val userId = principal?.userId ?: "GUEST-WALL-USER"
            val role = principal?.role?.name ?: "CUSTOMER"
            val projectId = principal?.projectId ?: "TENANT-001"
            val customerId = reqDto.customerId ?: principal?.effectiveCustomerId ?: "CUST-GUEST"

            val orchestrated = orchestrator.orchestrateQuery(
                userId = userId,
                role = role,
                projectId = projectId,
                customerId = customerId,
                query = reqDto.prompt
            )

            val proposalDtos = orchestrated.actionProposals.map { prop ->
                CopilotActionProposalDto(
                    proposalId = prop.proposalId,
                    toolId = prop.toolId,
                    toolName = prop.toolName,
                    actionType = prop.actionType,
                    targetEntityId = prop.targetEntityId,
                    previewDescription = prop.previewDescription,
                    isConfirmationRequired = prop.isConfirmationRequired,
                    isExecuted = prop.isExecuted,
                    isConfirmedByHuman = prop.isConfirmedByHuman
                )
            }

            val responseDto = BusinessCopilotResponseDto(
                query = orchestrated.query,
                reply = orchestrated.responseText,
                rawResponseText = orchestrated.responseText,
                toolProposals = proposalDtos,
                detectedIntent = orchestrated.outcomeType.name,
                language = reqDto.language,
                isConfirmationPending = orchestrated.isConfirmationRequired,
                isShadowErpDatabaseCreated = false,
                generatedAt = orchestrated.generatedAt
            )

            HttpResponse(200, ApiSuccessResponse(data = responseDto, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/copilot/proposals/[^/]+/confirm$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val proposalId = request.path.removePrefix("/api/v1/copilot/proposals/").removeSuffix("/confirm")

            val executed = copilotService.confirmAndExecuteProposal(
                proposalId = proposalId,
                actorId = principal.userId
            )

            val dto = CopilotActionProposalDto(
                proposalId = executed.proposalId,
                toolId = executed.toolId,
                toolName = executed.toolName,
                actionType = executed.actionType,
                targetEntityId = executed.targetEntityId,
                previewDescription = executed.previewDescription,
                isConfirmationRequired = executed.isConfirmationRequired,
                isExecuted = executed.isExecuted,
                isConfirmedByHuman = executed.isConfirmedByHuman
            )

            HttpResponse(200, ApiSuccessResponse(data = dto, correlationId = correlationId), correlationId)
        }

        else -> null
    }
}
