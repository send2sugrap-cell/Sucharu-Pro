package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.ApiSuccessResponse
import com.sucharu.sucharupro.data.api.model.ValidationException
import com.sucharu.sucharupro.data.api.model.inventory.ReceiveFinishedGoodsRequestDto
import com.sucharu.sucharupro.data.event.serialization.EventSerializationHelper

fun parseReceiveFinishedGoodsRequest(body: Any?): ReceiveFinishedGoodsRequestDto {
    val map = EventSerializationHelper.parseJsonObject(body?.toString() ?: "{}")
    val warehouseId = map["warehouseId"]?.toString()
        ?: throw ValidationException("Field 'warehouseId' is required.")
    val binId = map["binId"]?.toString()
    val notes = map["notes"]?.toString()
    return ReceiveFinishedGoodsRequestDto(
        warehouseId = warehouseId,
        binId = binId,
        notes = notes
    )
}

suspend fun handleInventoryRouterRequest(
    securityContext: BackendSecurityContext,
    useCases: BackendUseCases,
    request: HttpRequest
): HttpResponse? {
    val correlationId = request.correlationId

    return when {
        // POST /api/v1/inventory/finished-goods/jobs/{jobId}/receive
        request.path.matches(Regex("^/api/v1/inventory/finished-goods/jobs/[^/]+/receive$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val jobId = request.path.removePrefix("/api/v1/inventory/finished-goods/jobs/").removeSuffix("/receive")
            val reqDto = parseReceiveFinishedGoodsRequest(request.body)
            val res = useCases.receiveFinishedGoodsFromProduction(principal, jobId, reqDto)
            HttpResponse(201, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        // GET /api/v1/inventory/finished-goods/jobs/{jobId}/eligibility
        request.path.matches(Regex("^/api/v1/inventory/finished-goods/jobs/[^/]+/eligibility$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val jobId = request.path.removePrefix("/api/v1/inventory/finished-goods/jobs/").removeSuffix("/eligibility")
            val res = useCases.evaluateInventoryEligibility(principal, jobId)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        // GET /api/v1/inventory/finished-goods/jobs/{jobId}/receipt
        request.path.matches(Regex("^/api/v1/inventory/finished-goods/jobs/[^/]+/receipt$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val jobId = request.path.removePrefix("/api/v1/inventory/finished-goods/jobs/").removeSuffix("/receipt")
            val res = useCases.getFinishedGoodsReceiptForJob(principal, jobId)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        else -> null
    }
}
