package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignActivityEvent
import com.sucharu.sucharupro.domain.model.design.DesignAssignment
import com.sucharu.sucharupro.domain.model.design.DesignProject
import com.sucharu.sucharupro.domain.model.design.DesignStatus
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionOperator
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Design Domain Foundation in Sucharu Pro ERP (Module 05 Step 01).
 */
interface DesignProjectRepository {

    /** Reactive stream observing all design projects. */
    fun observeDesignProjects(): Flow<List<DesignProject>>

    /** Reactive stream observing a single design project by [projectId]. */
    fun getDesignProjectById(projectId: String): Flow<DesignProject?>

    /** Direct one-shot lookup of a design project by [projectId]. */
    suspend fun findDesignProjectById(projectId: String): DomainResult<DesignProject>

    /** Reactive stream of the design project associated with a specific [productionJobId]. */
    fun getDesignProjectForJob(productionJobId: String): Flow<DesignProject?>

    /** Reactive stream of design projects assigned to a specific [designerId]. */
    fun getDesignProjectsForDesigner(designerId: String): Flow<List<DesignProject>>

    /**
     * Initializes and persists a new [DesignProject] for an existing [ProductionJob].
     */
    suspend fun createDesignProject(
        job: ProductionJob,
        title: String? = null,
        notes: String? = null,
        createdBy: String? = null,
        timestamp: String
    ): DomainResult<DesignProject>

    /**
     * Updates an existing [DesignProject] after validating domain constraints.
     */
    suspend fun updateDesignProject(project: DesignProject): DomainResult<DesignProject>

    /**
     * Advances or changes the status of a [DesignProject].
     */
    suspend fun updateDesignStatus(
        projectId: String,
        targetStatus: DesignStatus,
        actorId: String? = null,
        actorName: String? = null,
        notes: String? = null,
        timestamp: String
    ): DomainResult<DesignProject>

    /**
     * Starts active design work, transitioning status from ASSIGNED to IN_DESIGN.
     */
    suspend fun startDesign(
        projectId: String,
        actorId: String? = null,
        actorName: String? = null,
        notes: String? = null,
        timestamp: String
    ): DomainResult<DesignProject>

    /**
     * Assigns a creative designer to a design project.
     */
    suspend fun assignDesigner(
        projectId: String,
        designerId: String,
        designerName: String,
        assignedBy: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignProject>

    /**
     * Reassigns an active design project to a new designer, preserving assignment history.
     */
    suspend fun reassignDesigner(
        projectId: String,
        newDesignerId: String,
        newDesignerName: String,
        reassignedBy: String? = null,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignProject>

    /**
     * Removes an active designer assignment from a design project.
     */
    suspend fun unassignDesigner(
        projectId: String,
        unassignedBy: String? = null,
        reason: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignProject>

    /**
     * Cancels a design project with a mandatory reason.
     */
    suspend fun cancelDesignProject(
        projectId: String,
        reason: String,
        cancelledBy: String? = null,
        timestamp: String
    ): DomainResult<DesignProject>

    /** Reactive stream of all designer assignment records. */
    fun observeDesignAssignments(): Flow<List<DesignAssignment>>

    /** Reactive stream of assignment history for a specific [projectId]. */
    fun getAssignmentsForProject(projectId: String): Flow<List<DesignAssignment>>

    /** Reactive stream of assignments associated with a specific [designerId]. */
    fun getAssignmentsForDesigner(designerId: String): Flow<List<DesignAssignment>>

    /** Reactive stream of all design activity audit events. */
    fun observeDesignActivityEvents(): Flow<List<DesignActivityEvent>>

    /** Reactive stream of activity audit events for a specific [projectId]. */
    fun getActivityEventsForProject(projectId: String): Flow<List<DesignActivityEvent>>

    /** Returns standard available creative designers. */
    fun getAvailableDesigners(): List<ProductionOperator>
}
