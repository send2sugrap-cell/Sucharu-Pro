package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.ProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.ProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.QcChecklistDataSource
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
import com.sucharu.sucharupro.domain.model.qc.QcDefectActivityType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ProductionDefectRepository
import com.sucharu.sucharupro.domain.validation.ProductionDefectAssignmentValidator
import com.sucharu.sucharupro.domain.validation.ProductionDefectLifecycleValidator
import com.sucharu.sucharupro.domain.validation.ProductionDefectValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Authoritative implementation of [ProductionDefectRepository] (Module 06 Step 04).
 */
class ProductionDefectRepositoryImpl(
    private val defectDataSource: ProductionDefectDataSource,
    private val qcDataSource: ProductionQcDataSource? = null,
    private val checklistDataSource: QcChecklistDataSource? = null
) : ProductionDefectRepository {

    private val repositoryMutex = Mutex()

    private suspend fun recordActivity(
        defectId: String,
        productionJobId: String,
        actorId: String? = null,
        actorName: String? = null,
        activityType: QcDefectActivityType,
        notes: String? = null,
        timestamp: String
    ) {
        val event = QcDefectActivityEvent(
            eventId = "act-def-" + UUID.randomUUID().toString(),
            defectId = defectId,
            productionJobId = productionJobId,
            actorId = actorId,
            actorName = actorName,
            activityType = activityType,
            timestamp = timestamp,
            notes = notes
        )
        defectDataSource.insertActivityEvent(event)
    }

    override fun observeDefectList(): Flow<List<ProductionDefect>> = defectDataSource.observeDefects()

    override fun observeDefectById(defectId: String): Flow<ProductionDefect?> {
        return defectDataSource.observeDefects().map { list ->
            list.find { it.defectId == defectId }
        }
    }

    override suspend fun findDefectById(defectId: String): DomainResult<ProductionDefect> {
        return defectDataSource.fetchDefectById(defectId)
    }

    override fun observeDefectsByJob(productionJobId: String): Flow<List<ProductionDefect>> {
        return defectDataSource.observeDefects().map { list ->
            list.filter { it.productionJobId == productionJobId }
        }
    }

    override fun observeDefectsByQc(qcId: String): Flow<List<ProductionDefect>> {
        return defectDataSource.observeDefects().map { list ->
            list.filter { it.qcId == qcId }
        }
    }

    override fun observeDefectsByStatus(status: DefectStatus): Flow<List<ProductionDefect>> {
        return defectDataSource.observeDefects().map { list ->
            list.filter { it.status == status }
        }
    }

    override fun observeDefectsBySeverity(severity: DefectSeverity): Flow<List<ProductionDefect>> {
        return defectDataSource.observeDefects().map { list ->
            list.filter { it.severity == severity }
        }
    }

    override suspend fun createDefect(
        productionJobId: String,
        title: String,
        description: String,
        category: DefectCategory,
        severity: DefectSeverity,
        source: DefectSource,
        affectedQuantity: Int,
        affectedUnit: String,
        productionStageId: String?,
        qcId: String?,
        inspectionChecklistId: String?,
        checklistItemId: String?,
        detectedBy: String,
        detectedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val rbac = ProductionDefectValidator.validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (qcId != null && qcDataSource != null) {
            when (val qcRes = qcDataSource.fetchQcById(qcId)) {
                is DomainResult.Success -> {
                    val isolation = ProductionDefectValidator.validateQcCrossJobIsolation(productionJobId, qcRes.data)
                    if (isolation is DomainResult.Error) return isolation
                }
                is DomainResult.Error -> return qcRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading QC record")
            }
        }

        if (inspectionChecklistId != null && checklistDataSource != null) {
            when (val chkRes = checklistDataSource.fetchInspectionChecklistById(inspectionChecklistId)) {
                is DomainResult.Success -> {
                    val isolation = ProductionDefectValidator.validateChecklistCrossJobIsolation(productionJobId, chkRes.data)
                    if (isolation is DomainResult.Error) return isolation
                }
                is DomainResult.Error -> return chkRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading Checklist")
            }
        }

        val paramsVal = ProductionDefectValidator.validateCreationParams(
            productionJobId = productionJobId,
            title = title,
            description = description,
            affectedQuantity = affectedQuantity,
            affectedUnit = affectedUnit,
            detectedBy = detectedBy,
            timestamp = timestamp,
            callerRole = callerRole
        )
        if (paramsVal is DomainResult.Error) return paramsVal

        val defectId = "def-qc-" + UUID.randomUUID().toString()
        val defect = ProductionDefect(
            defectId = defectId,
            productionJobId = productionJobId,
            productionStageId = productionStageId,
            qcId = qcId,
            inspectionChecklistId = inspectionChecklistId,
            checklistItemId = checklistItemId,
            category = category,
            severity = severity,
            source = source,
            status = DefectStatus.OPEN,
            title = title,
            description = description,
            affectedQuantity = affectedQuantity,
            affectedUnit = affectedUnit,
            detectedAt = timestamp,
            detectedBy = detectedBy,
            detectedByName = detectedByName,
            createdAt = timestamp,
            updatedAt = timestamp,
            notes = notes
        )

        val validation = ProductionDefectValidator.validateDefect(defect)
        if (validation is DomainResult.Error) return validation

        val insertRes = defectDataSource.insertDefect(defect)
        if (insertRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = productionJobId,
                actorId = detectedBy,
                actorName = detectedByName,
                activityType = QcDefectActivityType.DEFECT_CREATED,
                notes = "Logged ${severity.defaultLabel} defect: $title ($affectedQuantity $affectedUnit)",
                timestamp = timestamp
            )
        }
        return insertRes
    }

    override suspend fun updateDefect(
        defect: ProductionDefect,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val rbac = ProductionDefectValidator.validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = defectDataSource.fetchDefectById(defect.defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        if (current.isTerminal) {
            return DomainResult.Error(
                message = "Cannot modify terminal defect '${current.defectId}' (Status: ${current.status.defaultLabel})."
            )
        }

        val validation = ProductionDefectValidator.validateDefect(defect)
        if (validation is DomainResult.Error) return validation

        val updateRes = defectDataSource.updateDefect(defect)
        if (updateRes is DomainResult.Success) {
            if (defect.severity != current.severity) {
                recordActivity(
                    defectId = defect.defectId,
                    productionJobId = defect.productionJobId,
                    activityType = QcDefectActivityType.DEFECT_SEVERITY_CHANGED,
                    notes = "Severity changed from ${current.severity.defaultLabel} to ${defect.severity.defaultLabel}.",
                    timestamp = defect.updatedAt
                )
            } else {
                recordActivity(
                    defectId = defect.defectId,
                    productionJobId = defect.productionJobId,
                    activityType = QcDefectActivityType.DEFECT_UPDATED,
                    notes = "Updated defect details.",
                    timestamp = defect.updatedAt
                )
            }
        }
        return updateRes
    }

    override suspend fun changeDefectStatus(
        defectId: String,
        targetStatus: DefectStatus,
        actorId: String?,
        actorName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val rbac = ProductionDefectValidator.validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionDefectLifecycleValidator.validateStatusTransition(current, targetStatus)
        if (transition is DomainResult.Error) return transition

        if (targetStatus == DefectStatus.RESOLVED) {
            val resVal = ProductionDefectValidator.validateResolution(current, notes, actorId, callerRole)
            if (resVal is DomainResult.Error) return resVal
        }

        if (targetStatus == DefectStatus.CLOSED) {
            val closeVal = ProductionDefectValidator.validateClosure(current, actorId, callerRole)
            if (closeVal is DomainResult.Error) return closeVal
        }

        if (targetStatus == DefectStatus.CANCELLED) {
            val cancelVal = ProductionDefectLifecycleValidator.validateCancellation(current, notes)
            if (cancelVal is DomainResult.Error) return cancelVal
        }

        val updated = current.copy(
            status = targetStatus,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\n$notes".trim()
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            val actType = when (targetStatus) {
                DefectStatus.ACKNOWLEDGED -> QcDefectActivityType.DEFECT_ACKNOWLEDGED
                DefectStatus.UNDER_INVESTIGATION -> QcDefectActivityType.DEFECT_INVESTIGATION_STARTED
                DefectStatus.CONTAINED -> QcDefectActivityType.DEFECT_CONTAINED
                DefectStatus.RESOLUTION_PENDING -> QcDefectActivityType.DEFECT_RESOLUTION_STARTED
                DefectStatus.RESOLVED -> QcDefectActivityType.DEFECT_RESOLVED
                DefectStatus.CLOSED -> QcDefectActivityType.DEFECT_CLOSED
                DefectStatus.CANCELLED -> QcDefectActivityType.DEFECT_CANCELLED
                else -> QcDefectActivityType.DEFECT_STATUS_CHANGED
            }
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = actorId,
                actorName = actorName,
                activityType = actType,
                notes = "Transitioned status to ${targetStatus.defaultLabel}. ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun acknowledgeDefect(
        defectId: String,
        acknowledgedBy: String,
        acknowledgedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val rbac = ProductionDefectValidator.validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionDefectLifecycleValidator.validateStatusTransition(current, DefectStatus.ACKNOWLEDGED)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = DefectStatus.ACKNOWLEDGED,
            acknowledgedBy = acknowledgedBy,
            acknowledgedAt = timestamp,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\n$notes".trim()
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = acknowledgedBy,
                actorName = acknowledgedByName,
                activityType = QcDefectActivityType.DEFECT_ACKNOWLEDGED,
                notes = "Acknowledged defect: ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun investigateDefect(
        defectId: String,
        investigatorId: String,
        investigatorName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val rbac = ProductionDefectValidator.validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionDefectLifecycleValidator.validateStatusTransition(current, DefectStatus.UNDER_INVESTIGATION)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = DefectStatus.UNDER_INVESTIGATION,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\n$notes".trim()
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = investigatorId,
                actorName = investigatorName,
                activityType = QcDefectActivityType.DEFECT_INVESTIGATION_STARTED,
                notes = "Started investigation: ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun containDefect(
        defectId: String,
        containmentNotes: String,
        containedBy: String,
        containedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val rbac = ProductionDefectValidator.validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        if (containmentNotes.isBlank()) {
            return DomainResult.Error(message = "Containment notes cannot be blank.")
        }

        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionDefectLifecycleValidator.validateStatusTransition(current, DefectStatus.CONTAINED)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = DefectStatus.CONTAINED,
            containmentNotes = containmentNotes,
            updatedAt = timestamp
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = containedBy,
                actorName = containedByName,
                activityType = QcDefectActivityType.DEFECT_CONTAINED,
                notes = "Contained defect: $containmentNotes",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun startResolution(
        defectId: String,
        notes: String?,
        initiatedBy: String,
        initiatedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val rbac = ProductionDefectValidator.validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionDefectLifecycleValidator.validateStatusTransition(current, DefectStatus.RESOLUTION_PENDING)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = DefectStatus.RESOLUTION_PENDING,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\n$notes".trim()
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = initiatedBy,
                actorName = initiatedByName,
                activityType = QcDefectActivityType.DEFECT_RESOLUTION_STARTED,
                notes = "Resolution pending: ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun resolveDefect(
        defectId: String,
        resolutionNotes: String,
        resolvedBy: String,
        resolvedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionDefectLifecycleValidator.validateStatusTransition(current, DefectStatus.RESOLVED)
        if (transition is DomainResult.Error) return transition

        val resVal = ProductionDefectValidator.validateResolution(current, resolutionNotes, resolvedBy, callerRole)
        if (resVal is DomainResult.Error) return resVal

        val updated = current.copy(
            status = DefectStatus.RESOLVED,
            resolutionNotes = resolutionNotes,
            resolvedBy = resolvedBy,
            resolvedAt = timestamp,
            updatedAt = timestamp
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = resolvedBy,
                actorName = resolvedByName,
                activityType = QcDefectActivityType.DEFECT_RESOLVED,
                notes = "Resolved defect: $resolutionNotes",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun closeDefect(
        defectId: String,
        closedBy: String,
        closedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionDefectLifecycleValidator.validateStatusTransition(current, DefectStatus.CLOSED)
        if (transition is DomainResult.Error) return transition

        val closeVal = ProductionDefectValidator.validateClosure(current, closedBy, callerRole)
        if (closeVal is DomainResult.Error) return closeVal

        val updated = current.copy(
            status = DefectStatus.CLOSED,
            closedBy = closedBy,
            closedAt = timestamp,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\nClosed: $notes".trim()
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = closedBy,
                actorName = closedByName,
                activityType = QcDefectActivityType.DEFECT_CLOSED,
                notes = "Permanently closed defect. ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun cancelDefect(
        defectId: String,
        reason: String,
        cancelledBy: String,
        cancelledByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val rbac = ProductionDefectValidator.validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionDefectLifecycleValidator.validateStatusTransition(current, DefectStatus.CANCELLED)
        if (transition is DomainResult.Error) return transition

        val cancelVal = ProductionDefectLifecycleValidator.validateCancellation(current, reason)
        if (cancelVal is DomainResult.Error) return cancelVal

        val updated = current.copy(
            status = DefectStatus.CANCELLED,
            updatedAt = timestamp,
            notes = "${current.notes ?: ""}\nCancelled: $reason".trim()
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = cancelledBy,
                actorName = cancelledByName,
                activityType = QcDefectActivityType.DEFECT_CANCELLED,
                notes = "Cancelled defect: $reason",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun assignDefect(
        defectId: String,
        assigneeId: String,
        assigneeName: String,
        assignedBy: String,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val valRes = ProductionDefectAssignmentValidator.validateAssignment(current, assigneeId, assigneeName, callerRole)
        if (valRes is DomainResult.Error) return valRes

        val existingAssignments = defectDataSource.observeAssignments().first().filter { it.defectId == defectId && it.active }
        for (asgn in existingAssignments) {
            defectDataSource.updateAssignment(asgn.copy(active = false))
        }

        val assignmentId = "asgn-def-" + UUID.randomUUID().toString()
        val assignment = DefectAssignment(
            assignmentId = assignmentId,
            defectId = defectId,
            assigneeId = assigneeId,
            assigneeName = assigneeName,
            assignedBy = assignedBy,
            assignedAt = timestamp,
            active = true,
            reason = reason
        )
        val asgnInsert = defectDataSource.insertAssignment(assignment)
        if (asgnInsert is DomainResult.Error) return asgnInsert

        val updated = current.copy(
            assignedToId = assigneeId,
            assignedToName = assigneeName,
            updatedAt = timestamp
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = assignedBy,
                activityType = QcDefectActivityType.DEFECT_ASSIGNED,
                notes = "Assigned defect to $assigneeName ($assigneeId). ${reason ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun reassignDefect(
        defectId: String,
        newAssigneeId: String,
        newAssigneeName: String,
        reassignedBy: String,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val valRes = ProductionDefectAssignmentValidator.validateAssignment(current, newAssigneeId, newAssigneeName, callerRole)
        if (valRes is DomainResult.Error) return valRes

        val existingAssignments = defectDataSource.observeAssignments().first().filter { it.defectId == defectId && it.active }
        for (asgn in existingAssignments) {
            defectDataSource.updateAssignment(asgn.copy(active = false))
        }

        val assignmentId = "asgn-def-" + UUID.randomUUID().toString()
        val assignment = DefectAssignment(
            assignmentId = assignmentId,
            defectId = defectId,
            assigneeId = newAssigneeId,
            assigneeName = newAssigneeName,
            assignedBy = reassignedBy,
            assignedAt = timestamp,
            active = true,
            reason = reason
        )
        val asgnInsert = defectDataSource.insertAssignment(assignment)
        if (asgnInsert is DomainResult.Error) return asgnInsert

        val updated = current.copy(
            assignedToId = newAssigneeId,
            assignedToName = newAssigneeName,
            updatedAt = timestamp
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = reassignedBy,
                activityType = QcDefectActivityType.DEFECT_REASSIGNED,
                notes = "Reassigned defect to $newAssigneeName ($newAssigneeId). ${reason ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun unassignDefect(
        defectId: String,
        unassignedBy: String,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionDefect> = repositoryMutex.withLock {
        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val valRes = ProductionDefectAssignmentValidator.validateUnassignment(current, callerRole)
        if (valRes is DomainResult.Error) return valRes

        val existingAssignments = defectDataSource.observeAssignments().first().filter { it.defectId == defectId && it.active }
        for (asgn in existingAssignments) {
            defectDataSource.updateAssignment(asgn.copy(active = false))
        }

        val updated = current.copy(
            assignedToId = null,
            assignedToName = null,
            updatedAt = timestamp
        )

        val updateRes = defectDataSource.updateDefect(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                defectId = defectId,
                productionJobId = current.productionJobId,
                actorId = unassignedBy,
                activityType = QcDefectActivityType.DEFECT_UNASSIGNED,
                notes = "Unassigned defect. ${reason ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun attachEvidence(
        defectId: String,
        fileReferenceId: String?,
        fileReference: FileReference?,
        description: String?,
        attachedBy: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DefectEvidence> = repositoryMutex.withLock {
        val rbac = ProductionDefectValidator.validateDefectPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = defectDataSource.fetchDefectById(defectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        if (current.isTerminal) {
            return DomainResult.Error(
                message = "Cannot attach evidence to terminal defect '${current.defectId}' (Status: ${current.status.defaultLabel})."
            )
        }

        val evidenceId = "evi-def-" + UUID.randomUUID().toString()
        val effectiveFileRefId = fileReferenceId ?: fileReference?.fileId
        val evidence = DefectEvidence(
            evidenceId = evidenceId,
            defectId = defectId,
            fileReferenceId = effectiveFileRefId,
            fileReference = fileReference,
            description = description,
            createdBy = attachedBy,
            createdAt = timestamp
        )

        val validation = ProductionDefectValidator.validateEvidence(evidence, defectId)
        if (validation is DomainResult.Error) return validation

        val insertEvidenceRes = defectDataSource.insertEvidence(evidence)
        if (insertEvidenceRes is DomainResult.Error) return insertEvidenceRes

        val updatedDefect = current.copy(
            evidenceList = current.evidenceList + evidence,
            updatedAt = timestamp
        )
        defectDataSource.updateDefect(updatedDefect)

        recordActivity(
            defectId = defectId,
            productionJobId = current.productionJobId,
            actorId = attachedBy,
            activityType = QcDefectActivityType.DEFECT_EVIDENCE_ATTACHED,
            notes = "Attached evidence: ${description ?: effectiveFileRefId ?: ""}".trim(),
            timestamp = timestamp
        )

        return insertEvidenceRes
    }

    override fun observeAssignments(defectId: String): Flow<List<DefectAssignment>> {
        return defectDataSource.observeAssignments().map { list ->
            list.filter { it.defectId == defectId }
        }
    }

    override fun observeDefectActivity(defectId: String): Flow<List<QcDefectActivityEvent>> {
        return defectDataSource.observeActivityEvents().map { list ->
            list.filter { it.defectId == defectId }
        }
    }
}
