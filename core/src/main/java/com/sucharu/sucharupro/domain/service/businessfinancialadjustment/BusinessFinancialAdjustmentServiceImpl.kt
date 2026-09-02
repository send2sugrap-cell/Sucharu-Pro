package com.sucharu.sucharupro.domain.service.businessfinancialadjustment

import com.sucharu.sucharupro.data.api.model.AuthenticatedPrincipal
import com.sucharu.sucharupro.data.api.model.UserRole
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.AdjustmentFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.RefundFilter
import com.sucharu.sucharupro.data.datasource.businessfinancialadjustment.WriteOffFilter
import com.sucharu.sucharupro.domain.repository.businessexpense.BusinessExpenseRepository
import com.sucharu.sucharupro.data.repository.businessfinancialadjustment.BusinessFinancialAdjustmentRepository
import com.sucharu.sucharupro.data.repository.businessreconciliation.BusinessFinancialReconciliationRepository
import com.sucharu.sucharupro.domain.repository.vendorpayable.VendorPayableRepository
import com.sucharu.sucharupro.domain.model.businesscostcontrol.BusinessFinancialPeriodStatus
import com.sucharu.sucharupro.domain.model.businessfinancialadjustment.*
import com.sucharu.sucharupro.domain.model.businessledger.BusinessLedgerAccountCategory
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.service.businesscostcontrol.BusinessCostControlService
import com.sucharu.sucharupro.domain.service.businessledger.BusinessLedgerService
import com.sucharu.sucharupro.domain.service.businessledger.PostBusinessAdjustmentCommand
import com.sucharu.sucharupro.domain.service.businessledger.ReversePostingCommand
import com.sucharu.sucharupro.domain.validation.businessfinancialadjustment.BusinessFinancialAdjustmentValidators
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

class BusinessFinancialAdjustmentServiceImpl(
    private val repository: BusinessFinancialAdjustmentRepository,
    private val ledgerService: BusinessLedgerService,
    private val costControlService: BusinessCostControlService? = null,
    private val expenseRepository: BusinessExpenseRepository? = null,
    private val payableRepository: VendorPayableRepository? = null,
    private val reconciliationRepository: BusinessFinancialReconciliationRepository? = null,
    private val defaultTenantId: String = "tenant-default"
) : BusinessFinancialAdjustmentService {

    private val mutex = Mutex()

    // =========================================================================
    // 1. FINANCIAL ADJUSTMENT LIFECYCLE
    // =========================================================================

    override suspend fun createAdjustment(
        principal: AuthenticatedPrincipal,
        command: CreateAdjustmentCommand
    ): DomainResult<BusinessFinancialAdjustment> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        // RBAC Check
        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)) {
            return DomainResult.Error(message = "Access denied: User does not have permission to create financial adjustments.")
        }

        // Idempotency Check
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existing = repository.listAdjustments(
                tenantId = tenantId,
                projectId = projectId,
                filter = AdjustmentFilter(sourceType = command.sourceType, sourceId = command.sourceId)
            ).find { it.idempotencyKey == command.idempotencyKey }
            if (existing != null) return DomainResult.Success(existing)
        }

        val adjustmentNumber = command.adjustmentNumber ?: "ADJ-${System.currentTimeMillis().toString().takeLast(6)}"

        val validation = BusinessFinancialAdjustmentValidators.validateAdjustmentCreation(
            tenantId = tenantId,
            projectId = projectId,
            adjustmentNumber = adjustmentNumber,
            adjustmentType = command.adjustmentType,
            sourceType = command.sourceType,
            sourceId = command.sourceId,
            originalAmount = command.originalAmount,
            adjustmentAmount = command.adjustmentAmount,
            currency = command.currency,
            reason = command.reason,
            justification = command.justification,
            periodId = command.periodId,
            createdBy = principal.userId
        )
        if (validation is DomainResult.Error) return validation

        // Period-end check
        val periodValidation = checkPeriodStatus(principal, command.periodId)
        if (periodValidation is DomainResult.Error) return periodValidation

        val effectiveAmount = (command.originalAmount + command.adjustmentAmount).setScale(4, RoundingMode.HALF_UP)
        if (effectiveAmount < BigDecimal.ZERO) {
            return DomainResult.Error(message = "Adjustment results in an invalid negative effective amount ($effectiveAmount).")
        }

        val id = UUID.randomUUID().toString()
        val adjustment = BusinessFinancialAdjustment(
            id = id,
            tenantId = tenantId,
            projectId = projectId,
            adjustmentNumber = adjustmentNumber,
            adjustmentType = command.adjustmentType,
            sourceType = command.sourceType,
            sourceId = command.sourceId,
            originalTransactionId = command.originalTransactionId,
            originalAmount = command.originalAmount.setScale(4, RoundingMode.HALF_UP),
            adjustmentAmount = command.adjustmentAmount.setScale(4, RoundingMode.HALF_UP),
            effectiveAmount = effectiveAmount,
            currency = command.currency,
            reason = command.reason,
            justification = command.justification,
            status = AdjustmentStatus.DRAFT,
            periodId = command.periodId,
            costCenterId = command.costCenterId,
            jobId = command.jobId,
            customerId = command.customerId,
            vendorId = command.vendorId,
            createdBy = principal.userId,
            idempotencyKey = command.idempotencyKey
        )

        val saved = repository.saveAdjustment(adjustment)

        // Append Audit Event
        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ADJUSTMENT",
                entityId = saved.id,
                eventType = "CREATED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = null,
                newStatus = AdjustmentStatus.DRAFT.name,
                reason = command.reason,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun submitAdjustment(
        principal: AuthenticatedPrincipal,
        command: SubmitAdjustmentCommand
    ): DomainResult<BusinessFinancialAdjustment> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val adjustment = repository.findAdjustmentById(command.adjustmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Adjustment '${command.adjustmentId}' not found.")

        val transition = BusinessFinancialAdjustmentValidators.validateAdjustmentStateTransition(
            currentStatus = adjustment.status,
            newStatus = AdjustmentStatus.SUBMITTED
        )
        if (transition is DomainResult.Error) return transition

        val updated = adjustment.copy(
            status = AdjustmentStatus.SUBMITTED,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateAdjustment(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ADJUSTMENT",
                entityId = updated.id,
                eventType = "SUBMITTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = adjustment.status.name,
                newStatus = AdjustmentStatus.SUBMITTED.name,
                reason = command.notes,
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun reviewAdjustment(
        principal: AuthenticatedPrincipal,
        command: ReviewAdjustmentCommand
    ): DomainResult<BusinessFinancialAdjustment> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)) {
            return DomainResult.Error(message = "Access denied: User cannot review adjustments.")
        }

        val adjustment = repository.findAdjustmentById(command.adjustmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Adjustment '${command.adjustmentId}' not found.")

        // Separation of Duties check
        val sod = BusinessFinancialAdjustmentValidators.validateSeparationOfDuties(
            creatorId = adjustment.createdBy,
            actorId = principal.userId,
            actionName = "review"
        )
        if (sod is DomainResult.Error) return sod

        val transition = BusinessFinancialAdjustmentValidators.validateAdjustmentStateTransition(
            currentStatus = adjustment.status,
            newStatus = AdjustmentStatus.UNDER_REVIEW
        )
        if (transition is DomainResult.Error) return transition

        val updated = adjustment.copy(
            status = AdjustmentStatus.UNDER_REVIEW,
            reviewedBy = principal.userId,
            reviewedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.updateAdjustment(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ADJUSTMENT",
                entityId = updated.id,
                eventType = "REVIEW_STARTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = adjustment.status.name,
                newStatus = AdjustmentStatus.UNDER_REVIEW.name,
                reason = command.notes,
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun approveAdjustment(
        principal: AuthenticatedPrincipal,
        command: ApproveAdjustmentCommand
    ): DomainResult<BusinessFinancialAdjustment> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER)) {
            return DomainResult.Error(message = "Access denied: Only ADMIN or MANAGER can approve adjustments.")
        }

        val adjustment = repository.findAdjustmentById(command.adjustmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Adjustment '${command.adjustmentId}' not found.")

        // Separation of Duties check: Creator cannot approve own adjustment
        val sod = BusinessFinancialAdjustmentValidators.validateSeparationOfDuties(
            creatorId = adjustment.createdBy,
            actorId = principal.userId,
            actionName = "approve"
        )
        if (sod is DomainResult.Error) return sod

        val transition = BusinessFinancialAdjustmentValidators.validateAdjustmentStateTransition(
            currentStatus = adjustment.status,
            newStatus = AdjustmentStatus.APPROVED
        )
        if (transition is DomainResult.Error) return transition

        val updated = adjustment.copy(
            status = AdjustmentStatus.APPROVED,
            approvedBy = principal.userId,
            approvedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.updateAdjustment(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ADJUSTMENT",
                entityId = updated.id,
                eventType = "APPROVED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = adjustment.status.name,
                newStatus = AdjustmentStatus.APPROVED.name,
                reason = command.notes,
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun rejectAdjustment(
        principal: AuthenticatedPrincipal,
        command: RejectAdjustmentCommand
    ): DomainResult<BusinessFinancialAdjustment> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER)) {
            return DomainResult.Error(message = "Access denied: Only ADMIN or MANAGER can reject adjustments.")
        }
        if (command.reason.trim().length < 5) {
            return DomainResult.Error(message = "Rejection reason must be at least 5 characters.")
        }

        val adjustment = repository.findAdjustmentById(command.adjustmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Adjustment '${command.adjustmentId}' not found.")

        val transition = BusinessFinancialAdjustmentValidators.validateAdjustmentStateTransition(
            currentStatus = adjustment.status,
            newStatus = AdjustmentStatus.REJECTED
        )
        if (transition is DomainResult.Error) return transition

        val updated = adjustment.copy(
            status = AdjustmentStatus.REJECTED,
            rejectedBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateAdjustment(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ADJUSTMENT",
                entityId = updated.id,
                eventType = "REJECTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = adjustment.status.name,
                newStatus = AdjustmentStatus.REJECTED.name,
                reason = command.reason,
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun cancelAdjustment(
        principal: AuthenticatedPrincipal,
        command: CancelAdjustmentCommand
    ): DomainResult<BusinessFinancialAdjustment> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val adjustment = repository.findAdjustmentById(command.adjustmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Adjustment '${command.adjustmentId}' not found.")

        val transition = BusinessFinancialAdjustmentValidators.validateAdjustmentStateTransition(
            currentStatus = adjustment.status,
            newStatus = AdjustmentStatus.CANCELLED
        )
        if (transition is DomainResult.Error) return transition

        val updated = adjustment.copy(
            status = AdjustmentStatus.CANCELLED,
            cancelledBy = principal.userId,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateAdjustment(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ADJUSTMENT",
                entityId = updated.id,
                eventType = "CANCELLED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = adjustment.status.name,
                newStatus = AdjustmentStatus.CANCELLED.name,
                reason = command.reason,
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun postAdjustment(
        principal: AuthenticatedPrincipal,
        command: PostAdjustmentCommand
    ): DomainResult<BusinessFinancialAdjustment> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)) {
            return DomainResult.Error(message = "Access denied: User does not have permission to post adjustments.")
        }

        val adjustment = repository.findAdjustmentById(command.adjustmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Adjustment '${command.adjustmentId}' not found.")

        val transition = BusinessFinancialAdjustmentValidators.validateAdjustmentStateTransition(
            currentStatus = adjustment.status,
            newStatus = AdjustmentStatus.POSTED
        )
        if (transition is DomainResult.Error) return transition

        // Period-end check
        val periodValidation = checkPeriodStatus(principal, adjustment.periodId)
        if (periodValidation is DomainResult.Error) return periodValidation

        // Ledger Recognition: Call BusinessLedgerService to post compensating entry
        val isDebit = adjustment.adjustmentAmount < BigDecimal.ZERO
        val absAmount = adjustment.adjustmentAmount.abs()

        val ledgerResult = ledgerService.postBusinessAdjustment(
            principal = principal,
            command = PostBusinessAdjustmentCommand(
                amount = absAmount,
                isDebit = isDebit,
                accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
                description = "Compensating Adjustment (${adjustment.adjustmentType.name}) for ${adjustment.sourceType.name} #${adjustment.sourceId}: ${adjustment.reason}",
                reference = adjustment.adjustmentNumber,
                jobId = adjustment.jobId,
                vendorId = adjustment.vendorId,
                currency = adjustment.currency,
                idempotencyKey = command.idempotencyKey ?: "POST-ADJ-${adjustment.id}",
                correlationId = command.correlationId
            )
        )

        val ledgerPostingId = when (ledgerResult) {
            is DomainResult.Success -> ledgerResult.data.id
            is DomainResult.Error -> return DomainResult.Error(message = "Ledger posting failed: ${ledgerResult.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Ledger posting in progress.")
        }

        // Save Compensating Posting record
        val posting = BusinessFinancialAdjustmentPosting(
            id = UUID.randomUUID().toString(),
            tenantId = tenantId,
            projectId = projectId,
            adjustmentId = adjustment.id,
            postingNumber = "PST-ADJ-${System.currentTimeMillis().toString().takeLast(6)}",
            ledgerPostingId = ledgerPostingId,
            postingType = if (isDebit) AdjustmentPostingType.DEBIT_COMPENSATING else AdjustmentPostingType.CREDIT_COMPENSATING,
            debitAccount = command.debitAccount ?: "ADJUSTMENT_EXPENSE",
            creditAccount = command.creditAccount ?: "CANONICAL_CLEARING",
            amount = absAmount,
            currency = adjustment.currency,
            postedBy = principal.userId,
            postedAt = System.currentTimeMillis(),
            idempotencyKey = command.idempotencyKey
        )
        repository.savePosting(posting)

        val updated = adjustment.copy(
            status = AdjustmentStatus.POSTED,
            postedBy = principal.userId,
            postedAt = System.currentTimeMillis(),
            ledgerPostingId = ledgerPostingId,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateAdjustment(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ADJUSTMENT",
                entityId = updated.id,
                eventType = "POSTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = adjustment.status.name,
                newStatus = AdjustmentStatus.POSTED.name,
                reason = "Compensating ledger entry #$ledgerPostingId posted.",
                correlationId = command.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun reverseAdjustment(
        principal: AuthenticatedPrincipal,
        command: ReverseAdjustmentCommand
    ): DomainResult<BusinessFinancialAdjustment> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER)) {
            return DomainResult.Error(message = "Access denied: Only ADMIN or MANAGER can reverse adjustments.")
        }

        val adjustment = repository.findAdjustmentById(command.adjustmentId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Adjustment '${command.adjustmentId}' not found.")

        val reversalValidation = BusinessFinancialAdjustmentValidators.validateReversalRequest(
            adjustment = adjustment,
            reversalReason = command.reason,
            requestedBy = principal.userId
        )
        if (reversalValidation is DomainResult.Error) return reversalValidation

        // Period-end check
        val periodValidation = checkPeriodStatus(principal, adjustment.periodId)
        if (periodValidation is DomainResult.Error) return periodValidation

        // If a ledger posting exists, reverse it through BusinessLedgerService
        var reversingPostingId: String? = null
        if (!adjustment.ledgerPostingId.isNullOrBlank()) {
            val revResult = ledgerService.reversePosting(
                principal = principal,
                command = ReversePostingCommand(
                    postingId = adjustment.ledgerPostingId,
                    reason = "Reversal of Adjustment ${adjustment.adjustmentNumber}: ${command.reason}",
                    correlationId = command.correlationId
                )
            )
            reversingPostingId = when (revResult) {
                is DomainResult.Success -> revResult.data.id
                is DomainResult.Error -> return DomainResult.Error(message = "Reversal posting failed: ${revResult.message}")
                is DomainResult.Loading -> return DomainResult.Error(message = "Reversal posting in progress.")
            }
        }

        val updated = adjustment.copy(
            status = AdjustmentStatus.REVERSED,
            reversalRequestedBy = principal.userId,
            reversalApprovedBy = principal.userId,
            reversalRequestedAt = System.currentTimeMillis(),
            reversalApprovedAt = System.currentTimeMillis(),
            reversedAt = System.currentTimeMillis(),
            reversingPostingId = reversingPostingId,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateAdjustment(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "ADJUSTMENT",
                entityId = updated.id,
                eventType = "REVERSED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = adjustment.status.name,
                newStatus = AdjustmentStatus.REVERSED.name,
                reason = command.reason,
                correlationId = command.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun getAdjustmentById(
        principal: AuthenticatedPrincipal,
        id: String
    ): DomainResult<BusinessFinancialAdjustment> {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val adjustment = repository.findAdjustmentById(id, tenantId, projectId)
            ?: return DomainResult.Error(message = "Adjustment '$id' not found.")

        return DomainResult.Success(adjustment)
    }

    override suspend fun listAdjustments(
        principal: AuthenticatedPrincipal,
        filter: AdjustmentFilter
    ): DomainResult<List<BusinessFinancialAdjustment>> {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val list = repository.listAdjustments(tenantId, projectId, filter)
        return DomainResult.Success(list)
    }

    // =========================================================================
    // 2. REFUND WORKFLOW
    // =========================================================================

    override suspend fun createRefund(
        principal: AuthenticatedPrincipal,
        command: CreateRefundCommand
    ): DomainResult<BusinessFinancialRefund> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        // Idempotency Check
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existing = repository.listRefunds(
                tenantId = tenantId,
                projectId = projectId,
                filter = RefundFilter(sourceType = command.sourceType, sourceId = command.sourceId)
            ).find { it.idempotencyKey == command.idempotencyKey }
            if (existing != null) return DomainResult.Success(existing)
        }

        val refundNumber = command.refundNumber ?: "REF-${System.currentTimeMillis().toString().takeLast(6)}"

        val validation = BusinessFinancialAdjustmentValidators.validateRefundCreation(
            tenantId = tenantId,
            projectId = projectId,
            refundNumber = refundNumber,
            sourceId = command.sourceId,
            eligibleBalance = command.eligibleBalance,
            requestedAmount = command.requestedAmount,
            currency = command.currency,
            refundReason = command.refundReason,
            periodId = command.periodId,
            requestedBy = principal.userId
        )
        if (validation is DomainResult.Error) return validation

        val id = UUID.randomUUID().toString()
        val refund = BusinessFinancialRefund(
            id = id,
            tenantId = tenantId,
            projectId = projectId,
            refundNumber = refundNumber,
            sourceType = command.sourceType,
            sourceId = command.sourceId,
            customerId = command.customerId,
            vendorId = command.vendorId,
            originalTransactionId = command.originalTransactionId,
            eligibleBalance = command.eligibleBalance.setScale(4, RoundingMode.HALF_UP),
            requestedAmount = command.requestedAmount.setScale(4, RoundingMode.HALF_UP),
            approvedAmount = BigDecimal.ZERO,
            currency = command.currency,
            refundReason = command.refundReason,
            paymentMethod = command.paymentMethod,
            status = RefundStatus.REQUESTED,
            periodId = command.periodId,
            requestedBy = principal.userId,
            idempotencyKey = command.idempotencyKey
        )

        val saved = repository.saveRefund(refund)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "REFUND",
                entityId = saved.id,
                eventType = "REFUND_REQUESTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = null,
                newStatus = RefundStatus.REQUESTED.name,
                reason = command.refundReason,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun approveRefund(
        principal: AuthenticatedPrincipal,
        command: ApproveRefundCommand
    ): DomainResult<BusinessFinancialRefund> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER)) {
            return DomainResult.Error(message = "Access denied: Only ADMIN or MANAGER can approve refunds.")
        }

        val refund = repository.findRefundById(command.refundId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Refund '${command.refundId}' not found.")

        if (!refund.status.canBeApproved) {
            return DomainResult.Error(message = "Cannot approve refund in status '${refund.status}'.")
        }

        // Separation of Duties check: Requester cannot approve own refund
        val sod = BusinessFinancialAdjustmentValidators.validateSeparationOfDuties(
            creatorId = refund.requestedBy,
            actorId = principal.userId,
            actionName = "approve"
        )
        if (sod is DomainResult.Error) return sod

        val approvedAmount = (command.approvedAmount ?: refund.requestedAmount).setScale(4, RoundingMode.HALF_UP)
        if (approvedAmount <= BigDecimal.ZERO || (refund.eligibleBalance > BigDecimal.ZERO && approvedAmount > refund.eligibleBalance)) {
            return DomainResult.Error(message = "Invalid approved refund amount ($approvedAmount).")
        }

        val updated = refund.copy(
            approvedAmount = approvedAmount,
            status = RefundStatus.APPROVED,
            approvedBy = principal.userId,
            approvedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.updateRefund(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "REFUND",
                entityId = updated.id,
                eventType = "REFUND_APPROVED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = refund.status.name,
                newStatus = RefundStatus.APPROVED.name,
                reason = command.notes,
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun postRefund(
        principal: AuthenticatedPrincipal,
        command: PostRefundCommand
    ): DomainResult<BusinessFinancialRefund> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)) {
            return DomainResult.Error(message = "Access denied: User does not have permission to post refunds.")
        }

        val refund = repository.findRefundById(command.refundId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Refund '${command.refundId}' not found.")

        if (!refund.status.canBePosted) {
            return DomainResult.Error(message = "Cannot post refund in status '${refund.status}'. Must be APPROVED.")
        }

        // Period-end check
        val periodValidation = checkPeriodStatus(principal, refund.periodId)
        if (periodValidation is DomainResult.Error) return periodValidation

        // Post in Business Ledger
        val ledgerResult = ledgerService.postBusinessAdjustment(
            principal = principal,
            command = PostBusinessAdjustmentCommand(
                amount = refund.approvedAmount,
                isDebit = true,
                accountCategory = BusinessLedgerAccountCategory.CASH,
                description = "Customer Refund Recognition #${refund.refundNumber}: ${refund.refundReason}",
                reference = refund.refundNumber,
                currency = refund.currency,
                idempotencyKey = command.idempotencyKey ?: "POST-REF-${refund.id}",
                correlationId = command.correlationId
            )
        )

        val ledgerPostingId = when (ledgerResult) {
            is DomainResult.Success -> ledgerResult.data.id
            is DomainResult.Error -> return DomainResult.Error(message = "Ledger posting failed: ${ledgerResult.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Ledger posting in progress.")
        }

        val updated = refund.copy(
            status = RefundStatus.POSTED,
            postedBy = principal.userId,
            postedAt = System.currentTimeMillis(),
            ledgerPostingId = ledgerPostingId,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateRefund(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "REFUND",
                entityId = updated.id,
                eventType = "REFUND_POSTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = refund.status.name,
                newStatus = RefundStatus.POSTED.name,
                reason = "Ledger posting #$ledgerPostingId created for refund.",
                correlationId = command.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun getRefundById(
        principal: AuthenticatedPrincipal,
        id: String
    ): DomainResult<BusinessFinancialRefund> {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val refund = repository.findRefundById(id, tenantId, projectId)
            ?: return DomainResult.Error(message = "Refund '$id' not found.")

        return DomainResult.Success(refund)
    }

    override suspend fun listRefunds(
        principal: AuthenticatedPrincipal,
        filter: RefundFilter
    ): DomainResult<List<BusinessFinancialRefund>> {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val list = repository.listRefunds(tenantId, projectId, filter)
        return DomainResult.Success(list)
    }

    // =========================================================================
    // 3. WRITE-OFF WORKFLOW
    // =========================================================================

    override suspend fun createWriteOff(
        principal: AuthenticatedPrincipal,
        command: CreateWriteOffCommand
    ): DomainResult<BusinessFinancialWriteOff> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)) {
            return DomainResult.Error(message = "Access denied: User does not have permission to create write-offs.")
        }

        // Idempotency Check
        if (!command.idempotencyKey.isNullOrBlank()) {
            val existing = repository.listWriteOffs(
                tenantId = tenantId,
                projectId = projectId,
                filter = WriteOffFilter(sourceType = command.sourceType, sourceId = command.sourceId)
            ).find { it.idempotencyKey == command.idempotencyKey }
            if (existing != null) return DomainResult.Success(existing)
        }

        val writeOffNumber = command.writeOffNumber ?: "WO-${System.currentTimeMillis().toString().takeLast(6)}"

        val validation = BusinessFinancialAdjustmentValidators.validateWriteOffCreation(
            tenantId = tenantId,
            projectId = projectId,
            writeOffNumber = writeOffNumber,
            sourceId = command.sourceId,
            eligibleBalance = command.eligibleBalance,
            amount = command.amount,
            currency = command.currency,
            reason = command.reason,
            justification = command.justification,
            periodId = command.periodId,
            requestedBy = principal.userId
        )
        if (validation is DomainResult.Error) return validation

        val id = UUID.randomUUID().toString()
        val writeOff = BusinessFinancialWriteOff(
            id = id,
            tenantId = tenantId,
            projectId = projectId,
            writeOffNumber = writeOffNumber,
            sourceType = command.sourceType,
            sourceId = command.sourceId,
            writeOffType = command.writeOffType,
            eligibleBalance = command.eligibleBalance.setScale(4, RoundingMode.HALF_UP),
            amount = command.amount.setScale(4, RoundingMode.HALF_UP),
            currency = command.currency,
            reason = command.reason,
            justification = command.justification,
            status = WriteOffStatus.REQUESTED,
            periodId = command.periodId,
            customerId = command.customerId,
            vendorId = command.vendorId,
            requestedBy = principal.userId,
            idempotencyKey = command.idempotencyKey
        )

        val saved = repository.saveWriteOff(writeOff)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "WRITE_OFF",
                entityId = saved.id,
                eventType = "WRITE_OFF_REQUESTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = null,
                newStatus = WriteOffStatus.REQUESTED.name,
                reason = command.reason,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(saved)
    }

    override suspend fun approveWriteOff(
        principal: AuthenticatedPrincipal,
        command: ApproveWriteOffCommand
    ): DomainResult<BusinessFinancialWriteOff> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER)) {
            return DomainResult.Error(message = "Access denied: Only ADMIN or MANAGER can approve write-offs.")
        }

        val writeOff = repository.findWriteOffById(command.writeOffId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Write-off '${command.writeOffId}' not found.")

        if (!writeOff.status.canBeApproved) {
            return DomainResult.Error(message = "Cannot approve write-off in status '${writeOff.status}'.")
        }

        // Separation of Duties check: Creator cannot approve own write-off
        val sod = BusinessFinancialAdjustmentValidators.validateSeparationOfDuties(
            creatorId = writeOff.requestedBy,
            actorId = principal.userId,
            actionName = "approve"
        )
        if (sod is DomainResult.Error) return sod

        val updated = writeOff.copy(
            status = WriteOffStatus.APPROVED,
            approvedBy = principal.userId,
            approvedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.updateWriteOff(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "WRITE_OFF",
                entityId = updated.id,
                eventType = "WRITE_OFF_APPROVED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = writeOff.status.name,
                newStatus = WriteOffStatus.APPROVED.name,
                reason = command.notes,
                correlationId = command.correlationId
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun postWriteOff(
        principal: AuthenticatedPrincipal,
        command: PostWriteOffCommand
    ): DomainResult<BusinessFinancialWriteOff> = mutex.withLock {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER, UserRole.STAFF)) {
            return DomainResult.Error(message = "Access denied: User does not have permission to post write-offs.")
        }

        val writeOff = repository.findWriteOffById(command.writeOffId, tenantId, projectId)
            ?: return DomainResult.Error(message = "Write-off '${command.writeOffId}' not found.")

        if (!writeOff.status.canBePosted) {
            return DomainResult.Error(message = "Cannot post write-off in status '${writeOff.status}'. Must be APPROVED.")
        }

        // Period-end check
        val periodValidation = checkPeriodStatus(principal, writeOff.periodId)
        if (periodValidation is DomainResult.Error) return periodValidation

        // Post in Business Ledger
        val ledgerResult = ledgerService.postBusinessAdjustment(
            principal = principal,
            command = PostBusinessAdjustmentCommand(
                amount = writeOff.amount,
                isDebit = true,
                accountCategory = BusinessLedgerAccountCategory.OPERATING_EXPENSE,
                description = "Write-Off Recognition (${writeOff.writeOffType.name}) for ${writeOff.sourceType.name} #${writeOff.sourceId}: ${writeOff.reason}",
                reference = writeOff.writeOffNumber,
                vendorId = writeOff.vendorId,
                currency = writeOff.currency,
                idempotencyKey = command.idempotencyKey ?: "POST-WO-${writeOff.id}",
                correlationId = command.correlationId
            )
        )

        val ledgerPostingId = when (ledgerResult) {
            is DomainResult.Success -> ledgerResult.data.id
            is DomainResult.Error -> return DomainResult.Error(message = "Ledger posting failed: ${ledgerResult.message}")
            is DomainResult.Loading -> return DomainResult.Error(message = "Ledger posting in progress.")
        }

        val updated = writeOff.copy(
            status = WriteOffStatus.POSTED,
            postedBy = principal.userId,
            postedAt = System.currentTimeMillis(),
            ledgerPostingId = ledgerPostingId,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateWriteOff(updated)

        repository.recordAuditEvent(
            BusinessFinancialAdjustmentAuditEvent(
                id = UUID.randomUUID().toString(),
                tenantId = tenantId,
                projectId = projectId,
                entityType = "WRITE_OFF",
                entityId = updated.id,
                eventType = "WRITE_OFF_POSTED",
                actorId = principal.userId,
                actorRole = principal.role.name,
                previousStatus = writeOff.status.name,
                newStatus = WriteOffStatus.POSTED.name,
                reason = "Ledger posting #$ledgerPostingId created for write-off.",
                correlationId = command.correlationId,
                idempotencyKey = command.idempotencyKey
            )
        )

        return DomainResult.Success(updated)
    }

    override suspend fun getWriteOffById(
        principal: AuthenticatedPrincipal,
        id: String
    ): DomainResult<BusinessFinancialWriteOff> {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val writeOff = repository.findWriteOffById(id, tenantId, projectId)
            ?: return DomainResult.Error(message = "Write-off '$id' not found.")

        return DomainResult.Success(writeOff)
    }

    override suspend fun listWriteOffs(
        principal: AuthenticatedPrincipal,
        filter: WriteOffFilter
    ): DomainResult<List<BusinessFinancialWriteOff>> {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val list = repository.listWriteOffs(tenantId, projectId, filter)
        return DomainResult.Success(list)
    }

    // =========================================================================
    // 4. ANALYTICS, EXCEPTIONS & AUDIT
    // =========================================================================

    override suspend fun getSummary(
        principal: AuthenticatedPrincipal,
        periodId: String?
    ): DomainResult<BusinessFinancialAdjustmentSummary> {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val adjustments = repository.listAdjustments(tenantId, projectId, AdjustmentFilter(periodId = periodId))
        val refunds = repository.listRefunds(tenantId, projectId, RefundFilter(periodId = periodId))
        val writeOffs = repository.listWriteOffs(tenantId, projectId, WriteOffFilter(periodId = periodId))

        var totalAdjusted = BigDecimal.ZERO
        var totalRefunded = BigDecimal.ZERO
        var totalWrittenOff = BigDecimal.ZERO
        var totalReversed = BigDecimal.ZERO
        var pendingAmount = BigDecimal.ZERO
        var postedAmount = BigDecimal.ZERO

        adjustments.forEach { adj ->
            when (adj.status) {
                AdjustmentStatus.POSTED, AdjustmentStatus.RECONCILED -> {
                    totalAdjusted += adj.adjustmentAmount.abs()
                    postedAmount += adj.adjustmentAmount.abs()
                }
                AdjustmentStatus.REVERSED -> {
                    totalReversed += adj.adjustmentAmount.abs()
                }
                AdjustmentStatus.DRAFT, AdjustmentStatus.SUBMITTED, AdjustmentStatus.UNDER_REVIEW, AdjustmentStatus.APPROVED -> {
                    pendingAmount += adj.adjustmentAmount.abs()
                }
                else -> {}
            }
        }

        refunds.forEach { ref ->
            if (ref.status in setOf(RefundStatus.POSTED, RefundStatus.SETTLED, RefundStatus.RECONCILED)) {
                totalRefunded += ref.approvedAmount
            }
        }

        writeOffs.forEach { wo ->
            if (wo.status in setOf(WriteOffStatus.POSTED, WriteOffStatus.RECONCILED)) {
                totalWrittenOff += wo.amount
            }
        }

        val summary = BusinessFinancialAdjustmentSummary(
            tenantId = tenantId,
            projectId = projectId,
            periodId = periodId,
            totalAdjustmentsCount = adjustments.size,
            pendingAdjustmentsCount = adjustments.count { it.status in setOf(AdjustmentStatus.DRAFT, AdjustmentStatus.SUBMITTED, AdjustmentStatus.UNDER_REVIEW, AdjustmentStatus.APPROVED) },
            approvedAdjustmentsCount = adjustments.count { it.status == AdjustmentStatus.APPROVED },
            postedAdjustmentsCount = adjustments.count { it.status == AdjustmentStatus.POSTED },
            totalAdjustedAmount = totalAdjusted.setScale(4, RoundingMode.HALF_UP),
            totalRefundedAmount = totalRefunded.setScale(4, RoundingMode.HALF_UP),
            totalWrittenOffAmount = totalWrittenOff.setScale(4, RoundingMode.HALF_UP),
            totalReversedAmount = totalReversed.setScale(4, RoundingMode.HALF_UP),
            pendingApprovalAmount = pendingAmount.setScale(4, RoundingMode.HALF_UP),
            postedAmount = postedAmount.setScale(4, RoundingMode.HALF_UP),
            unresolvedExceptionsCount = adjustments.count { it.status == AdjustmentStatus.UNDER_REVIEW } + writeOffs.count { it.status == WriteOffStatus.UNDER_REVIEW }
        )

        return DomainResult.Success(summary)
    }

    override suspend fun listExceptions(
        principal: AuthenticatedPrincipal
    ): DomainResult<List<BusinessFinancialException>> {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val adjustments = repository.listAdjustments(tenantId, projectId)
        val refunds = repository.listRefunds(tenantId, projectId)
        val writeOffs = repository.listWriteOffs(tenantId, projectId)

        val exceptions = mutableListOf<BusinessFinancialException>()

        // 1. Pending adjustments under review for too long
        adjustments.filter { it.status == AdjustmentStatus.UNDER_REVIEW }.forEach { adj ->
            exceptions.add(
                BusinessFinancialException(
                    id = UUID.randomUUID().toString(),
                    entityType = "ADJUSTMENT",
                    entityId = adj.id,
                    referenceNumber = adj.adjustmentNumber,
                    issueType = "ADJUSTMENT_UNDER_REVIEW",
                    severity = "MEDIUM",
                    description = "Adjustment ${adj.adjustmentNumber} requires manager approval.",
                    amount = adj.adjustmentAmount.abs(),
                    status = adj.status.name
                )
            )
        }

        // 2. High value refunds
        refunds.filter { it.status == RefundStatus.REQUESTED && it.requestedAmount > BigDecimal("50000.00") }.forEach { ref ->
            exceptions.add(
                BusinessFinancialException(
                    id = UUID.randomUUID().toString(),
                    entityType = "REFUND",
                    entityId = ref.id,
                    referenceNumber = ref.refundNumber,
                    issueType = "HIGH_VALUE_REFUND",
                    severity = "HIGH",
                    description = "High value refund request exceeding BDT 50,000.",
                    amount = ref.requestedAmount,
                    status = ref.status.name
                )
            )
        }

        // 3. Write-offs pending approval
        writeOffs.filter { it.status == WriteOffStatus.REQUESTED || it.status == WriteOffStatus.UNDER_REVIEW }.forEach { wo ->
            exceptions.add(
                BusinessFinancialException(
                    id = UUID.randomUUID().toString(),
                    entityType = "WRITE_OFF",
                    entityId = wo.id,
                    referenceNumber = wo.writeOffNumber,
                    issueType = "UNAPPROVED_WRITE_OFF",
                    severity = "HIGH",
                    description = "Write-off of type ${wo.writeOffType.name} pending authorization.",
                    amount = wo.amount,
                    status = wo.status.name
                )
            )
        }

        return DomainResult.Success(exceptions)
    }

    override suspend fun listAuditEvents(
        principal: AuthenticatedPrincipal,
        entityId: String?,
        entityType: String?
    ): DomainResult<List<BusinessFinancialAdjustmentAuditEvent>> {
        val tenantId = defaultTenantId
        val projectId = principal.projectId

        val events = repository.listAuditEvents(tenantId, projectId, entityId, entityType)
        return DomainResult.Success(events)
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private suspend fun checkPeriodStatus(
        principal: AuthenticatedPrincipal,
        periodId: String
    ): DomainResult<Unit> {
        if (costControlService == null) return DomainResult.Success(Unit)

        val periodResult = costControlService.getFinancialPeriodById(principal, periodId)
        if (periodResult is DomainResult.Success) {
            val period = periodResult.data
            if (period.status == BusinessFinancialPeriodStatus.CLOSED) {
                return DomainResult.Error(
                    message = "Financial period '${period.periodCode}' is hard-closed. Direct financial posting is prohibited. Post compensating entry in the current open period."
                )
            }
            if (period.status == BusinessFinancialPeriodStatus.SOFT_CLOSED) {
                if (!hasRole(principal, UserRole.ADMIN, UserRole.MANAGER)) {
                    return DomainResult.Error(
                        message = "Financial period '${period.periodCode}' is soft-closed. Elevated ADMIN or MANAGER authorization is required."
                    )
                }
            }
        }
        return DomainResult.Success(Unit)
    }

    private fun hasRole(principal: AuthenticatedPrincipal, vararg allowedRoles: UserRole): Boolean {
        return principal.role in allowedRoles
    }
}
