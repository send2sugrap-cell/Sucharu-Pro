package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryItemVerificationDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryOrderDataSource
import com.sucharu.sucharupro.data.datasource.DispatchExecutionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerification
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationActivityType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationIssueType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationLine
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationResultType
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationStatus
import com.sucharu.sucharupro.domain.model.delivery.verification.DeliveryItemVerificationSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryItemVerificationRepository
import com.sucharu.sucharupro.domain.service.DeliveryItemVerificationClassificationService
import com.sucharu.sucharupro.domain.service.DeliveryItemVerificationReconciliationService
import com.sucharu.sucharupro.domain.validation.DeliveryItemVerificationAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.DeliveryItemVerificationLifecycleValidator
import com.sucharu.sucharupro.domain.validation.DeliveryItemVerificationLineValidator
import com.sucharu.sucharupro.domain.validation.DeliveryItemVerificationOperation
import com.sucharu.sucharupro.domain.validation.DeliveryItemVerificationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production implementation of [DeliveryItemVerificationRepository] (Module 08 Step 04).
 */
class DeliveryItemVerificationRepositoryImpl(
    private val verificationDataSource: DeliveryItemVerificationDataSource,
    private val dispatchDataSource: DispatchExecutionDataSource,
    private val challanDataSource: DeliveryChallanDataSource? = null,
    private val deliveryOrderDataSource: DeliveryOrderDataSource? = null
) : DeliveryItemVerificationRepository {

    private val mutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeVerifications(projectId: String): Flow<List<DeliveryItemVerification>> {
        return verificationDataSource.observeVerifications(projectId)
    }

    override fun observeVerificationsForDispatch(dispatchExecutionId: String): Flow<List<DeliveryItemVerification>> {
        return verificationDataSource.observeVerificationsForDispatch(dispatchExecutionId)
    }

    override fun observeVerification(verificationId: String): Flow<DeliveryItemVerification?> {
        return verificationDataSource.observeVerification(verificationId)
    }

    override suspend fun getVerification(
        verificationId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryItemVerification> {
        val verification = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.VIEW,
                targetProjectId = verification.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(verification)
    }

    override suspend fun getVerificationsForDispatch(
        dispatchExecutionId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryItemVerification>> {
        val dispatch = dispatchDataSource.getDispatch(dispatchExecutionId)
            ?: return DomainResult.Error(message = "Dispatch execution '$dispatchExecutionId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.VIEW,
                targetProjectId = dispatch.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val verifications = verificationDataSource.getVerificationsForDispatch(dispatchExecutionId)
        return DomainResult.Success(verifications)
    }

    override fun observeVerificationLines(verificationId: String): Flow<List<DeliveryItemVerificationLine>> {
        return verificationDataSource.observeVerificationLines(verificationId)
    }

    override suspend fun getVerificationLines(
        verificationId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryItemVerificationLine>> {
        val verification = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.VIEW,
                targetProjectId = verification.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lines = verificationDataSource.getVerificationLines(verificationId)
        return DomainResult.Success(lines)
    }

    override fun observeActivityEvents(verificationId: String): Flow<List<DeliveryItemVerificationActivityEvent>> {
        return verificationDataSource.observeActivityEvents(verificationId)
    }

    override suspend fun getActivityEvents(
        verificationId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryItemVerificationActivityEvent>> {
        val verification = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.VIEW,
                targetProjectId = verification.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val events = verificationDataSource.getActivityEvents(verificationId)
        return DomainResult.Success(events)
    }

    override suspend fun getVerificationSummary(
        verificationId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryItemVerificationSummary> {
        val verification = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.VIEW,
                targetProjectId = verification.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lines = verificationDataSource.getVerificationLines(verificationId)
        val summary = DeliveryItemVerificationReconciliationService.calculateSummary(verification, lines)
        return DomainResult.Success(summary)
    }

    // ──────────────────────────────────────────────────────────────
    // Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun createVerification(
        verification: DeliveryItemVerification,
        lines: List<DeliveryItemVerificationLine>,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryItemVerification> = mutex.withLock {
        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.CREATE,
                targetProjectId = verification.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Structural validation
        val validationResult = DeliveryItemVerificationValidator.validateVerification(verification, lines)
        if (validationResult is DomainResult.Error) return validationResult

        // 3. Unique verification number check
        val existingNo = verificationDataSource.getVerificationByNo(verification.projectId, verification.verificationNo)
        if (existingNo != null) {
            return DomainResult.Error(
                message = "Verification number '${verification.verificationNo}' already exists in project '${verification.projectId}'."
            )
        }

        // 4. Duplicate check for active/completed verification for the same dispatch
        val existingForDispatch = verificationDataSource.getVerificationsForDispatch(verification.dispatchExecutionId)
        val activeDuplicate = existingForDispatch.firstOrNull { it.status != DeliveryItemVerificationStatus.CANCELLED }
        if (activeDuplicate != null) {
            return DomainResult.Error(
                message = "An active verification '${activeDuplicate.verificationNo}' already exists for dispatch '${verification.dispatchExecutionId}'."
            )
        }

        // 5. Dispatch eligibility check
        val dispatch = dispatchDataSource.getDispatch(verification.dispatchExecutionId)
            ?: return DomainResult.Error(message = "Referenced Dispatch Execution '${verification.dispatchExecutionId}' not found.")

        val eligibilityCheck = DeliveryItemVerificationValidator.validateDispatchEligibility(dispatch, verification.projectId)
        if (eligibilityCheck is DomainResult.Error) return eligibilityCheck

        // 6. Lines against dispatch lines validation
        val dispatchLines = dispatchDataSource.getDispatchLines(verification.dispatchExecutionId)
        val linesMatchCheck = DeliveryItemVerificationValidator.validateLinesAgainstDispatch(dispatchLines, lines)
        if (linesMatchCheck is DomainResult.Error) return linesMatchCheck

        // 7. Insert
        verificationDataSource.insertVerification(verification, lines)

        // 8. Audit
        val activity = DeliveryItemVerificationActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = verification.projectId,
            verificationId = verification.verificationId,
            activityType = DeliveryItemVerificationActivityType.CREATED,
            performedBy = verification.createdBy,
            performedAt = verification.createdAt,
            newStatus = verification.status.name,
            details = "Verification initialized for dispatch '${dispatch.dispatchNo}' with ${lines.size} line(s)."
        )
        verificationDataSource.insertActivityEvent(activity)

        DomainResult.Success(verification)
    }

    override suspend fun updateDraftVerification(
        verificationId: String,
        remarks: String?,
        lines: List<DeliveryItemVerificationLine>,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryItemVerification> = mutex.withLock {
        val existing = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.EDIT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (existing.status != DeliveryItemVerificationStatus.DRAFT) {
            return DomainResult.Error(
                message = "Only DRAFT delivery verifications can be updated. Current status: '${existing.status}'."
            )
        }

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            remarks = remarks,
            updatedBy = actorId,
            updatedAt = now
        )

        val validationResult = DeliveryItemVerificationValidator.validateVerification(updated, lines)
        if (validationResult is DomainResult.Error) return validationResult

        val immutabilityCheck = DeliveryItemVerificationValidator.validateImmutableIdentity(existing, updated)
        if (immutabilityCheck is DomainResult.Error) return immutabilityCheck

        val dispatchLines = dispatchDataSource.getDispatchLines(existing.dispatchExecutionId)
        val linesMatchCheck = DeliveryItemVerificationValidator.validateLinesAgainstDispatch(dispatchLines, lines)
        if (linesMatchCheck is DomainResult.Error) return linesMatchCheck

        verificationDataSource.updateVerificationWithLines(updated, lines)

        val activity = DeliveryItemVerificationActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            verificationId = existing.verificationId,
            activityType = DeliveryItemVerificationActivityType.UPDATED,
            performedBy = actorId,
            performedAt = now,
            details = "Draft verification updated with ${lines.size} line(s)."
        )
        verificationDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun submitVerification(
        verificationId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryItemVerification> = mutex.withLock {
        val existing = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.SUBMIT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryItemVerificationLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryItemVerificationStatus.PENDING
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryItemVerificationStatus.PENDING,
            updatedBy = actorId,
            updatedAt = now
        )
        verificationDataSource.updateVerification(updated)

        val activity = DeliveryItemVerificationActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            verificationId = existing.verificationId,
            activityType = DeliveryItemVerificationActivityType.SUBMITTED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryItemVerificationStatus.PENDING.name,
            details = "Verification submitted for warehouse execution."
        )
        verificationDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun startVerification(
        verificationId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryItemVerification> = mutex.withLock {
        val existing = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.START_VERIFICATION,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryItemVerificationLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryItemVerificationStatus.IN_PROGRESS
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryItemVerificationStatus.IN_PROGRESS,
            updatedBy = actorId,
            updatedAt = now
        )
        verificationDataSource.updateVerification(updated)

        val activity = DeliveryItemVerificationActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            verificationId = existing.verificationId,
            activityType = DeliveryItemVerificationActivityType.STARTED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryItemVerificationStatus.IN_PROGRESS.name,
            details = "Physical delivery item verification started."
        )
        verificationDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun verifyLine(
        verificationId: String,
        verificationLineId: String,
        verifiedQuantity: Double,
        isDamaged: Boolean,
        damagedQuantity: Double,
        isMissing: Boolean,
        isProductMismatch: Boolean,
        isBatchMismatch: Boolean,
        isLotMismatch: Boolean,
        remarks: String?,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryItemVerificationLine> = mutex.withLock {
        val verification = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.VERIFY_LINE,
                targetProjectId = verification.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        if (verification.status != DeliveryItemVerificationStatus.IN_PROGRESS) {
            return DomainResult.Error(
                message = "Lines can only be verified when verification status is IN_PROGRESS. Current: '${verification.status}'."
            )
        }

        val lines = verificationDataSource.getVerificationLines(verificationId)
        val targetLine = lines.find { it.verificationLineId == verificationLineId }
            ?: return DomainResult.Error(message = "Verification line '$verificationLineId' not found.")

        val classification = DeliveryItemVerificationClassificationService.classifyLine(
            expectedQuantity = targetLine.expectedQuantity,
            verifiedQuantity = verifiedQuantity,
            isDamaged = isDamaged,
            damagedQuantity = damagedQuantity,
            isMissing = isMissing,
            isProductMismatch = isProductMismatch,
            isBatchMismatch = isBatchMismatch,
            isLotMismatch = isLotMismatch
        )

        val updatedLine = targetLine.copy(
            verifiedQuantity = verifiedQuantity,
            issueQuantity = classification.issueQuantity,
            resultType = classification.resultType,
            issueType = classification.issueType,
            remarks = remarks ?: targetLine.remarks
        )

        val lineValidation = DeliveryItemVerificationLineValidator.validateLine(updatedLine)
        if (lineValidation is DomainResult.Error) return lineValidation

        verificationDataSource.updateVerificationLine(updatedLine)

        val activityType = if (classification.resultType == DeliveryItemVerificationResultType.VERIFIED) {
            DeliveryItemVerificationActivityType.LINE_VERIFIED
        } else {
            DeliveryItemVerificationActivityType.ISSUE_REPORTED
        }

        val activity = DeliveryItemVerificationActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = verification.projectId,
            verificationId = verification.verificationId,
            lineId = targetLine.verificationLineId,
            activityType = activityType,
            performedBy = actorId,
            performedAt = System.currentTimeMillis(),
            details = "Line for product '${targetLine.productId}' verified as '${classification.resultType}' (Issue: '${classification.issueType}')."
        )
        verificationDataSource.insertActivityEvent(activity)

        DomainResult.Success(updatedLine)
    }

    override suspend fun completeVerification(
        verificationId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryItemVerification> = mutex.withLock {
        val existing = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.COMPLETE_VERIFICATION,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryItemVerificationLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryItemVerificationStatus.VERIFIED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryItemVerificationStatus.VERIFIED,
            verifiedBy = actorId,
            verifiedAt = now,
            updatedBy = actorId,
            updatedAt = now
        )
        verificationDataSource.updateVerification(updated)

        val activity = DeliveryItemVerificationActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            verificationId = existing.verificationId,
            activityType = DeliveryItemVerificationActivityType.VERIFIED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryItemVerificationStatus.VERIFIED.name,
            details = "All item verifications completed and recorded."
        )
        verificationDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun closeVerification(
        verificationId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryItemVerification> = mutex.withLock {
        val existing = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.CLOSE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryItemVerificationLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryItemVerificationStatus.CLOSED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryItemVerificationStatus.CLOSED,
            updatedBy = actorId,
            updatedAt = now
        )
        verificationDataSource.updateVerification(updated)

        val activity = DeliveryItemVerificationActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            verificationId = existing.verificationId,
            activityType = DeliveryItemVerificationActivityType.CLOSED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryItemVerificationStatus.CLOSED.name,
            details = "Delivery verification formally closed."
        )
        verificationDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun cancelVerification(
        verificationId: String,
        actorId: String,
        reason: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryItemVerification> = mutex.withLock {
        val existing = verificationDataSource.getVerification(verificationId)
            ?: return DomainResult.Error(message = "Delivery verification '$verificationId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryItemVerificationAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryItemVerificationOperation.CANCEL,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val transitionCheck = DeliveryItemVerificationLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryItemVerificationStatus.CANCELLED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryItemVerificationStatus.CANCELLED,
            updatedBy = actorId,
            updatedAt = now
        )
        verificationDataSource.updateVerification(updated)

        val activity = DeliveryItemVerificationActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            verificationId = existing.verificationId,
            activityType = DeliveryItemVerificationActivityType.CANCELLED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryItemVerificationStatus.CANCELLED.name,
            details = reason ?: "Delivery verification cancelled."
        )
        verificationDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }
}
