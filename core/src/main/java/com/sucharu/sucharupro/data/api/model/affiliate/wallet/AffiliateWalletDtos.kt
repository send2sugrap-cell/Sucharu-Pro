package com.sucharu.sucharupro.data.api.model.affiliate.wallet

import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWallet
import com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletStatus

/**
 * REST API DTOs for Affiliate Wallet Foundation (Module 23 Step 01).
 */
data class CreateWalletRequestDto(
    val affiliateId: String,
    val currency: String = "BDT"
)

data class UpdateWalletStatusRequestDto(
    val status: AffiliateWalletStatus
)

data class AffiliateWalletResponseDto(
    val walletId: String,
    val tenantId: String,
    val affiliateId: String,
    val currency: String,
    val status: AffiliateWalletStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long
) {
    companion object {
        fun fromDomain(domain: AffiliateWallet): AffiliateWalletResponseDto = AffiliateWalletResponseDto(
            walletId = domain.walletId,
            tenantId = domain.tenantId,
            affiliateId = domain.affiliateId,
            currency = domain.currency,
            status = domain.status,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
            version = domain.version
        )
    }
}

data class PostLedgerEntryRequestDto(
    val amount: java.math.BigDecimal,
    val referenceId: String? = null,
    val idempotencyKey: String? = null,
    val reason: String? = null
)

data class ReverseLedgerEntryRequestDto(
    val originalEntryId: String,
    val reason: String
)

data class AdjustWalletBalanceRequestDto(
    val amount: java.math.BigDecimal,
    val direction: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerDirection,
    val reason: String
)

data class AffiliateWalletLedgerEntryResponseDto(
    val entryId: String,
    val tenantId: String,
    val walletId: String,
    val affiliateId: String,
    val currency: String,
    val entryType: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntryType,
    val direction: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerDirection,
    val amount: java.math.BigDecimal,
    val referenceId: String?,
    val idempotencyKey: String?,
    val reversalOfEntryId: String?,
    val reason: String?,
    val actorId: String,
    val createdAt: Long
) {
    companion object {
        fun fromDomain(domain: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletLedgerEntry): AffiliateWalletLedgerEntryResponseDto = AffiliateWalletLedgerEntryResponseDto(
            entryId = domain.entryId,
            tenantId = domain.tenantId,
            walletId = domain.walletId,
            affiliateId = domain.affiliateId,
            currency = domain.currency,
            entryType = domain.entryType,
            direction = domain.direction,
            amount = domain.amount.amount,
            referenceId = domain.referenceId,
            idempotencyKey = domain.idempotencyKey,
            reversalOfEntryId = domain.reversalOfEntryId,
            reason = domain.reason,
            actorId = domain.actorId,
            createdAt = domain.createdAt
        )
    }
}

data class AffiliateWalletBalanceResponseDto(
    val walletId: String,
    val tenantId: String,
    val affiliateId: String,
    val currency: String,
    val currentBalance: java.math.BigDecimal,
    val availableBalance: java.math.BigDecimal,
    val pendingBalance: java.math.BigDecimal,
    val heldAmount: java.math.BigDecimal,
    val totalCredits: java.math.BigDecimal,
    val totalDebits: java.math.BigDecimal,
    val lastLedgerEntryId: String?,
    val lastUpdatedAt: Long
) {
    companion object {
        fun fromDomain(domain: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletBalance): AffiliateWalletBalanceResponseDto = AffiliateWalletBalanceResponseDto(
            walletId = domain.walletId,
            tenantId = domain.tenantId,
            affiliateId = domain.affiliateId,
            currency = domain.currency,
            currentBalance = domain.currentBalance.amount,
            availableBalance = domain.availableBalance.amount,
            pendingBalance = domain.pendingBalance.amount,
            heldAmount = domain.heldAmount.amount,
            totalCredits = domain.totalCredits.amount,
            totalDebits = domain.totalDebits.amount,
            lastLedgerEntryId = domain.lastLedgerEntryId,
            lastUpdatedAt = domain.lastUpdatedAt
        )
    }
}

data class CreditEarningToWalletRequestDto(
    val earningReferenceId: String,
    val affiliateId: String,
    val amount: java.math.BigDecimal,
    val currency: String = "BDT",
    val source: String = "APPROVED_COMMISSION",
    val sourceOrderId: String? = null,
    val approvedAt: Long? = null,
    val idempotencyKey: String? = null
)

data class CreateWalletHoldRequestDto(
    val amount: java.math.BigDecimal,
    val holdType: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHoldType = com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHoldType.BUSINESS_POLICY_HOLD,
    val reason: String,
    val referenceId: String? = null
)

data class ReleaseWalletHoldRequestDto(
    val releaseReason: String
)

data class AffiliateWalletHoldResponseDto(
    val holdId: String,
    val tenantId: String,
    val walletId: String,
    val affiliateId: String,
    val amount: java.math.BigDecimal,
    val currency: String,
    val holdType: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHoldType,
    val holdReason: String,
    val status: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHoldStatus,
    val referenceId: String?,
    val createdBy: String,
    val createdAt: Long,
    val releasedBy: String?,
    val releasedAt: Long?,
    val releaseReason: String?
) {
    companion object {
        fun fromDomain(domain: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliateWalletHold): AffiliateWalletHoldResponseDto = AffiliateWalletHoldResponseDto(
            holdId = domain.holdId,
            tenantId = domain.tenantId,
            walletId = domain.walletId,
            affiliateId = domain.affiliateId,
            amount = domain.amount.amount,
            currency = domain.currency,
            holdType = domain.holdType,
            holdReason = domain.holdReason,
            status = domain.status,
            referenceId = domain.referenceId,
            createdBy = domain.createdBy,
            createdAt = domain.createdAt,
            releasedBy = domain.releasedBy,
            releasedAt = domain.releasedAt,
            releaseReason = domain.releaseReason
        )
    }
}

data class AffiliatePayoutEligibilityResponseDto(
    val walletId: String,
    val tenantId: String,
    val affiliateId: String,
    val currency: String,
    val isEligible: Boolean,
    val currentBalance: java.math.BigDecimal,
    val availableBalance: java.math.BigDecimal,
    val pendingBalance: java.math.BigDecimal,
    val heldAmount: java.math.BigDecimal,
    val minimumThreshold: java.math.BigDecimal?,
    val isThresholdSatisfied: Boolean,
    val blockingReasons: List<String>,
    val evaluatedAt: Long
) {
    companion object {
        fun fromDomain(domain: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutEligibility): AffiliatePayoutEligibilityResponseDto = AffiliatePayoutEligibilityResponseDto(
            walletId = domain.walletId,
            tenantId = domain.tenantId,
            affiliateId = domain.affiliateId,
            currency = domain.currency,
            isEligible = domain.isEligible,
            currentBalance = domain.currentBalance.amount,
            availableBalance = domain.availableBalance.amount,
            pendingBalance = domain.pendingBalance.amount,
            heldAmount = domain.heldAmount.amount,
            minimumThreshold = domain.minimumThreshold?.amount,
            isThresholdSatisfied = domain.isThresholdSatisfied,
            blockingReasons = domain.blockingReasons,
            evaluatedAt = domain.evaluatedAt
        )
    }
}

data class SubmitPayoutRequestDto(
    val requestedAmount: java.math.BigDecimal,
    val currency: String = "BDT",
    val payoutMethodType: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutMethodType = com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutMethodType.BANK_TRANSFER,
    val payoutMethodAccountName: String,
    val payoutMethodAccountNumber: String,
    val payoutMethodProvider: String? = null,
    val payoutMethodBranchRouting: String? = null,
    val idempotencyKey: String? = null,
    val minimumThreshold: java.math.BigDecimal? = null
)

data class UpdatePayoutRequestStatusDto(
    val newStatus: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequestStatus,
    val reason: String? = null
)

data class ReviewPayoutRequestDto(
    val notes: String? = null
)

data class ApprovePayoutRequestDto(
    val notes: String? = null
)

data class RejectPayoutRequestDto(
    val rejectionReason: String
)

data class AffiliatePayoutRequestResponseDto(
    val requestId: String,
    val tenantId: String,
    val walletId: String,
    val affiliateId: String,
    val requestedAmount: java.math.BigDecimal,
    val currency: String,
    val payoutMethodType: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutMethodType,
    val payoutMethodAccountName: String,
    val payoutMethodAccountNumber: String,
    val payoutMethodProvider: String?,
    val payoutMethodBranchRouting: String?,
    val status: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequestStatus,
    val reservationHoldId: String?,
    val payoutReference: String,
    val idempotencyKey: String?,
    val rejectionReason: String?,
    val reviewNotes: String?,
    val requestedBy: String,
    val requestedAt: Long,
    val updatedAt: Long,
    val version: Long
) {
    companion object {
        fun fromDomain(domain: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequest): AffiliatePayoutRequestResponseDto = AffiliatePayoutRequestResponseDto(
            requestId = domain.requestId,
            tenantId = domain.tenantId,
            walletId = domain.walletId,
            affiliateId = domain.affiliateId,
            requestedAmount = domain.requestedAmount.amount,
            currency = domain.currency,
            payoutMethodType = domain.payoutMethodType,
            payoutMethodAccountName = domain.payoutMethodAccountName,
            payoutMethodAccountNumber = domain.payoutMethodAccountNumber,
            payoutMethodProvider = domain.payoutMethodProvider,
            payoutMethodBranchRouting = domain.payoutMethodBranchRouting,
            status = domain.status,
            reservationHoldId = domain.reservationHoldId,
            payoutReference = domain.payoutReference,
            idempotencyKey = domain.idempotencyKey,
            rejectionReason = domain.rejectionReason,
            reviewNotes = domain.reviewNotes,
            requestedBy = domain.requestedBy,
            requestedAt = domain.requestedAt,
            updatedAt = domain.updatedAt,
            version = domain.version
        )
    }
}

data class DisbursePayoutRequestDto(
    val notes: String? = null
)

data class AffiliatePayoutDisbursementResponseDto(
    val disbursementId: String,
    val tenantId: String,
    val requestId: String,
    val walletId: String,
    val affiliateId: String,
    val amount: java.math.BigDecimal,
    val currency: String,
    val providerName: String,
    val providerTransactionRef: String?,
    val providerStatus: com.sucharu.sucharupro.domain.model.affiliate.wallet.DisbursementProviderStatus,
    val providerResponseCode: String?,
    val failureReason: String?,
    val ledgerEntryId: String?,
    val processedAt: Long
) {
    companion object {
        fun fromDomain(domain: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutDisbursementRecord): AffiliatePayoutDisbursementResponseDto = AffiliatePayoutDisbursementResponseDto(
            disbursementId = domain.disbursementId,
            tenantId = domain.tenantId,
            requestId = domain.requestId,
            walletId = domain.walletId,
            affiliateId = domain.affiliateId,
            amount = domain.amount.amount,
            currency = domain.currency,
            providerName = domain.providerName,
            providerTransactionRef = domain.providerTransactionRef,
            providerStatus = domain.providerStatus,
            providerResponseCode = domain.providerResponseCode,
            failureReason = domain.failureReason,
            ledgerEntryId = domain.ledgerEntryId,
            processedAt = domain.processedAt
        )
    }
}

data class ReversePayoutRequestDto(
    val reversalReason: String
)

data class ReconcilePayoutRequestDto(
    val notes: String? = null
)

data class AffiliatePayoutReversalResponseDto(
    val reversalId: String,
    val tenantId: String,
    val requestId: String,
    val walletId: String,
    val affiliateId: String,
    val reversedAmount: java.math.BigDecimal,
    val currency: String,
    val originalLedgerEntryId: String?,
    val compensatingLedgerEntryId: String,
    val reversalReason: String,
    val reversedBy: String,
    val reversedAt: Long
) {
    companion object {
        fun fromDomain(domain: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReversalRecord): AffiliatePayoutReversalResponseDto = AffiliatePayoutReversalResponseDto(
            reversalId = domain.reversalId,
            tenantId = domain.tenantId,
            requestId = domain.requestId,
            walletId = domain.walletId,
            affiliateId = domain.affiliateId,
            reversedAmount = domain.reversedAmount.amount,
            currency = domain.currency,
            originalLedgerEntryId = domain.originalLedgerEntryId,
            compensatingLedgerEntryId = domain.compensatingLedgerEntryId,
            reversalReason = domain.reversalReason,
            reversedBy = domain.reversedBy,
            reversedAt = domain.reversedAt
        )
    }
}

data class AffiliatePayoutReconciliationResponseDto(
    val reconciliationId: String,
    val tenantId: String,
    val requestId: String,
    val walletId: String,
    val internalStatus: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutRequestStatus,
    val providerStatus: com.sucharu.sucharupro.domain.model.affiliate.wallet.DisbursementProviderStatus?,
    val providerTransactionRef: String?,
    val ledgerEntryId: String?,
    val reconciliationStatus: com.sucharu.sucharupro.domain.model.affiliate.wallet.PayoutReconciliationStatus,
    val reconciliationNotes: String,
    val reconciledBy: String,
    val reconciledAt: Long
) {
    companion object {
        fun fromDomain(domain: com.sucharu.sucharupro.domain.model.affiliate.wallet.AffiliatePayoutReconciliationRecord): AffiliatePayoutReconciliationResponseDto = AffiliatePayoutReconciliationResponseDto(
            reconciliationId = domain.reconciliationId,
            tenantId = domain.tenantId,
            requestId = domain.requestId,
            walletId = domain.walletId,
            internalStatus = domain.internalStatus,
            providerStatus = domain.providerStatus,
            providerTransactionRef = domain.providerTransactionRef,
            ledgerEntryId = domain.ledgerEntryId,
            reconciliationStatus = domain.reconciliationStatus,
            reconciliationNotes = domain.reconciliationNotes,
            reconciledBy = domain.reconciledBy,
            reconciledAt = domain.reconciledAt
        )
    }
}
