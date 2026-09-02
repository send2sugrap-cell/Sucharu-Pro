package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.DesignProjectDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignActivityEvent
import com.sucharu.sucharupro.domain.model.design.DesignActivityType
import com.sucharu.sucharupro.domain.model.design.DesignAssignment
import com.sucharu.sucharupro.domain.model.design.DesignAssignmentStatus
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.DesignProjectRepository
import com.sucharu.sucharupro.domain.validation.DesignAssignmentValidator
import com.sucharu.sucharupro.domain.validation.DesignLifecycleValidator
import com.sucharu.sucharupro.domain.validation.DesignProjectValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Authoritative implementation of [DesignProjectRepository] enforcing domain validation,
 * state machine integrity, assignment history, and thread-safe operations.
 */
class DesignProjectRepositoryImpl(
    private val dataSource: DesignProjectDataSource
) : DesignProjectRepository {

    private val repositoryMutex = Mutex()

    private suspend fun recordActivity(
        projectId: String,
        productionJobId: String,
        designerId: String? = null,
        designerName: String? = null,
        eventType: DesignActivityType,
        message: String? = null,
        timestamp: String,
        createdBy: String? = null
    ) {
        val event = DesignActivityEvent(
            eventId = "act-des-" + UUID.randomUUID().toString(),
            projectId = projectId,
            productionJobId = productionJobId,
            designerId = designerId,
            designerName = designerName,
            eventType = eventType,
            message = message,
            timestamp = timestamp,
            createdBy = createdBy
        )
        dataSource.insertActivityEvent(event)
    }

    override fun observeDesignProjects(): Flow<List<DesignProject>> = dataSource.observeProjects()

    override fun getDesignProjectById(projectId: String): Flow<DesignProject?> {
        return dataSource.observeProjects().map { projects ->
            projects.find { it.projectId == projectId }
        }
    }

    override suspend fun findDesignProjectById(projectId: String): DomainResult<DesignProject> {
        return dataSource.fetchProjectById(projectId)
    }

    override fun getDesignProjectForJob(productionJobId: String): Flow<DesignProject?> {
        return dataSource.observeProjects().map { projects ->
            projects.find { it.productionJobId == productionJobId }
        }
    }

    override fun getDesignProjectsForDesigner(designerId: String): Flow<List<DesignProject>> {
        return dataSource.observeProjects().map { projects ->
            projects.filter { it.assignedDesignerId == designerId }
        }
    }

    override suspend fun createDesignProject(
        job: ProductionJob,
        title: String?,
        notes: String?,
        createdBy: String?,
        timestamp: String
    ): DomainResult<DesignProject> = repositoryMutex.withLock {
        val existingProjects = dataSource.observeProjects().first()
        val creationValidation = DesignProjectValidator.validateCreation(job, existingProjects)
        if (creationValidation is DomainResult.Error) {
            return creationValidation
        }

        val seqNumber = String.format("%04d", existingProjects.size + 1)
        val projectNumber = "DES-2026-$seqNumber"
        val projectId = "des-" + UUID.randomUUID().toString()

        val project = DesignProject.fromProductionJob(
            projectId = projectId,
            projectNumber = projectNumber,
            job = job,
            title = title,
            notes = notes,
            createdBy = createdBy,
            timestamp = timestamp
        )

        val projectValidation = DesignProjectValidator.validateProject(project)
        if (projectValidation is DomainResult.Error) {
            return projectValidation
        }

        val insertResult = dataSource.insertProject(project)
        if (insertResult is DomainResult.Success) {
            recordActivity(
                projectId = project.projectId,
                productionJobId = project.productionJobId,
                eventType = DesignActivityType.PROJECT_CREATED,
                message = "Design Project '${project.projectNumber}' created for Job '${job.jobNumber}'.",
                timestamp = timestamp,
                createdBy = createdBy
            )
        }
        return insertResult
    }

    override suspend fun updateDesignProject(project: DesignProject): DomainResult<DesignProject> = repositoryMutex.withLock {
        val validation = DesignProjectValidator.validateProject(project)
        if (validation is DomainResult.Error) {
            return validation
        }
        return dataSource.updateProject(project)
    }

    override suspend fun updateDesignStatus(
        projectId: String,
        targetStatus: DesignStatus,
        actorId: String?,
        actorName: String?,
        notes: String?,
        timestamp: String
    ): DomainResult<DesignProject> = repositoryMutex.withLock {
        val currentProject = when (val res = dataSource.fetchProjectById(projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val transitionValidation = DesignLifecycleValidator.validateStatusTransition(currentProject, targetStatus)
        if (transitionValidation is DomainResult.Error) {
            return transitionValidation
        }

        val startedAt = if (targetStatus == DesignStatus.IN_DESIGN && currentProject.startedAt == null) {
            timestamp
        } else {
            currentProject.startedAt
        }

        val completedAt = if (targetStatus == DesignStatus.APPROVED || targetStatus == DesignStatus.FINALIZED || targetStatus == DesignStatus.HANDED_OFF_TO_PRODUCTION) {
            timestamp
        } else {
            currentProject.completedAt
        }

        val updatedNotes = if (!notes.isNullOrBlank()) {
            if (currentProject.notes.isNullOrBlank()) notes else "${currentProject.notes}\n$notes"
        } else {
            currentProject.notes
        }

        val updatedProject = currentProject.copy(
            status = targetStatus,
            startedAt = startedAt,
            completedAt = completedAt,
            notes = updatedNotes,
            updatedAt = timestamp,
            updatedBy = actorName ?: actorId
        )

        val updateResult = dataSource.updateProject(updatedProject)
        if (updateResult is DomainResult.Success) {
            val eventType = if (targetStatus == DesignStatus.IN_DESIGN && currentProject.status != DesignStatus.IN_DESIGN) {
                DesignActivityType.PROJECT_STARTED
            } else {
                DesignActivityType.STATUS_CHANGED
            }

            recordActivity(
                projectId = updatedProject.projectId,
                productionJobId = updatedProject.productionJobId,
                designerId = updatedProject.assignedDesignerId,
                designerName = updatedProject.assignedDesignerName,
                eventType = eventType,
                message = "Status updated from ${currentProject.status.defaultLabel} to ${targetStatus.defaultLabel}." + (notes?.let { " Notes: $it" } ?: ""),
                timestamp = timestamp,
                createdBy = actorName ?: actorId
            )
        }
        return updateResult
    }

    override suspend fun startDesign(
        projectId: String,
        actorId: String?,
        actorName: String?,
        notes: String?,
        timestamp: String
    ): DomainResult<DesignProject> {
        return updateDesignStatus(
            projectId = projectId,
            targetStatus = DesignStatus.IN_DESIGN,
            actorId = actorId,
            actorName = actorName,
            notes = notes,
            timestamp = timestamp
        )
    }

    override suspend fun assignDesigner(
        projectId: String,
        designerId: String,
        designerName: String,
        assignedBy: String?,
        notes: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignProject> = repositoryMutex.withLock {
        val currentProject = when (val res = dataSource.fetchProjectById(projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = DesignAssignmentValidator.validateAssignment(currentProject, designerId, designerName, callerRole)
        if (validation is DomainResult.Error) {
            return validation
        }

        val assignmentId = "asgn-des-" + UUID.randomUUID().toString()
        val assignment = DesignAssignment(
            assignmentId = assignmentId,
            projectId = projectId,
            designerId = designerId,
            designerName = designerName,
            assignedAt = timestamp,
            assignedBy = assignedBy,
            status = DesignAssignmentStatus.ACTIVE,
            notes = notes
        )

        dataSource.insertAssignment(assignment)

        val nextStatus = if (currentProject.status == DesignStatus.NOT_STARTED) DesignStatus.ASSIGNED else currentProject.status
        val updatedProject = currentProject.copy(
            assignedDesignerId = designerId,
            assignedDesignerName = designerName,
            status = nextStatus,
            updatedAt = timestamp,
            updatedBy = assignedBy
        )

        val updateResult = dataSource.updateProject(updatedProject)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                projectId = updatedProject.projectId,
                productionJobId = updatedProject.productionJobId,
                designerId = designerId,
                designerName = designerName,
                eventType = DesignActivityType.DESIGNER_ASSIGNED,
                message = "Assigned designer '$designerName' ($designerId) by ${assignedBy ?: "Manager"}.",
                timestamp = timestamp,
                createdBy = assignedBy
            )
        }
        return updateResult
    }

    override suspend fun reassignDesigner(
        projectId: String,
        newDesignerId: String,
        newDesignerName: String,
        reassignedBy: String?,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignProject> = repositoryMutex.withLock {
        val currentProject = when (val res = dataSource.fetchProjectById(projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val allAssignments = dataSource.observeAssignments().first()
        val activeAssignment = allAssignments.find { it.projectId == projectId && it.isActive }

        val validation = DesignAssignmentValidator.validateReassignment(
            project = currentProject,
            currentAssignment = activeAssignment,
            newDesignerId = newDesignerId,
            newDesignerName = newDesignerName,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        if (activeAssignment != null) {
            val updatedOldAssignment = activeAssignment.copy(
                status = DesignAssignmentStatus.REASSIGNED,
                reassignedAt = timestamp,
                reassignedBy = reassignedBy
            )
            dataSource.updateAssignment(updatedOldAssignment)
        }

        val newAssignmentId = "asgn-des-" + UUID.randomUUID().toString()
        val newAssignment = DesignAssignment(
            assignmentId = newAssignmentId,
            projectId = projectId,
            designerId = newDesignerId,
            designerName = newDesignerName,
            assignedAt = timestamp,
            assignedBy = reassignedBy,
            status = DesignAssignmentStatus.ACTIVE,
            notes = reason
        )
        dataSource.insertAssignment(newAssignment)

        val updatedProject = currentProject.copy(
            assignedDesignerId = newDesignerId,
            assignedDesignerName = newDesignerName,
            updatedAt = timestamp,
            updatedBy = reassignedBy
        )

        val updateResult = dataSource.updateProject(updatedProject)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                projectId = updatedProject.projectId,
                productionJobId = updatedProject.productionJobId,
                designerId = newDesignerId,
                designerName = newDesignerName,
                eventType = DesignActivityType.DESIGNER_REASSIGNED,
                message = "Reassigned designer from '${activeAssignment?.designerName ?: "None"}' to '$newDesignerName'." + (reason?.let { " Reason: $it" } ?: ""),
                timestamp = timestamp,
                createdBy = reassignedBy
            )
        }
        return updateResult
    }

    override suspend fun unassignDesigner(
        projectId: String,
        unassignedBy: String?,
        reason: String?,
        timestamp: String,
        callerRole: UserRole?
    ): DomainResult<DesignProject> = repositoryMutex.withLock {
        val currentProject = when (val res = dataSource.fetchProjectById(projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val allAssignments = dataSource.observeAssignments().first()
        val activeAssignment = allAssignments.find { it.projectId == projectId && it.isActive }

        val validation = DesignAssignmentValidator.validateUnassignment(
            project = currentProject,
            currentAssignment = activeAssignment,
            callerRole = callerRole
        )
        if (validation is DomainResult.Error) {
            return validation
        }

        if (activeAssignment != null) {
            val updatedOldAssignment = activeAssignment.copy(
                status = DesignAssignmentStatus.UNASSIGNED,
                unassignedAt = timestamp,
                unassignedBy = unassignedBy
            )
            dataSource.updateAssignment(updatedOldAssignment)
        }

        val nextStatus = if (currentProject.status == DesignStatus.ASSIGNED) DesignStatus.NOT_STARTED else currentProject.status
        val updatedProject = currentProject.copy(
            assignedDesignerId = null,
            assignedDesignerName = null,
            status = nextStatus,
            updatedAt = timestamp,
            updatedBy = unassignedBy
        )

        val updateResult = dataSource.updateProject(updatedProject)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                projectId = updatedProject.projectId,
                productionJobId = updatedProject.productionJobId,
                designerId = null,
                designerName = null,
                eventType = DesignActivityType.DESIGNER_UNASSIGNED,
                message = "Unassigned designer '${activeAssignment?.designerName ?: "None"}'." + (reason?.let { " Reason: $it" } ?: ""),
                timestamp = timestamp,
                createdBy = unassignedBy
            )
        }
        return updateResult
    }

    override suspend fun cancelDesignProject(
        projectId: String,
        reason: String,
        cancelledBy: String?,
        timestamp: String
    ): DomainResult<DesignProject> = repositoryMutex.withLock {
        val currentProject = when (val res = dataSource.fetchProjectById(projectId)) {
            is DomainResult.Success -> res.data
            is DomainResult.Error -> return res
            is DomainResult.Loading -> return DomainResult.Error(message = "Data source is loading.")
        }

        val validation = DesignLifecycleValidator.validateCancellation(currentProject, reason)
        if (validation is DomainResult.Error) {
            return validation
        }

        val allAssignments = dataSource.observeAssignments().first()
        val activeAssignment = allAssignments.find { it.projectId == projectId && it.isActive }
        if (activeAssignment != null) {
            val updatedOldAssignment = activeAssignment.copy(
                status = DesignAssignmentStatus.UNASSIGNED,
                unassignedAt = timestamp,
                unassignedBy = cancelledBy
            )
            dataSource.updateAssignment(updatedOldAssignment)
        }

        val cancellationNote = "Cancelled: $reason"
        val updatedNotes = if (currentProject.notes.isNullOrBlank()) cancellationNote else "${currentProject.notes}\n$cancellationNote"

        val updatedProject = currentProject.copy(
            status = DesignStatus.CANCELLED,
            notes = updatedNotes,
            updatedAt = timestamp,
            updatedBy = cancelledBy
        )

        val updateResult = dataSource.updateProject(updatedProject)
        if (updateResult is DomainResult.Success) {
            recordActivity(
                projectId = updatedProject.projectId,
                productionJobId = updatedProject.productionJobId,
                eventType = DesignActivityType.PROJECT_CANCELLED,
                message = "Project cancelled. Reason: $reason",
                timestamp = timestamp,
                createdBy = cancelledBy
            )
        }
        return updateResult
    }

    override fun observeDesignAssignments(): Flow<List<DesignAssignment>> = dataSource.observeAssignments()

    override fun getAssignmentsForProject(projectId: String): Flow<List<DesignAssignment>> {
        return dataSource.observeAssignments().map { assignments ->
            assignments.filter { it.projectId == projectId }
        }
    }

    override fun getAssignmentsForDesigner(designerId: String): Flow<List<DesignAssignment>> {
        return dataSource.observeAssignments().map { assignments ->
            assignments.filter { it.designerId == designerId }
        }
    }

    override fun observeDesignActivityEvents(): Flow<List<DesignActivityEvent>> = dataSource.observeActivityEvents()

    override fun getActivityEventsForProject(projectId: String): Flow<List<DesignActivityEvent>> {
        return dataSource.observeActivityEvents().map { events ->
            events.filter { it.projectId == projectId }
        }
    }

    override fun getAvailableDesigners(): List<ProductionOperator> {
        return listOf(
            ProductionOperator("des-01", "তানভীর হাসান (Tanveer Hassan)", UserRole.DESIGNER, "+8801711003344"),
            ProductionOperator("des-02", "নুসরাত জাহান (Nusrat Jahan)", UserRole.DESIGNER, "+8801711003355"),
            ProductionOperator("des-03", "আরিফুল ইসলাম (Ariful Islam)", UserRole.DESIGNER, "+8801711003366")
        )
    }
}
