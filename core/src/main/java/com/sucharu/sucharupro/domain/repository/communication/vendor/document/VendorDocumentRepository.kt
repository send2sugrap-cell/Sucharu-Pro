package com.sucharu.sucharupro.domain.repository.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.*
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Vendor Document, Compliance & Record Communication Management (Module 10 Step 06).
 */
interface VendorDocumentRepository {

    // =========================================================================
    // Document Requests
    // =========================================================================

    suspend fun createDocumentRequest(
        projectId: String,
        vendorId: String,
        documentType: VendorDocumentType,
        title: String,
        description: String = "",
        required: Boolean = true,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        dueDate: Long? = null,
        idempotencyKey: String? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorDocumentRequest>

    suspend fun getDocumentRequest(
        projectId: String,
        requestId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorDocumentRequest>

    suspend fun listDocumentRequests(
        projectId: String,
        vendorId: String? = null,
        documentType: VendorDocumentType? = null,
        status: VendorDocumentRequestStatus? = null,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<List<VendorDocumentRequest>>

    suspend fun cancelDocumentRequest(
        projectId: String,
        requestId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorDocumentRequest>

    // =========================================================================
    // Document Submissions & Lifecycle
    // =========================================================================

    suspend fun submitDocument(
        projectId: String,
        vendorId: String,
        documentType: VendorDocumentType,
        title: String,
        description: String = "",
        fileReferenceId: String,
        fileName: String = "",
        mimeType: String = "",
        fileSize: Long = 0L,
        issueDate: Long? = null,
        expiryDate: Long? = null,
        requestId: String? = null,
        notes: String = "",
        idempotencyKey: String? = null,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorDocument>

    suspend fun createNewVersion(
        projectId: String,
        documentId: String,
        fileReferenceId: String,
        fileName: String = "",
        mimeType: String = "",
        fileSize: Long = 0L,
        issueDate: Long? = null,
        expiryDate: Long? = null,
        notes: String = "",
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorDocumentVersion>

    suspend fun getDocument(
        projectId: String,
        documentId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorDocument>

    suspend fun listDocuments(
        projectId: String,
        vendorId: String? = null,
        documentType: VendorDocumentType? = null,
        status: VendorDocumentStatus? = null,
        verificationStatus: VendorDocumentVerificationStatus? = null,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<List<VendorDocument>>

    // =========================================================================
    // Document Review & Verification
    // =========================================================================

    suspend fun startReview(
        projectId: String,
        documentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorDocument>

    suspend fun approveDocument(
        projectId: String,
        documentId: String,
        remarks: String = "",
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorDocumentReview>

    suspend fun rejectDocument(
        projectId: String,
        documentId: String,
        rejectionReason: String,
        remarks: String = "",
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorDocumentReview>

    suspend fun getDocumentReviews(
        projectId: String,
        documentId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<List<VendorDocumentReview>>

    suspend fun getDocumentVersions(
        projectId: String,
        documentId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<List<VendorDocumentVersion>>

    // =========================================================================
    // Compliance & Expiry Analytics
    // =========================================================================

    suspend fun getComplianceSummary(
        projectId: String,
        vendorId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorComplianceSummary>

    suspend fun getProjectComplianceSummaries(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<VendorComplianceSummary>>

    suspend fun getExpirySummary(
        projectId: String,
        vendorId: String? = null,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<List<VendorDocumentExpiryInfo>>

    suspend fun processRenewalReminders(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Int>

    // =========================================================================
    // Activity & Audit
    // =========================================================================

    suspend fun recordActivity(
        event: VendorDocumentActivityEvent
    ): DomainResult<Unit>

    suspend fun getActivityEvents(
        projectId: String,
        vendorId: String? = null,
        documentId: String? = null,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<List<VendorDocumentActivityEvent>>

    fun observeDocuments(
        projectId: String,
        vendorId: String? = null,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): Flow<List<VendorDocument>>

    fun observeDocumentRequests(
        projectId: String,
        vendorId: String? = null,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): Flow<List<VendorDocumentRequest>>
}
