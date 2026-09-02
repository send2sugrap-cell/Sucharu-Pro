package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FinalQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionDefectDataSource
import com.sucharu.sucharupro.data.datasource.ProductionJobDataSource
import com.sucharu.sucharupro.data.datasource.ProductionQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReQcDataSource
import com.sucharu.sucharupro.data.datasource.ProductionReworkDataSource
import com.sucharu.sucharupro.data.datasource.QcChecklistDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.qc.FinalQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.FinalQcActivityType
import com.sucharu.sucharupro.domain.model.qc.FinalQcDecision
import com.sucharu.sucharupro.domain.model.qc.FinalQcEligibilityResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseAuthorization
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseStatus
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.FinalQcRepository
import com.sucharu.sucharupro.domain.validation.FinalQcAssignmentValidator
import com.sucharu.sucharupro.domain.validation.FinalQcEligibilityValidator
import com.sucharu.sucharupro.domain.validation.FinalQcLifecycleValidator
import com.sucharu.sucharupro.domain.validation.FinalQcValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Thread-safe, authoritative repository implementation for Final QC & Production Release (Module 06 Step 07).
 */
class FinalQcRepositoryImpl(
    private val finalQcDataSource: FinalQcDataSource,
    private val productionJobDataSource: ProductionJobDataSource? = null,
    private val qcDataSource: ProductionQcDataSource? = null,
    private val checklistDataSource: QcChecklistDataSource? = null,
    private val defectDataSource: ProductionDefectDataSource? = null,
    private val reworkDataSource: ProductionReworkDataSource? = null,
    private val reQcDataSource: ProductionReQcDataSource? = null
) : FinalQcRepository {

    private val mutex = Mutex()

    private suspend fun recordActivity(
        finalQcId: String,
        projectId: String,
        productionJobId: String,
        actorId: String,
        actorName: String? = null,
        activityType: FinalQcActivityType,
        notes: String? = null,
        timestamp: String,
        metadata: Map<String, String> = emptyMap()
    ) {
        val event = FinalQcActivityEvent(
            eventId = "act-finalqc-" + UUID.randomUUID().toString(),
            projectId = projectId,
            productionJobId = productionJobId,
            finalQcId = finalQcId,
            actorId = actorId,
            actorName = actorName,
            activityType = activityType,
            notes = notes,
            timestamp = timestamp,
            metadata = metadata
        )
        finalQcDataSource.insertActivityEvent(event)
    }

    override fun observeFinalQcList(): Flow<List<FinalQcInspection>> =
        finalQcDataSource.observeFinalQcList()

    override fun observeFinalQcById(finalQcId: String): Flow<FinalQcInspection?> =
        finalQcDataSource.observeFinalQcById(finalQcId)

    override fun observeFinalQcByJob(productionJobId: String): Flow<List<FinalQcInspection>> =
        finalQcDataSource.observeFinalQcList().map { list ->
            list.filter { it.productionJobId == productionJobId }
        }

    override fun observeFinalQcByProject(projectId: String): Flow<List<FinalQcInspection>> =
        finalQcDataSource.observeFinalQcList().map { list ->
            list.filter { it.projectId == projectId }
        }

    override suspend fun findFinalQcById(finalQcId: String): DomainResult<FinalQcInspection> {
        val inspection = finalQcDataSource.findFinalQcById(finalQcId)
            ?: return DomainResult.Error(message = "Final QC inspection record not found: $finalQcId")
        return DomainResult.Success(inspection)
    }

    override suspend fun createFinalQc(
        projectId: String,
        productionJobId: String,
        productionJobItemId: String?,
        totalQuantity: Int,
        quantityUnit: String,
        preProductionQcId: String?,
        checklistId: String?,
        sourceDefectIds: List<String>,
        sourceReworkIds: List<String>,
        sourceReQcIds: List<String>,
        notes: String?,
        createdBy: String?,
        createdByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<FinalQcInspection> = mutex.withLock {
        // 1. RBAC Check
        val rbacResult = FinalQcAssignmentValidator.validateCreatePermission(callerRole)
        if (rbacResult is DomainResult.Error) return rbacResult

        // 2. Param Validation
        val paramResult = FinalQcValidator.validateCreationParams(
            projectId = projectId,
            productionJobId = productionJobId,
            totalQuantity = totalQuantity,
            quantityUnit = quantityUnit,
            timestamp = timestamp
        )
        if (paramResult is DomainResult.Error) return paramResult

        // 3. Job Validation if DataSource available
        if (productionJobDataSource != null) {
            val jobResult = productionJobDataSource.fetchJobById(productionJobId)
            if (jobResult is DomainResult.Error) {
                return DomainResult.Error(message = "Referenced production job not found: $productionJobId")
            }
            val job = (jobResult as DomainResult.Success).data
            if (job.status.isTerminal) {
                return DomainResult.Error(message = "Cannot create Final QC for terminal job: $productionJobId")
            }
            if (job.orderId != projectId) {
                return DomainResult.Error(message = "Project ID mismatch: Job belongs to ${job.orderId}, but $projectId provided.")
            }
        }

        // 4. Cross-Job Isolation Checks
        if (preProductionQcId != null && qcDataSource != null) {
            val qcs = qcDataSource.observeQcList().first()
            val preQc = qcs.find { it.qcId == preProductionQcId }
            if (preQc != null) {
                val iso = FinalQcValidator.validatePreProductionQcCrossJobIsolation(productionJobId, preQc)
                if (iso is DomainResult.Error) return iso
            }
        }

        if (checklistId != null && checklistDataSource != null) {
            val checklists = checklistDataSource.observeInspectionChecklists().first()
            val chk = checklists.find { it.inspectionChecklistId == checklistId }
            if (chk != null) {
                val iso = FinalQcValidator.validateChecklistCrossJobIsolation(productionJobId, chk)
                if (iso is DomainResult.Error) return iso
            }
        }

        if (sourceDefectIds.isNotEmpty() && defectDataSource != null) {
            val defects = defectDataSource.observeDefects().first()
            for (defId in sourceDefectIds) {
                val def = defects.find { it.defectId == defId }
                if (def != null) {
                    val iso = FinalQcValidator.validateDefectCrossJobIsolation(productionJobId, def)
                    if (iso is DomainResult.Error) return iso
                }
            }
        }

        if (sourceReworkIds.isNotEmpty() && reworkDataSource != null) {
            val reworks = reworkDataSource.observeReworks().first()
            for (rewId in sourceReworkIds) {
                val rew = reworks.find { it.reworkId == rewId }
                if (rew != null) {
                    val iso = FinalQcValidator.validateReworkCrossJobIsolation(productionJobId, rew)
                    if (iso is DomainResult.Error) return iso
                }
            }
        }

        if (sourceReQcIds.isNotEmpty() && reQcDataSource != null) {
            val reQcs = reQcDataSource.observeReQcList().first()
            for (rqId in sourceReQcIds) {
                val rq = reQcs.find { it.reQcId == rqId }
                if (rq != null) {
                    val iso = FinalQcValidator.validateReQcCrossJobIsolation(productionJobId, rq)
                    if (iso is DomainResult.Error) return iso
                }
            }
        }

        // 5. Prevent multiple active Final QC records for the same job
        val existingForJob = finalQcDataSource.observeFinalQcList().first()
            .filter { it.productionJobId == productionJobId }
        val activeExisting = existingForJob.find {
            it.status in setOf(
                FinalQcStatus.DRAFT,
                FinalQcStatus.PENDING,
                FinalQcStatus.ASSIGNED,
                FinalQcStatus.IN_INSPECTION,
                FinalQcStatus.PASSED
            )
        }
        if (activeExisting != null) {
            return DomainResult.Error(message = 
                "An active Final QC record already exists for job $productionJobId (ID: ${activeExisting.finalQcId}, Status: ${activeExisting.status})."
            )
        }

        val finalQcId = "fqc-" + UUID.randomUUID().toString()
        val inspection = FinalQcInspection(
            finalQcId = finalQcId,
            projectId = projectId,
            productionJobId = productionJobId,
            productionJobItemId = productionJobItemId,
            status = FinalQcStatus.PENDING,
            decision = FinalQcDecision.PENDING,
            releaseStatus = FinalQcReleaseStatus.PENDING_AUTHORIZATION,
            totalQuantity = totalQuantity,
            inspectedQuantity = 0,
            acceptedQuantity = 0,
            rejectedQuantity = 0,
            quantityUnit = quantityUnit,
            notes = notes,
            preProductionQcId = preProductionQcId,
            checklistId = checklistId,
            sourceDefectIds = sourceDefectIds,
            sourceReworkIds = sourceReworkIds,
            sourceReQcIds = sourceReQcIds,
            createdBy = createdBy,
            createdByName = createdByName,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        finalQcDataSource.insertFinalQc(inspection)

        recordActivity(
            finalQcId = finalQcId,
            projectId = projectId,
            productionJobId = productionJobId,
            actorId = createdBy ?: "system",
            actorName = createdByName,
            activityType = FinalQcActivityType.FINAL_QC_CREATED,
            notes = notes ?: "Final QC record created",
            timestamp = timestamp
        )

        return DomainResult.Success(inspection)
    }

    override suspend fun assignInspector(
        finalQcId: String,
        inspectorId: String,
        inspectorName: String,
        assignedBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<FinalQcInspection> = mutex.withLock {
        val rbac = FinalQcAssignmentValidator.validateAssignmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = finalQcDataSource.findFinalQcById(finalQcId)
            ?: return DomainResult.Error(message = "Final QC record not found: $finalQcId")

        val immutability = FinalQcLifecycleValidator.validateTerminalImmutability(current)
        if (immutability is DomainResult.Error) return immutability

        val transition = FinalQcLifecycleValidator.validateTransition(current.status, FinalQcStatus.ASSIGNED)
        if (transition is DomainResult.Error) return transition

        val updated = current.copy(
            assignedInspectorId = inspectorId,
            assignedInspectorName = inspectorName,
            status = FinalQcStatus.ASSIGNED,
            updatedAt = timestamp
        )
        finalQcDataSource.updateFinalQc(updated)

        recordActivity(
            finalQcId = finalQcId,
            projectId = current.projectId,
            productionJobId = current.productionJobId,
            actorId = assignedBy ?: inspectorId,
            activityType = FinalQcActivityType.FINAL_QC_ASSIGNED,
            notes = "Assigned to inspector $inspectorName ($inspectorId)",
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    override suspend fun reassignInspector(
        finalQcId: String,
        newInspectorId: String,
        newInspectorName: String,
        reassignedBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<FinalQcInspection> = mutex.withLock {
        val rbac = FinalQcAssignmentValidator.validateAssignmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = finalQcDataSource.findFinalQcById(finalQcId)
            ?: return DomainResult.Error(message = "Final QC record not found: $finalQcId")

        val immutability = FinalQcLifecycleValidator.validateTerminalImmutability(current)
        if (immutability is DomainResult.Error) return immutability

        val updated = current.copy(
            assignedInspectorId = newInspectorId,
            assignedInspectorName = newInspectorName,
            status = FinalQcStatus.ASSIGNED,
            updatedAt = timestamp
        )
        finalQcDataSource.updateFinalQc(updated)

        recordActivity(
            finalQcId = finalQcId,
            projectId = current.projectId,
            productionJobId = current.productionJobId,
            actorId = reassignedBy ?: newInspectorId,
            activityType = FinalQcActivityType.FINAL_QC_REASSIGNED,
            notes = "Reassigned to inspector $newInspectorName ($newInspectorId)",
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    override suspend fun unassignInspector(
        finalQcId: String,
        unassignedBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<FinalQcInspection> = mutex.withLock {
        val rbac = FinalQcAssignmentValidator.validateAssignmentPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = finalQcDataSource.findFinalQcById(finalQcId)
            ?: return DomainResult.Error(message = "Final QC record not found: $finalQcId")

        val immutability = FinalQcLifecycleValidator.validateTerminalImmutability(current)
        if (immutability is DomainResult.Error) return immutability

        val updated = current.copy(
            assignedInspectorId = null,
            assignedInspectorName = null,
            status = FinalQcStatus.PENDING,
            updatedAt = timestamp
        )
        finalQcDataSource.updateFinalQc(updated)

        recordActivity(
            finalQcId = finalQcId,
            projectId = current.projectId,
            productionJobId = current.productionJobId,
            actorId = unassignedBy ?: "system",
            activityType = FinalQcActivityType.FINAL_QC_UNASSIGNED,
            notes = "Unassigned inspector",
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    override suspend fun startInspection(
        finalQcId: String,
        inspectorId: String,
        inspectorName: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<FinalQcInspection> = mutex.withLock {
        val current = finalQcDataSource.findFinalQcById(finalQcId)
            ?: return DomainResult.Error(message = "Final QC record not found: $finalQcId")

        val execRbac = FinalQcAssignmentValidator.validateInspectionExecutionPermission(callerRole, current, inspectorId)
        if (execRbac is DomainResult.Error) return execRbac

        val startCheck = FinalQcLifecycleValidator.validateStartInspection(current)
        if (startCheck is DomainResult.Error) return startCheck

        val updated = current.copy(
            status = FinalQcStatus.IN_INSPECTION,
            inspectedBy = inspectorId,
            inspectedByName = inspectorName ?: current.assignedInspectorName,
            inspectedAt = timestamp,
            notes = notes ?: current.notes,
            updatedAt = timestamp
        )
        finalQcDataSource.updateFinalQc(updated)

        recordActivity(
            finalQcId = finalQcId,
            projectId = current.projectId,
            productionJobId = current.productionJobId,
            actorId = inspectorId,
            actorName = inspectorName,
            activityType = FinalQcActivityType.FINAL_QC_STARTED,
            notes = notes ?: "Final QC inspection started",
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    override suspend fun submitPass(
        finalQcId: String,
        acceptedQuantity: Int?,
        notes: String?,
        inspectorId: String,
        inspectorName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<FinalQcInspection> = mutex.withLock {
        val current = finalQcDataSource.findFinalQcById(finalQcId)
            ?: return DomainResult.Error(message = "Final QC record not found: $finalQcId")

        val execRbac = FinalQcAssignmentValidator.validateInspectionExecutionPermission(callerRole, current, inspectorId)
        if (execRbac is DomainResult.Error) return execRbac

        val immutability = FinalQcLifecycleValidator.validateTerminalImmutability(current)
        if (immutability is DomainResult.Error) return immutability

        val actualAccepted = acceptedQuantity ?: current.totalQuantity
        val passValidation = FinalQcValidator.validatePassPrerequisites(
            acceptedQuantity = actualAccepted,
            rejectedQuantity = 0,
            inspectorId = inspectorId,
            timestamp = timestamp
        )
        if (passValidation is DomainResult.Error) return passValidation

        val updated = current.copy(
            status = FinalQcStatus.PASSED,
            decision = FinalQcDecision.PASS,
            inspectedQuantity = actualAccepted,
            acceptedQuantity = actualAccepted,
            rejectedQuantity = 0,
            inspectedBy = inspectorId,
            inspectedByName = inspectorName ?: current.inspectedByName,
            inspectedAt = timestamp,
            notes = notes ?: current.notes,
            failureReason = null,
            blockingReasons = emptyList(),
            updatedAt = timestamp
        )

        val modelCheck = FinalQcValidator.validateInspectionModel(updated)
        if (modelCheck is DomainResult.Error) return modelCheck

        finalQcDataSource.updateFinalQc(updated)

        recordActivity(
            finalQcId = finalQcId,
            projectId = current.projectId,
            productionJobId = current.productionJobId,
            actorId = inspectorId,
            actorName = inspectorName,
            activityType = FinalQcActivityType.FINAL_QC_PASSED,
            notes = notes ?: "Final QC Passed with accepted quantity $actualAccepted",
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    override suspend fun submitFail(
        finalQcId: String,
        rejectedQuantity: Int,
        failureReason: String,
        notes: String?,
        inspectorId: String,
        inspectorName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<FinalQcInspection> = mutex.withLock {
        val current = finalQcDataSource.findFinalQcById(finalQcId)
            ?: return DomainResult.Error(message = "Final QC record not found: $finalQcId")

        val execRbac = FinalQcAssignmentValidator.validateInspectionExecutionPermission(callerRole, current, inspectorId)
        if (execRbac is DomainResult.Error) return execRbac

        val immutability = FinalQcLifecycleValidator.validateTerminalImmutability(current)
        if (immutability is DomainResult.Error) return immutability

        val failValidation = FinalQcValidator.validateFailPrerequisites(
            failureReason = failureReason,
            rejectedQuantity = rejectedQuantity,
            inspectorId = inspectorId,
            timestamp = timestamp
        )
        if (failValidation is DomainResult.Error) return failValidation

        val accepted = (current.totalQuantity - rejectedQuantity).coerceAtLeast(0)
        val inspected = accepted + rejectedQuantity

        val updated = current.copy(
            status = FinalQcStatus.FAILED,
            decision = FinalQcDecision.FAIL,
            inspectedQuantity = inspected,
            acceptedQuantity = accepted,
            rejectedQuantity = rejectedQuantity,
            failureReason = failureReason,
            inspectedBy = inspectorId,
            inspectedByName = inspectorName ?: current.inspectedByName,
            inspectedAt = timestamp,
            notes = notes ?: current.notes,
            blockingReasons = listOf(failureReason),
            updatedAt = timestamp
        )

        val modelCheck = FinalQcValidator.validateInspectionModel(updated)
        if (modelCheck is DomainResult.Error) return modelCheck

        finalQcDataSource.updateFinalQc(updated)

        recordActivity(
            finalQcId = finalQcId,
            projectId = current.projectId,
            productionJobId = current.productionJobId,
            actorId = inspectorId,
            actorName = inspectorName,
            activityType = FinalQcActivityType.FINAL_QC_FAILED,
            notes = "Final QC Failed: $failureReason (Rejected: $rejectedQuantity)",
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    override suspend fun evaluateReleaseEligibility(finalQcId: String): DomainResult<FinalQcEligibilityResult> = mutex.withLock {
        val current = finalQcDataSource.findFinalQcById(finalQcId)
            ?: return DomainResult.Error(message = "Final QC record not found: $finalQcId")

        var job: ProductionJob? = null
        if (productionJobDataSource != null) {
            val jobResult = productionJobDataSource.fetchJobById(current.productionJobId)
            if (jobResult is DomainResult.Success) {
                job = jobResult.data
            }
        }

        val preProdQcs: List<ProductionQc> = qcDataSource?.observeQcList()?.first() ?: emptyList()
        val checklists: List<QcInspectionChecklist> = checklistDataSource?.observeInspectionChecklists()?.first() ?: emptyList()
        val defects: List<ProductionDefect> = defectDataSource?.observeDefects()?.first() ?: emptyList()
        val reworks: List<ProductionRework> = reworkDataSource?.observeReworks()?.first() ?: emptyList()
        val reQcs: List<ReQcInspection> = reQcDataSource?.observeReQcList()?.first() ?: emptyList()

        val eligibility = FinalQcEligibilityValidator.evaluateEligibility(
            inspection = current,
            job = job,
            preProductionQcList = preProdQcs,
            checklistList = checklists,
            defectList = defects,
            reworkList = reworks,
            reQcList = reQcs
        )

        recordActivity(
            finalQcId = finalQcId,
            projectId = current.projectId,
            productionJobId = current.productionJobId,
            actorId = "system",
            activityType = FinalQcActivityType.FINAL_QC_RELEASE_ELIGIBILITY_CHECKED,
            notes = "Release Eligibility: ${if (eligibility.isEligible) "ELIGIBLE" else "BLOCKED"} (${eligibility.message})",
            timestamp = current.updatedAt
        )

        return DomainResult.Success(eligibility)
    }

    override suspend fun authorizeProductionRelease(
        finalQcId: String,
        releaseNotes: String?,
        authorizedBy: String,
        authorizedByName: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<FinalQcReleaseAuthorization> = mutex.withLock {
        // 1. RBAC & Separation of Duties Check (Inspector cannot release)
        val rbac = FinalQcAssignmentValidator.validateReleaseAuthorizationPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = finalQcDataSource.findFinalQcById(finalQcId)
            ?: return DomainResult.Error(message = "Final QC record not found: $finalQcId")

        // 2. Idempotency Check: If already released, return existing authorization
        if (current.status == FinalQcStatus.RELEASED && current.releaseAuthorizationId != null) {
            val existingAuth = finalQcDataSource.findReleaseAuthorizationById(current.releaseAuthorizationId)
            if (existingAuth != null) {
                return DomainResult.Success(existingAuth)
            }
        }

        // 3. Lifecycle Check
        val releaseReady = FinalQcLifecycleValidator.validateReleaseReady(current)
        if (releaseReady is DomainResult.Error) return releaseReady

        // 4. Complete 14-Point Quality Gate Eligibility Validation
        var job: ProductionJob? = null
        if (productionJobDataSource != null) {
            val jobResult = productionJobDataSource.fetchJobById(current.productionJobId)
            if (jobResult is DomainResult.Success) {
                job = jobResult.data
            }
        }

        val preProdQcs: List<ProductionQc> = qcDataSource?.observeQcList()?.first() ?: emptyList()
        val checklists: List<QcInspectionChecklist> = checklistDataSource?.observeInspectionChecklists()?.first() ?: emptyList()
        val defects: List<ProductionDefect> = defectDataSource?.observeDefects()?.first() ?: emptyList()
        val reworks: List<ProductionRework> = reworkDataSource?.observeReworks()?.first() ?: emptyList()
        val reQcs: List<ReQcInspection> = reQcDataSource?.observeReQcList()?.first() ?: emptyList()

        val eligibility = FinalQcEligibilityValidator.evaluateEligibility(
            inspection = current,
            job = job,
            preProductionQcList = preProdQcs,
            checklistList = checklists,
            defectList = defects,
            reworkList = reworks,
            reQcList = reQcs
        )

        if (!eligibility.isEligible) {
            recordActivity(
                finalQcId = finalQcId,
                projectId = current.projectId,
                productionJobId = current.productionJobId,
                actorId = authorizedBy,
                actorName = authorizedByName,
                activityType = FinalQcActivityType.FINAL_QC_RELEASE_REJECTED,
                notes = "Release rejected: ${eligibility.message}",
                timestamp = timestamp
            )
            return DomainResult.Error(message = "Cannot authorize production release: ${eligibility.message}")
        }

        // 5. Create and persist immutable release authorization
        val authId = "rel-auth-" + UUID.randomUUID().toString()
        val authorization = FinalQcReleaseAuthorization(
            releaseAuthorizationId = authId,
            projectId = current.projectId,
            productionJobId = current.productionJobId,
            productionJobItemId = current.productionJobItemId,
            finalQcId = finalQcId,
            finalQcDecision = current.decision,
            finalQcStatus = FinalQcStatus.RELEASED,
            authorizedBy = authorizedBy,
            authorizedByName = authorizedByName,
            authorizedAt = timestamp,
            releaseNotes = releaseNotes,
            finalQcVersion = 1,
            preProductionQcId = current.preProductionQcId,
            checklistId = current.checklistId,
            sourceReQcIds = current.sourceReQcIds,
            sourceDefectIds = current.sourceDefectIds,
            sourceReworkIds = current.sourceReworkIds,
            createdAt = timestamp
        )
        finalQcDataSource.insertReleaseAuthorization(authorization)

        // 6. Update Final QC status to RELEASED
        val updatedInspection = current.copy(
            status = FinalQcStatus.RELEASED,
            releaseStatus = FinalQcReleaseStatus.AUTHORIZED,
            releaseAuthorizationId = authId,
            updatedAt = timestamp
        )
        finalQcDataSource.updateFinalQc(updatedInspection)

        // 7. Record Audit Event
        recordActivity(
            finalQcId = finalQcId,
            projectId = current.projectId,
            productionJobId = current.productionJobId,
            actorId = authorizedBy,
            actorName = authorizedByName,
            activityType = FinalQcActivityType.FINAL_QC_RELEASE_AUTHORIZED,
            notes = releaseNotes ?: "Production release formally authorized",
            timestamp = timestamp
        )

        return DomainResult.Success(authorization)
    }

    override suspend fun getReleaseAuthorization(productionJobId: String): DomainResult<FinalQcReleaseAuthorization?> {
        val auth = finalQcDataSource.findReleaseAuthorizationByJob(productionJobId)
        return DomainResult.Success(auth)
    }

    override fun observeReleaseAuthorization(productionJobId: String): Flow<FinalQcReleaseAuthorization?> =
        finalQcDataSource.observeReleaseAuthorization(productionJobId)

    override suspend fun cancelFinalQc(
        finalQcId: String,
        reason: String,
        cancelledBy: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<FinalQcInspection> = mutex.withLock {
        val rbac = FinalQcAssignmentValidator.validateCancelPermission(callerRole)
        if (rbac is DomainResult.Error) return rbac

        val current = finalQcDataSource.findFinalQcById(finalQcId)
            ?: return DomainResult.Error(message = "Final QC record not found: $finalQcId")

        val cancelCheck = FinalQcLifecycleValidator.validateCancellation(current, reason)
        if (cancelCheck is DomainResult.Error) return cancelCheck

        val updated = current.copy(
            status = FinalQcStatus.CANCELLED,
            notes = "Cancelled: $reason",
            updatedAt = timestamp
        )
        finalQcDataSource.updateFinalQc(updated)

        recordActivity(
            finalQcId = finalQcId,
            projectId = current.projectId,
            productionJobId = current.productionJobId,
            actorId = cancelledBy ?: "system",
            activityType = FinalQcActivityType.FINAL_QC_CANCELLED,
            notes = "Final QC cancelled: $reason",
            timestamp = timestamp
        )

        return DomainResult.Success(updated)
    }

    override fun observeFinalQcActivity(finalQcId: String): Flow<List<FinalQcActivityEvent>> =
        finalQcDataSource.observeActivityEvents(finalQcId)
}
