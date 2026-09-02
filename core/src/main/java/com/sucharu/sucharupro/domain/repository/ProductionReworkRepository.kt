package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReworkAssignment
import com.sucharu.sucharupro.domain.model.qc.ReworkEvidence
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for QC Rework Management & Workflow in Sucharu Pro ERP (Module 06 Step 05).
 */
interface ProductionReworkRepository {

    /** Reactive stream of all rework records. */
    fun observeReworkList(): Flow<List<ProductionRework>>

    /** Reactive stream of a single rework record by [reworkId]. */
    fun observeReworkById(reworkId: String): Flow<ProductionRework?>

    /** Convenience alias for observing a single rework record. */
    fun getReworkById(reworkId: String): Flow<ProductionRework?> = observeReworkById(reworkId)

    /** Direct one-shot lookup of a rework record by [reworkId]. */
    suspend fun findReworkById(reworkId: String): DomainResult<ProductionRework>

    /** Reactive stream of rework records for a specific [productionJobId]. */
    fun observeReworksByJob(productionJobId: String): Flow<List<ProductionRework>>

    /** Reactive stream of rework records for a specific [projectId]. */
    fun observeReworksByProject(projectId: String): Flow<List<ProductionRework>>

    /** Reactive stream of rework records originating from a specific [defectId]. */
    fun observeReworksByDefect(defectId: String): Flow<List<ProductionRework>>

    /** Reactive stream of rework records filtered by [status]. */
    fun observeReworksByStatus(status: ReworkStatus): Flow<List<ProductionRework>>

    /** Reactive stream of rework records filtered by assignee ID. */
    fun observeReworksByAssignee(assigneeId: String): Flow<List<ProductionRework>>

    /**
     * Creates and registers a new [ProductionRework] request.
     */
    suspend fun createRework(
        projectId: String,
        productionJobId: String,
        reworkType: ReworkType,
        reason: ReworkReason,
        affectedQuantity: Int,
        quantityUnit: String = "pcs",
        description: String,
        productionStageId: String? = null,
        qcId: String? = null,
        inspectionChecklistId: String? = null,
        defectId: String? = null,
        requestedBy: String,
        requestedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Updates an existing rework's details after validation.
     */
    suspend fun updateRework(
        rework: ProductionRework,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Transitions rework status through the state machine.
     */
    suspend fun changeReworkStatus(
        reworkId: String,
        targetStatus: ReworkStatus,
        actorId: String? = null,
        actorName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Initiates formal review (transitions from REQUESTED to UNDER_REVIEW).
     */
    suspend fun startReview(
        reworkId: String,
        reviewerId: String,
        reviewerName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Formally approves a rework request (transitions to APPROVED).
     */
    suspend fun approveRework(
        reworkId: String,
        approvedBy: String,
        approvedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Rejects a rework request (transitions to terminal REJECTED).
     */
    suspend fun rejectRework(
        reworkId: String,
        reason: String,
        rejectedBy: String,
        rejectedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Assigns rework to a designated operator/technician (transitions to ASSIGNED).
     */
    suspend fun assignRework(
        reworkId: String,
        assignedTo: String,
        assignedToName: String,
        assignedBy: String,
        assignedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Reassigns rework to a new operator/technician.
     */
    suspend fun reassignRework(
        reworkId: String,
        newAssignedTo: String,
        newAssignedToName: String,
        reassignedBy: String,
        reassignedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Unassigns rework (clears active assignee and reverts to APPROVED).
     */
    suspend fun unassignRework(
        reworkId: String,
        unassignedBy: String,
        unassignedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Starts execution of corrective rework (transitions from ASSIGNED to IN_PROGRESS).
     */
    suspend fun startRework(
        reworkId: String,
        startedBy: String,
        startedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Marks rework execution as completed (transitions from IN_PROGRESS to COMPLETED).
     */
    suspend fun completeRework(
        reworkId: String,
        correctiveAction: String,
        actualReworkedQuantity: Int,
        completedBy: String,
        completedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Hands off completed rework to QC for subsequent Re-QC (transitions to RETURNED_TO_QC).
     */
    suspend fun returnToQc(
        reworkId: String,
        returnedBy: String,
        returnedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Cancels a rework with mandatory reason (transitions to terminal CANCELLED).
     */
    suspend fun cancelRework(
        reworkId: String,
        reason: String,
        cancelledBy: String,
        cancelledByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionRework>

    /**
     * Attaches supporting evidence to a rework.
     */
    suspend fun attachEvidence(
        reworkId: String,
        fileReferenceId: String? = null,
        fileReference: FileReference? = null,
        description: String? = null,
        attachedBy: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ReworkEvidence>

    /** Reactive stream of assignment history records for a [reworkId]. */
    fun observeAssignments(reworkId: String): Flow<List<ReworkAssignment>>

    /** Reactive stream of audit activity events for a [reworkId]. */
    fun observeReworkActivity(reworkId: String): Flow<List<ReworkActivityEvent>>

    /** Reactive stream of attached evidence for a [reworkId]. */
    fun observeEvidence(reworkId: String): Flow<List<ReworkEvidence>>
}
