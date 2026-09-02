package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.ProductionQcDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.PreProductionItemStatus
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcItem
import com.sucharu.sucharupro.domain.model.qc.PreProductionQcSnapshot
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.QcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.QcActivityType
import com.sucharu.sucharupro.domain.model.qc.QcAssignment
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ProductionQcRepository
import com.sucharu.sucharupro.domain.validation.PreProductionQcValidator
import com.sucharu.sucharupro.domain.validation.ProductionQcLifecycleValidator
import com.sucharu.sucharupro.domain.validation.ProductionQcValidator
import com.sucharu.sucharupro.domain.validation.QcAssignmentValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Authoritative implementation of [ProductionQcRepository] enforcing domain validation,
 * lifecycle integrity, inspector assignment history, Pre-Production QC checklists, and atomic state transitions.
 */
class ProductionQcRepositoryImpl(
    private val dataSource: ProductionQcDataSource
) : ProductionQcRepository {

    private val repositoryMutex = Mutex()

    private suspend fun recordActivity(
        qcId: String,
        productionJobId: String,
        actorId: String? = null,
        actorName: String? = null,
        activityType: QcActivityType,
        notes: String? = null,
        timestamp: String
    ) {
        val event = QcActivityEvent(
            eventId = "act-qc-" + UUID.randomUUID().toString(),
            qcId = qcId,
            productionJobId = productionJobId,
            actorId = actorId,
            actorName = actorName,
            activityType = activityType,
            timestamp = timestamp,
            notes = notes
        )
        dataSource.insertActivityEvent(event)
    }

    override fun observeQcList(): Flow<List<ProductionQc>> = dataSource.observeQcList()

    override fun observeQcById(qcId: String): Flow<ProductionQc?> {
        return dataSource.observeQcList().map { list ->
            list.find { it.qcId == qcId }
        }
    }

    override fun getQcById(qcId: String): Flow<ProductionQc?> = observeQcById(qcId)

    override suspend fun findQcById(qcId: String): DomainResult<ProductionQc> {
        return dataSource.fetchQcById(qcId)
    }

    override fun getQcForJob(productionJobId: String): Flow<List<ProductionQc>> {
        return dataSource.observeQcList().map { list ->
            list.filter { it.productionJobId == productionJobId }
        }
    }

    override fun getQcForInspector(inspectorId: String): Flow<List<ProductionQc>> {
        return dataSource.observeQcList().map { list ->
            list.filter { it.assignedInspectorId == inspectorId }
        }
    }

    override suspend fun createQc(
        productionJobId: String,
        productionStageId: String?,
        qcType: QcType,
        notes: String?,
        createdBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionQc> = repositoryMutex.withLock {
        val validation = ProductionQcValidator.validateCreation(
            productionJobId = productionJobId,
            qcType = qcType,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val qcId = "qc-" + UUID.randomUUID().toString()
        val qc = ProductionQc(
            qcId = qcId,
            productionJobId = productionJobId,
            productionStageId = productionStageId,
            qcType = qcType,
            status = QcStatus.DRAFT,
            decision = QcDecision.PENDING,
            createdBy = createdBy,
            createdAt = timestamp,
            notes = notes,
            updatedAt = timestamp,
            updatedBy = createdBy
        )

        val entityValidation = ProductionQcValidator.validateQc(qc)
        if (entityValidation is DomainResult.Error) {
            return entityValidation
        }

        val insertResult = dataSource.insertQc(qc)
        if (insertResult is DomainResult.Success) {
            recordActivity(
                qcId = qc.qcId,
                productionJobId = qc.productionJobId,
                actorId = createdBy,
                activityType = QcActivityType.QC_CREATED,
                notes = "Created ${qcType.defaultLabel} record for Job '$productionJobId'.",
                timestamp = timestamp
            )
        }
        return insertResult
    }

    override suspend fun updateQc(qc: ProductionQc): DomainResult<ProductionQc> = repositoryMutex.withLock {
        val validation = ProductionQcValidator.validateQc(qc)
        if (validation is DomainResult.Error) {
            return validation
        }
        return dataSource.updateQc(qc)
    }

    override suspend fun assignInspector(
        qcId: String,
        inspectorId: String,
        inspectorName: String,
        assignedBy: String?,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionQc> = repositoryMutex.withLock {
        val currentQc = when (val res = dataSource.fetchQcById(qcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = QcAssignmentValidator.validateAssignment(
            qc = currentQc,
            inspectorId = inspectorId,
            inspectorName = inspectorName,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val assignmentId = "asgn-qc-" + UUID.randomUUID().toString()
        val assignment = QcAssignment(
            assignmentId = assignmentId,
            qcId = qcId,
            inspectorId = inspectorId,
            inspectorName = inspectorName,
            assignedBy = assignedBy,
            assignedAt = timestamp,
            reason = reason,
            isActive = true
        )
        dataSource.insertAssignment(assignment)

        val nextStatus = if (currentQc.status == QcStatus.DRAFT) QcStatus.PENDING_INSPECTION else currentQc.status
        val updatedQc = currentQc.copy(
            assignedInspectorId = inspectorId,
            assignedInspectorName = inspectorName,
            status = nextStatus,
            updatedAt = timestamp,
            updatedBy = assignedBy
        )

        val updateResult = dataSource.updateQc(updatedQc)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                qcId = updatedQc.qcId,
                productionJobId = updatedQc.productionJobId,
                actorId = assignedBy,
                activityType = QcActivityType.QC_ASSIGNED,
                notes = "Assigned QC Inspector '$inspectorName' ($inspectorId).",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun reassignInspector(
        qcId: String,
        newInspectorId: String,
        newInspectorName: String,
        reassignedBy: String?,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionQc> = repositoryMutex.withLock {
        val currentQc = when (val res = dataSource.fetchQcById(qcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val allAssignments = dataSource.observeAssignments().first()
        val activeAssignment = allAssignments.find { it.qcId == qcId && it.isActive }

        val validation = QcAssignmentValidator.validateReassignment(
            qc = currentQc,
            currentAssignment = activeAssignment,
            newInspectorId = newInspectorId,
            newInspectorName = newInspectorName,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        if (activeAssignment != null) {
            val updatedOld = activeAssignment.copy(
                isActive = false,
                unassignedAt = timestamp
            )
            dataSource.updateAssignment(updatedOld)
        }

        val newAssignmentId = "asgn-qc-" + UUID.randomUUID().toString()
        val newAssignment = QcAssignment(
            assignmentId = newAssignmentId,
            qcId = qcId,
            inspectorId = newInspectorId,
            inspectorName = newInspectorName,
            assignedBy = reassignedBy,
            assignedAt = timestamp,
            reason = reason,
            isActive = true
        )
        dataSource.insertAssignment(newAssignment)

        val updatedQc = currentQc.copy(
            assignedInspectorId = newInspectorId,
            assignedInspectorName = newInspectorName,
            updatedAt = timestamp,
            updatedBy = reassignedBy
        )

        val updateResult = dataSource.updateQc(updatedQc)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                qcId = updatedQc.qcId,
                productionJobId = updatedQc.productionJobId,
                actorId = reassignedBy,
                activityType = QcActivityType.QC_REASSIGNED,
                notes = "Reassigned QC Inspector from '${activeAssignment?.inspectorName ?: "None"}' to '$newInspectorName'." + (reason?.let { " Reason: $it" } ?: ""),
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun unassignInspector(
        qcId: String,
        unassignedBy: String?,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionQc> = repositoryMutex.withLock {
        val currentQc = when (val res = dataSource.fetchQcById(qcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = QcAssignmentValidator.validateUnassignment(currentQc, callerRole)
        if (validation is DomainResult.Error) {
            return validation
        }

        val allAssignments = dataSource.observeAssignments().first()
        val activeAssignment = allAssignments.find { it.qcId == qcId && it.isActive }
        if (activeAssignment != null) {
            val updatedOld = activeAssignment.copy(
                isActive = false,
                unassignedAt = timestamp
            )
            dataSource.updateAssignment(updatedOld)
        }

        val nextStatus = if (currentQc.status == QcStatus.PENDING_INSPECTION) QcStatus.DRAFT else currentQc.status
        val updatedQc = currentQc.copy(
            assignedInspectorId = null,
            assignedInspectorName = null,
            status = nextStatus,
            updatedAt = timestamp,
            updatedBy = unassignedBy
        )

        val updateResult = dataSource.updateQc(updatedQc)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                qcId = updatedQc.qcId,
                productionJobId = updatedQc.productionJobId,
                actorId = unassignedBy,
                activityType = QcActivityType.QC_UNASSIGNED,
                notes = "Unassigned QC Inspector '${activeAssignment?.inspectorName ?: "None"}'." + (reason?.let { " Reason: $it" } ?: ""),
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun startInspection(
        qcId: String,
        inspectorId: String,
        inspectorName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionQc> = repositoryMutex.withLock {
        val currentQc = when (val res = dataSource.fetchQcById(qcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val rbacResult = QcAssignmentValidator.validateInspectionPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        val transitionValidation = ProductionQcLifecycleValidator.validateStatusTransition(currentQc, QcStatus.IN_INSPECTION)
        if (transitionValidation is DomainResult.Error) {
            return transitionValidation
        }

        val updatedNotes = if (!notes.isNullOrBlank()) {
            if (currentQc.notes.isNullOrBlank()) notes else "${currentQc.notes}\n$notes"
        } else {
            currentQc.notes
        }

        val updatedQc = currentQc.copy(
            status = QcStatus.IN_INSPECTION,
            startedAt = timestamp,
            notes = updatedNotes,
            updatedAt = timestamp,
            updatedBy = inspectorName ?: inspectorId
        )

        val updateResult = dataSource.updateQc(updatedQc)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                qcId = updatedQc.qcId,
                productionJobId = updatedQc.productionJobId,
                actorId = inspectorId,
                actorName = inspectorName,
                activityType = QcActivityType.QC_INSPECTION_STARTED,
                notes = "Started QC inspection." + (notes?.let { " Notes: $it" } ?: ""),
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun completeInspection(
        qcId: String,
        decision: QcDecision,
        notes: String?,
        inspectorId: String,
        inspectorName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionQc> = repositoryMutex.withLock {
        val currentQc = when (val res = dataSource.fetchQcById(qcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val completionValidation = ProductionQcLifecycleValidator.validateCompletion(
            qc = currentQc,
            decision = decision,
            callerRole = callerRole
        )
        if (completionValidation is DomainResult.Error) {
            return completionValidation
        }

        val targetStatus = if (decision == QcDecision.PASS) QcStatus.PASSED else QcStatus.FAILED
        val updatedNotes = if (!notes.isNullOrBlank()) {
            if (currentQc.notes.isNullOrBlank()) notes else "${currentQc.notes}\n$notes"
        } else {
            currentQc.notes
        }

        val updatedQc = currentQc.copy(
            status = targetStatus,
            decision = decision,
            completedAt = timestamp,
            notes = updatedNotes,
            updatedAt = timestamp,
            updatedBy = inspectorName ?: inspectorId
        )

        val updateResult = dataSource.updateQc(updatedQc)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                qcId = updatedQc.qcId,
                productionJobId = updatedQc.productionJobId,
                actorId = inspectorId,
                actorName = inspectorName,
                activityType = QcActivityType.QC_INSPECTION_COMPLETED,
                notes = "Completed QC inspection with decision '${decision.defaultLabel}'." + (notes?.let { " Notes: $it" } ?: ""),
                timestamp = timestamp
            )
            recordActivity(
                qcId = updatedQc.qcId,
                productionJobId = updatedQc.productionJobId,
                actorId = inspectorId,
                actorName = inspectorName,
                activityType = if (decision == QcDecision.PASS) QcActivityType.QC_PASSED else QcActivityType.QC_FAILED,
                notes = "QC inspection result: ${decision.defaultLabel}.",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun cancelQc(
        qcId: String,
        reason: String,
        cancelledBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionQc> = repositoryMutex.withLock {
        val currentQc = when (val res = dataSource.fetchQcById(qcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = ProductionQcLifecycleValidator.validateCancellation(
            qc = currentQc,
            reason = reason,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        val cancellationNote = "Cancelled: $reason"
        val updatedNotes = if (currentQc.notes.isNullOrBlank()) cancellationNote else "${currentQc.notes}\n$cancellationNote"

        val updatedQc = currentQc.copy(
            status = QcStatus.CANCELLED,
            notes = updatedNotes,
            updatedAt = timestamp,
            updatedBy = cancelledBy
        )

        val updateResult = dataSource.updateQc(updatedQc)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                qcId = updatedQc.qcId,
                productionJobId = updatedQc.productionJobId,
                actorId = cancelledBy,
                activityType = QcActivityType.QC_CANCELLED,
                notes = "QC cancelled. Reason: $reason",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override fun observeAssignments(qcId: String): Flow<List<QcAssignment>> {
        return dataSource.observeAssignments().map { list ->
            list.filter { it.qcId == qcId }
        }
    }

    override fun observeActivityEvents(qcId: String): Flow<List<QcActivityEvent>> {
        return dataSource.observeActivityEvents().map { list ->
            list.filter { it.qcId == qcId }
        }
    }

    // ==========================================
    // Pre-Production QC Operations (Step 02)
    // ==========================================

    override fun observePreProductionItems(qcId: String): Flow<List<PreProductionQcItem>> {
        return dataSource.observePreProductionItems().map { list ->
            list.filter { it.qcId == qcId }
        }
    }

    override suspend fun initializePreProductionItems(
        qcId: String,
        callerRole: UserRole?
    ): DomainResult<List<PreProductionQcItem>> = repositoryMutex.withLock {
        val currentQc = when (val res = dataSource.fetchQcById(qcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = PreProductionQcValidator.validateQcTypeAndStatus(currentQc)
        if (validation is DomainResult.Error) {
            return validation
        }

        val existingItems = dataSource.observePreProductionItems().first().filter { it.qcId == qcId }
        if (existingItems.isNotEmpty()) {
            return DomainResult.Success(existingItems)
        }

        val items = PreProductionQcItem.createCanonicalItems(qcId)
        val insertResult = dataSource.insertPreProductionItems(items)
        if (insertResult is DomainResult.Success) {
            recordActivity(
                qcId = qcId,
                productionJobId = currentQc.productionJobId,
                activityType = QcActivityType.PRE_PRODUCTION_QC_ITEMS_INITIALIZED,
                notes = "Initialized ${items.size} Pre-Production QC check items.",
                timestamp = currentQc.createdAt
            )
        }
        return insertResult
    }

    override suspend fun updatePreProductionItem(
        itemId: String,
        status: PreProductionItemStatus,
        notes: String?,
        checkedBy: String,
        checkedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<PreProductionQcItem> = repositoryMutex.withLock {
        val rbacResult = PreProductionQcValidator.validateInspectionPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        val allItems = dataSource.observePreProductionItems().first()
        val item = allItems.find { it.itemId == itemId }
            ?: return DomainResult.Error(message = "Pre-Production QC item not found: $itemId")

        val currentQc = when (val res = dataSource.fetchQcById(item.qcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = PreProductionQcValidator.validateQcTypeAndStatus(currentQc)
        if (validation is DomainResult.Error) {
            return validation
        }

        val updatedItem = item.copy(
            status = status,
            notes = notes ?: item.notes,
            checkedBy = checkedBy,
            checkedByName = checkedByName,
            checkedAt = timestamp
        )

        val updateResult = dataSource.updatePreProductionItem(updatedItem)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                qcId = currentQc.qcId,
                productionJobId = currentQc.productionJobId,
                actorId = checkedBy,
                actorName = checkedByName,
                activityType = QcActivityType.PRE_PRODUCTION_QC_ITEM_UPDATED,
                notes = "Updated '${item.category.defaultLabel}' check item to ${status.defaultLabel}.",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override suspend fun submitPreProductionQc(
        qcId: String,
        decision: QcDecision,
        snapshot: PreProductionQcSnapshot?,
        submittedBy: String,
        submittedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionQc> = repositoryMutex.withLock {
        val rbacResult = PreProductionQcValidator.validateInspectionPermission(callerRole)
        if (rbacResult is DomainResult.Error) {
            return rbacResult
        }

        val currentQc = when (val res = dataSource.fetchQcById(qcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val typeValidation = PreProductionQcValidator.validateQcTypeAndStatus(currentQc)
        if (typeValidation is DomainResult.Error) {
            return typeValidation
        }

        val allItems = dataSource.observePreProductionItems().first().filter { it.qcId == qcId }
        val itemsValidation = PreProductionQcValidator.validateItemsCompletion(allItems, decision)
        if (itemsValidation is DomainResult.Error) {
            return itemsValidation
        }

        if (snapshot != null) {
            dataSource.insertSnapshot(snapshot)
        }

        val targetStatus = if (decision == QcDecision.PASS) QcStatus.PASSED else QcStatus.FAILED
        val updatedNotes = if (!notes.isNullOrBlank()) {
            if (currentQc.notes.isNullOrBlank()) notes else "${currentQc.notes}\n$notes"
        } else {
            currentQc.notes
        }

        val updatedQc = currentQc.copy(
            status = targetStatus,
            decision = decision,
            completedAt = timestamp,
            notes = updatedNotes,
            updatedAt = timestamp,
            updatedBy = submittedByName ?: submittedBy
        )

        val updateResult = dataSource.updateQc(updatedQc)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                qcId = updatedQc.qcId,
                productionJobId = updatedQc.productionJobId,
                actorId = submittedBy,
                actorName = submittedByName,
                activityType = QcActivityType.PRE_PRODUCTION_QC_SUBMITTED,
                notes = "Submitted Pre-Production QC with decision '${decision.defaultLabel}'.",
                timestamp = timestamp
            )
            recordActivity(
                qcId = updatedQc.qcId,
                productionJobId = updatedQc.productionJobId,
                actorId = submittedBy,
                actorName = submittedByName,
                activityType = if (decision == QcDecision.PASS) QcActivityType.QC_PASSED else QcActivityType.QC_FAILED,
                notes = "Pre-Production QC outcome: ${decision.defaultLabel}.",
                timestamp = timestamp
            )
        }
        return updateResult
    }

    override fun getPreProductionSnapshot(qcId: String): Flow<PreProductionQcSnapshot?> {
        return dataSource.observeSnapshots().map { list ->
            list.find { it.qcId == qcId }
        }
    }
}
