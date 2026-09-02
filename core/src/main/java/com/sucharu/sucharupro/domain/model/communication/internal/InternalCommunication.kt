package com.sucharu.sucharupro.domain.model.communication.internal

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Aggregate Root for an Internal Communication message (Module 10 Step 03).
 */
data class InternalCommunication(
    val communicationId: String,
    val communicationNo: String,
    val projectId: String,
    val senderUserId: String,
    val senderRole: UserRole,
    val recipientType: InternalCommunicationRecipientType,
    val recipientUserIds: Set<String> = emptySet(),
    val recipientRole: UserRole? = null,
    val teamId: String? = null,
    val departmentId: String? = null,
    val communicationType: InternalCommunicationType,
    val priority: InternalCommunicationPriority = InternalCommunicationPriority.NORMAL,
    val status: InternalCommunicationStatus = InternalCommunicationStatus.DRAFT,
    val subject: String,
    val message: String,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val threadId: String? = null,
    val parentCommunicationId: String? = null,
    val requiresAcknowledgement: Boolean = false,
    val scheduledAt: Long? = null,
    val sentAt: Long? = null,
    val deliveredAt: Long? = null,
    val readAt: Long? = null,
    val acknowledgedAt: Long? = null,
    val createdBy: String,
    val updatedBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(communicationNo.isNotBlank()) { "Communication Number cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(senderUserId.isNotBlank()) { "Sender User ID cannot be blank." }
        require(subject.isNotBlank()) { "Subject cannot be blank." }
        require(message.isNotBlank()) { "Message cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(updatedBy.isNotBlank()) { "Updated By cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }

        // Metadata safety check: Ensure no sensitive keys are stored
        val forbiddenKeys = listOf("password", "token", "secret", "cvv", "card_number", "pin", "api_key", "bearer", "private_key")
        for (key in metadata.keys) {
            val lower = key.lowercase()
            require(forbiddenKeys.none { lower.contains(it) }) {
                "Sensitive key '$key' is prohibited in communication metadata."
            }
        }
    }

    val isRead: Boolean
        get() = readAt != null || status == InternalCommunicationStatus.READ || isAcknowledged

    val isAcknowledged: Boolean
        get() = acknowledgedAt != null || status == InternalCommunicationStatus.ACKNOWLEDGED
}

/**
 * Project-scoped summary statistics for internal communications (Module 10 Step 03).
 */
data class InternalCommunicationSummary(
    val projectId: String,
    val totalMessages: Int = 0,
    val unreadMessages: Int = 0,
    val urgentMessages: Int = 0,
    val criticalMessages: Int = 0,
    val pendingAcknowledgements: Int = 0,
    val todayMessages: Int = 0,
    val teamMessages: Int = 0,
    val departmentMessages: Int = 0,
    val directMessages: Int = 0,
    val failedMessages: Int = 0
)

/**
 * Provider-neutral interface for scheduling and processing internal communications (Module 10 Step 03).
 */
interface InternalCommunicationScheduler {
    suspend fun schedule(projectId: String, communicationId: String, scheduledAt: Long): DomainResult<Unit>
    suspend fun cancelScheduled(projectId: String, communicationId: String): DomainResult<Unit>
    suspend fun processDueCommunications(projectId: String): DomainResult<List<String>>
}
