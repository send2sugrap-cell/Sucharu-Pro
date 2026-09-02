package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcItem
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcSnapshot
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcAssignment
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Quality Control Foundation in Sucharu Pro ERP (Module 06).
 */
interface ProductionQcRepository {

    /** Reactive stream of all QC records. */
    fun observeQcList(): Flow<List<ProductionQc>>

    /** Reactive stream observing a single QC record by [qcId]. */
    fun observeQcById(qcId: String): Flow<ProductionQc?>

    /** Convenience reactive lookup. */
    fun getQcById(qcId: String): Flow<ProductionQc?>

    /** Direct one-shot lookup of a QC record by [qcId]. */
    suspend fun findQcById(qcId: String): DomainResult<ProductionQc>

    /** Reactive stream of QC records for a specific [productionJobId]. */
    fun getQcForJob(productionJobId: String): Flow<List<ProductionQc>>

    /** Reactive stream of QC records assigned to an [inspectorId]. */
    fun getQcForInspector(inspectorId: String): Flow<List<ProductionQc>>

    /**
     * Initializes and persists a new [ProductionQc] aggregate.
     */
    suspend fun createQc(
        productionJobId: String,
        productionStageId: String? = null,
        qcType: QcType,
        notes: String? = null,
        createdBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionQc>

    /**
     * Updates an existing [ProductionQc] after domain validation.
     */
    suspend fun updateQc(qc: ProductionQc): DomainResult<ProductionQc>

    /**
     * Assigns a QC inspector to an active QC record.
     */
    suspend fun assignInspector(
        qcId: String,
        inspectorId: String,
        inspectorName: String,
        assignedBy: String? = null,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionQc>

    /**
     * Reassigns an active QC record to a new inspector, preserving assignment history.
     */
    suspend fun reassignInspector(
        qcId: String,
        newInspectorId: String,
        newInspectorName: String,
        reassignedBy: String? = null,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionQc>

    /**
     * Removes the active inspector assignment.
     */
    suspend fun unassignInspector(
        qcId: String,
        unassignedBy: String? = null,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionQc>

    /**
     * Transitions status to IN_INSPECTION.
     */
    suspend fun startInspection(
        qcId: String,
        inspectorId: String,
        inspectorName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionQc>

    /**
     * Finalizes inspection with a PASS or FAIL decision.
     */
    suspend fun completeInspection(
        qcId: String,
        decision: QcDecision,
        notes: String? = null,
        inspectorId: String,
        inspectorName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionQc>

    /**
     * Cancels an active QC record.
     */
    suspend fun cancelQc(
        qcId: String,
        reason: String,
        cancelledBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionQc>

    /** Reactive stream of assignment history for a [qcId]. */
    fun observeAssignments(qcId: String): Flow<List<QcAssignment>>

    /** Reactive stream of audit activity events for a [qcId]. */
    fun observeActivityEvents(qcId: String): Flow<List<QcActivityEvent>>

    // ==========================================
    // Pre-Production QC Operations (Step 02)
    // ==========================================

    /** Reactive stream of Pre-Production check items for a [qcId]. */
    fun observePreProductionItems(qcId: String): Flow<List<PreProductionQcItem>>

    /** Convenience alias for observing items. */
    fun getPreProductionItems(qcId: String): Flow<List<PreProductionQcItem>> = observePreProductionItems(qcId)

    /**
     * Initializes canonical Pre-Production check items if not already initialized.
     */
    suspend fun initializePreProductionItems(
        qcId: String,
        callerRole: UserRole? = null
    ): DomainResult<List<PreProductionQcItem>>

    /**
     * Updates an individual Pre-Production check item result.
     */
    suspend fun updatePreProductionItem(
        itemId: String,
        status: PreProductionItemStatus,
        notes: String? = null,
        checkedBy: String,
        checkedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<PreProductionQcItem>

    /**
     * Submits the final Pre-Production QC decision along with an immutable specification snapshot.
     */
    suspend fun submitPreProductionQc(
        qcId: String,
        decision: QcDecision,
        snapshot: PreProductionQcSnapshot? = null,
        submittedBy: String,
        submittedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<ProductionQc>

    /** Reactive stream observing the specification snapshot for a [qcId]. */
    fun getPreProductionSnapshot(qcId: String): Flow<PreProductionQcSnapshot?>
}
