package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.ProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.ProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.QcChecklistDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReworkActivityType
import com.sucharu.sucharupro.domain.model.qc.ReworkAssignment
import com.sucharu.sucharupro.domain.model.qc.ReworkEvidence
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ProductionReworkRepository
import com.sucharu.sucharupro.domain.validation.ProductionReworkAssignmentValidator
import com.sucharu.sucharupro.domain.validation.ProductionReworkLifecycleValidator
import com.sucharu.sucharupro.domain.validation.ProductionReworkValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Authoritative implementation of [ProductionReworkRepository] (Module 06 Step 05).
 */
class ProductionReworkRepositoryImpl(
    private val reworkDataSource: ProductionReworkDataSource,
    private val defectDataSource: ProductionDefectDataSource? = null,
    private val qcDataSource: ProductionQcDataSource? = null,
    private val checklistDataSource: QcChecklistDataSource? = null
) : ProductionReworkRepository {

    private val repositoryMutex = Mutex()

    private suspend fun recordActivity(
        reworkId: String,
        productionJobId: String,
        projectId: String,
        defectId: String? = null,
        actorId: String? = null,
        actorName: String? = null,
        activityType: ReworkActivityType,
        notes: String? = null,
        timestamp: String
    ) {
        val event = ReworkActivityEvent(
            eventId = "act-rew-" + UUID.randomUUID().toString(),
            reworkId = reworkId,
            productionJobId = productionJobId,
            projectId = projectId,
            defectId = defectId,
            actorId = actorId,
            actorName = actorName,
            activityType = activityType,
            timestamp = timestamp,
            notes = notes
        )
        reworkDataSource.insertActivityEvent(event)
    }

    override fun observeReworkList(): Flow<List<ProductionRework>> = reworkDataSource.observeReworks()

    override fun observeReworkById(reworkId: String): Flow<ProductionRework?> {
        return reworkDataSource.observeReworks().map { list ->
            list.find { it.reworkId == reworkId }
        }
    }

    override suspend fun findReworkById(reworkId: String): DomainResult<ProductionRework> {
        return reworkDataSource.fetchReworkById(reworkId)
    }

    override fun observeReworksByJob(productionJobId: String): Flow<List<ProductionRework>> {
        return reworkDataSource.observeReworks().map { list ->
            list.filter { it.productionJobId == productionJobId }
        }
    }

    override fun observeReworksByProject(projectId: String): Flow<List<ProductionRework>> {
        return reworkDataSource.observeReworks().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeReworksByDefect(defectId: String): Flow<List<ProductionRework>> {
        return reworkDataSource.observeReworks().map { list ->
            list.filter { it.defectId == defectId }
        }
    }

    override fun observeReworksByStatus(status: ReworkStatus): Flow<List<ProductionRework>> {
        return reworkDataSource.observeReworks().map { list ->
            list.filter { it.status == status }
        }
    }

    override fun observeReworksByAssignee(assigneeId: String): Flow<List<ProductionRework>> {
        return reworkDataSource.observeReworks().map { list ->
            list.filter { it.assignedTo == assigneeId }
        }
    }

    override suspend fun createRework(
        projectId: String,
        productionJobId: String,
        reworkType: ReworkType,
        reason: ReworkReason,
        affectedQuantity: Int,
        quantityUnit: String,
        description: String,
        productionStageId: String?,
        qcId: String?,
        inspectionChecklistId: String?,
        defectId: String?,
        requestedBy: String,
        requestedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val rbac = ProductionReworkValidator.validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val paramsVal = ProductionReworkValidator.validateCreationParams(
            projectId = projectId,
            productionJobId = productionJobId,
            affectedQuantity = affectedQuantity,
            quantityUnit = quantityUnit,
            description = description,
            requestedBy = requestedBy,
            timestamp = timestamp,
            callerRole = callerRole
        )
        if (paramsVal is DomainResult.Error) return paramsVal

        // Defect verification & isolation
        if (defectId != null && defectDataSource != null) {
            when (val defectRes = defectDataSource.fetchDefectById(defectId)) {
                is DomainResult.Success -> {
                    val isolation = ProductionReworkValidator.validateDefectCrossJobIsolation(
                        reworkJobId = productionJobId,
                        defect = defectRes.data
                    )
                    if (isolation is DomainResult.Error) return isolation
                }
                is DomainResult.Error -> return defectRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading defect record")
            }
        }

        // Duplicate active rework check for the same defect
        if (defectId != null) {
            val existingActive = reworkDataSource.observeReworks().first().find {
                it.defectId == defectId && !it.isTerminal && !it.isReturnedToQc
            }
            if (existingActive != null) {
                return DomainResult.Error(
                    message = "An active rework request already exists for defect '$defectId' (Rework ID: ${existingActive.reworkId}, Status: ${existingActive.status.defaultLabel})."
                )
            }
        }

        // QC verification & isolation
        if (qcId != null && qcDataSource != null) {
            when (val qcRes = qcDataSource.fetchQcById(qcId)) {
                is DomainResult.Success -> {
                    val isolation = ProductionReworkValidator.validateQcCrossJobIsolation(
                        reworkJobId = productionJobId,
                        qc = qcRes.data
                    )
                    if (isolation is DomainResult.Error) return isolation
                }
                is DomainResult.Error -> return qcRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading QC record")
            }
        }

        // Checklist verification & isolation
        if (inspectionChecklistId != null && checklistDataSource != null) {
            when (val chkRes = checklistDataSource.fetchInspectionChecklistById(inspectionChecklistId)) {
                is DomainResult.Success -> {
                    val isolation = ProductionReworkValidator.validateChecklistCrossJobIsolation(
                        reworkJobId = productionJobId,
                        checklist = chkRes.data
                    )
                    if (isolation is DomainResult.Error) return isolation
                }
                is DomainResult.Error -> return chkRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading Checklist")
            }
        }

        val reworkId = "rew-" + UUID.randomUUID().toString()
        val rework = ProductionRework(
            reworkId = reworkId,
            projectId = projectId,
            productionJobId = productionJobId,
            productionStageId = productionStageId,
            qcId = qcId,
            inspectionChecklistId = inspectionChecklistId,
            defectId = defectId,
            reworkType = reworkType,
            reason = reason,
            status = ReworkStatus.REQUESTED,
            affectedQuantity = affectedQuantity,
            quantityUnit = quantityUnit,
            description = description,
            requestedBy = requestedBy,
            requestedByName = requestedByName,
            requestedAt = timestamp,
            createdAt = timestamp,
            updatedAt = timestamp,
            notes = notes
        )

        val validation = ProductionReworkValidator.validateRework(rework)
        if (validation is DomainResult.Error) return validation

        val insertRes = reworkDataSource.insertRework(rework)
        if (insertRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = productionJobId,
                projectId = projectId,
                defectId = defectId,
                actorId = requestedBy,
                actorName = requestedByName,
                activityType = ReworkActivityType.REWORK_REQUESTED,
                notes = "Created rework request for ${reworkType.defaultLabel}: $description ($affectedQuantity $quantityUnit)",
                timestamp = timestamp
            )
        }
        return insertRes
    }

    override suspend fun updateRework(
        rework: ProductionRework,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val rbac = ProductionReworkValidator.validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = reworkDataSource.fetchReworkById(rework.reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        if (current.isTerminal) {
            return DomainResult.Error(
                message = "Cannot modify terminal rework '${current.reworkId}' (Status: ${current.status.defaultLabel})."
            )
        }

        if (current.isReturnedToQc) {
            return DomainResult.Error(
                message = "Cannot modify rework '${current.reworkId}' after it has been returned to QC."
            )
        }

        val validation = ProductionReworkValidator.validateRework(rework)
        if (validation is DomainResult.Error) return validation

        val updateRes = reworkDataSource.updateRework(rework)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = rework.reworkId,
                productionJobId = rework.productionJobId,
                projectId = rework.projectId,
                defectId = rework.defectId,
                activityType = ReworkActivityType.REWORK_UPDATED,
                notes = "Updated rework details.",
                timestamp = rework.updatedAt
            )
        }
        return updateRes
    }

    override suspend fun changeReworkStatus(
        reworkId: String,
        targetStatus: ReworkStatus,
        actorId: String?,
        actorName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionReworkLifecycleValidator.validateStatusTransition(current, targetStatus)
        if (transition is DomainResult.Error) return transition

        if (targetStatus == ReworkStatus.UNDER_REVIEW) {
            val rbac = ProductionReworkValidator.validateApprovalPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }

        if (targetStatus == ReworkStatus.APPROVED) {
            val rbac = ProductionReworkValidator.validateApprovalPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
        }

        if (targetStatus == ReworkStatus.REJECTED) {
            val rbac = ProductionReworkValidator.validateApprovalPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
            val rejVal = ProductionReworkLifecycleValidator.validateRejection(current, notes)
            if (rejVal is DomainResult.Error) return rejVal
        }

        if (targetStatus == ReworkStatus.CANCELLED) {
            val rbac = ProductionReworkValidator.validateMutationPermission(callerRole)
            if (rbac is DomainResult.Error) return rbac
            val cancelVal = ProductionReworkLifecycleValidator.validateCancellation(current, notes)
            if (cancelVal is DomainResult.Error) return cancelVal
        }

        val updated = current.copy(
            status = targetStatus,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\n$notes".trim()
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            val actType = when (targetStatus) {
                ReworkStatus.UNDER_REVIEW -> ReworkActivityType.REWORK_REVIEW_STARTED
                ReworkStatus.APPROVED -> ReworkActivityType.REWORK_APPROVED
                ReworkStatus.REJECTED -> ReworkActivityType.REWORK_REJECTED
                ReworkStatus.IN_PROGRESS -> ReworkActivityType.REWORK_STARTED
                ReworkStatus.COMPLETED -> ReworkActivityType.REWORK_COMPLETED
                ReworkStatus.RETURNED_TO_QC -> ReworkActivityType.REWORK_RETURNED_TO_QC
                ReworkStatus.CANCELLED -> ReworkActivityType.REWORK_CANCELLED
                else -> ReworkActivityType.REWORK_UPDATED
            }
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = actorId,
                actorName = actorName,
                activityType = actType,
                notes = "Transitioned status to ${targetStatus.defaultLabel}. ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun startReview(
        reworkId: String,
        reviewerId: String,
        reviewerName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val rbac = ProductionReworkValidator.validateApprovalPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionReworkLifecycleValidator.validateStatusTransition(current, ReworkStatus.UNDER_REVIEW)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = ReworkStatus.UNDER_REVIEW,
            reviewedBy = reviewerId,
            reviewedByName = reviewerName,
            reviewedAt = timestamp,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\nReview: $notes".trim()
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = reviewerId,
                actorName = reviewerName,
                activityType = ReworkActivityType.REWORK_REVIEW_STARTED,
                notes = "Review started: ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun approveRework(
        reworkId: String,
        approvedBy: String,
        approvedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val rbac = ProductionReworkValidator.validateApprovalPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionReworkLifecycleValidator.validateStatusTransition(current, ReworkStatus.APPROVED)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = ReworkStatus.APPROVED,
            reviewedBy = approvedBy,
            reviewedByName = approvedByName,
            reviewedAt = timestamp,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\nApproved: $notes".trim()
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = approvedBy,
                actorName = approvedByName,
                activityType = ReworkActivityType.REWORK_APPROVED,
                notes = "Approved rework. ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun rejectRework(
        reworkId: String,
        reason: String,
        rejectedBy: String,
        rejectedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val rbac = ProductionReworkValidator.validateApprovalPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionReworkLifecycleValidator.validateStatusTransition(current, ReworkStatus.REJECTED)
        if (transition is DomainResult.Error) return transition

        val rejVal = ProductionReworkLifecycleValidator.validateRejection(current, reason)
        if (rejVal is DomainResult.Error) return rejVal

        val updated = current.copy(
            status = ReworkStatus.REJECTED,
            reviewedBy = rejectedBy,
            reviewedByName = rejectedByName,
            reviewedAt = timestamp,
            updatedAt = timestamp,
            notes = "${current.notes ?: ""}\nRejected: $reason".trim()
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = rejectedBy,
                actorName = rejectedByName,
                activityType = ReworkActivityType.REWORK_REJECTED,
                notes = "Rejected rework: $reason",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun assignRework(
        reworkId: String,
        assignedTo: String,
        assignedToName: String,
        assignedBy: String,
        assignedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val valRes = ProductionReworkAssignmentValidator.validateAssignment(
            rework = current,
            assigneeId = assignedTo,
            assigneeName = assignedToName,
            callerRole = callerRole
        )
        if (valRes is DomainResult.Error) return valRes

        val existingAssignments = reworkDataSource.observeAssignments().first().filter { it.reworkId == reworkId && it.active }
        for (asgn in existingAssignments) {
            reworkDataSource.updateAssignment(asgn.copy(active = false, unassignedAt = timestamp))
        }

        val assignmentId = "asgn-rew-" + UUID.randomUUID().toString()
        val assignment = ReworkAssignment(
            assignmentId = assignmentId,
            reworkId = reworkId,
            assignedTo = assignedTo,
            assignedToName = assignedToName,
            assignedBy = assignedBy,
            assignedByName = assignedByName,
            assignedAt = timestamp,
            active = true,
            notes = notes
        )
        val asgnInsert = reworkDataSource.insertAssignment(assignment)
        if (asgnInsert is DomainResult.Error) return asgnInsert

        val updated = current.copy(
            status = ReworkStatus.ASSIGNED,
            assignedTo = assignedTo,
            assignedToName = assignedToName,
            assignedAt = timestamp,
            updatedAt = timestamp
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = assignedBy,
                actorName = assignedByName,
                activityType = ReworkActivityType.REWORK_ASSIGNED,
                notes = "Assigned rework to $assignedToName ($assignedTo). ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun reassignRework(
        reworkId: String,
        newAssignedTo: String,
        newAssignedToName: String,
        reassignedBy: String,
        reassignedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val valRes = ProductionReworkAssignmentValidator.validateAssignment(
            rework = current,
            assigneeId = newAssignedTo,
            assigneeName = newAssignedToName,
            callerRole = callerRole
        )
        if (valRes is DomainResult.Error) return valRes

        val existingAssignments = reworkDataSource.observeAssignments().first().filter { it.reworkId == reworkId && it.active }
        for (asgn in existingAssignments) {
            reworkDataSource.updateAssignment(asgn.copy(active = false, unassignedAt = timestamp))
        }

        val assignmentId = "asgn-rew-" + UUID.randomUUID().toString()
        val assignment = ReworkAssignment(
            assignmentId = assignmentId,
            reworkId = reworkId,
            assignedTo = newAssignedTo,
            assignedToName = newAssignedToName,
            assignedBy = reassignedBy,
            assignedByName = reassignedByName,
            assignedAt = timestamp,
            active = true,
            notes = notes
        )
        val asgnInsert = reworkDataSource.insertAssignment(assignment)
        if (asgnInsert is DomainResult.Error) return asgnInsert

        val updated = current.copy(
            status = ReworkStatus.ASSIGNED,
            assignedTo = newAssignedTo,
            assignedToName = newAssignedToName,
            assignedAt = timestamp,
            updatedAt = timestamp
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = reassignedBy,
                actorName = reassignedByName,
                activityType = ReworkActivityType.REWORK_REASSIGNED,
                notes = "Reassigned rework to $newAssignedToName ($newAssignedTo). ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun unassignRework(
        reworkId: String,
        unassignedBy: String,
        unassignedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val valRes = ProductionReworkAssignmentValidator.validateUnassignment(current, callerRole)
        if (valRes is DomainResult.Error) return valRes

        val existingAssignments = reworkDataSource.observeAssignments().first().filter { it.reworkId == reworkId && it.active }
        for (asgn in existingAssignments) {
            reworkDataSource.updateAssignment(asgn.copy(active = false, unassignedAt = timestamp))
        }

        val updated = current.copy(
            status = ReworkStatus.APPROVED,
            assignedTo = null,
            assignedToName = null,
            assignedAt = null,
            updatedAt = timestamp
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = unassignedBy,
                actorName = unassignedByName,
                activityType = ReworkActivityType.REWORK_UNASSIGNED,
                notes = "Unassigned rework. ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun startRework(
        reworkId: String,
        startedBy: String,
        startedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val rbac = ProductionReworkValidator.validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionReworkLifecycleValidator.validateStatusTransition(current, ReworkStatus.IN_PROGRESS)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = ReworkStatus.IN_PROGRESS,
            startedAt = timestamp,
            updatedAt = timestamp
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = startedBy,
                actorName = startedByName,
                activityType = ReworkActivityType.REWORK_STARTED,
                notes = "Rework execution started.",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun completeRework(
        reworkId: String,
        correctiveAction: String,
        actualReworkedQuantity: Int,
        completedBy: String,
        completedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val compVal = ProductionReworkValidator.validateCompletion(
            rework = current,
            correctiveAction = correctiveAction,
            actualReworkedQuantity = actualReworkedQuantity,
            completedBy = completedBy,
            callerRole = callerRole
        )
        if (compVal is DomainResult.Error) return compVal

        val transition = ProductionReworkLifecycleValidator.validateStatusTransition(current, ReworkStatus.COMPLETED)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = ReworkStatus.COMPLETED,
            correctiveAction = correctiveAction,
            actualReworkedQuantity = actualReworkedQuantity,
            completedAt = timestamp,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\nCompleted: $notes".trim()
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = completedBy,
                actorName = completedByName,
                activityType = ReworkActivityType.REWORK_COMPLETED,
                notes = "Completed rework: $correctiveAction ($actualReworkedQuantity ${current.quantityUnit})",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun returnToQc(
        reworkId: String,
        returnedBy: String,
        returnedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val returnVal = ProductionReworkValidator.validateReturnToQc(
            rework = current,
            actorId = returnedBy,
            callerRole = callerRole
        )
        if (returnVal is DomainResult.Error) return returnVal

        val transition = ProductionReworkLifecycleValidator.validateStatusTransition(current, ReworkStatus.RETURNED_TO_QC)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            status = ReworkStatus.RETURNED_TO_QC,
            returnedToQcAt = timestamp,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\nReturned to QC: $notes".trim()
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = returnedBy,
                actorName = returnedByName,
                activityType = ReworkActivityType.REWORK_RETURNED_TO_QC,
                notes = "Rework returned to QC for Re-QC inspection. ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun cancelRework(
        reworkId: String,
        reason: String,
        cancelledBy: String,
        cancelledByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ProductionRework> = repositoryMutex.withLock {
        val rbac = ProductionReworkValidator.validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val transition = ProductionReworkLifecycleValidator.validateStatusTransition(current, ReworkStatus.CANCELLED)
        if (transition is DomainResult.Error) return transition

        val cancelVal = ProductionReworkLifecycleValidator.validateCancellation(current, reason)
        if (cancelVal is DomainResult.Error) return cancelVal

        val updated = current.copy(
            status = ReworkStatus.CANCELLED,
            updatedAt = timestamp,
            notes = "${current.notes ?: ""}\nCancelled: $reason".trim()
        )

        val updateRes = reworkDataSource.updateRework(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = cancelledBy,
                actorName = cancelledByName,
                activityType = ReworkActivityType.REWORK_CANCELLED,
                notes = "Cancelled rework: $reason",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun attachEvidence(
        reworkId: String,
        fileReferenceId: String?,
        fileReference: FileReference?,
        description: String?,
        attachedBy: String,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReworkEvidence> = repositoryMutex.withLock {
        val rbac = ProductionReworkValidator.validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = reworkDataSource.fetchReworkById(reworkId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        if (current.isTerminal) {
            return DomainResult.Error(
                message = "Cannot attach evidence to terminal rework '${current.reworkId}' (Status: ${current.status.defaultLabel})."
            )
        }

        if (current.isReturnedToQc) {
            return DomainResult.Error(
                message = "Cannot attach evidence to rework '${current.reworkId}' after it has been returned to QC."
            )
        }

        val evidenceId = "evi-rew-" + UUID.randomUUID().toString()
        val effectiveFileRefId = fileReferenceId ?: fileReference?.fileId
        val evidence = ReworkEvidence(
            evidenceId = evidenceId,
            reworkId = reworkId,
            fileReferenceId = effectiveFileRefId,
            fileReference = fileReference,
            description = description,
            createdBy = attachedBy,
            createdAt = timestamp
        )

        val validation = ProductionReworkValidator.validateEvidence(evidence, reworkId)
        if (validation is DomainResult.Error) return validation

        val insertRes = reworkDataSource.insertEvidence(evidence)
        if (insertRes is DomainResult.Success) {
            val updatedReferences = if (fileReference != null) {
                current.evidenceReferences + fileReference
            } else {
                current.evidenceReferences
            }
            reworkDataSource.updateRework(current.copy(evidenceReferences = updatedReferences, updatedAt = timestamp))

            recordActivity(
                reworkId = reworkId,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                defectId = current.defectId,
                actorId = attachedBy,
                activityType = ReworkActivityType.REWORK_EVIDENCE_ATTACHED,
                notes = "Attached evidence: ${description ?: effectiveFileRefId ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return insertRes
    }

    override fun observeAssignments(reworkId: String): Flow<List<ReworkAssignment>> {
        return reworkDataSource.observeAssignments().map { list ->
            list.filter { it.reworkId == reworkId }
        }
    }

    override fun observeReworkActivity(reworkId: String): Flow<List<ReworkActivityEvent>> {
        return reworkDataSource.observeActivityEvents().map { list ->
            list.filter { it.reworkId == reworkId }
        }
    }

    override fun observeEvidence(reworkId: String): Flow<List<ReworkEvidence>> {
        return reworkDataSource.observeEvidence().map { list ->
            list.filter { it.reworkId == reworkId }
        }
    }
}
