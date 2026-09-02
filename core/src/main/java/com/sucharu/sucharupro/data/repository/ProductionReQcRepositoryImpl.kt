package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.ProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.ProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.QcChecklistDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.ReQcActivityType
import com.sucharu.sucharupro.domain.model.qc.ReQcCycleType
import com.sucharu.sucharupro.domain.model.qc.ReQcDecision
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureReason
import com.sucharu.sucharupro.domain.model.qc.ReQcFailureRecord
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.ProductionReQcRepository
import com.sucharu.sucharupro.domain.validation.ReQcAssignmentValidator
import com.sucharu.sucharupro.domain.validation.ReQcCycleValidator
import com.sucharu.sucharupro.domain.validation.ReQcLifecycleValidator
import com.sucharu.sucharupro.domain.validation.ReQcValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Authoritative implementation of [ProductionReQcRepository] (Module 06 Step 06).
 */
class ProductionReQcRepositoryImpl(
    private val reQcDataSource: ProductionReQcDataSource,
    private val reworkDataSource: ProductionReworkDataSource? = null,
    private val defectDataSource: ProductionDefectDataSource? = null,
    private val qcDataSource: ProductionQcDataSource? = null,
    private val checklistDataSource: QcChecklistDataSource? = null
) : ProductionReQcRepository {

    private val repositoryMutex = Mutex()

    private suspend fun recordActivity(
        reQcId: String,
        cycleNumber: Int,
        productionJobId: String,
        projectId: String,
        relatedDefectId: String? = null,
        relatedReworkId: String? = null,
        actorId: String? = null,
        actorName: String? = null,
        role: UserRole? = null,
        activityType: ReQcActivityType,
        notes: String? = null,
        timestamp: String
    ) {
        val event = ReQcActivityEvent(
            eventId = "act-reqc-" + UUID.randomUUID().toString(),
            reQcId = reQcId,
            cycleNumber = cycleNumber,
            productionJobId = productionJobId,
            projectId = projectId,
            relatedDefectId = relatedDefectId,
            relatedReworkId = relatedReworkId,
            actorId = actorId,
            actorName = actorName,
            role = role,
            activityType = activityType,
            notes = notes,
            timestamp = timestamp
        )
        reQcDataSource.insertActivityEvent(event)
    }

    override fun observeReQcList(): Flow<List<ReQcInspection>> = reQcDataSource.observeReQcList()

    override fun observeReQcById(reQcId: String): Flow<ReQcInspection?> {
        return reQcDataSource.observeReQcList().map { list ->
            list.find { it.reQcId == reQcId }
        }
    }

    override suspend fun findReQcById(reQcId: String): DomainResult<ReQcInspection> {
        return reQcDataSource.fetchReQcById(reQcId)
    }

    override fun observeReQcByJob(productionJobId: String): Flow<List<ReQcInspection>> {
        return reQcDataSource.observeReQcList().map { list ->
            list.filter { it.productionJobId == productionJobId }
        }
    }

    override fun observeReQcByProject(projectId: String): Flow<List<ReQcInspection>> {
        return reQcDataSource.observeReQcList().map { list ->
            list.filter { it.projectId == projectId }
        }
    }

    override fun observeReQcByRework(reworkId: String): Flow<List<ReQcInspection>> {
        return reQcDataSource.observeReQcList().map { list ->
            list.filter { it.productionReworkId == reworkId }
        }
    }

    override fun observeReQcCycles(productionJobId: String): Flow<List<ReQcInspection>> {
        return reQcDataSource.observeReQcList().map { list ->
            list.filter { it.productionJobId == productionJobId }
                .sortedBy { it.cycleNumber }
        }
    }

    override suspend fun getLatestReQcCycle(productionJobId: String): DomainResult<ReQcInspection?> {
        val list = reQcDataSource.observeReQcList().first()
            .filter { it.productionJobId == productionJobId }
            .sortedBy { it.cycleNumber }
        return DomainResult.Success(list.lastOrNull())
    }

    override suspend fun createReQc(
        projectId: String,
        productionJobId: String,
        productionReworkId: String,
        cycleType: ReQcCycleType,
        originalQcId: String?,
        originalDefectId: String?,
        checklistId: String?,
        affectedQuantity: Int?,
        quantityUnit: String,
        createdBy: String,
        createdByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcInspection> = repositoryMutex.withLock {
        val rbac = ReQcValidator.validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val paramsVal = ReQcValidator.validateCreationParams(
            projectId = projectId,
            productionJobId = productionJobId,
            productionReworkId = productionReworkId,
            cycleNumber = 1,
            createdBy = createdBy,
            timestamp = timestamp,
            callerRole = callerRole
        )
        if (paramsVal is DomainResult.Error) return paramsVal

        // Validate source rework
        if (reworkDataSource != null) {
            when (val reworkRes = reworkDataSource.fetchReworkById(productionReworkId)) {
                is DomainResult.Success -> {
                    val rework = reworkRes.data
                    val reworkVal = ReQcValidator.validateSourceRework(productionJobId, rework)
                    if (reworkVal is DomainResult.Error) return reworkVal
                    val projVal = ReQcValidator.validateReworkCrossProjectIsolation(projectId, rework)
                    if (projVal is DomainResult.Error) return projVal
                }
                is DomainResult.Error -> return reworkRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading rework record")
            }
        }

        // Defect verification & isolation
        if (originalDefectId != null && defectDataSource != null) {
            when (val defectRes = defectDataSource.fetchDefectById(originalDefectId)) {
                is DomainResult.Success -> {
                    val isolation = ReQcValidator.validateDefectCrossJobIsolation(
                        reQcJobId = productionJobId,
                        defect = defectRes.data
                    )
                    if (isolation is DomainResult.Error) return isolation
                }
                is DomainResult.Error -> return defectRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading defect record")
            }
        }

        // QC verification & isolation
        if (originalQcId != null && qcDataSource != null) {
            when (val qcRes = qcDataSource.fetchQcById(originalQcId)) {
                is DomainResult.Success -> {
                    val isolation = ReQcValidator.validateQcCrossJobIsolation(
                        reQcJobId = productionJobId,
                        qc = qcRes.data
                    )
                    if (isolation is DomainResult.Error) return isolation
                }
                is DomainResult.Error -> return qcRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading QC record")
            }
        }

        // Checklist verification & isolation
        if (checklistId != null && checklistDataSource != null) {
            when (val chkRes = checklistDataSource.fetchInspectionChecklistById(checklistId)) {
                is DomainResult.Success -> {
                    val isolation = ReQcValidator.validateChecklistCrossJobIsolation(
                        reQcJobId = productionJobId,
                        checklist = chkRes.data
                    )
                    if (isolation is DomainResult.Error) return isolation
                }
                is DomainResult.Error -> return chkRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading Checklist")
            }
        }

        val existingCycles = reQcDataSource.observeReQcList().first().filter { it.productionJobId == productionJobId }
        val dupCheck = ReQcCycleValidator.validateDuplicateActiveCycle(existingCycles, productionReworkId)
        if (dupCheck is DomainResult.Error) return dupCheck

        val cycleCheck = ReQcCycleValidator.validateCycleNumber(existingCycles, 1)
        if (cycleCheck is DomainResult.Error) return cycleCheck

        val reQcId = "reqc-" + UUID.randomUUID().toString()
        val reQc = ReQcInspection(
            reQcId = reQcId,
            productionJobId = productionJobId,
            projectId = projectId,
            productionReworkId = productionReworkId,
            originalQcId = originalQcId,
            originalDefectId = originalDefectId,
            checklistId = checklistId,
            previousReQcId = null,
            cycleNumber = 1,
            cycleType = cycleType,
            status = ReQcStatus.PENDING,
            decision = ReQcDecision.PENDING,
            affectedQuantity = affectedQuantity,
            quantityUnit = quantityUnit,
            createdBy = createdBy,
            createdByName = createdByName,
            createdAt = timestamp,
            updatedAt = timestamp,
            notes = notes
        )

        val validation = ReQcValidator.validateReQc(reQc)
        if (validation is DomainResult.Error) return validation

        val insertRes = reQcDataSource.insertReQc(reQc)
        if (insertRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = 1,
                productionJobId = productionJobId,
                projectId = projectId,
                relatedDefectId = originalDefectId,
                relatedReworkId = productionReworkId,
                actorId = createdBy,
                actorName = createdByName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_CREATED,
                notes = "Created initial Re-QC cycle 1 for rework $productionReworkId",
                timestamp = timestamp
            )
        }
        return insertRes
    }

    override suspend fun createNextCycle(
        projectId: String,
        productionJobId: String,
        productionReworkId: String,
        previousReQcId: String,
        cycleType: ReQcCycleType,
        originalQcId: String?,
        originalDefectId: String?,
        checklistId: String?,
        affectedQuantity: Int?,
        quantityUnit: String,
        createdBy: String,
        createdByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcInspection> = repositoryMutex.withLock {
        val rbac = ReQcValidator.validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val existingCycles = reQcDataSource.observeReQcList().first().filter { it.productionJobId == productionJobId }
        val nextCycleNumber = (existingCycles.maxOfOrNull { it.cycleNumber } ?: 0) + 1

        val paramsVal = ReQcValidator.validateCreationParams(
            projectId = projectId,
            productionJobId = productionJobId,
            productionReworkId = productionReworkId,
            cycleNumber = nextCycleNumber,
            createdBy = createdBy,
            timestamp = timestamp,
            callerRole = callerRole
        )
        if (paramsVal is DomainResult.Error) return paramsVal

        val prevCycleVal = ReQcCycleValidator.validatePreviousCycle(nextCycleNumber, previousReQcId, existingCycles)
        if (prevCycleVal is DomainResult.Error) return prevCycleVal

        // Validate source rework
        if (reworkDataSource != null) {
            when (val reworkRes = reworkDataSource.fetchReworkById(productionReworkId)) {
                is DomainResult.Success -> {
                    val rework = reworkRes.data
                    val reworkVal = ReQcValidator.validateSourceRework(productionJobId, rework)
                    if (reworkVal is DomainResult.Error) return reworkVal
                    val projVal = ReQcValidator.validateReworkCrossProjectIsolation(projectId, rework)
                    if (projVal is DomainResult.Error) return projVal
                }
                is DomainResult.Error -> return reworkRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading rework record")
            }
        }

        // Defect verification & isolation
        if (originalDefectId != null && defectDataSource != null) {
            when (val defectRes = defectDataSource.fetchDefectById(originalDefectId)) {
                is DomainResult.Success -> {
                    val isolation = ReQcValidator.validateDefectCrossJobIsolation(
                        reQcJobId = productionJobId,
                        defect = defectRes.data
                    )
                    if (isolation is DomainResult.Error) return isolation
                }
                is DomainResult.Error -> return defectRes
                is DomainResult.Loading -> return DomainResult.Error(message = "Loading defect record")
            }
        }

        val dupCheck = ReQcCycleValidator.validateDuplicateActiveCycle(existingCycles, productionReworkId)
        if (dupCheck is DomainResult.Error) return dupCheck

        val reQcId = "reqc-" + UUID.randomUUID().toString()
        val reQc = ReQcInspection(
            reQcId = reQcId,
            productionJobId = productionJobId,
            projectId = projectId,
            productionReworkId = productionReworkId,
            originalQcId = originalQcId,
            originalDefectId = originalDefectId,
            checklistId = checklistId,
            previousReQcId = previousReQcId,
            cycleNumber = nextCycleNumber,
            cycleType = cycleType,
            status = ReQcStatus.PENDING,
            decision = ReQcDecision.PENDING,
            affectedQuantity = affectedQuantity,
            quantityUnit = quantityUnit,
            createdBy = createdBy,
            createdByName = createdByName,
            createdAt = timestamp,
            updatedAt = timestamp,
            notes = notes
        )

        val validation = ReQcValidator.validateReQc(reQc)
        if (validation is DomainResult.Error) return validation

        val insertRes = reQcDataSource.insertReQc(reQc)
        if (insertRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = nextCycleNumber,
                productionJobId = productionJobId,
                projectId = projectId,
                relatedDefectId = originalDefectId,
                relatedReworkId = productionReworkId,
                actorId = createdBy,
                actorName = createdByName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_CYCLE_CREATED,
                notes = "Created Re-QC cycle $nextCycleNumber linked to previous cycle $previousReQcId",
                timestamp = timestamp
            )
        }
        return insertRes
    }

    override suspend fun assignReQc(
        reQcId: String,
        inspectorId: String,
        inspectorName: String,
        assignedBy: String,
        assignedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcInspection> = repositoryMutex.withLock {
        val current = when (val res = reQcDataSource.fetchReQcById(reQcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val valRes = ReQcAssignmentValidator.validateAssignment(
            reQc = current,
            inspectorId = inspectorId,
            inspectorName = inspectorName,
            callerRole = callerRole
        )
        if (valRes is DomainResult.Error) return valRes

        val updated = current.copy(
            status = ReQcStatus.ASSIGNED,
            assignedInspectorId = inspectorId,
            assignedInspectorName = inspectorName,
            assignedAt = timestamp,
            updatedAt = timestamp
        )

        val updateRes = reQcDataSource.updateReQc(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = current.cycleNumber,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                relatedDefectId = current.originalDefectId,
                relatedReworkId = current.productionReworkId,
                actorId = assignedBy,
                actorName = assignedByName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_ASSIGNED,
                notes = "Assigned Re-QC to inspector $inspectorName ($inspectorId). ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun reassignReQc(
        reQcId: String,
        newInspectorId: String,
        newInspectorName: String,
        reassignedBy: String,
        reassignedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcInspection> = repositoryMutex.withLock {
        val current = when (val res = reQcDataSource.fetchReQcById(reQcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val valRes = ReQcAssignmentValidator.validateAssignment(
            reQc = current,
            inspectorId = newInspectorId,
            inspectorName = newInspectorName,
            callerRole = callerRole
        )
        if (valRes is DomainResult.Error) return valRes

        val updated = current.copy(
            status = ReQcStatus.ASSIGNED,
            assignedInspectorId = newInspectorId,
            assignedInspectorName = newInspectorName,
            assignedAt = timestamp,
            updatedAt = timestamp
        )

        val updateRes = reQcDataSource.updateReQc(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = current.cycleNumber,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                relatedDefectId = current.originalDefectId,
                relatedReworkId = current.productionReworkId,
                actorId = reassignedBy,
                actorName = reassignedByName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_REASSIGNED,
                notes = "Reassigned Re-QC to inspector $newInspectorName ($newInspectorId). ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun unassignReQc(
        reQcId: String,
        unassignedBy: String,
        unassignedByName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcInspection> = repositoryMutex.withLock {
        val current = when (val res = reQcDataSource.fetchReQcById(reQcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val valRes = ReQcAssignmentValidator.validateUnassignment(current, callerRole)
        if (valRes is DomainResult.Error) return valRes

        val updated = current.copy(
            status = ReQcStatus.PENDING,
            assignedInspectorId = null,
            assignedInspectorName = null,
            assignedAt = null,
            updatedAt = timestamp
        )

        val updateRes = reQcDataSource.updateReQc(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = current.cycleNumber,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                relatedDefectId = current.originalDefectId,
                relatedReworkId = current.productionReworkId,
                actorId = unassignedBy,
                actorName = unassignedByName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_UNASSIGNED,
                notes = "Unassigned Re-QC inspector. ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun startInspection(
        reQcId: String,
        inspectorId: String,
        inspectorName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcInspection> = repositoryMutex.withLock {
        val current = when (val res = reQcDataSource.fetchReQcById(reQcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val execRbac = ReQcAssignmentValidator.validateExecutionPermission(current, inspectorId, callerRole)
        if (execRbac is DomainResult.Error) return execRbac

        val startVal = ReQcLifecycleValidator.validateInspectionStart(current, inspectorId)
        if (startVal is DomainResult.Error) return startVal

        val effectiveInspectorId = if (current.assignedInspectorId.isNullOrBlank()) inspectorId else current.assignedInspectorId
        val effectiveInspectorName = if (current.assignedInspectorName.isNullOrBlank()) inspectorName else current.assignedInspectorName

        val updated = current.copy(
            status = ReQcStatus.IN_INSPECTION,
            assignedInspectorId = effectiveInspectorId,
            assignedInspectorName = effectiveInspectorName,
            startedAt = timestamp,
            updatedAt = timestamp
        )

        val updateRes = reQcDataSource.updateReQc(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = current.cycleNumber,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                relatedDefectId = current.originalDefectId,
                relatedReworkId = current.productionReworkId,
                actorId = inspectorId,
                actorName = inspectorName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_STARTED,
                notes = "Re-QC inspection started. ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun passReQc(
        reQcId: String,
        inspectorId: String,
        inspectorName: String?,
        passNotes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcInspection> = repositoryMutex.withLock {
        val current = when (val res = reQcDataSource.fetchReQcById(reQcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        // Idempotency: if already PASSED, return cleanly
        if (current.status == ReQcStatus.PASSED && current.decision == ReQcDecision.PASS) {
            return DomainResult.Success(current)
        }

        val execRbac = ReQcAssignmentValidator.validateExecutionPermission(current, inspectorId, callerRole)
        if (execRbac is DomainResult.Error) return execRbac

        val passVal = ReQcLifecycleValidator.validatePassTransition(current)
        if (passVal is DomainResult.Error) return passVal

        val prereqVal = ReQcValidator.validatePassPrerequisites(current, inspectorId, callerRole)
        if (prereqVal is DomainResult.Error) return prereqVal

        val updated = current.copy(
            status = ReQcStatus.PASSED,
            decision = ReQcDecision.PASS,
            passNotes = passNotes,
            completedAt = timestamp,
            updatedAt = timestamp
        )

        val updateRes = reQcDataSource.updateReQc(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = current.cycleNumber,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                relatedDefectId = current.originalDefectId,
                relatedReworkId = current.productionReworkId,
                actorId = inspectorId,
                actorName = inspectorName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_PASSED,
                notes = "Re-QC inspection passed: ${passNotes ?: "All quality standards met."}",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun failReQc(
        reQcId: String,
        failureReason: ReQcFailureReason,
        failureNotes: String,
        affectedQuantity: Int,
        quantityUnit: String,
        failedItemIds: List<String>,
        inspectorId: String,
        inspectorName: String?,
        nextAction: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcInspection> = repositoryMutex.withLock {
        val current = when (val res = reQcDataSource.fetchReQcById(reQcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        // Idempotency check
        if ((current.status == ReQcStatus.FAILED || current.status == ReQcStatus.RETURNED_TO_REWORK) && current.decision == ReQcDecision.FAIL) {
            return DomainResult.Success(current)
        }

        val execRbac = ReQcAssignmentValidator.validateExecutionPermission(current, inspectorId, callerRole)
        if (execRbac is DomainResult.Error) return execRbac

        val failVal = ReQcLifecycleValidator.validateFailTransition(current, failureReason, failureNotes)
        if (failVal is DomainResult.Error) return failVal

        val prereqVal = ReQcValidator.validateFailPrerequisites(
            reQc = current,
            failureReason = failureReason,
            failureNotes = failureNotes,
            affectedQuantity = affectedQuantity,
            inspectorId = inspectorId,
            callerRole = callerRole
        )
        if (prereqVal is DomainResult.Error) return prereqVal

        // Create immutable failure record
        val failureRecordId = "fail-rec-" + UUID.randomUUID().toString()
        val failureRecord = ReQcFailureRecord(
            failureRecordId = failureRecordId,
            reQcId = reQcId,
            cycleNumber = current.cycleNumber,
            productionJobId = current.productionJobId,
            projectId = current.projectId,
            defectId = current.originalDefectId,
            checklistId = current.checklistId,
            failedItemIds = failedItemIds,
            failureReason = failureReason,
            failureNotes = failureNotes,
            affectedQuantity = affectedQuantity,
            quantityUnit = quantityUnit,
            detectedBy = inspectorId,
            detectedByName = inspectorName,
            detectedAt = timestamp,
            nextAction = nextAction ?: "Additional rework required."
        )
        val insertRecordRes = reQcDataSource.insertFailureRecord(failureRecord)
        if (insertRecordRes is DomainResult.Error) return insertRecordRes

        val updated = current.copy(
            status = ReQcStatus.FAILED,
            decision = ReQcDecision.FAIL,
            failureReason = failureReason,
            failureNotes = failureNotes,
            affectedQuantity = affectedQuantity,
            quantityUnit = quantityUnit,
            completedAt = timestamp,
            updatedAt = timestamp
        )

        val updateRes = reQcDataSource.updateReQc(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = current.cycleNumber,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                relatedDefectId = current.originalDefectId,
                relatedReworkId = current.productionReworkId,
                actorId = inspectorId,
                actorName = inspectorName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_FAILURE_RECORDED,
                notes = "Failure recorded (${failureReason.defaultLabel}): $failureNotes ($affectedQuantity $quantityUnit)",
                timestamp = timestamp
            )
            recordActivity(
                reQcId = reQcId,
                cycleNumber = current.cycleNumber,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                relatedDefectId = current.originalDefectId,
                relatedReworkId = current.productionReworkId,
                actorId = inspectorId,
                actorName = inspectorName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_FAILED,
                notes = "Re-QC cycle ${current.cycleNumber} marked as FAILED.",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun returnToRework(
        reQcId: String,
        actorId: String,
        actorName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcInspection> = repositoryMutex.withLock {
        val rbac = ReQcValidator.validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = reQcDataSource.fetchReQcById(reQcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        // Idempotency: if already RETURNED_TO_REWORK, return cleanly
        if (current.status == ReQcStatus.RETURNED_TO_REWORK) {
            return DomainResult.Success(current)
        }

        val returnVal = ReQcLifecycleValidator.validateReturnToRework(current, actorId)
        if (returnVal is DomainResult.Error) return returnVal

        val updated = current.copy(
            status = ReQcStatus.RETURNED_TO_REWORK,
            returnedToReworkAt = timestamp,
            updatedAt = timestamp,
            notes = if (notes.isNullOrBlank()) current.notes else "${current.notes ?: ""}\nReturned to Rework: $notes".trim()
        )

        val updateRes = reQcDataSource.updateReQc(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = current.cycleNumber,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                relatedDefectId = current.originalDefectId,
                relatedReworkId = current.productionReworkId,
                actorId = actorId,
                actorName = actorName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_RETURNED_TO_REWORK,
                notes = "Returned to Rework workflow for subsequent corrective action. ${notes ?: ""}".trim(),
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun cancelReQc(
        reQcId: String,
        reason: String,
        cancelledBy: String,
        cancelledByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcInspection> = repositoryMutex.withLock {
        val rbac = ReQcValidator.validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = reQcDataSource.fetchReQcById(reQcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val cancelVal = ReQcLifecycleValidator.validateCancellation(current, reason)
        if (cancelVal is DomainResult.Error) return cancelVal

        val updated = current.copy(
            status = ReQcStatus.CANCELLED,
            updatedAt = timestamp,
            notes = "${current.notes ?: ""}\nCancelled: $reason".trim()
        )

        val updateRes = reQcDataSource.updateReQc(updated)
        if (updateRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = current.cycleNumber,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                relatedDefectId = current.originalDefectId,
                relatedReworkId = current.productionReworkId,
                actorId = cancelledBy,
                actorName = cancelledByName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_CANCELLED,
                notes = "Cancelled Re-QC: $reason",
                timestamp = timestamp
            )
        }
        return updateRes
    }

    override suspend fun recordFailure(
        reQcId: String,
        failureReason: ReQcFailureReason,
        failureNotes: String,
        affectedQuantity: Int,
        quantityUnit: String,
        failedItemIds: List<String>,
        detectedBy: String,
        detectedByName: String?,
        nextAction: String?,
        linkedReworkId: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<ReQcFailureRecord> = repositoryMutex.withLock {
        val rbac = ReQcValidator.validateMutationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = when (val res = reQcDataSource.fetchReQcById(reQcId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Loading")
        }

        val recordId = "fail-rec-" + UUID.randomUUID().toString()
        val record = ReQcFailureRecord(
            failureRecordId = recordId,
            reQcId = reQcId,
            cycleNumber = current.cycleNumber,
            productionJobId = current.productionJobId,
            projectId = current.projectId,
            defectId = current.originalDefectId,
            checklistId = current.checklistId,
            failedItemIds = failedItemIds,
            failureReason = failureReason,
            failureNotes = failureNotes,
            affectedQuantity = affectedQuantity,
            quantityUnit = quantityUnit,
            detectedBy = detectedBy,
            detectedByName = detectedByName,
            detectedAt = timestamp,
            nextAction = nextAction,
            linkedReworkId = linkedReworkId
        )

        val insertRes = reQcDataSource.insertFailureRecord(record)
        if (insertRes is DomainResult.Success) {
            recordActivity(
                reQcId = reQcId,
                cycleNumber = current.cycleNumber,
                productionJobId = current.productionJobId,
                projectId = current.projectId,
                relatedDefectId = current.originalDefectId,
                relatedReworkId = current.productionReworkId,
                actorId = detectedBy,
                actorName = detectedByName,
                role = callerRole,
                activityType = ReQcActivityType.RE_QC_FAILURE_RECORDED,
                notes = "Recorded failure: ${failureReason.defaultLabel} - $failureNotes",
                timestamp = timestamp
            )
        }
        return insertRes
    }

    override fun observeFailureHistory(
        reQcId: String?,
        productionJobId: String?
    ): Flow<List<ReQcFailureRecord>> {
        return reQcDataSource.observeFailureRecords().map { list ->
            list.filter { item ->
                (reQcId == null || item.reQcId == reQcId) &&
                        (productionJobId == null || item.productionJobId == productionJobId)
            }
        }
    }

    override suspend fun findFailureRecordById(failureRecordId: String): DomainResult<ReQcFailureRecord> {
        return reQcDataSource.fetchFailureRecordById(failureRecordId)
    }

    override fun observeReQcActivity(reQcId: String): Flow<List<ReQcActivityEvent>> {
        return reQcDataSource.observeActivityEvents().map { list ->
            list.filter { it.reQcId == reQcId }
        }
    }
}
