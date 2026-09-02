package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.InventoryMovementLedgerDataSource
import com.sucharu.sucharupro.data.datasource.InventoryReceivingDataSource
import com.sucharu.sucharupro.data.datasource.ReturnDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.InventoryUnit
import com.sucharu.sucharupro.domain.model.inventory.receiving.InventoryStockInRecord
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityEvent
import com.sucharu.sucharupro.domain.model.returns.ReturnActivityType
import com.sucharu.sucharupro.domain.model.returns.ReturnDecision
import com.sucharu.sucharupro.domain.model.returns.ReturnInspection
import com.sucharu.sucharupro.domain.model.returns.ReturnInspectionStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnItem
import com.sucharu.sucharupro.domain.model.returns.ReturnReceivingInfo
import com.sucharu.sucharupro.domain.model.returns.ReturnReconciliationResult
import com.sucharu.sucharupro.domain.model.returns.ReturnRequest
import com.sucharu.sucharupro.domain.model.returns.ReturnResolutionType
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlement
import com.sucharu.sucharupro.domain.model.returns.ReturnSettlementStatus
import com.sucharu.sucharupro.domain.model.returns.ReturnStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ReturnRepository
import com.sucharu.sucharupro.domain.service.inventory.InventoryMovementLedgerBuilder
import com.sucharu.sucharupro.domain.validation.returns.ReturnAuthorizationValidator
import com.sucharu.sucharupro.domain.validation.returns.ReturnDomainValidator
import com.sucharu.sucharupro.domain.validation.returns.ReturnInspectionValidator
import com.sucharu.sucharupro.domain.validation.returns.ReturnLifecycleValidator
import com.sucharu.sucharupro.domain.validation.returns.ReturnOperation
import com.sucharu.sucharupro.domain.validation.returns.ReturnReceivingValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID

/**
 * Production-grade implementation of [ReturnRepository] (Module 11 Step 01, 02, 03, 04).
 *
 * Enforces at every boundary:
 *   - Project isolation: callerProjectId must match the target Return's projectId.
 *   - RBAC: [ReturnAuthorizationValidator] is invoked for all write operations.
 *   - Domain validation: [ReturnDomainValidator], [ReturnInspectionValidator], and [ReturnReceivingValidator].
 *   - Lifecycle: [ReturnLifecycleValidator] validates status transitions.
 *   - Optimistic concurrency: [expectedVersion] checked before status transitions and reconciliation.
 *   - Inventory reconciliation: [InventoryStockInRecord] and [InventoryMovementLedgerBuilder] integration.
 *   - Idempotency and append-only audit logging.
 */
class ReturnRepositoryImpl(
    private val dataSource: ReturnDataSource,
    private val inventoryReceivingDataSource: InventoryReceivingDataSource? = null,
    private val inventoryLedgerDataSource: InventoryMovementLedgerDataSource? = null
) : ReturnRepository {

    private val mutex = Mutex()

    // =========================================================================
    // Observation
    // =========================================================================

    override fun observeReturns(projectId: String): Flow<List<ReturnRequest>> =
        dataSource.observeReturns(projectId)

    override fun observeReturn(returnId: String): Flow<ReturnRequest?> =
        dataSource.observeReturn(returnId)

    override fun observeAuditHistory(returnId: String): Flow<List<ReturnActivityEvent>> =
        dataSource.observeActivityEvents(returnId)

    override fun observeInspection(returnId: String): Flow<ReturnInspection?> =
        dataSource.observeInspection(returnId)

    override fun observeReceiving(returnId: String): Flow<ReturnReceivingInfo?> =
        dataSource.observeReceiving(returnId)

    override fun observeReconciliationResult(returnId: String): Flow<ReturnReconciliationResult?> =
        dataSource.observeReconciliationResult(returnId)

    // =========================================================================
    // Reads
    // =========================================================================

    override suspend fun getReturn(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnRequest> = mutex.withLock {
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        // Project isolation
        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        // RBAC — view permission
        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.VIEW_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        DomainResult.Success(request)
    }

    override suspend fun getReturnItems(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<ReturnItem>> = mutex.withLock {
        // Verify parent return exists and access is authorized
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.VIEW_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        DomainResult.Success(dataSource.getReturnItems(returnId))
    }

    override suspend fun listReturns(
        projectId: String,
        customerId: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<ReturnRequest>> = mutex.withLock {
        // Project isolation: caller cannot list another project's returns
        if (callerProjectId != null && callerProjectId != projectId) {
            return DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' cannot list returns " +
                    "of project '$projectId'."
            )
        }

        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.VIEW_RETURN,
                targetProjectId = projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        DomainResult.Success(dataSource.getReturnsByProject(projectId, customerId))
    }

    override suspend fun listReturnsByCustomer(
        projectId: String,
        customerId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<ReturnRequest>> {
        return listReturns(
            projectId = projectId,
            customerId = customerId,
            callerRole = callerRole,
            callerProjectId = callerProjectId
        )
    }

    override suspend fun getAuditHistory(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<ReturnActivityEvent>> = mutex.withLock {
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.VIEW_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        DomainResult.Success(dataSource.getActivityEvents(returnId))
    }

    override suspend fun getInspection(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnInspection?> = mutex.withLock {
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.VIEW_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        DomainResult.Success(dataSource.getInspection(returnId))
    }

    override suspend fun getReceiving(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnReceivingInfo?> = mutex.withLock {
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.VIEW_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        DomainResult.Success(dataSource.getReceiving(returnId))
    }

    override suspend fun getReconciliationResult(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnReconciliationResult?> = mutex.withLock {
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.VIEW_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        DomainResult.Success(dataSource.getReconciliationResult(returnId))
    }

    // =========================================================================
    // Writes – Step 01 & Step 02
    // =========================================================================

    override suspend fun createReturn(
        request: ReturnRequest,
        items: List<ReturnItem>,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnRequest> = mutex.withLock {
        // Project isolation
        if (callerProjectId != null && callerProjectId != request.projectId) {
            return DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' cannot create a Return " +
                    "in project '${request.projectId}'."
            )
        }

        // RBAC
        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.CREATE_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        // Domain validation of the aggregate
        val validation = ReturnDomainValidator.validateReturnRequest(request)
        if (validation is DomainResult.Error) return validation

        // Validate each item
        for (item in items) {
            val itemValidation = ReturnDomainValidator.validateReturnItem(item)
            if (itemValidation is DomainResult.Error) return itemValidation

            val belongs = ReturnDomainValidator.validateItemBelongsToReturn(item, request.returnId)
            if (belongs is DomainResult.Error) return belongs
        }

        // Verify a Return with this ID does not already exist (idempotency guard)
        val existing = dataSource.getReturn(request.returnId)
        if (existing != null) {
            return DomainResult.Error(
                message = "Return '${request.returnId}' already exists. " +
                    "A duplicate create is not allowed."
            )
        }

        dataSource.insertReturn(request, items)

        // Record audit activity event
        val createEvent = ReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = request.projectId,
            returnId = request.returnId,
            activityType = ReturnActivityType.RETURN_REQUEST_CREATED,
            actorId = actorId,
            actorRole = callerRole,
            timestamp = request.createdAt,
            previousStatus = null,
            newStatus = request.status,
            notes = "Return request ${request.returnNo} created."
        )
        dataSource.insertActivityEvent(createEvent)

        DomainResult.Success(request)
    }

    override suspend fun transitionReturnStatus(
        returnId: String,
        targetStatus: ReturnStatus,
        actorId: String,
        expectedVersion: Long,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnRequest> = mutex.withLock {
        // Load the return
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        // Project isolation
        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        // Optimistic concurrency check
        if (request.version != expectedVersion) {
            return DomainResult.Error(
                message = "Concurrency conflict on Return '$returnId': " +
                    "expected version $expectedVersion but found ${request.version}. " +
                    "Please reload and retry."
            )
        }

        // Lifecycle validation
        val lifecycle = ReturnLifecycleValidator.validateTransition(request.status, targetStatus)
        if (lifecycle is DomainResult.Error) return lifecycle

        // RBAC: map target status to the required operation
        if (callerRole != null) {
            val operation = targetStatus.toRequiredOperation()
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = operation,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        // Persist the updated Return with incremented version and updated timestamp
        val updatedRequest = request.copy(
            status = targetStatus,
            version = request.version + 1L,
            updatedAt = System.currentTimeMillis()
        )
        dataSource.updateReturn(updatedRequest)

        // Record audit activity event
        val activityType = when (targetStatus) {
            ReturnStatus.CANCELLED -> ReturnActivityType.RETURN_REQUEST_CANCELLED
            ReturnStatus.UNDER_INSPECTION -> ReturnActivityType.RETURN_REQUEST_SUBMITTED_FOR_INSPECTION
            ReturnStatus.APPROVED -> ReturnActivityType.RETURN_REQUEST_APPROVED
            ReturnStatus.REJECTED -> ReturnActivityType.RETURN_REQUEST_REJECTED
            ReturnStatus.RETURN_RECEIVED -> ReturnActivityType.RETURN_RECEIVED
            else -> ReturnActivityType.RETURN_REQUEST_UPDATED
        }
        val event = ReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = request.projectId,
            returnId = request.returnId,
            activityType = activityType,
            actorId = actorId,
            actorRole = callerRole,
            timestamp = System.currentTimeMillis(),
            previousStatus = request.status,
            newStatus = targetStatus,
            notes = "Status transitioned from ${request.status} to $targetStatus"
        )
        dataSource.insertActivityEvent(event)

        DomainResult.Success(updatedRequest)
    }

    override suspend fun updateReturnRequest(
        request: ReturnRequest,
        items: List<ReturnItem>,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnRequest> = mutex.withLock {
        // Project isolation
        if (callerProjectId != null && callerProjectId != request.projectId) {
            return DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' cannot update a Return " +
                    "in project '${request.projectId}'."
            )
        }

        // RBAC: update uses CREATE_RETURN permission (STAFF, ADMIN, MANAGER)
        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.CREATE_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        // Verify existing return exists
        val existing = dataSource.getReturn(request.returnId)
            ?: return DomainResult.Error(message = "Return '${request.returnId}' not found.")

        // Verify project isolation on existing entity
        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(existing, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        // Concurrency check
        if (existing.version != request.version) {
            return DomainResult.Error(
                message = "Concurrency conflict on Return '${request.returnId}': " +
                    "expected version ${request.version} but found ${existing.version}. " +
                    "Please reload and retry."
            )
        }

        // Validate that return is still in editable status (REQUESTED)
        val editableCheck = ReturnDomainValidator.validateEditableStatus(existing)
        if (editableCheck is DomainResult.Error) return editableCheck

        // Validate aggregate
        val validation = ReturnDomainValidator.validateReturnRequest(request)
        if (validation is DomainResult.Error) return validation

        // Validate items
        for (item in items) {
            val itemValidation = ReturnDomainValidator.validateReturnItem(item)
            if (itemValidation is DomainResult.Error) return itemValidation

            val belongs = ReturnDomainValidator.validateItemBelongsToReturn(item, request.returnId)
            if (belongs is DomainResult.Error) return belongs
        }

        // Build updated record preserving immutable creation fields
        val updatedRequest = existing.copy(
            reason = request.reason,
            description = request.description,
            originalChallanId = request.originalChallanId,
            version = existing.version + 1L,
            updatedAt = System.currentTimeMillis()
        )

        dataSource.updateReturn(updatedRequest)
        for (item in items) {
            dataSource.updateReturnItem(item)
        }

        // Record audit event
        val updateEvent = ReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = request.projectId,
            returnId = request.returnId,
            activityType = ReturnActivityType.RETURN_REQUEST_UPDATED,
            actorId = actorId,
            actorRole = callerRole,
            timestamp = System.currentTimeMillis(),
            previousStatus = existing.status,
            newStatus = updatedRequest.status,
            notes = "Return request ${existing.returnNo} updated."
        )
        dataSource.insertActivityEvent(updateEvent)

        DomainResult.Success(updatedRequest)
    }

    override suspend fun cancelReturnRequest(
        returnId: String,
        actorId: String,
        expectedVersion: Long,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnRequest> {
        return transitionReturnStatus(
            returnId = returnId,
            targetStatus = ReturnStatus.CANCELLED,
            actorId = actorId,
            expectedVersion = expectedVersion,
            callerRole = callerRole,
            callerProjectId = callerProjectId
        )
    }

    override suspend fun submitForInspection(
        returnId: String,
        actorId: String,
        expectedVersion: Long,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnRequest> {
        return transitionReturnStatus(
            returnId = returnId,
            targetStatus = ReturnStatus.UNDER_INSPECTION,
            actorId = actorId,
            expectedVersion = expectedVersion,
            callerRole = callerRole,
            callerProjectId = callerProjectId
        )
    }

    // =========================================================================
    // Module 11 Step 03 Operations – Inspection & Decision Management
    // =========================================================================

    override suspend fun recordInspection(
        inspection: ReturnInspection,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnInspection> = mutex.withLock {
        // Load target return
        val request = dataSource.getReturn(inspection.returnId)
            ?: return DomainResult.Error(message = "Return '${inspection.returnId}' not found.")

        // Project isolation
        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        // RBAC: INSPECT_RETURN
        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.INSPECT_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        // Validate that return is currently in UNDER_INSPECTION
        val eligible = ReturnInspectionValidator.validateEligibleForInspection(request)
        if (eligible is DomainResult.Error) return eligible

        // Validate inspection domain invariants
        val inspectionVal = ReturnInspectionValidator.validateInspection(inspection)
        if (inspectionVal is DomainResult.Error) return inspectionVal

        val existingInspection = dataSource.getInspection(inspection.returnId)
        val version = if (existingInspection != null) existingInspection.version + 1L else 1L

        val updatedInspection = inspection.copy(
            projectId = request.projectId,
            version = version,
            updatedAt = System.currentTimeMillis()
        )

        dataSource.insertOrUpdateInspection(updatedInspection)

        // Record audit event
        val eventType = if (updatedInspection.status == ReturnInspectionStatus.COMPLETED) {
            ReturnActivityType.RETURN_INSPECTION_COMPLETED
        } else {
            ReturnActivityType.RETURN_INSPECTION_RECORDED
        }

        val event = ReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = request.projectId,
            returnId = request.returnId,
            activityType = eventType,
            actorId = actorId,
            actorRole = callerRole,
            timestamp = System.currentTimeMillis(),
            previousStatus = request.status,
            newStatus = request.status,
            metadata = mapOf("inspectionId" to updatedInspection.inspectionId, "status" to updatedInspection.status.name),
            notes = "Inspection recorded by $actorId (Status: ${updatedInspection.status.displayName})."
        )
        dataSource.insertActivityEvent(event)

        DomainResult.Success(updatedInspection)
    }

    override suspend fun approveReturn(
        returnId: String,
        actorId: String,
        expectedVersion: Long,
        inspection: ReturnInspection?,
        items: List<ReturnItem>?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnRequest> = mutex.withLock {
        // Load target return
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        // Project isolation
        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        // Concurrency check
        if (request.version != expectedVersion) {
            return DomainResult.Error(
                message = "Concurrency conflict on Return '$returnId': " +
                    "expected version $expectedVersion but found ${request.version}. " +
                    "Please reload and retry."
            )
        }

        // RBAC: APPROVE_RETURN
        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.APPROVE_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        // Lifecycle validation
        val lifecycle = ReturnLifecycleValidator.validateTransition(request.status, ReturnStatus.APPROVED)
        if (lifecycle is DomainResult.Error) return lifecycle

        // Update items if supplied
        if (items != null) {
            val qtyVal = ReturnInspectionValidator.validateInspectionItemQuantities(items, ReturnDecision.APPROVE)
            if (qtyVal is DomainResult.Error) return qtyVal

            for (item in items) {
                val belongs = ReturnDomainValidator.validateItemBelongsToReturn(item, request.returnId)
                if (belongs is DomainResult.Error) return belongs
                dataSource.updateReturnItem(item)
            }
        }

        // Save inspection if supplied or complete existing
        if (inspection != null) {
            val inspectionVal = ReturnInspectionValidator.validateInspection(
                inspection.copy(status = ReturnInspectionStatus.COMPLETED, decision = ReturnDecision.APPROVE)
            )
            if (inspectionVal is DomainResult.Error) return inspectionVal

            dataSource.insertOrUpdateInspection(
                inspection.copy(
                    status = ReturnInspectionStatus.COMPLETED,
                    decision = ReturnDecision.APPROVE,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            val existing = dataSource.getInspection(returnId)
            val toSave = existing?.copy(
                status = ReturnInspectionStatus.COMPLETED,
                decision = ReturnDecision.APPROVE,
                updatedAt = System.currentTimeMillis()
            ) ?: ReturnInspection(
                inspectionId = UUID.randomUUID().toString(),
                returnId = returnId,
                projectId = request.projectId,
                inspectorId = actorId,
                status = ReturnInspectionStatus.COMPLETED,
                decision = ReturnDecision.APPROVE,
                findings = "Approved by $actorId."
            )
            dataSource.insertOrUpdateInspection(toSave)
        }

        // Persist return with updated status
        val updatedRequest = request.copy(
            status = ReturnStatus.APPROVED,
            version = request.version + 1L,
            updatedAt = System.currentTimeMillis()
        )
        dataSource.updateReturn(updatedRequest)

        // Record audit event
        val event = ReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = request.projectId,
            returnId = request.returnId,
            activityType = ReturnActivityType.RETURN_REQUEST_APPROVED,
            actorId = actorId,
            actorRole = callerRole,
            timestamp = System.currentTimeMillis(),
            previousStatus = request.status,
            newStatus = ReturnStatus.APPROVED,
            notes = "Return request ${request.returnNo} APPROVED by $actorId."
        )
        dataSource.insertActivityEvent(event)

        DomainResult.Success(updatedRequest)
    }

    override suspend fun rejectReturn(
        returnId: String,
        actorId: String,
        expectedVersion: Long,
        rejectionReason: String,
        inspection: ReturnInspection?,
        items: List<ReturnItem>?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnRequest> = mutex.withLock {
        if (rejectionReason.isBlank()) {
            return DomainResult.Error(message = "Rejection reason cannot be blank.")
        }

        // Load target return
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        // Project isolation
        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        // Concurrency check
        if (request.version != expectedVersion) {
            return DomainResult.Error(
                message = "Concurrency conflict on Return '$returnId': " +
                    "expected version $expectedVersion but found ${request.version}. " +
                    "Please reload and retry."
            )
        }

        // RBAC: REJECT_RETURN
        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.REJECT_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        // Lifecycle validation
        val lifecycle = ReturnLifecycleValidator.validateTransition(request.status, ReturnStatus.REJECTED)
        if (lifecycle is DomainResult.Error) return lifecycle

        // Update items if supplied
        if (items != null) {
            val qtyVal = ReturnInspectionValidator.validateInspectionItemQuantities(items, ReturnDecision.REJECT)
            if (qtyVal is DomainResult.Error) return qtyVal

            for (item in items) {
                val belongs = ReturnDomainValidator.validateItemBelongsToReturn(item, request.returnId)
                if (belongs is DomainResult.Error) return belongs
                dataSource.updateReturnItem(item)
            }
        }

        // Save inspection if supplied or complete existing
        if (inspection != null) {
            val inspectionVal = ReturnInspectionValidator.validateInspection(
                inspection.copy(
                    status = ReturnInspectionStatus.COMPLETED,
                    decision = ReturnDecision.REJECT,
                    decisionReason = rejectionReason
                )
            )
            if (inspectionVal is DomainResult.Error) return inspectionVal

            dataSource.insertOrUpdateInspection(
                inspection.copy(
                    status = ReturnInspectionStatus.COMPLETED,
                    decision = ReturnDecision.REJECT,
                    decisionReason = rejectionReason,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            val existing = dataSource.getInspection(returnId)
            val toSave = existing?.copy(
                status = ReturnInspectionStatus.COMPLETED,
                decision = ReturnDecision.REJECT,
                decisionReason = rejectionReason,
                updatedAt = System.currentTimeMillis()
            ) ?: ReturnInspection(
                inspectionId = UUID.randomUUID().toString(),
                returnId = returnId,
                projectId = request.projectId,
                inspectorId = actorId,
                status = ReturnInspectionStatus.COMPLETED,
                decision = ReturnDecision.REJECT,
                decisionReason = rejectionReason,
                findings = "Rejected by $actorId. Reason: $rejectionReason"
            )
            dataSource.insertOrUpdateInspection(toSave)
        }

        // Persist return with updated status
        val updatedRequest = request.copy(
            status = ReturnStatus.REJECTED,
            version = request.version + 1L,
            updatedAt = System.currentTimeMillis()
        )
        dataSource.updateReturn(updatedRequest)

        // Record audit event
        val event = ReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = request.projectId,
            returnId = request.returnId,
            activityType = ReturnActivityType.RETURN_REQUEST_REJECTED,
            actorId = actorId,
            actorRole = callerRole,
            timestamp = System.currentTimeMillis(),
            previousStatus = request.status,
            newStatus = ReturnStatus.REJECTED,
            notes = "Return request ${request.returnNo} REJECTED by $actorId. Reason: $rejectionReason"
        )
        dataSource.insertActivityEvent(event)

        DomainResult.Success(updatedRequest)
    }

    override suspend fun updateReturnItemQuantities(
        returnId: String,
        items: List<ReturnItem>,
        actorId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<List<ReturnItem>> = mutex.withLock {
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.INSPECT_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        val eligible = ReturnInspectionValidator.validateEligibleForInspection(request)
        if (eligible is DomainResult.Error) return eligible

        val qtyCheck = ReturnInspectionValidator.validateInspectionItemQuantities(items, null)
        if (qtyCheck is DomainResult.Error) return qtyCheck

        for (item in items) {
            val belongs = ReturnDomainValidator.validateItemBelongsToReturn(item, returnId)
            if (belongs is DomainResult.Error) return belongs
            dataSource.updateReturnItem(item)
        }

        DomainResult.Success(items)
    }

    // =========================================================================
    // Module 11 Step 04 Operations – Return Receiving
    // =========================================================================

    override suspend fun receiveReturn(
        receivingInfo: ReturnReceivingInfo,
        actorId: String,
        expectedVersion: Long,
        callerCustomerId: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnRequest> = mutex.withLock {
        // Load target return
        val request = dataSource.getReturn(receivingInfo.returnId)
            ?: return DomainResult.Error(message = "Return '${receivingInfo.returnId}' not found.")

        // Project isolation
        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        // RBAC: RECEIVE_RETURN (Warehouse, Admin, Manager)
        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.RECEIVE_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        // Idempotency check
        val existingByIdempotency = dataSource.getReceivingByIdempotencyKey(receivingInfo.idempotencyKey)
        if (existingByIdempotency != null) {
            if (existingByIdempotency.returnId == request.returnId && request.status == ReturnStatus.RETURN_RECEIVED) {
                // Idempotent replay: return current state of the ReturnRequest
                return DomainResult.Success(request)
            } else {
                return DomainResult.Error(
                    message = "Duplicate idempotency key '${receivingInfo.idempotencyKey}' already used for return '${existingByIdempotency.returnId}'."
                )
            }
        }

        val existingReceivingForReturn = dataSource.getReceiving(receivingInfo.returnId)
        if (existingReceivingForReturn != null || request.status == ReturnStatus.RETURN_RECEIVED) {
            return DomainResult.Error(
                message = "Return '${receivingInfo.returnId}' has already been received."
            )
        }

        // Optimistic concurrency check
        if (request.version != expectedVersion) {
            return DomainResult.Error(
                message = "Concurrency conflict on Return '${receivingInfo.returnId}': " +
                    "expected version $expectedVersion but found ${request.version}. " +
                    "Please reload and retry."
            )
        }

        // Domain validation on ReturnReceivingInfo using ReturnReceivingValidator
        val receivingValidation = ReturnReceivingValidator.validate(
            request = request,
            receivingInfo = receivingInfo,
            callerCustomerId = callerCustomerId,
            callerRole = callerRole,
            callerProjectId = callerProjectId
        )
        if (receivingValidation is DomainResult.Error) return receivingValidation

        // Ensure projectId matches target return and persist receiving record
        val receivingToPersist = receivingInfo.copy(
            projectId = request.projectId
        )
        dataSource.insertOrUpdateReceiving(receivingToPersist)

        // Persist ReturnRequest with updated status RETURN_RECEIVED, incremented version, updated timestamp
        val updatedRequest = request.copy(
            status = ReturnStatus.RETURN_RECEIVED,
            version = request.version + 1L,
            updatedAt = System.currentTimeMillis()
        )
        dataSource.updateReturn(updatedRequest)

        // Record audit activity event
        val event = ReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = request.projectId,
            returnId = request.returnId,
            activityType = ReturnActivityType.RETURN_RECEIVED,
            actorId = actorId,
            actorRole = callerRole,
            timestamp = System.currentTimeMillis(),
            previousStatus = request.status,
            newStatus = ReturnStatus.RETURN_RECEIVED,
            metadata = mapOf(
                "receivingEventId" to receivingToPersist.receivingEventId,
                "idempotencyKey" to receivingToPersist.idempotencyKey,
                "actualQty" to receivingToPersist.actualQty.toString(),
                "acceptedQty" to receivingToPersist.acceptedQty.toString(),
                "rejectedQty" to receivingToPersist.rejectedQty.toString(),
                "damagedQty" to receivingToPersist.damagedQty.toString(),
                "mismatchFlag" to receivingToPersist.mismatchFlag.toString()
            ),
            notes = "Return receiving recorded by $actorId. Actual qty: ${receivingToPersist.actualQty} (Accepted: ${receivingToPersist.acceptedQty}, Rejected: ${receivingToPersist.rejectedQty}, Damaged: ${receivingToPersist.damagedQty})."
        )
        dataSource.insertActivityEvent(event)

        DomainResult.Success(updatedRequest)
    }

    // =========================================================================
    // Module 11 Step 04 Chunk 04 – Inventory Reconciliation & Closeout
    // =========================================================================

    override suspend fun reconcileInventoryAndProcess(
        returnId: String,
        warehouseId: String,
        locationId: String,
        actorId: String,
        expectedVersion: Long,
        callerCustomerId: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnReconciliationResult> = mutex.withLock {
        // 1. Load target return
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        // 2. Project isolation
        if (callerProjectId != null) {
            val isolation = ReturnDomainValidator.validateProjectIsolation(request, callerProjectId)
            if (isolation is DomainResult.Error) return isolation
        }

        // 3. Customer ownership verification (if customer ID provided)
        if (callerCustomerId != null) {
            val ownership = ReturnDomainValidator.validateCustomerOwnership(request, callerCustomerId)
            if (ownership is DomainResult.Error) return ownership
        }

        // 4. RBAC: PROCESS_RETURN (WAREHOUSE, ADMIN, MANAGER)
        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.PROCESS_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        // 5. Idempotency guard: If return is already PROCESSED and has a reconciliation record, return safely
        val existingReconciliation = dataSource.getReconciliationResult(returnId)
        if (request.status == ReturnStatus.PROCESSED && existingReconciliation != null) {
            return DomainResult.Success(existingReconciliation)
        }

        // 6. Enforce lifecycle precondition: Only RETURN_RECEIVED returns can be reconciled and processed
        if (request.status != ReturnStatus.RETURN_RECEIVED) {
            return DomainResult.Error(
                message = "Return '$returnId' is in status ${request.status}; " +
                    "only returns in ${ReturnStatus.RETURN_RECEIVED} can be reconciled and processed."
            )
        }

        // 7. Verify receiving record exists and belongs to this return
        val receivingInfo = dataSource.getReceiving(returnId)
            ?: return DomainResult.Error(
                message = "No receiving record found for Return '$returnId'. Physical receiving must be completed first."
            )

        if (receivingInfo.projectId != request.projectId) {
            return DomainResult.Error(
                message = "Receiving record project '${receivingInfo.projectId}' does not match return project '${request.projectId}'."
            )
        }

        // 8. Optimistic Concurrency check
        if (request.version != expectedVersion) {
            return DomainResult.Error(
                message = "Concurrency conflict on Return '$returnId': " +
                    "expected version $expectedVersion but found ${request.version}. " +
                    "Please reload and retry."
            )
        }

        // 9. Load return items to determine product ID and unit
        val items = dataSource.getReturnItems(returnId)
        val targetProduct = items.firstOrNull()

        val acceptedQty = receivingInfo.acceptedQty
        val mutationApplied = acceptedQty > 0
        var stockInId: String? = null
        var ledgerEntryId: String? = null

        val nowMillis = System.currentTimeMillis()
        val nowIso = Instant.now().toString()

        // 10. If acceptedQty > 0, perform canonical inventory stock-in and ledger creation
        if (mutationApplied) {
            if (warehouseId.isBlank()) {
                return DomainResult.Error(message = "Warehouse ID cannot be blank when accepted quantity is greater than 0.")
            }
            if (locationId.isBlank()) {
                return DomainResult.Error(message = "Location ID cannot be blank when accepted quantity is greater than 0.")
            }

            val productId = targetProduct?.productId ?: "RETURN-ITEM-DEFAULT"
            val lineId = targetProduct?.returnItemId ?: receivingInfo.receivingEventId
            val newStockInId = UUID.randomUUID().toString()
            stockInId = newStockInId

            val stockInRecord = InventoryStockInRecord(
                stockInId = newStockInId,
                receivingId = returnId,
                receivingLineId = lineId,
                projectId = request.projectId,
                inventoryProductId = productId,
                warehouseId = warehouseId,
                locationId = locationId,
                quantity = acceptedQty,
                unit = InventoryUnit.PCS,
                createdBy = actorId,
                createdAt = nowIso,
                sourceReference = "RETURN:${request.returnNo}"
            )

            // Persist to canonical inventory receiving data source if configured
            if (inventoryReceivingDataSource != null) {
                val stockInRes = inventoryReceivingDataSource.insertStockInRecord(stockInRecord)
                if (stockInRes is DomainResult.Error) return stockInRes
            }

            // Build and persist canonical movement ledger entry
            val ledgerEntry = InventoryMovementLedgerBuilder.buildFromStockIn(
                record = stockInRecord,
                unitCost = null
            )
            ledgerEntryId = ledgerEntry.ledgerEntryId

            if (inventoryLedgerDataSource != null) {
                val ledgerRes = inventoryLedgerDataSource.insertEntries(listOf(ledgerEntry))
                if (ledgerRes is DomainResult.Error) return ledgerRes
            }
        }

        // 11. Advance ReturnRequest lifecycle to PROCESSED, increment version, update timestamp
        val updatedRequest = request.copy(
            status = ReturnStatus.PROCESSED,
            version = request.version + 1L,
            updatedAt = nowMillis
        )
        dataSource.updateReturn(updatedRequest)

        // 12. Create and persist ReturnReconciliationResult
        val result = ReturnReconciliationResult(
            returnId = returnId,
            receivingEventId = receivingInfo.receivingEventId,
            projectId = request.projectId,
            acceptedQty = acceptedQty,
            stockInRecordId = stockInId,
            ledgerEntryId = ledgerEntryId,
            inventoryMutationApplied = mutationApplied,
            resultingStatus = ReturnStatus.PROCESSED,
            reconciledBy = actorId,
            completedAt = nowMillis
        )
        dataSource.insertOrUpdateReconciliationResult(result)

        // 13. Record append-only audit activity event
        val auditMetadata = mutableMapOf(
            "receivingEventId" to receivingInfo.receivingEventId,
            "acceptedQty" to acceptedQty.toString(),
            "inventoryMutationApplied" to mutationApplied.toString()
        )
        if (stockInId != null) auditMetadata["stockInRecordId"] = stockInId
        if (ledgerEntryId != null) auditMetadata["ledgerEntryId"] = ledgerEntryId
        if (mutationApplied) {
            auditMetadata["warehouseId"] = warehouseId
            auditMetadata["locationId"] = locationId
        }

        val event = ReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = request.projectId,
            returnId = request.returnId,
            activityType = ReturnActivityType.RETURN_PROCESSED,
            actorId = actorId,
            actorRole = callerRole,
            timestamp = nowMillis,
            previousStatus = request.status,
            newStatus = ReturnStatus.PROCESSED,
            metadata = auditMetadata,
            notes = if (mutationApplied) {
                "Return ${request.returnNo} reconciled into inventory: $acceptedQty pcs (Stock-In: $stockInId, Ledger: $ledgerEntryId) at WH '$warehouseId' Loc '$locationId'."
            } else {
                "Return ${request.returnNo} closed out with 0 accepted quantity. No inventory restock applied."
            }
        )
        dataSource.insertActivityEvent(event)

        DomainResult.Success(result)
    }

    // =========================================================================
    // Module 11 Step 05 Operations (Customer Return Settlement)
    // =========================================================================

    override suspend fun settleReturn(
        settlement: ReturnSettlement,
        actorId: String,
        expectedVersion: Long,
        callerCustomerId: String?,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnSettlement> = mutex.withLock {
        // 1. Project isolation on caller
        if (callerProjectId != null && callerProjectId != settlement.projectId) {
            return DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' cannot settle a Return " +
                    "in project '${settlement.projectId}'."
            )
        }

        // 2. RBAC validation — Financial settlement authorized for ADMIN, MANAGER, ACCOUNTS
        if (callerRole != null) {
            val authorizedRoles = setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.ACCOUNTS)
            if (callerRole !in authorizedRoles) {
                return DomainResult.Error(
                    message = "Role '$callerRole' is unauthorized to settle Return Requests. " +
                        "Requires ADMIN, MANAGER, or ACCOUNTS."
                )
            }
        }

        // 3. Idempotency check via idempotencyKey
        val existingByIdemp = dataSource.getSettlementByIdempotencyKey(settlement.idempotencyKey)
        if (existingByIdemp != null) {
            if (existingByIdemp.returnId == settlement.returnId) {
                return DomainResult.Success(existingByIdemp)
            } else {
                return DomainResult.Error(
                    message = "Idempotency key '${settlement.idempotencyKey}' was already used for Return '${existingByIdemp.returnId}'."
                )
            }
        }

        // 4. Duplicate settlement check for this return
        val existingSettlement = dataSource.getSettlement(settlement.returnId)
        if (existingSettlement != null && existingSettlement.status == ReturnSettlementStatus.COMPLETED) {
            if (existingSettlement.idempotencyKey == settlement.idempotencyKey) {
                return DomainResult.Success(existingSettlement)
            } else {
                return DomainResult.Error(
                    message = "Return '${settlement.returnId}' is already settled."
                )
            }
        }

        // 5. Fetch target ReturnRequest
        val request = dataSource.getReturn(settlement.returnId)
            ?: return DomainResult.Error(message = "Return '${settlement.returnId}' not found.")

        // 6. Project isolation against return entity
        if (callerProjectId != null && callerProjectId != request.projectId) {
            return DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' cannot settle Return in project '${request.projectId}'."
            )
        }
        if (settlement.projectId != request.projectId) {
            return DomainResult.Error(
                message = "Settlement project '${settlement.projectId}' does not match Return project '${request.projectId}'."
            )
        }

        // 7. Customer ownership validation
        if (callerCustomerId != null && callerCustomerId != request.customerId) {
            return DomainResult.Error(
                message = "Access denied: Caller customer '$callerCustomerId' cannot settle Return belonging to customer '${request.customerId}'."
            )
        }
        if (settlement.customerId != request.customerId) {
            return DomainResult.Error(
                message = "Settlement customer '${settlement.customerId}' does not match Return customer '${request.customerId}'."
            )
        }

        // 8. Eligibility validation: only PROCESSED returns may enter financial settlement
        if (request.status != ReturnStatus.PROCESSED) {
            return DomainResult.Error(
                message = "Return '${request.returnId}' is in status '${request.status}' and cannot be settled. " +
                    "Return must be in PROCESSED status."
            )
        }

        // 9. Optimistic concurrency check
        if (request.version != expectedVersion) {
            return DomainResult.Error(
                message = "Concurrency conflict on Return '${request.returnId}': " +
                    "expected version $expectedVersion but found ${request.version}. " +
                    "Record was updated by another user. Please refresh."
            )
        }

        // 10. Amount validation
        if (settlement.amount.isNegative()) {
            return DomainResult.Error(message = "Settlement amount cannot be negative.")
        }

        // 11. Update ReturnRequest version & timestamp
        val nowMillis = System.currentTimeMillis()
        val updatedRequest = request.copy(
            version = request.version + 1,
            updatedAt = nowMillis
        )
        dataSource.updateReturn(updatedRequest)

        // 12. Persist ReturnSettlement record
        val finalSettlement = settlement.copy(
            settledAt = if (settlement.settledAt > 0) settlement.settledAt else nowMillis,
            version = 1L
        )
        dataSource.insertOrUpdateSettlement(finalSettlement)

        // 13. Record append-only audit activity event
        val auditMetadata = mutableMapOf(
            "settlementId" to finalSettlement.settlementId,
            "resolutionType" to finalSettlement.resolutionType.name,
            "amount" to finalSettlement.amount.formatted(),
            "customerId" to finalSettlement.customerId,
            "idempotencyKey" to finalSettlement.idempotencyKey
        )
        finalSettlement.creditNoteId?.let { auditMetadata["creditNoteId"] = it }
        finalSettlement.replacementOrderId?.let { auditMetadata["replacementOrderId"] = it }
        finalSettlement.reworkId?.let { auditMetadata["reworkId"] = it }

        val event = ReturnActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = request.projectId,
            returnId = request.returnId,
            activityType = ReturnActivityType.RETURN_SETTLED,
            actorId = actorId,
            actorRole = callerRole,
            timestamp = nowMillis,
            previousStatus = ReturnStatus.PROCESSED,
            newStatus = ReturnStatus.PROCESSED,
            metadata = auditMetadata,
            notes = "Return ${request.returnNo} settled via ${finalSettlement.resolutionType.displayName} for ${finalSettlement.amount.formatted()}."
        )
        dataSource.insertActivityEvent(event)

        DomainResult.Success(finalSettlement)
    }

    override suspend fun getSettlement(
        returnId: String,
        callerRole: UserRole?,
        callerProjectId: String?
    ): DomainResult<ReturnSettlement?> = mutex.withLock {
        val request = dataSource.getReturn(returnId)
            ?: return DomainResult.Error(message = "Return '$returnId' not found.")

        // Project isolation
        if (callerProjectId != null && callerProjectId != request.projectId) {
            return DomainResult.Error(
                message = "Access denied: Caller project '$callerProjectId' cannot view settlement " +
                    "in project '${request.projectId}'."
            )
        }

        // RBAC validation on view
        if (callerRole != null) {
            val auth = ReturnAuthorizationValidator.validateOperation(
                callerRole = callerRole,
                operation = ReturnOperation.VIEW_RETURN,
                targetProjectId = request.projectId,
                callerProjectId = callerProjectId
            )
            if (auth is DomainResult.Error) return auth
        }

        DomainResult.Success(dataSource.getSettlement(returnId))
    }

    override fun observeSettlement(
        returnId: String
    ): Flow<ReturnSettlement?> = dataSource.observeSettlement(returnId)

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Maps a target [ReturnStatus] to the [ReturnOperation] required to perform the transition.
     * Used for RBAC enforcement inside [transitionReturnStatus].
     */
    private fun ReturnStatus.toRequiredOperation(): ReturnOperation = when (this) {
        ReturnStatus.UNDER_INSPECTION -> ReturnOperation.INSPECT_RETURN
        ReturnStatus.APPROVED         -> ReturnOperation.APPROVE_RETURN
        ReturnStatus.REJECTED         -> ReturnOperation.REJECT_RETURN
        ReturnStatus.RETURN_RECEIVED  -> ReturnOperation.RECEIVE_RETURN
        ReturnStatus.PROCESSED        -> ReturnOperation.PROCESS_RETURN
        ReturnStatus.CANCELLED        -> ReturnOperation.CANCEL_RETURN
        ReturnStatus.REQUESTED        -> ReturnOperation.CREATE_RETURN
    }
}
