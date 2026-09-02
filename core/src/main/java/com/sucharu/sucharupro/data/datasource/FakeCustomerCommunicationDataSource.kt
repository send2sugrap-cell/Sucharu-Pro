package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.customer.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Concurrency-safe in-memory Fake implementation of CustomerCommunicationDataSource (Module 10 Step 02).
 */
class FakeCustomerCommunicationDataSource : CustomerCommunicationDataSource {

    private val mutex = Mutex()
    private val communicationsState = MutableStateFlow<Map<String, CustomerCommunication>>(emptyMap())
    private val historyState = MutableStateFlow<List<CustomerCommunicationHistory>>(emptyList())
    private val engagementEventsState = MutableStateFlow<List<CustomerEngagementEvent>>(emptyList())

    private var sequenceCounter = 0

    override suspend fun saveCommunication(communication: CustomerCommunication): Unit = mutex.withLock {
        communicationsState.update { current ->
            current + (communication.communicationId to communication)
        }
    }

    override suspend fun getCommunicationById(projectId: String, communicationId: String): CustomerCommunication? = mutex.withLock {
        communicationsState.value[communicationId]?.takeIf { it.projectId == projectId }
    }

    override suspend fun getCommunicationByNo(projectId: String, communicationNo: String): CustomerCommunication? = mutex.withLock {
        communicationsState.value.values.firstOrNull { it.projectId == projectId && it.communicationNo == communicationNo }
    }

    override suspend fun getByIdempotencyKey(projectId: String, idempotencyKey: String): CustomerCommunication? = mutex.withLock {
        communicationsState.value.values.firstOrNull { it.projectId == projectId && it.idempotencyKey == idempotencyKey }
    }

    override suspend fun getByDuplicateCriteria(
        projectId: String,
        customerId: String,
        communicationType: CustomerCommunicationType,
        referenceType: String?,
        referenceId: String?,
        groupKey: String?
    ): CustomerCommunication? = mutex.withLock {
        communicationsState.value.values.firstOrNull { c ->
            c.projectId == projectId &&
                    c.customerId == customerId &&
                    c.communicationType == communicationType &&
                    c.referenceType == referenceType &&
                    c.referenceId == referenceId &&
                    c.groupKey == groupKey &&
                    (c.status == CustomerCommunicationStatus.QUEUED ||
                            c.status == CustomerCommunicationStatus.SENT ||
                            c.status == CustomerCommunicationStatus.DELIVERED ||
                            c.status == CustomerCommunicationStatus.SCHEDULED)
        }
    }

    override fun observeCommunicationsByProject(projectId: String): Flow<List<CustomerCommunication>> {
        return communicationsState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeCommunicationsByCustomer(projectId: String, customerId: String): Flow<List<CustomerCommunication>> {
        return communicationsState.map { map ->
            map.values
                .filter { it.projectId == projectId && it.customerId == customerId }
                .sortedByDescending { it.createdAt }
        }
    }

    override suspend fun getCommunicationsByReference(
        projectId: String,
        referenceType: String,
        referenceId: String
    ): List<CustomerCommunication> = mutex.withLock {
        communicationsState.value.values
            .filter { it.projectId == projectId && it.referenceType == referenceType && it.referenceId == referenceId }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun generateCommunicationNumber(projectId: String): String = mutex.withLock {
        sequenceCounter++
        val year = Calendar.getInstance().get(Calendar.YEAR)
        "CCM-$year-%05d".format(sequenceCounter)
    }

    override suspend fun recordHistory(history: CustomerCommunicationHistory): Unit = mutex.withLock {
        historyState.update { current ->
            current + history
        }
    }

    override suspend fun getHistory(projectId: String, communicationId: String): List<CustomerCommunicationHistory> = mutex.withLock {
        historyState.value
            .filter { it.projectId == projectId && it.communicationId == communicationId }
            .sortedBy { it.timestamp }
    }

    override suspend fun recordEngagementEvent(event: CustomerEngagementEvent): Unit = mutex.withLock {
        engagementEventsState.update { current ->
            current + event
        }
    }

    override suspend fun getEngagementEventsByCustomer(projectId: String, customerId: String): List<CustomerEngagementEvent> = mutex.withLock {
        engagementEventsState.value
            .filter { it.projectId == projectId && it.customerId == customerId }
            .sortedByDescending { it.timestamp }
    }

    override fun observeEngagementEvents(projectId: String): Flow<List<CustomerEngagementEvent>> {
        return engagementEventsState.map { list ->
            list.filter { it.projectId == projectId }.sortedByDescending { it.timestamp }
        }
    }
}
