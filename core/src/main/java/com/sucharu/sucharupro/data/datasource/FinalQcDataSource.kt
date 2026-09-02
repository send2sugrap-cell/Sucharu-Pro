package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.qc.FinalQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseAuthorization
import kotlinx.coroutines.flow.Flow

/**
 * Data source contract for Final QC inspections, Release Authorizations, and Audit Activity (Module 06 Step 07).
 */
interface FinalQcDataSource {

    fun observeFinalQcList(): Flow<List<FinalQcInspection>>

    fun observeFinalQcById(finalQcId: String): Flow<FinalQcInspection?>

    fun getFinalQcById(finalQcId: String): Flow<FinalQcInspection?> = observeFinalQcById(finalQcId)

    suspend fun findFinalQcById(finalQcId: String): FinalQcInspection?

    suspend fun insertFinalQc(inspection: FinalQcInspection)

    suspend fun updateFinalQc(inspection: FinalQcInspection)

    fun observeReleaseAuthorization(productionJobId: String): Flow<FinalQcReleaseAuthorization?>

    suspend fun findReleaseAuthorizationByJob(productionJobId: String): FinalQcReleaseAuthorization?

    suspend fun findReleaseAuthorizationById(releaseAuthorizationId: String): FinalQcReleaseAuthorization?

    suspend fun insertReleaseAuthorization(authorization: FinalQcReleaseAuthorization)

    fun observeActivityEvents(finalQcId: String): Flow<List<FinalQcActivityEvent>>

    suspend fun insertActivityEvent(event: FinalQcActivityEvent)
}
