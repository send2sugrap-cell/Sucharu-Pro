package com.sucharu.sucharupro.data.api.server

import com.sucharu.sucharupro.data.api.model.*
import com.sucharu.sucharupro.data.api.model.affiliate.wallet.*
import com.sucharu.sucharupro.data.persistence.postgres.PostgresRepositoryFactory
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus
import com.sucharu.sucharupro.domain.model.affiliate.wallet.ApprovedEarningHandoff

/**
 * Backend Use Cases for Affiliate Wallet Foundation (Module 23 Step 01).
 */

suspend fun BackendUseCases.getOrCreateAffiliateWallet(
    principal: AuthenticatedPrincipal,
    request: CreateWalletRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliateWalletService(principal.projectId)
    val res = service.getOrCreateWallet(
        tenantId = principal.projectId,
        affiliateId = request.affiliateId,
        currency = request.currency,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliateWalletResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.getAffiliateWalletDetails(
    principal: AuthenticatedPrincipal,
    walletId: String,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletResponseDto? {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliateWalletService(principal.projectId)
    val res = service.getWalletDetails(principal.projectId, walletId)
    return when (res) {
        is DomainResult.Success -> res.data?.let { AffiliateWalletResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.getAffiliateWalletByAffiliate(
    principal: AuthenticatedPrincipal,
    affiliateId: String,
    currency: String = "BDT",
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletResponseDto? {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliateWalletService(principal.projectId)
    val res = service.getWalletByAffiliate(principal.projectId, affiliateId, currency)
    return when (res) {
        is DomainResult.Success -> res.data?.let { AffiliateWalletResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.updateAffiliateWalletStatus(
    principal: AuthenticatedPrincipal,
    walletId: String,
    request: UpdateWalletStatusRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliateWalletService(principal.projectId)
    val res = service.updateWalletStatus(
        tenantId = principal.projectId,
        walletId = walletId,
        status = request.status,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliateWalletResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

// Step 02 Ledger & Balance Use Cases

suspend fun BackendUseCases.creditAffiliateWallet(
    principal: AuthenticatedPrincipal,
    walletId: String,
    request: PostLedgerEntryRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletLedgerEntryResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliateWalletLedgerService(principal.projectId)
    val res = service.creditWallet(
        tenantId = principal.projectId,
        walletId = walletId,
        amount = com.sucharu.sucharupro.domain.model.common.Money(request.amount),
        referenceId = request.referenceId,
        idempotencyKey = request.idempotencyKey,
        reason = request.reason,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliateWalletLedgerEntryResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.debitAffiliateWallet(
    principal: AuthenticatedPrincipal,
    walletId: String,
    request: PostLedgerEntryRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletLedgerEntryResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliateWalletLedgerService(principal.projectId)
    val res = service.debitWallet(
        tenantId = principal.projectId,
        walletId = walletId,
        amount = com.sucharu.sucharupro.domain.model.common.Money(request.amount),
        referenceId = request.referenceId,
        idempotencyKey = request.idempotencyKey,
        reason = request.reason,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliateWalletLedgerEntryResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.reverseAffiliateWalletLedgerEntry(
    principal: AuthenticatedPrincipal,
    walletId: String,
    request: ReverseLedgerEntryRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletLedgerEntryResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliateWalletLedgerService(principal.projectId)
    val res = service.reverseLedgerEntry(
        tenantId = principal.projectId,
        walletId = walletId,
        originalEntryId = request.originalEntryId,
        reason = request.reason,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliateWalletLedgerEntryResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.adjustAffiliateWalletBalance(
    principal: AuthenticatedPrincipal,
    walletId: String,
    request: AdjustWalletBalanceRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletLedgerEntryResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliateWalletLedgerService(principal.projectId)
    val res = service.adjustWalletBalance(
        tenantId = principal.projectId,
        walletId = walletId,
        amount = com.sucharu.sucharupro.domain.model.common.Money(request.amount),
        direction = request.direction,
        reason = request.reason,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliateWalletLedgerEntryResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.getAffiliateWalletBalance(
    principal: AuthenticatedPrincipal,
    walletId: String,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletBalanceResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliateWalletLedgerService(principal.projectId)
    val res = service.calculateWalletBalance(principal.projectId, walletId)
    return when (res) {
        is DomainResult.Success -> AffiliateWalletBalanceResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listAffiliateWalletLedgerEntries(
    principal: AuthenticatedPrincipal,
    walletId: String,
    limit: Int = 100,
    repositoryFactory: PostgresRepositoryFactory
): List<AffiliateWalletLedgerEntryResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliateWalletLedgerService(principal.projectId)
    val res = service.listLedgerEntries(principal.projectId, walletId, limit)
    return when (res) {
        is DomainResult.Success -> res.data.map { AffiliateWalletLedgerEntryResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

// Step 03 Earning -> Wallet Credit Integration Use Case

suspend fun BackendUseCases.creditApprovedEarningToWallet(
    principal: AuthenticatedPrincipal,
    request: CreditEarningToWalletRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletLedgerEntryResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliateEarningWalletIntegrationService(principal.projectId)
    val handoff = ApprovedEarningHandoff(
        earningReferenceId = request.earningReferenceId,
        tenantId = principal.projectId,
        affiliateId = request.affiliateId,
        amount = com.sucharu.sucharupro.domain.model.common.Money(request.amount),
        currency = request.currency,
        source = request.source,
        sourceOrderId = request.sourceOrderId,
        approvedAt = request.approvedAt ?: System.currentTimeMillis(),
        idempotencyKey = request.idempotencyKey
    )
    val res = service.processApprovedEarningCredit(
        principalTenantId = principal.projectId,
        handoff = handoff,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliateWalletLedgerEntryResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

// Step 04 Available Balance & Hold Governance Use Cases

suspend fun BackendUseCases.createWalletHold(
    principal: AuthenticatedPrincipal,
    walletId: String,
    request: CreateWalletHoldRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletHoldResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliateWalletHoldService(principal.projectId)
    val res = service.createHold(
        tenantId = principal.projectId,
        walletId = walletId,
        amount = com.sucharu.sucharupro.domain.model.common.Money(request.amount),
        holdType = request.holdType,
        reason = request.reason,
        referenceId = request.referenceId,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliateWalletHoldResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.releaseWalletHold(
    principal: AuthenticatedPrincipal,
    walletId: String,
    holdId: String,
    request: ReleaseWalletHoldRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliateWalletHoldResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliateWalletHoldService(principal.projectId)
    val res = service.releaseHold(
        tenantId = principal.projectId,
        walletId = walletId,
        holdId = holdId,
        releaseReason = request.releaseReason,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliateWalletHoldResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listWalletHolds(
    principal: AuthenticatedPrincipal,
    walletId: String,
    activeOnly: Boolean = true,
    repositoryFactory: PostgresRepositoryFactory
): List<AffiliateWalletHoldResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliateWalletHoldService(principal.projectId)
    val res = service.listHoldsForWallet(principal.projectId, walletId, activeOnly)
    return when (res) {
        is DomainResult.Success -> res.data.map { AffiliateWalletHoldResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.evaluateAffiliatePayoutEligibility(
    principal: AuthenticatedPrincipal,
    walletId: String,
    requestedAmount: java.math.BigDecimal? = null,
    minimumThreshold: java.math.BigDecimal? = null,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutEligibilityResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliateWalletHoldService(principal.projectId)
    val reqMoney = requestedAmount?.let { com.sucharu.sucharupro.domain.model.common.Money(it) }
    val threshMoney = minimumThreshold?.let { com.sucharu.sucharupro.domain.model.common.Money(it) }
    val res = service.evaluatePayoutEligibility(
        tenantId = principal.projectId,
        walletId = walletId,
        requestedAmount = reqMoney,
        minimumThreshold = threshMoney
    )
    return when (res) {
        is DomainResult.Success -> AffiliatePayoutEligibilityResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

// Step 05 Payout Request Use Cases

suspend fun BackendUseCases.submitAffiliatePayoutRequest(
    principal: AuthenticatedPrincipal,
    walletId: String,
    request: SubmitPayoutRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutRequestResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliatePayoutRequestService(principal.projectId)
    val minThresh = request.minimumThreshold?.let { com.sucharu.sucharupro.domain.model.common.Money(it) }
    val res = service.submitPayoutRequest(
        tenantId = principal.projectId,
        walletId = walletId,
        requestedAmount = com.sucharu.sucharupro.domain.model.common.Money(request.requestedAmount),
        payoutMethodType = request.payoutMethodType,
        accountName = request.payoutMethodAccountName,
        accountNumber = request.payoutMethodAccountNumber,
        provider = request.payoutMethodProvider,
        branchRouting = request.payoutMethodBranchRouting,
        idempotencyKey = request.idempotencyKey,
        minimumThreshold = minThresh,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliatePayoutRequestResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.getAffiliatePayoutRequestDetails(
    principal: AuthenticatedPrincipal,
    requestId: String,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutRequestResponseDto? {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliatePayoutRequestService(principal.projectId)
    val res = service.getPayoutRequestDetails(principal.projectId, requestId)
    return when (res) {
        is DomainResult.Success -> res.data?.let { AffiliatePayoutRequestResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listAffiliatePayoutRequestsForWallet(
    principal: AuthenticatedPrincipal,
    walletId: String,
    repositoryFactory: PostgresRepositoryFactory
): List<AffiliatePayoutRequestResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliatePayoutRequestService(principal.projectId)
    val res = service.listPayoutRequestsForWallet(principal.projectId, walletId)
    return when (res) {
        is DomainResult.Success -> res.data.map { AffiliatePayoutRequestResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.updateAffiliatePayoutRequestStatus(
    principal: AuthenticatedPrincipal,
    requestId: String,
    request: UpdatePayoutRequestStatusDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutRequestResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createAffiliatePayoutRequestService(principal.projectId)
    val res = service.updatePayoutRequestStatus(
        tenantId = principal.projectId,
        requestId = requestId,
        newStatus = request.newStatus,
        reason = request.reason,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliatePayoutRequestResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

// Step 06 Payout Review & Approval Governance Use Cases

suspend fun BackendUseCases.reviewAffiliatePayoutRequest(
    principal: AuthenticatedPrincipal,
    requestId: String,
    request: ReviewPayoutRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutRequestResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)
    val service = repositoryFactory.createAffiliatePayoutRequestService(principal.projectId)
    val res = service.reviewPayoutRequest(
        tenantId = principal.projectId,
        requestId = requestId,
        notes = request.notes,
        reviewerId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliatePayoutRequestResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.approveAffiliatePayoutRequest(
    principal: AuthenticatedPrincipal,
    requestId: String,
    request: ApprovePayoutRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutRequestResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliatePayoutRequestService(principal.projectId)
    val res = service.approvePayoutRequest(
        tenantId = principal.projectId,
        requestId = requestId,
        notes = request.notes,
        approverId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliatePayoutRequestResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.rejectAffiliatePayoutRequest(
    principal: AuthenticatedPrincipal,
    requestId: String,
    request: RejectPayoutRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutRequestResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliatePayoutRequestService(principal.projectId)
    val res = service.rejectPayoutRequest(
        tenantId = principal.projectId,
        requestId = requestId,
        rejectionReason = request.rejectionReason,
        reviewerId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliatePayoutRequestResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

// Step 07 Payout Disbursement Use Cases

suspend fun BackendUseCases.disburseAffiliatePayout(
    principal: AuthenticatedPrincipal,
    requestId: String,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutDisbursementResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliatePayoutDisbursementService(principal.projectId)
    val res = service.processPayoutDisbursement(
        tenantId = principal.projectId,
        requestId = requestId,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliatePayoutDisbursementResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.listAffiliatePayoutDisbursements(
    principal: AuthenticatedPrincipal,
    requestId: String,
    repositoryFactory: PostgresRepositoryFactory
): List<AffiliatePayoutDisbursementResponseDto> {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF, UserRole.AFFILIATE, UserRole.AI_AGENT)
    val service = repositoryFactory.createAffiliatePayoutDisbursementService(principal.projectId)
    val res = service.listDisbursementsForRequest(principal.projectId, requestId)
    return when (res) {
        is DomainResult.Success -> res.data.map { AffiliatePayoutDisbursementResponseDto.fromDomain(it) }
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

// Step 08 Payout Recovery, Reversal & Reconciliation Use Cases

suspend fun BackendUseCases.retryAffiliatePayoutDisbursement(
    principal: AuthenticatedPrincipal,
    requestId: String,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutDisbursementResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliatePayoutRecoveryService(principal.projectId)
    val res = service.retryFailedPayoutDisbursement(
        tenantId = principal.projectId,
        requestId = requestId,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliatePayoutDisbursementResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.reverseAffiliatePayout(
    principal: AuthenticatedPrincipal,
    requestId: String,
    request: ReversePayoutRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutReversalResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliatePayoutRecoveryService(principal.projectId)
    val res = service.reverseCompletedPayout(
        tenantId = principal.projectId,
        requestId = requestId,
        reversalReason = request.reversalReason,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliatePayoutReversalResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}

suspend fun BackendUseCases.reconcileAffiliatePayoutDisbursement(
    principal: AuthenticatedPrincipal,
    requestId: String,
    request: ReconcilePayoutRequestDto,
    repositoryFactory: PostgresRepositoryFactory
): AffiliatePayoutReconciliationResponseDto {
    BackendAuthorizationPolicy.requireRole(principal, UserRole.ADMIN, UserRole.MANAGER)
    val service = repositoryFactory.createAffiliatePayoutRecoveryService(principal.projectId)
    val res = service.reconcilePayoutDisbursement(
        tenantId = principal.projectId,
        requestId = requestId,
        notes = request.notes,
        actorId = principal.userId
    )
    return when (res) {
        is DomainResult.Success -> AffiliatePayoutReconciliationResponseDto.fromDomain(res.data)
        is DomainResult.Error -> throw IllegalArgumentException(res.message)
        else -> throw IllegalStateException("Unexpected result state")
    }
}
