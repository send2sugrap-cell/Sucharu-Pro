package com.sucharu.sucharupro.domain.model.communication.vendor.document

import com.sucharu.sucharupro.domain.model.notification.NotificationPriority

/**
 * Formal document request issued by internal staff/manager/admin to a vendor (Module 10 Step 06).
 */
data class VendorDocumentRequest(
    val requestId: String,
    val requestNo: String,
    val projectId: String,
    val vendorId: String,
    val documentType: VendorDocumentType,
    val title: String,
    val description: String = "",
    val required: Boolean = true,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val requestedBy: String,
    val requestedAt: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val status: VendorDocumentRequestStatus = VendorDocumentRequestStatus.OPEN,
    val communicationId: String? = null,
    val notificationId: String? = null,
    val submittedDocumentId: String? = null,
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val cancelledAt: Long? = null
) {
    init {
        require(requestId.isNotBlank()) { "requestId cannot be blank" }
        require(requestNo.isNotBlank()) { "requestNo cannot be blank" }
        require(projectId.isNotBlank()) { "projectId cannot be blank" }
        require(vendorId.isNotBlank()) { "vendorId cannot be blank" }
        require(title.isNotBlank()) { "title cannot be blank" }
        require(requestedBy.isNotBlank()) { "requestedBy cannot be blank" }
    }

    val isOverdue: Boolean
        get() = dueDate != null && dueDate < System.currentTimeMillis() && status == VendorDocumentRequestStatus.OPEN
}
