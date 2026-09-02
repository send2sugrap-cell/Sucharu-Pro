package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.qc.FinalQcActivityEvent
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseAuthorization
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory, thread-safe, reactive implementation of [FinalQcDataSource] (Module 06 Step 07).
 */
class FakeFinalQcDataSource : FinalQcDataSource {

    private val mutex = Mutex()

    private val finalQcListState = MutableStateFlow<List<FinalQcInspection>>(emptyList())
    private val releaseAuthorizationsState = MutableStateFlow<List<FinalQcReleaseAuthorization>>(emptyList())
    private val activityEventsState = MutableStateFlow<List<FinalQcActivityEvent>>(emptyList())

    override fun observeFinalQcList(): Flow<List<FinalQcInspection>> =
        finalQcListState.asStateFlow()

    override fun observeFinalQcById(finalQcId: String): Flow<FinalQcInspection?> =
        finalQcListState.map { list -> list.find { it.finalQcId == finalQcId } }

    override suspend fun findFinalQcById(finalQcId: String): FinalQcInspection? = mutex.withLock {
        finalQcListState.value.find { it.finalQcId == finalQcId }
    }

    override suspend fun insertFinalQc(inspection: FinalQcInspection) = mutex.withLock {
        val current = finalQcListState.value.toMutableList()
        current.removeAll { it.finalQcId == inspection.finalQcId }
        current.add(inspection)
        finalQcListState.value = current
    }

    override suspend fun updateFinalQc(inspection: FinalQcInspection) = mutex.withLock {
        val current = finalQcListState.value.toMutableList()
        val index = current.indexOfFirst { it.finalQcId == inspection.finalQcId }
        if (index != -1) {
            current[index] = inspection
        } else {
            current.add(inspection)
        }
        finalQcListState.value = current
    }

    override fun observeReleaseAuthorization(productionJobId: String): Flow<FinalQcReleaseAuthorization?> =
        releaseAuthorizationsState.map { list -> list.find { it.productionJobId == productionJobId } }

    override suspend fun findReleaseAuthorizationByJob(productionJobId: String): FinalQcReleaseAuthorization? = mutex.withLock {
        releaseAuthorizationsState.value.find { it.productionJobId == productionJobId }
    }

    override suspend fun findReleaseAuthorizationById(releaseAuthorizationId: String): FinalQcReleaseAuthorization? = mutex.withLock {
        releaseAuthorizationsState.value.find { it.releaseAuthorizationId == releaseAuthorizationId }
    }

    override suspend fun insertReleaseAuthorization(authorization: FinalQcReleaseAuthorization) = mutex.withLock {
        val current = releaseAuthorizationsState.value.toMutableList()
        current.removeAll { it.releaseAuthorizationId == authorization.releaseAuthorizationId }
        current.add(authorization)
        releaseAuthorizationsState.value = current
    }

    override fun observeActivityEvents(finalQcId: String): Flow<List<FinalQcActivityEvent>> =
        activityEventsState.map { list -> list.filter { it.finalQcId == finalQcId } }

    override suspend fun insertActivityEvent(event: FinalQcActivityEvent) = mutex.withLock {
        val current = activityEventsState.value.toMutableList()
        current.add(event)
        activityEventsState.value = current
    }
}
