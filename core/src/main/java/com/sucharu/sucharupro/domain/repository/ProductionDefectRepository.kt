package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.qc.DefectAssignment
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectEvidence
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.QcDefectActivityEvent
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for QC Defect & Failure Management in Sucharu Pro ERP (Module 06 Step 04).
 */
interface ProductionDefectRepository {

    /** Reactive stream of all defects. */
    fun observeDefectList(): Flow<List<ProductionDefect>>

    /** Reactive stream of a single defect by [defectId]. */
    fun observeDefectById(defectId: String): Flow<ProductionDefect?>

    /** Convenience alias for observing single defect. */
    fun getDefectById(defectId: String): Flow<ProductionDefect?> = observeDefectById(defectId)

    /** Direct one-shot lookup of a defect by [defectId]. */
    suspend fun findDefectById(defectId: String): DomainResult<ProductionDefect>

    /** Reactive stream of defects for a specific [productionJobId]. */
    fun observeDefectsByJob(productionJobId: String): Flow<List<ProductionDefect>>

    /** Reactive stream of defects originating from a specific [qcId]. */
    fun observeDefectsByQc(qcId: String): Flow<List<ProductionDefect>>

    /** Reactive stream of defects filtered by [status]. */
    fun observeDefectsByStatus(status: DefectStatus): Flow<List<ProductionDefect>>

    /** Reactive stream of defects filtered by [severity]. */
    fun observeDefectsBySeverity(severity: DefectSeverity): Flow<List<ProductionDefect>>

    /**
     * Creates and registers a new [ProductionDefect].
     */
    suspend fun createDefect(
        productionJobId: String,
        title: String,
        description: String,
        category: DefectCategory,
        severity: DefectSeverity,
        source: DefectSource,
        affectedQuantity: Int,
        affectedUnit: String = "pcs",
        productionStageId: String? = null,
        qcId: String? = null,
        inspectionChecklistId: String? = null,
        checklistItemId: String? = null,
        detectedBy: String,
        detectedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Updates an existing defect's details after validation.
     */
    suspend fun updateDefect(
        defect: ProductionDefect,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Transitions defect status through the state machine.
     */
    suspend fun changeDefectStatus(
        defectId: String,
        targetStatus: DefectStatus,
        actorId: String? = null,
        actorName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Acknowledges a logged defect (transitions to ACKNOWLEDGED).
     */
    suspend fun acknowledgeDefect(
        defectId: String,
        acknowledgedBy: String,
        acknowledgedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Initiates investigation (transitions to UNDER_INVESTIGATION).
     */
    suspend fun investigateDefect(
        defectId: String,
        investigatorId: String,
        investigatorName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Applies immediate containment (transitions to CONTAINED).
     */
    suspend fun containDefect(
        defectId: String,
        containmentNotes: String,
        containedBy: String,
        containedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Sets status to RESOLUTION_PENDING.
     */
    suspend fun startResolution(
        defectId: String,
        notes: String? = null,
        initiatedBy: String,
        initiatedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Resolves a defect with mandatory corrective resolution notes (transitions to RESOLVED).
     */
    suspend fun resolveDefect(
        defectId: String,
        resolutionNotes: String,
        resolvedBy: String,
        resolvedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Formally closes a resolved defect (transitions to terminal CLOSED).
     */
    suspend fun closeDefect(
        defectId: String,
        closedBy: String,
        closedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Cancels a defect with mandatory reason (transitions to terminal CANCELLED).
     */
    suspend fun cancelDefect(
        defectId: String,
        reason: String,
        cancelledBy: String,
        cancelledByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Assigns defect ownership to an inspector/technician.
     */
    suspend fun assignDefect(
        defectId: String,
        assigneeId: String,
        assigneeName: String,
        assignedBy: String,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Reassigns defect ownership to a new inspector/technician.
     */
    suspend fun reassignDefect(
        defectId: String,
        newAssigneeId: String,
        newAssigneeName: String,
        reassignedBy: String,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Removes active defect assignment.
     */
    suspend fun unassignDefect(
        defectId: String,
        unassignedBy: String,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionDefect>

    /**
     * Attaches supporting evidence to a defect.
     */
    suspend fun attachEvidence(
        defectId: String,
        fileReferenceId: String? = null,
        fileReference: FileReference? = null,
        description: String? = null,
        attachedBy: String,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DefectEvidence>

    /** Reactive stream of assignment records for a [defectId]. */
    fun observeAssignments(defectId: String): Flow<List<DefectAssignment>>

    /** Reactive stream of audit activity events for a [defectId]. */
    fun observeDefectActivity(defectId: String): Flow<List<QcDefectActivityEvent>>
}
