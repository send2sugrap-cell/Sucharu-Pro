package com.sucharu.sucharupro.domain.model.communication.internal

import com.sucharu.sucharupro.domain.model.user.UserRole

/**
 * Recipient record for internal communication tracking (Module 10 Step 03).
 */
data class InternalCommunicationRecipient(
    val recipientId: String,
    val communicationId: String,
    val projectId: String,
    val recipientType: InternalCommunicationRecipientType,
    val recipientUserId: String? = null,
    val recipientRole: UserRole? = null,
    val teamId: String? = null,
    val departmentId: String? = null,
    val recipientStatus: InternalCommunicationStatus = InternalCommunicationStatus.QUEUED,
    val readAt: Long? = null,
    val acknowledgedAt: Long? = null
) {
    init {
        require(recipientId.isNotBlank()) { "Recipient ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
    }

    val isRead: Boolean
        get() = readAt != null || recipientStatus == InternalCommunicationStatus.READ || isAcknowledged

    val isAcknowledged: Boolean
        get() = acknowledgedAt != null || recipientStatus == InternalCommunicationStatus.ACKNOWLEDGED
}

/**
 * Conversation Thread Aggregate Root (Module 10 Step 03).
 */
data class InternalCommunicationThread(
    val threadId: String,
    val projectId: String,
    val rootCommunicationId: String,
    val subject: String,
    val participantUserIds: Set<String> = emptySet(),
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
) {
    init {
        require(threadId.isNotBlank()) { "Thread ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(rootCommunicationId.isNotBlank()) { "Root Communication ID cannot be blank." }
        require(subject.isNotBlank()) { "Thread Subject cannot be blank." }
        require(createdBy.isNotBlank()) { "Created By cannot be blank." }
        require(createdAt > 0) { "Creation timestamp must be positive." }
        require(updatedAt >= createdAt) { "Updated timestamp cannot precede creation timestamp." }
    }
}

/**
 * User Mention Record (Module 10 Step 03).
 */
data class InternalCommunicationMention(
    val mentionId: String,
    val projectId: String,
    val communicationId: String,
    val mentionedUserId: String,
    val mentionedBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val readAt: Long? = null
) {
    init {
        require(mentionId.isNotBlank()) { "Mention ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(mentionedUserId.isNotBlank()) { "Mentioned User ID cannot be blank." }
        require(mentionedBy.isNotBlank()) { "Mentioned By cannot be blank." }
        require(createdAt > 0) { "Timestamp must be positive." }
    }
}

/**
 * Immutable Acknowledgement Record (Module 10 Step 03).
 */
data class InternalCommunicationAcknowledgement(
    val acknowledgementId: String,
    val projectId: String,
    val communicationId: String,
    val recipientUserId: String,
    val acknowledgedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
) {
    init {
        require(acknowledgementId.isNotBlank()) { "Acknowledgement ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(recipientUserId.isNotBlank()) { "Recipient User ID cannot be blank." }
        require(acknowledgedAt > 0) { "Acknowledged timestamp must be positive." }
    }
}

/**
 * Metadata-only Attachment record (Module 10 Step 03).
 */
data class InternalCommunicationAttachment(
    val attachmentId: String,
    val projectId: String,
    val communicationId: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val storageReference: String,
    val uploadedBy: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(attachmentId.isNotBlank()) { "Attachment ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(fileName.isNotBlank()) { "File Name cannot be blank." }
        require(mimeType.isNotBlank()) { "MIME type cannot be blank." }
        require(fileSize > 0) { "File size must be positive." }
        require(storageReference.isNotBlank()) { "Storage reference cannot be blank." }
        require(uploadedBy.isNotBlank()) { "Uploaded By cannot be blank." }
    }
}

/**
 * Append-only audit activity event (Module 10 Step 03).
 */
data class InternalCommunicationActivityEvent(
    val eventId: String,
    val projectId: String,
    val communicationId: String,
    val eventType: String,
    val previousStatus: InternalCommunicationStatus?,
    val newStatus: InternalCommunicationStatus?,
    val actorUserId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(communicationId.isNotBlank()) { "Communication ID cannot be blank." }
        require(eventType.isNotBlank()) { "Event Type cannot be blank." }
        require(actorUserId.isNotBlank()) { "Actor User ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
