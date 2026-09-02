package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsActivityEvent
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FakeCommunicationAnalyticsDataSource : CommunicationAnalyticsDataSource {

    private val mutex = Mutex()
    private val snapshotsState = MutableStateFlow<List<CommunicationAnalyticsSnapshot>>(emptyList())
    private val activityEventsState = MutableStateFlow<List<CommunicationAnalyticsActivityEvent>>(emptyList())
    
    // Step 10 States
    private val governanceActionsState = MutableStateFlow<List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceAction>>(emptyList())
    private val exportRequestsState = MutableStateFlow<List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest>>(emptyList())
    private val auditEventsState = MutableStateFlow<List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAuditEvent>>(emptyList())

    override suspend fun saveSnapshot(snapshot: CommunicationAnalyticsSnapshot): CommunicationAnalyticsSnapshot {
        mutex.withLock {
            snapshotsState.update { current ->
                current.filter { it.snapshotId != snapshot.snapshotId } + snapshot
            }
        }
        return snapshot
    }

    override suspend fun getSnapshotById(projectId: String, snapshotId: String): CommunicationAnalyticsSnapshot? {
        return mutex.withLock {
            snapshotsState.value.find { it.projectId == projectId && it.snapshotId == snapshotId }
        }
    }

    override suspend fun getSnapshots(projectId: String): List<CommunicationAnalyticsSnapshot> {
        return mutex.withLock {
            snapshotsState.value.filter { it.projectId == projectId }.sortedByDescending { it.generatedAt }
        }
    }

    override fun observeSnapshots(projectId: String): Flow<List<CommunicationAnalyticsSnapshot>> {
        return snapshotsState.map { list -> list.filter { it.projectId == projectId }.sortedByDescending { it.generatedAt } }
    }

    override suspend fun recordActivity(event: CommunicationAnalyticsActivityEvent): CommunicationAnalyticsActivityEvent {
        mutex.withLock {
            activityEventsState.update { current ->
                current + event
            }
        }
        return event
    }

    override suspend fun getActivityEvents(projectId: String): List<CommunicationAnalyticsActivityEvent> {
        return mutex.withLock {
            activityEventsState.value.filter { it.projectId == projectId }.sortedByDescending { it.timestamp }
        }
    }

    override fun observeActivityEvents(projectId: String): Flow<List<CommunicationAnalyticsActivityEvent>> {
        return activityEventsState.map { list -> list.filter { it.projectId == projectId }.sortedByDescending { it.timestamp } }
    }
    
    // Step 10: Governance Actions
    override suspend fun saveGovernanceAction(action: com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceAction): com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceAction {
        mutex.withLock {
            governanceActionsState.update { current -> current + action }
        }
        return action
    }

    override suspend fun getGovernanceActions(projectId: String): List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceAction> {
        return mutex.withLock {
            governanceActionsState.value.filter { it.projectId == projectId }.sortedByDescending { it.timestamp }
        }
    }

    // Step 10: Export Requests
    override suspend fun saveExportRequest(request: com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest): com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest {
        mutex.withLock {
            exportRequestsState.update { current ->
                current.filter { it.exportId != request.exportId } + request
            }
        }
        return request
    }

    override suspend fun updateExportRequest(request: com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest): com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest {
        return saveExportRequest(request)
    }

    override suspend fun getExportRequest(projectId: String, exportId: String): com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest? {
        return mutex.withLock {
            exportRequestsState.value.find { it.projectId == projectId && it.exportId == exportId }
        }
    }

    override suspend fun getExportRequests(projectId: String): List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest> {
        return mutex.withLock {
            exportRequestsState.value.filter { it.projectId == projectId }.sortedByDescending { it.requestedAt }
        }
    }

    override fun observeExportRequests(projectId: String): Flow<List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest>> {
        return exportRequestsState.map { list -> list.filter { it.projectId == projectId }.sortedByDescending { it.requestedAt } }
    }

    // Step 10: Audit Events
    override suspend fun saveAuditEvent(event: com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAuditEvent): com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAuditEvent {
        mutex.withLock {
            auditEventsState.update { current -> current + event }
        }
        return event
    }

    override suspend fun getAuditEvents(projectId: String): List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAuditEvent> {
        return mutex.withLock {
            auditEventsState.value.filter { it.projectId == projectId }.sortedByDescending { it.timestamp }
        }
    }
}
