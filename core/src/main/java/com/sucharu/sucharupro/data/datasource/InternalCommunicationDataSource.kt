package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.internal.*
import kotlinx.coroutines.flow.Flow

/**
 * Data source contract for Internal Staff & Team Communications (Module 10 Step 03).
 */
interface InternalCommunicationDataSource {

    suspend fun saveCommunication(communication: InternalCommunication)

    suspend fun getCommunicationById(projectId: String, communicationId: String): InternalCommunication?

    suspend fun getCommunicationByNo(projectId: String, communicationNo: String): InternalCommunication?

    suspend fun getByIdempotencyKey(projectId: String, idempotencyKey: String): InternalCommunication?

    fun observeCommunicationsByProject(projectId: String): Flow<List<InternalCommunication>>

    fun observeCommunicationsForUser(projectId: String, userId: String): Flow<List<InternalCommunication>>

    suspend fun generateCommunicationNumber(projectId: String): String

    // Thread management
    suspend fun saveThread(thread: InternalCommunicationThread)

    suspend fun getThreadById(projectId: String, threadId: String): InternalCommunicationThread?

    suspend fun getThreadMessages(projectId: String, threadId: String): List<InternalCommunication>

    suspend fun generateThreadNumber(projectId: String): String

    // Mentions
    suspend fun saveMention(mention: InternalCommunicationMention)

    suspend fun getMentionsForUser(projectId: String, userId: String): List<InternalCommunicationMention>

    suspend fun generateMentionNumber(projectId: String): String

    // Acknowledgements
    suspend fun saveAcknowledgement(acknowledgement: InternalCommunicationAcknowledgement)

    suspend fun getAcknowledgements(projectId: String, communicationId: String): List<InternalCommunicationAcknowledgement>

    suspend fun generateAcknowledgementNumber(projectId: String): String

    // Recipients
    suspend fun saveRecipient(recipient: InternalCommunicationRecipient)

    suspend fun getRecipients(projectId: String, communicationId: String): List<InternalCommunicationRecipient>

    // Audit Activity
    suspend fun recordActivityEvent(event: InternalCommunicationActivityEvent)

    suspend fun getActivityEvents(projectId: String, communicationId: String): List<InternalCommunicationActivityEvent>
}
