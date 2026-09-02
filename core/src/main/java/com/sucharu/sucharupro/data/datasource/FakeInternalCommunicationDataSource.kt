package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.internal.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Concurrency-safe in-memory fake data source for Internal Communications (Module 10 Step 03).
 */
class FakeInternalCommunicationDataSource : InternalCommunicationDataSource {

    private val mutex = Mutex()
    private val communicationsState = MutableStateFlow<Map<String, InternalCommunication>>(emptyMap())
    private val threadsState = MutableStateFlow<Map<String, InternalCommunicationThread>>(emptyMap())
    private val mentionsState = MutableStateFlow<List<InternalCommunicationMention>>(emptyList())
    private val acknowledgementsState = MutableStateFlow<List<InternalCommunicationAcknowledgement>>(emptyList())
    private val recipientsState = MutableStateFlow<List<InternalCommunicationRecipient>>(emptyList())
    private val activityEventsState = MutableStateFlow<List<InternalCommunicationActivityEvent>>(emptyList())

    private var commCounter = 0
    private var threadCounter = 0
    private var mentionCounter = 0
    private var ackCounter = 0

    override suspend fun saveCommunication(communication: InternalCommunication): Unit = mutex.withLock {
        communicationsState.update { current ->
            current + (communication.communicationId to communication)
        }
    }

    override suspend fun getCommunicationById(projectId: String, communicationId: String): InternalCommunication? = mutex.withLock {
        communicationsState.value[communicationId]?.takeIf { it.projectId == projectId }
    }

    override suspend fun getCommunicationByNo(projectId: String, communicationNo: String): InternalCommunication? = mutex.withLock {
        communicationsState.value.values.firstOrNull { it.projectId == projectId && it.communicationNo == communicationNo }
    }

    override suspend fun getByIdempotencyKey(projectId: String, idempotencyKey: String): InternalCommunication? = mutex.withLock {
        communicationsState.value.values.firstOrNull { it.projectId == projectId && it.idempotencyKey == idempotencyKey }
    }

    override fun observeCommunicationsByProject(projectId: String): Flow<List<InternalCommunication>> {
        return communicationsState.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeCommunicationsForUser(projectId: String, userId: String): Flow<List<InternalCommunication>> {
        return communicationsState.map { map ->
            map.values
                .filter { c ->
                    c.projectId == projectId &&
                            (c.senderUserId == userId ||
                                    c.recipientUserIds.contains(userId) ||
                                    c.recipientType == InternalCommunicationRecipientType.ALL_INTERNAL_USERS ||
                                    c.recipientType == InternalCommunicationRecipientType.PROJECT)
                }
                .sortedByDescending { it.createdAt }
        }
    }

    override suspend fun generateCommunicationNumber(projectId: String): String = mutex.withLock {
        commCounter++
        val year = Calendar.getInstance().get(Calendar.YEAR)
        "ICM-$year-%05d".format(commCounter)
    }

    override suspend fun saveThread(thread: InternalCommunicationThread): Unit = mutex.withLock {
        threadsState.update { current ->
            current + (thread.threadId to thread)
        }
    }

    override suspend fun getThreadById(projectId: String, threadId: String): InternalCommunicationThread? = mutex.withLock {
        threadsState.value[threadId]?.takeIf { it.projectId == projectId }
    }

    override suspend fun getThreadMessages(projectId: String, threadId: String): List<InternalCommunication> = mutex.withLock {
        communicationsState.value.values
            .filter { it.projectId == projectId && it.threadId == threadId }
            .sortedBy { it.createdAt }
    }

    override suspend fun generateThreadNumber(projectId: String): String = mutex.withLock {
        threadCounter++
        val year = Calendar.getInstance().get(Calendar.YEAR)
        "THR-$year-%05d".format(threadCounter)
    }

    override suspend fun saveMention(mention: InternalCommunicationMention): Unit = mutex.withLock {
        mentionsState.update { current ->
            current + mention
        }
    }

    override suspend fun getMentionsForUser(projectId: String, userId: String): List<InternalCommunicationMention> = mutex.withLock {
        mentionsState.value
            .filter { it.projectId == projectId && it.mentionedUserId == userId }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun generateMentionNumber(projectId: String): String = mutex.withLock {
        mentionCounter++
        val year = Calendar.getInstance().get(Calendar.YEAR)
        "MNT-$year-%05d".format(mentionCounter)
    }

    override suspend fun saveAcknowledgement(acknowledgement: InternalCommunicationAcknowledgement): Unit = mutex.withLock {
        acknowledgementsState.update { current ->
            current + acknowledgement
        }
    }

    override suspend fun getAcknowledgements(projectId: String, communicationId: String): List<InternalCommunicationAcknowledgement> = mutex.withLock {
        acknowledgementsState.value
            .filter { it.projectId == projectId && it.communicationId == communicationId }
            .sortedBy { it.acknowledgedAt }
    }

    override suspend fun generateAcknowledgementNumber(projectId: String): String = mutex.withLock {
        ackCounter++
        val year = Calendar.getInstance().get(Calendar.YEAR)
        "ACK-$year-%05d".format(ackCounter)
    }

    override suspend fun saveRecipient(recipient: InternalCommunicationRecipient): Unit = mutex.withLock {
        recipientsState.update { current ->
            current + recipient
        }
    }

    override suspend fun getRecipients(projectId: String, communicationId: String): List<InternalCommunicationRecipient> = mutex.withLock {
        recipientsState.value.filter { it.projectId == projectId && it.communicationId == communicationId }
    }

    override suspend fun recordActivityEvent(event: InternalCommunicationActivityEvent): Unit = mutex.withLock {
        activityEventsState.update { current ->
            current + event
        }
    }

    override suspend fun getActivityEvents(projectId: String, communicationId: String): List<InternalCommunicationActivityEvent> = mutex.withLock {
        activityEventsState.value
            .filter { it.projectId == projectId && it.communicationId == communicationId }
            .sortedBy { it.timestamp }
    }
}
