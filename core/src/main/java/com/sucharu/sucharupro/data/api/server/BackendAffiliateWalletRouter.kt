package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.affiliate.wallet.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus

/**
 * Standalone Router Extension for Module 23 Step 01 Affiliate Wallet Foundation Endpoints.
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

private fun parseCreateWalletRequest(body: Any?): CreateWalletRequestDto {
    if (body is CreateWalletRequestDto) return body
    val map = parseBodyMap(body)
    if (map.isNotEmpty()) {
        val affId = (map["affiliateId"] as? String)?.trim()?.ifBlank { null }
            ?: throw ValidationException("Missing 'affiliateId' parameter.")
        val curr = (map["currency"] as? String)?.trim()?.ifBlank { "BDT" } ?: "BDT"
        return CreateWalletRequestDto(affiliateId = affId, currency = curr)
    }
    throw ValidationException("Request body must be a valid CreateWalletRequestDto.")
}

private fun parseUpdateWalletStatusRequest(body: Any?): UpdateWalletStatusRequestDto {
    if (body is UpdateWalletStatusRequestDto) return body
    val map = parseBodyMap(body)
    val statusStr = (map["status"] as? String)?.uppercase()
        ?: throw ValidationException("Missing 'status' parameter.")
    val status = try {
        AffiliateWalletStatus.valueOf(statusStr)
    } catch (_: Exception) {
        throw ValidationException("Invalid status '$statusStr'. Must be ACTIVE, SUSPENDED, or CLOSED.")
    }
    return UpdateWalletStatusRequestDto(status = status)
}

private fun parsePostLedgerEntryRequest(body: Any?): PostLedgerEntryRequestDto {
    if (body is PostLedgerEntryRequestDto) return body
    val map = parseBodyMap(body)
    if (map.isNotEmpty()) {
        val amountNum = map["amount"]
        val amount = when (amountNum) {
            is java.math.BigDecimal -> amountNum
            is Number -> java.math.BigDecimal.valueOf(amountNum.toDouble())
            is String -> java.math.BigDecimal(amountNum)
            else -> throw ValidationException("Missing or invalid 'amount' parameter.")
        }
        val refId = map["referenceId"] as? String
        val idempKey = map["idempotencyKey"] as? String
        val reason = map["reason"] as? String

        return PostLedgerEntryRequestDto(
            amount = amount,
            referenceId = refId,
            idempotencyKey = idempKey,
            reason = reason
        )
    }
    throw ValidationException("Request body must be a valid PostLedgerEntryRequestDto.")
}

private fun parseReverseLedgerEntryRequest(body: Any?): ReverseLedgerEntryRequestDto {
    if (body is ReverseLedgerEntryRequestDto) return body
    val map = parseBodyMap(body)
    val origId = (map["originalEntryId"] as? String)?.trim()?.ifBlank { null }
        ?: throw ValidationException("Missing 'originalEntryId' parameter.")
    val reason = (map["reason"] as? String)?.trim()?.ifBlank { null }
        ?: throw ValidationException("Missing 'reason' parameter.")
    return ReverseLedgerEntryRequestDto(originalEntryId = origId, reason = reason)
}

private fun parseAdjustWalletBalanceRequest(body: Any?): AdjustWalletBalanceRequestDto {
    if (body is AdjustWalletBalanceRequestDto) return body
    val map = parseBodyMap(body)
    val amountNum = map["amount"]
    val amount = when (amountNum) {
        is java.math.BigDecimal -> amountNum
        is Number -> java.math.BigDecimal.valueOf(amountNum.toDouble())
        is String -> java.math.BigDecimal(amountNum)
        else -> throw ValidationException("Missing or invalid 'amount' parameter.")
    }
    val dirStr = (map["direction"] as? String)?.uppercase()
        ?: throw ValidationException("Missing 'direction' parameter.")
    val direction = try {
        com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerDirection.valueOf(dirStr)
    } catch (_: Exception) {
        throw ValidationException("Invalid direction '$dirStr'. Must be CREDIT or DEBIT.")
    }
    val reason = (map["reason"] as? String)?.trim()?.ifBlank { null }
        ?: throw ValidationException("Missing 'reason' parameter.")

    return AdjustWalletBalanceRequestDto(amount = amount, direction = direction, reason = reason)
}

private fun parseCreditEarningToWalletRequest(body: Any?): CreditEarningToWalletRequestDto {
    if (body is CreditEarningToWalletRequestDto) return body
    val map = parseBodyMap(body)
    if (map.isNotEmpty()) {
        val earnRef = (map["earningReferenceId"] as? String)?.trim()?.ifBlank { null }
            ?: throw ValidationException("Missing 'earningReferenceId' parameter.")
        val affId = (map["affiliateId"] as? String)?.trim()?.ifBlank { null }
            ?: throw ValidationException("Missing 'affiliateId' parameter.")
        val amountNum = map["amount"]
        val amount = when (amountNum) {
            is java.math.BigDecimal -> amountNum
            is Number -> java.math.BigDecimal.valueOf(amountNum.toDouble())
            is String -> java.math.BigDecimal(amountNum)
            else -> throw ValidationException("Missing or invalid 'amount' parameter.")
        }
        val curr = (map["currency"] as? String)?.trim()?.ifBlank { "BDT" } ?: "BDT"
        val source = (map["source"] as? String)?.trim()?.ifBlank { "APPROVED_COMMISSION" } ?: "APPROVED_COMMISSION"
        val sourceOrderId = map["sourceOrderId"] as? String
        val approvedAt = (map["approvedAt"] as? Number)?.toLong()
        val idempKey = map["idempotencyKey"] as? String

        return CreditEarningToWalletRequestDto(
            earningReferenceId = earnRef,
            affiliateId = affId,
            amount = amount,
            currency = curr,
            source = source,
            sourceOrderId = sourceOrderId,
            approvedAt = approvedAt,
            idempotencyKey = idempKey
        )
    }
    throw ValidationException("Request body must be a valid CreditEarningToWalletRequestDto.")
}

private fun parseCreateWalletHoldRequest(body: Any?): CreateWalletHoldRequestDto {
    if (body is CreateWalletHoldRequestDto) return body
    val map = parseBodyMap(body)
    val amountNum = map["amount"]
    val amount = when (amountNum) {
        is java.math.BigDecimal -> amountNum
        is Number -> java.math.BigDecimal.valueOf(amountNum.toDouble())
        is String -> java.math.BigDecimal(amountNum)
        else -> throw ValidationException("Missing or invalid 'amount' parameter.")
    }
    val typeStr = (map["holdType"] as? String)?.uppercase()
    val holdType = try {
        if (typeStr != null) com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHoldType.valueOf(typeStr)
        else com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHoldType.BUSINESS_POLICY_HOLD
    } catch (_: Exception) {
        com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHoldType.BUSINESS_POLICY_HOLD
    }
    val reason = (map["reason"] as? String)?.trim()?.ifBlank { null }
        ?: throw ValidationException("Missing 'reason' parameter.")
    val refId = map["referenceId"] as? String

    return CreateWalletHoldRequestDto(amount = amount, holdType = holdType, reason = reason, referenceId = refId)
}

private fun parseReleaseWalletHoldRequest(body: Any?): ReleaseWalletHoldRequestDto {
    if (body is ReleaseWalletHoldRequestDto) return body
    val map = parseBodyMap(body)
    val reason = (map["releaseReason"] as? String)?.trim()?.ifBlank { null }
        ?: throw ValidationException("Missing 'releaseReason' parameter.")
    return ReleaseWalletHoldRequestDto(releaseReason = reason)
}

private fun parseSubmitPayoutRequest(body: Any?): SubmitPayoutRequestDto {
    if (body is SubmitPayoutRequestDto) return body
    val map = parseBodyMap(body)
    if (map.isNotEmpty()) {
        val amountNum = map["requestedAmount"] ?: map["amount"]
        val amount = when (amountNum) {
            is java.math.BigDecimal -> amountNum
            is Number -> java.math.BigDecimal.valueOf(amountNum.toDouble())
            is String -> java.math.BigDecimal(amountNum)
            else -> throw ValidationException("Missing or invalid 'requestedAmount' parameter.")
        }
        val curr = (map["currency"] as? String)?.trim()?.ifBlank { "BDT" } ?: "BDT"
        val typeStr = (map["payoutMethodType"] as? String)?.uppercase()
        val methodType = try {
            if (typeStr != null) com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutMethodType.valueOf(typeStr)
            else com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutMethodType.BANK_TRANSFER
        } catch (_: Exception) {
            com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutMethodType.BANK_TRANSFER
        }
        val accName = (map["payoutMethodAccountName"] as? String)?.trim()?.ifBlank { null }
            ?: throw ValidationException("Missing 'payoutMethodAccountName' parameter.")
        val accNum = (map["payoutMethodAccountNumber"] as? String)?.trim()?.ifBlank { null }
            ?: throw ValidationException("Missing 'payoutMethodAccountNumber' parameter.")
        val provider = map["payoutMethodProvider"] as? String
        val branch = map["payoutMethodBranchRouting"] as? String
        val idempKey = map["idempotencyKey"] as? String
        val minThresh = (map["minimumThreshold"] as? Number)?.toDouble()?.let { java.math.BigDecimal.valueOf(it) }

        return SubmitPayoutRequestDto(
            requestedAmount = amount,
            currency = curr,
            payoutMethodType = methodType,
            payoutMethodAccountName = accName,
            payoutMethodAccountNumber = accNum,
            payoutMethodProvider = provider,
            payoutMethodBranchRouting = branch,
            idempotencyKey = idempKey,
            minimumThreshold = minThresh
        )
    }
    throw ValidationException("Request body must be a valid SubmitPayoutRequestDto.")
}

private fun parseUpdatePayoutRequestStatus(body: Any?): UpdatePayoutRequestStatusDto {
    if (body is UpdatePayoutRequestStatusDto) return body
    val map = parseBodyMap(body)
    val statusStr = (map["newStatus"] as? String ?: map["status"] as? String)?.uppercase()
        ?: throw ValidationException("Missing 'newStatus' parameter.")
    val status = try {
        com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequestStatus.valueOf(statusStr)
    } catch (_: Exception) {
        throw ValidationException("Invalid payout request status '$statusStr'.")
    }
    val reason = map["reason"] as? String

    return UpdatePayoutRequestStatusDto(newStatus = status, reason = reason)
}

private fun parseReviewPayoutRequest(body: Any?): ReviewPayoutRequestDto {
    if (body is ReviewPayoutRequestDto) return body
    val map = parseBodyMap(body)
    val notes = map["notes"] as? String
    return ReviewPayoutRequestDto(notes = notes)
}

private fun parseApprovePayoutRequest(body: Any?): ApprovePayoutRequestDto {
    if (body is ApprovePayoutRequestDto) return body
    val map = parseBodyMap(body)
    val notes = map["notes"] as? String
    return ApprovePayoutRequestDto(notes = notes)
}

private fun parseRejectPayoutRequest(body: Any?): RejectPayoutRequestDto {
    if (body is RejectPayoutRequestDto) return body
    val map = parseBodyMap(body)
    val reason = (map["rejectionReason"] as? String ?: map["reason"] as? String)?.trim()?.ifBlank { null }
        ?: throw ValidationException("Missing 'rejectionReason' parameter.")
    return RejectPayoutRequestDto(rejectionReason = reason)
}

private fun parseReversePayoutRequest(body: Any?): ReversePayoutRequestDto {
    if (body is ReversePayoutRequestDto) return body
    val map = parseBodyMap(body)
    val reason = (map["reversalReason"] as? String ?: map["reason"] as? String)?.trim()?.ifBlank { null }
        ?: throw ValidationException("Missing 'reversalReason' parameter.")
    return ReversePayoutRequestDto(reversalReason = reason)
}

private fun parseReconcilePayoutRequest(body: Any?): ReconcilePayoutRequestDto {
    if (body is ReconcilePayoutRequestDto) return body
    val map = parseBodyMap(body)
    val notes = map["notes"] as? String
    return ReconcilePayoutRequestDto(notes = notes)
}

suspend fun BackendRouter.handleAffiliateWalletRoutes(
    request: HttpRequest,
    correlationId: String
): HttpResponse? {
    val secContext = securityContextField.get(this) as BackendSecurityContext
    val uCases = useCasesField.get(this) as BackendUseCases
    return handleAffiliateWalletRoutes(secContext, uCases, this.repositoryFactory, request, correlationId)
}

suspend fun handleAffiliateWalletRoutes(
    securityContext: BackendSecurityContext,
    useCases: BackendUseCases,
    repositoryFactory: PostgresRepositoryFactory?,
    request: HttpRequest,
    correlationId: String
): HttpResponse? {
    return when {
        request.path == "/api/v1/affiliates/wallets" && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val reqDto = parseCreateWalletRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.getOrCreateAffiliateWallet(principal, reqDto, rf)
            HttpResponse(201, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.getAffiliateWalletDetails(principal, walletId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/[^/]+/wallet$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val affiliateId = request.path.removePrefix("/api/v1/affiliates/").removeSuffix("/wallet")
            val queryParams = parseQueryParams(request.path)
            val currency = queryParams["currency"] ?: "BDT"
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.getAffiliateWalletByAffiliate(principal, affiliateId, currency, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/status$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/status")
            val reqDto = parseUpdateWalletStatusRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.updateAffiliateWalletStatus(principal, walletId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/balance$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/balance")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.getAffiliateWalletBalance(principal, walletId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/ledger$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/ledger")
            val queryParams = parseQueryParams(request.path)
            val limit = queryParams["limit"]?.toIntOrNull() ?: 100
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.listAffiliateWalletLedgerEntries(principal, walletId, limit, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/credit$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/credit")
            val reqDto = parsePostLedgerEntryRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.creditAffiliateWallet(principal, walletId, reqDto, rf)
            HttpResponse(201, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/debit$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/debit")
            val reqDto = parsePostLedgerEntryRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.debitAffiliateWallet(principal, walletId, reqDto, rf)
            HttpResponse(201, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/reverse$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/reverse")
            val reqDto = parseReverseLedgerEntryRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.reverseAffiliateWalletLedgerEntry(principal, walletId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/adjust$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/adjust")
            val reqDto = parseAdjustWalletBalanceRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.adjustAffiliateWalletBalance(principal, walletId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path == "/api/v1/affiliates/wallets/credits/from-earning" && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val reqDto = parseCreditEarningToWalletRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.creditApprovedEarningToWallet(principal, reqDto, rf)
            HttpResponse(201, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/holds$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/holds")
            val queryParams = parseQueryParams(request.path)
            val activeOnly = queryParams["activeOnly"]?.toBooleanStrictOrNull() ?: true
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.listWalletHolds(principal, walletId, activeOnly, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/holds$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/holds")
            val reqDto = parseCreateWalletHoldRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.createWalletHold(principal, walletId, reqDto, rf)
            HttpResponse(201, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/holds/[^/]+/release$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val pathRemainder = request.path.removePrefix("/api/v1/affiliates/wallets/")
            val walletId = pathRemainder.substringBefore("/holds/")
            val holdId = pathRemainder.substringAfter("/holds/").removeSuffix("/release")
            val reqDto = parseReleaseWalletHoldRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.releaseWalletHold(principal, walletId, holdId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/eligibility$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/eligibility")
            val queryParams = parseQueryParams(request.path)
            val reqAmount = queryParams["requestedAmount"]?.let { java.math.BigDecimal(it) }
            val minThresh = queryParams["minimumThreshold"]?.let { java.math.BigDecimal(it) }
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.evaluateAffiliatePayoutEligibility(principal, walletId, reqAmount, minThresh, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/payout-requests$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/payout-requests")
            val reqDto = parseSubmitPayoutRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.submitAffiliatePayoutRequest(principal, walletId, reqDto, rf)
            HttpResponse(201, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/wallets/[^/]+/payout-requests$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val walletId = request.path.removePrefix("/api/v1/affiliates/wallets/").removeSuffix("/payout-requests")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.listAffiliatePayoutRequestsForWallet(principal, walletId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/payout-requests/[^/]+$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val requestId = request.path.removePrefix("/api/v1/affiliates/payout-requests/")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.getAffiliatePayoutRequestDetails(principal, requestId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/payout-requests/[^/]+/status$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val requestId = request.path.removePrefix("/api/v1/affiliates/payout-requests/").removeSuffix("/status")
            val reqDto = parseUpdatePayoutRequestStatus(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.updateAffiliatePayoutRequestStatus(principal, requestId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/payout-requests/[^/]+/review$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val requestId = request.path.removePrefix("/api/v1/affiliates/payout-requests/").removeSuffix("/review")
            val reqDto = parseReviewPayoutRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.reviewAffiliatePayoutRequest(principal, requestId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/payout-requests/[^/]+/approve$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val requestId = request.path.removePrefix("/api/v1/affiliates/payout-requests/").removeSuffix("/approve")
            val reqDto = parseApprovePayoutRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.approveAffiliatePayoutRequest(principal, requestId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/payout-requests/[^/]+/reject$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val requestId = request.path.removePrefix("/api/v1/affiliates/payout-requests/").removeSuffix("/reject")
            val reqDto = parseRejectPayoutRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.rejectAffiliatePayoutRequest(principal, requestId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/payout-requests/[^/]+/disburse$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val requestId = request.path.removePrefix("/api/v1/affiliates/payout-requests/").removeSuffix("/disburse")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.disburseAffiliatePayout(principal, requestId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/payout-requests/[^/]+/disbursements$")) && request.method == "GET" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val requestId = request.path.removePrefix("/api/v1/affiliates/payout-requests/").removeSuffix("/disbursements")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.listAffiliatePayoutDisbursements(principal, requestId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/payout-requests/[^/]+/retry$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val requestId = request.path.removePrefix("/api/v1/affiliates/payout-requests/").removeSuffix("/retry")
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.retryAffiliatePayoutDisbursement(principal, requestId, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/payout-requests/[^/]+/reverse$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val requestId = request.path.removePrefix("/api/v1/affiliates/payout-requests/").removeSuffix("/reverse")
            val reqDto = parseReversePayoutRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.reverseAffiliatePayout(principal, requestId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        request.path.matches(Regex("^/api/v1/affiliates/payout-requests/[^/]+/reconcile$")) && request.method == "POST" -> {
            val principal = securityContext.authenticate(request.authorizationHeader)
            val requestId = request.path.removePrefix("/api/v1/affiliates/payout-requests/").removeSuffix("/reconcile")
            val reqDto = parseReconcilePayoutRequest(request.body)
            val rf = repositoryFactory ?: throw IllegalStateException("RepositoryFactory is required")
            val res = useCases.reconcileAffiliatePayoutDisbursement(principal, requestId, reqDto, rf)
            HttpResponse(200, ApiSuccessResponse(data = res, correlationId = correlationId), correlationId)
        }

        else -> null
    }
}
