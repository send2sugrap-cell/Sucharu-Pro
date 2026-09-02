package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.design.DesignProductionHandoff
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Controlled Production Handoff Authorization (Module 05 Step 05).
 */
interface DesignProductionHandoffRepository {

    /**
     * Pre-validates whether an approval and its referenced proof/artwork versions satisfy all 15 handoff criteria.
     */
    suspend fun canHandoffToProduction(
        approvalId: String,
        callerRole: UserRole? = null
    ): DomainResult<Boolean>

    /**
     * Atomically authorizes production handoff for a FINAL_LOCKED approval, recording traceability and audit events.
     * Idempotent: repeated calls for the same approval safely return the existing authorization.
     */
    suspend fun authorizeProductionHandoff(
        approvalId: String,
        authorizedBy: String,
        authorizedByName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<DesignProductionHandoff>

    /** Reactive stream observing a single handoff record by [approvalId]. */
    fun getHandoffByApprovalId(approvalId: String): Flow<DesignProductionHandoff?>

    /** Reactive stream of handoff records associated with a [projectId]. */
    fun getHandoffForProject(projectId: String): Flow<List<DesignProductionHandoff>>

    /** Reactive stream of handoff records associated with a [productionJobId]. */
    fun getHandoffForJob(productionJobId: String): Flow<List<DesignProductionHandoff>>

    /** Reactive stream observing all authorized handoffs. */
    fun observeHandoffs(): Flow<List<DesignProductionHandoff>>
}
