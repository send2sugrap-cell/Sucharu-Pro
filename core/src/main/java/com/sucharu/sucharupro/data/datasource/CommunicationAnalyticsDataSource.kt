package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsActivityEvent
import com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAnalyticsSnapshot
import kotlinx.coroutines.flow.Flow

interface CommunicationAnalyticsDataSource {
    suspend fun saveSnapshot(snapshot: CommunicationAnalyticsSnapshot): CommunicationAnalyticsSnapshot
    suspend fun getSnapshotById(projectId: String, snapshotId: String): CommunicationAnalyticsSnapshot?
    suspend fun getSnapshots(projectId: String): List<CommunicationAnalyticsSnapshot>
    fun observeSnapshots(projectId: String): Flow<List<CommunicationAnalyticsSnapshot>>
    
    suspend fun recordActivity(event: CommunicationAnalyticsActivityEvent): CommunicationAnalyticsActivityEvent
    suspend fun getActivityEvents(projectId: String): List<CommunicationAnalyticsActivityEvent>
    fun observeActivityEvents(projectId: String): Flow<List<CommunicationAnalyticsActivityEvent>>
    
    // Step 10: Governance Actions
    suspend fun saveGovernanceAction(action: com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceAction): com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceAction
    suspend fun getGovernanceActions(projectId: String): List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationGovernanceAction>
    
    // Step 10: Export Requests
    suspend fun saveExportRequest(request: com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest): com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest
    suspend fun updateExportRequest(request: com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest): com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest
    suspend fun getExportRequest(projectId: String, exportId: String): com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest?
    suspend fun getExportRequests(projectId: String): List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest>
    fun observeExportRequests(projectId: String): Flow<List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationExportRequest>>
    
    // Step 10: Audit Events (Append-only)
    suspend fun saveAuditEvent(event: com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAuditEvent): com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAuditEvent
    suspend fun getAuditEvents(projectId: String): List<com.sucharu.sucharupro.domain.model.communication.analytics.CommunicationAuditEvent>
}
