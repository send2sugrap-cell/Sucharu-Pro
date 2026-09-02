package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.customer.*
import kotlinx.coroutines.flow.Flow

/**
 * Data source contract for Customer Communications, History, and Engagement Events (Module 10 Step 02).
 */
interface CustomerCommunicationDataSource {

    suspend fun saveCommunication(communication: CustomerCommunication)

    suspend fun getCommunicationById(projectId: String, communicationId: String): CustomerCommunication?

    suspend fun getCommunicationByNo(projectId: String, communicationNo: String): CustomerCommunication?

    suspend fun getByIdempotencyKey(projectId: String, idempotencyKey: String): CustomerCommunication?

    suspend fun getByDuplicateCriteria(
        projectId: String,
        customerId: String,
        communicationType: CustomerCommunicationType,
        referenceType: String?,
        referenceId: String?,
        groupKey: String?
    ): CustomerCommunication?

    fun observeCommunicationsByProject(projectId: String): Flow<List<CustomerCommunication>>

    fun observeCommunicationsByCustomer(projectId: String, customerId: String): Flow<List<CustomerCommunication>>

    suspend fun getCommunicationsByReference(
        projectId: String,
        referenceType: String,
        referenceId: String
    ): List<CustomerCommunication>

    suspend fun generateCommunicationNumber(projectId: String): String

    // History
    suspend fun recordHistory(history: CustomerCommunicationHistory)

    suspend fun getHistory(projectId: String, communicationId: String): List<CustomerCommunicationHistory>

    // Engagement Events
    suspend fun recordEngagementEvent(event: CustomerEngagementEvent)

    suspend fun getEngagementEventsByCustomer(projectId: String, customerId: String): List<CustomerEngagementEvent>

    fun observeEngagementEvents(projectId: String): Flow<List<CustomerEngagementEvent>>
}
