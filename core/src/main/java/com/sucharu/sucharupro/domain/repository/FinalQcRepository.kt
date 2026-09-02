package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.FinalQcEligibilityResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseAuthorization
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface contract for Final QC & Production Release (Module 06 Step 07).
 */
interface FinalQcRepository {

    fun observeFinalQcList(): Flow<List<FinalQcInspection>>

    fun observeFinalQcById(finalQcId: String): Flow<FinalQcInspection?>

    fun getFinalQcById(finalQcId: String): Flow<FinalQcInspection?> = observeFinalQcById(finalQcId)

    fun observeFinalQcByJob(productionJobId: String): Flow<List<FinalQcInspection>>

    fun observeFinalQcByProject(projectId: String): Flow<List<FinalQcInspection>>

    suspend fun findFinalQcById(finalQcId: String): DomainResult<FinalQcInspection>

    suspend fun createFinalQc(
        projectId: String,
        productionJobId: String,
        productionJobItemId: String? = null,
        totalQuantity: Int,
        quantityUnit: String = "units",
        preProductionQcId: String? = null,
        checklistId: String? = null,
        sourceDefectIds: List<String> = emptyList(),
        sourceReworkIds: List<String> = emptyList(),
        sourceReQcIds: List<String> = emptyList(),
        notes: String? = null,
        createdBy: String? = null,
        createdByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<FinalQcInspection>

    suspend fun assignInspector(
        finalQcId: String,
        inspectorId: String,
        inspectorName: String,
        assignedBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<FinalQcInspection>

    suspend fun reassignInspector(
        finalQcId: String,
        newInspectorId: String,
        newInspectorName: String,
        reassignedBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<FinalQcInspection>

    suspend fun unassignInspector(
        finalQcId: String,
        unassignedBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<FinalQcInspection>

    suspend fun startInspection(
        finalQcId: String,
        inspectorId: String,
        inspectorName: String? = null,
        notes: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<FinalQcInspection>

    suspend fun submitPass(
        finalQcId: String,
        acceptedQuantity: Int? = null,
        notes: String? = null,
        inspectorId: String,
        inspectorName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<FinalQcInspection>

    suspend fun submitFail(
        finalQcId: String,
        rejectedQuantity: Int,
        failureReason: String,
        notes: String? = null,
        inspectorId: String,
        inspectorName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<FinalQcInspection>

    suspend fun evaluateReleaseEligibility(finalQcId: String): DomainResult<FinalQcEligibilityResult>

    suspend fun authorizeProductionRelease(
        finalQcId: String,
        releaseNotes: String? = null,
        authorizedBy: String,
        authorizedByName: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<FinalQcReleaseAuthorization>

    suspend fun getReleaseAuthorization(productionJobId: String): DomainResult<FinalQcReleaseAuthorization?>

    fun observeReleaseAuthorization(productionJobId: String): Flow<FinalQcReleaseAuthorization?>

    suspend fun cancelFinalQc(
        finalQcId: String,
        reason: String,
        cancelledBy: String? = null,
        timestamp: String,
        callerRole: UserRole? = null
    ): DomainResult<FinalQcInspection>

    fun observeFinalQcActivity(finalQcId: String): Flow<List<FinalQcActivityEvent>>
}
