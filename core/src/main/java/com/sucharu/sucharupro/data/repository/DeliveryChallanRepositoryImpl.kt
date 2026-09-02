package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DeliveryChallanDataSource
import com.sucharu.sucharupro.data.datasource.DeliveryOrderDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallan
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanActivityType
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanLine
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanStatus
import com.sucharu.sucharupro.domain.model.delivery.challan.DeliveryChallanType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DeliveryChallanRepository
import com.sucharu.sucharupro.domain.validation.DeliveryChallanAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.DeliveryChallanLifecycleValidator
import com.sucharu.sucharupro.domain.validation.DeliveryChallanOperation
import com.sucharu.sucharupro.domain.validation.DeliveryChallanValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Production implementation of [DeliveryChallanRepository] (Module 08 Step 02).
 *
 * Enforces project isolation, RBAC permissions, lifecycle validations, quantity allocation,
 * and immutable audit logging.
 * Does NOT mutate physical stock or ledger records.
 */
class DeliveryChallanRepositoryImpl(
    private val challanDataSource: DeliveryChallanDataSource,
    private val deliveryOrderDataSource: DeliveryOrderDataSource
) : DeliveryChallanRepository {

    private val mutex = Mutex()

    // ──────────────────────────────────────────────────────────────
    // Challan Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeChallans(projectId: String): Flow<List<DeliveryChallan>> {
        return challanDataSource.observeChallans(projectId)
    }

    override fun observeChallansForDeliveryOrder(deliveryOrderId: String): Flow<List<DeliveryChallan>> {
        return challanDataSource.observeChallansForDeliveryOrder(deliveryOrderId)
    }

    override fun observeChallan(challanId: String): Flow<DeliveryChallan?> {
        return challanDataSource.observeChallan(challanId)
    }

    override suspend fun getChallan(
        challanId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryChallan> {
        val challan = challanDataSource.getChallan(challanId)
            ?: return DomainResult.Error(message = "Delivery challan '$challanId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.VIEW,
                targetProjectId = challan.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(challan)
    }

    override suspend fun getChallansForDeliveryOrder(
        deliveryOrderId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryChallan>> {
        val order = deliveryOrderDataSource.getDeliveryOrder(deliveryOrderId)
            ?: return DomainResult.Error(message = "Delivery order '$deliveryOrderId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.VIEW,
                targetProjectId = order.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val challans = challanDataSource.getChallansForDeliveryOrder(deliveryOrderId)
        return DomainResult.Success(challans)
    }

    // ──────────────────────────────────────────────────────────────
    // Challan Line Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeChallanLines(challanId: String): Flow<List<DeliveryChallanLine>> {
        return challanDataSource.observeChallanLines(challanId)
    }

    override suspend fun getChallanLines(
        challanId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryChallanLine>> {
        val challan = challanDataSource.getChallan(challanId)
            ?: return DomainResult.Error(message = "Delivery challan '$challanId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.VIEW,
                targetProjectId = challan.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val lines = challanDataSource.getChallanLines(challanId)
        return DomainResult.Success(lines)
    }

    override suspend fun getChallanLine(
        lineId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryChallanLine> {
        val line = challanDataSource.getChallanLine(lineId)
            ?: return DomainResult.Error(message = "Delivery challan line '$lineId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.VIEW,
                targetProjectId = line.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        return DomainResult.Success(line)
    }

    override suspend fun getAllocatedQuantityForDeliveryOrderLine(deliveryOrderLineId: String): Double = mutex.withLock {
        val doLine = deliveryOrderDataSource.getDeliveryOrderLine(deliveryOrderLineId) ?: return@withLock 0.0
        val activeChallans = challanDataSource.getChallansForDeliveryOrder(doLine.deliveryOrderId)
            .filter { it.status.consumesAllocation }
        val activeLines = challanDataSource.getLinesForChallans(activeChallans.map { it.challanId })
        activeLines.filter { it.deliveryOrderLineId == deliveryOrderLineId }.sumOf { it.quantity }
    }

    // ──────────────────────────────────────────────────────────────
    // Activity Queries
    // ──────────────────────────────────────────────────────────────

    override fun observeActivityEvents(challanId: String): Flow<List<DeliveryChallanActivityEvent>> {
        return challanDataSource.observeActivityEvents(challanId)
    }

    override suspend fun getActivityEvents(
        challanId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<DeliveryChallanActivityEvent>> {
        val challan = challanDataSource.getChallan(challanId)
            ?: return DomainResult.Error(message = "Delivery challan '$challanId' not found.")

        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.VIEW,
                targetProjectId = challan.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        val events = challanDataSource.getActivityEvents(challanId)
        return DomainResult.Success(events)
    }

    // ──────────────────────────────────────────────────────────────
    // Challan Mutations
    // ──────────────────────────────────────────────────────────────

    override suspend fun createChallan(
        challan: DeliveryChallan,
        lines: List<DeliveryChallanLine>,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryChallan> = mutex.withLock {
        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.CREATE,
                targetProjectId = challan.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Structural validation
        val validationResult = DeliveryChallanValidator.validateChallan(challan, lines)
        if (validationResult is DomainResult.Error) return validationResult

        // 3. Unique challan number check
        val existingChallan = challanDataSource.getChallanByNo(challan.projectId, challan.challanNo)
        if (existingChallan != null) {
            return DomainResult.Error(
                message = "Challan number '${challan.challanNo}' already exists in project '${challan.projectId}'."
            )
        }

        // 4. DeliveryOrder existence and eligibility check
        val deliveryOrder = deliveryOrderDataSource.getDeliveryOrder(challan.deliveryOrderId)
            ?: return DomainResult.Error(message = "Referenced Delivery Order '${challan.deliveryOrderId}' not found.")

        val eligibilityCheck = DeliveryChallanValidator.validateDeliveryOrderEligibility(deliveryOrder, challan.projectId)
        if (eligibilityCheck is DomainResult.Error) return eligibilityCheck

        // 5. Quantity allocation check
        val doLines = deliveryOrderDataSource.getDeliveryOrderLines(challan.deliveryOrderId)
        val activeChallans = challanDataSource.getChallansForDeliveryOrder(challan.deliveryOrderId)
            .filter { it.status.consumesAllocation }
        val activeLines = challanDataSource.getLinesForChallans(activeChallans.map { it.challanId })

        val allocationCheck = DeliveryChallanValidator.validateQuantityAllocation(
            orderLines = doLines,
            existingActiveChallanLines = activeLines,
            newChallanLines = lines
        )
        if (allocationCheck is DomainResult.Error) return allocationCheck

        // 6. Persistence
        challanDataSource.insertChallan(challan, lines)

        // 7. Audit event
        val activity = DeliveryChallanActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = challan.projectId,
            challanId = challan.challanId,
            activityType = DeliveryChallanActivityType.CREATED,
            performedBy = challan.createdBy,
            performedAt = challan.createdAt,
            details = "Challan created for DO '${deliveryOrder.deliveryOrderNo}' with ${lines.size} item line(s).",
            newStatus = challan.status.name
        )
        challanDataSource.insertActivityEvent(activity)

        DomainResult.Success(challan)
    }

    override suspend fun updateDraftChallan(
        challanId: String,
        challanType: DeliveryChallanType,
        issueDate: Long,
        notes: String?,
        lines: List<DeliveryChallanLine>,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryChallan> = mutex.withLock {
        val existing = challanDataSource.getChallan(challanId)
            ?: return DomainResult.Error(message = "Delivery challan '$challanId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.EDIT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Draft check
        if (existing.status != DeliveryChallanStatus.DRAFT) {
            return DomainResult.Error(
                message = "Only DRAFT challans can be modified. Current status: '${existing.status}'."
            )
        }

        val updatedChallan = existing.copy(
            challanType = challanType,
            issueDate = issueDate,
            notes = notes,
            updatedAt = System.currentTimeMillis()
        )

        // 3. Validation
        val validationResult = DeliveryChallanValidator.validateChallan(updatedChallan, lines)
        if (validationResult is DomainResult.Error) return validationResult

        val immutabilityCheck = DeliveryChallanValidator.validateImmutableIdentity(existing, updatedChallan)
        if (immutabilityCheck is DomainResult.Error) return immutabilityCheck

        // 4. Quantity allocation check (excluding this challan's old lines)
        val doLines = deliveryOrderDataSource.getDeliveryOrderLines(existing.deliveryOrderId)
        val otherActiveChallans = challanDataSource.getChallansForDeliveryOrder(existing.deliveryOrderId)
            .filter { it.challanId != challanId && it.status.consumesAllocation }
        val otherActiveLines = challanDataSource.getLinesForChallans(otherActiveChallans.map { it.challanId })

        val allocationCheck = DeliveryChallanValidator.validateQuantityAllocation(
            orderLines = doLines,
            existingActiveChallanLines = otherActiveLines,
            newChallanLines = lines
        )
        if (allocationCheck is DomainResult.Error) return allocationCheck

        // 5. Persistence
        challanDataSource.updateChallanWithLines(updatedChallan, lines)

        // 6. Audit
        val activity = DeliveryChallanActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            challanId = existing.challanId,
            activityType = DeliveryChallanActivityType.UPDATED,
            performedBy = actorId,
            performedAt = updatedChallan.updatedAt,
            details = "Draft challan updated with ${lines.size} line(s)."
        )
        challanDataSource.insertActivityEvent(activity)

        DomainResult.Success(updatedChallan)
    }

    override suspend fun submitChallan(
        challanId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryChallan> = mutex.withLock {
        val existing = challanDataSource.getChallan(challanId)
            ?: return DomainResult.Error(message = "Delivery challan '$challanId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.SUBMIT,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Lifecycle
        val transitionCheck = DeliveryChallanLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryChallanStatus.PENDING
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryChallanStatus.PENDING,
            updatedAt = now
        )

        challanDataSource.updateChallan(updated)

        // 3. Audit
        val activity = DeliveryChallanActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            challanId = existing.challanId,
            activityType = DeliveryChallanActivityType.SUBMITTED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryChallanStatus.PENDING.name,
            details = "Challan submitted for approval."
        )
        challanDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun approveChallan(
        challanId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryChallan> = mutex.withLock {
        val existing = challanDataSource.getChallan(challanId)
            ?: return DomainResult.Error(message = "Delivery challan '$challanId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.APPROVE,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Lifecycle
        val transitionCheck = DeliveryChallanLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryChallanStatus.APPROVED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        // 3. Atomically re-verify parent Delivery Order status and quantity allocations
        val order = deliveryOrderDataSource.getDeliveryOrder(existing.deliveryOrderId)
            ?: return DomainResult.Error(message = "Referenced Delivery Order '${existing.deliveryOrderId}' not found.")

        val eligibilityCheck = DeliveryChallanValidator.validateDeliveryOrderEligibility(order, existing.projectId)
        if (eligibilityCheck is DomainResult.Error) return eligibilityCheck

        val doLines = deliveryOrderDataSource.getDeliveryOrderLines(existing.deliveryOrderId)
        val myLines = challanDataSource.getChallanLines(challanId)
        val otherActiveChallans = challanDataSource.getChallansForDeliveryOrder(existing.deliveryOrderId)
            .filter { it.challanId != challanId && it.status.consumesAllocation }
        val otherActiveLines = challanDataSource.getLinesForChallans(otherActiveChallans.map { it.challanId })

        val allocationCheck = DeliveryChallanValidator.validateQuantityAllocation(
            orderLines = doLines,
            existingActiveChallanLines = otherActiveLines,
            newChallanLines = myLines
        )
        if (allocationCheck is DomainResult.Error) return allocationCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryChallanStatus.APPROVED,
            updatedAt = now
        )

        challanDataSource.updateChallan(updated)

        // 4. Audit
        val activity = DeliveryChallanActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            challanId = existing.challanId,
            activityType = DeliveryChallanActivityType.APPROVED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryChallanStatus.APPROVED.name,
            details = "Challan approved."
        )
        challanDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun markReadyForDispatch(
        challanId: String,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryChallan> = mutex.withLock {
        val existing = challanDataSource.getChallan(challanId)
            ?: return DomainResult.Error(message = "Delivery challan '$challanId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.READY_FOR_DISPATCH,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Lifecycle
        val transitionCheck = DeliveryChallanLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryChallanStatus.READY_FOR_DISPATCH
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryChallanStatus.READY_FOR_DISPATCH,
            updatedAt = now
        )

        challanDataSource.updateChallan(updated)

        // 3. Audit
        val activity = DeliveryChallanActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            challanId = existing.challanId,
            activityType = DeliveryChallanActivityType.READY_FOR_DISPATCH,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryChallanStatus.READY_FOR_DISPATCH.name,
            details = "Challan marked as ready for dispatch."
        )
        challanDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }

    override suspend fun cancelChallan(
        challanId: String,
        actorId: String,
        reason: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<DeliveryChallan> = mutex.withLock {
        val existing = challanDataSource.getChallan(challanId)
            ?: return DomainResult.Error(message = "Delivery challan '$challanId' not found.")

        // 1. RBAC
        if (callerRole != null) {
            val authCheck = DeliveryChallanAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = DeliveryChallanOperation.CANCEL,
                targetProjectId = existing.projectId,
                callerProjectId = callerProjectId
            )
            if (authCheck is DomainResult.Error) return authCheck
        }

        // 2. Lifecycle
        val transitionCheck = DeliveryChallanLifecycleValidator.validateTransition(
            currentStatus = existing.status,
            targetStatus = DeliveryChallanStatus.CANCELLED
        )
        if (transitionCheck is DomainResult.Error) return transitionCheck

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = DeliveryChallanStatus.CANCELLED,
            updatedAt = now
        )

        challanDataSource.updateChallan(updated)

        // 3. Audit
        val activity = DeliveryChallanActivityEvent(
            activityId = UUID.randomUUID().toString(),
            projectId = existing.projectId,
            challanId = existing.challanId,
            activityType = DeliveryChallanActivityType.CANCELLED,
            performedBy = actorId,
            performedAt = now,
            previousStatus = existing.status.name,
            newStatus = DeliveryChallanStatus.CANCELLED.name,
            details = reason ?: "Challan cancelled."
        )
        challanDataSource.insertActivityEvent(activity)

        DomainResult.Success(updated)
    }
}
