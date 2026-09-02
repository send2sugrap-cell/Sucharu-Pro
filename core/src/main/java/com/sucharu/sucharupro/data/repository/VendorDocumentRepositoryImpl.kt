package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.VendorDocumentDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.*
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.communication.vendor.document.VendorDocumentRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository
import com.sucharu.sucharupro.domain.validation.communication.vendor.document.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Production repository implementation for Vendor Document, Compliance & Record Communication (Module 10 Step 06).
 *
 * Integrates with:
 * - Step 01 [NotificationRepository] for canonical delivery
 *
 * ZERO mutation of: Vendor, Finance, Inventory, Production, Delivery, Customer, Order domains.
 */
class VendorDocumentRepositoryImpl(
    private val dataSource: VendorDocumentDataSource,
    private val notificationRepository: NotificationRepository
) : VendorDocumentRepository {

    // =========================================================================
    // Document Requests
    // =========================================================================

    override suspend fun createDocumentRequest(
        projectId: String,
        vendorId: String,
        documentType: VendorDocumentType,
        title: String,
        description: String,
        required: Boolean,
        priority: NotificationPriority,
        dueDate: Long?,
        idempotencyKey: String?,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorDocumentRequest> {
        val authResult = VendorDocumentAuthorizationValidator.validateCreateRequest(callerRole, documentType)
        if (authResult is DomainResult.Error) return authResult

        val fieldResult = VendorDocumentRequestValidator.validateCreate(
            projectId, vendorId, documentType, title, dueDate, actorId
        )
        if (fieldResult is DomainResult.Error) return fieldResult

        // Idempotency check
        if (idempotencyKey != null) {
            val existing = dataSource.getRequestByIdempotencyKey(projectId, idempotencyKey)
            if (existing != null) return DomainResult.Success(existing)
        }

        // Duplicate check - only one active request per vendor+type
        val active = dataSource.getActiveRequest(projectId, vendorId, documentType)
        if (active != null) {
            return DomainResult.Error(message = "An active request for '${documentType.defaultLabel}' already exists for vendor '$vendorId' in project '$projectId'.")
        }

        val requestNo = dataSource.generateRequestNumber(projectId)
        val now = System.currentTimeMillis()
        val requestId = UUID.randomUUID().toString()

        val request = VendorDocumentRequest(
            requestId = requestId,
            requestNo = requestNo,
            projectId = projectId,
            vendorId = vendorId,
            documentType = documentType,
            title = title,
            description = description,
            required = required,
            priority = priority,
            requestedBy = actorId,
            requestedAt = now,
            dueDate = dueDate,
            status = VendorDocumentRequestStatus.OPEN,
            idempotencyKey = idempotencyKey,
            createdAt = now,
            updatedAt = now
        )

        dataSource.saveRequest(request)

        // Notify vendor of request
        notificationRepository.createNotification(
            projectId = projectId,
            recipientUserId = vendorId,
            recipientType = "VENDOR",
            notificationType = NotificationType.GENERAL,
            priority = priority,
            title = "Document Requested: ${documentType.defaultLabel}",
            message = "A document request has been issued: $title. Please submit by ${if (dueDate != null) java.util.Date(dueDate) else "as soon as possible"}.",
            referenceType = "VendorDocumentRequest",
            referenceId = requestId,
            actorId = actorId,
            callerRole = callerRole
        )

        recordActivity(VendorDocumentActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            vendorId = vendorId,
            requestId = requestId,
            eventType = VendorDocumentActivityEventType.DOCUMENT_REQUESTED,
            actorId = actorId,
            actorRole = callerRole.name,
            newState = VendorDocumentRequestStatus.OPEN.name
        ))

        return DomainResult.Success(request)
    }

    override suspend fun getDocumentRequest(
        projectId: String,
        requestId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<VendorDocumentRequest> {
        val request = dataSource.getRequestById(projectId, requestId)
            ?: return DomainResult.Error(message = "Document request '$requestId' not found in project '$projectId'.")
        val authResult = VendorDocumentAuthorizationValidator.validateReadAccess(callerRole, request.vendorId, callerVendorId)
        if (authResult is DomainResult.Error) return authResult
        return DomainResult.Success(request)
    }

    override suspend fun listDocumentRequests(
        projectId: String,
        vendorId: String?,
        documentType: VendorDocumentType?,
        status: VendorDocumentRequestStatus?,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<List<VendorDocumentRequest>> {
        val effectiveVendorId = if (callerRole == UserRole.VENDOR) callerVendorId else vendorId
        if (callerRole == UserRole.VENDOR && effectiveVendorId.isNullOrBlank()) {
            return DomainResult.Error(message = "Vendor ID required for VENDOR role.")
        }
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role cannot access document requests.")
        }
        val all = dataSource.listRequests(projectId, effectiveVendorId)
        val filtered = all.filter { r ->
            (documentType == null || r.documentType == documentType) &&
            (status == null || r.status == status)
        }
        return DomainResult.Success(filtered)
    }

    override suspend fun cancelDocumentRequest(
        projectId: String,
        requestId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorDocumentRequest> {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER) {
            return DomainResult.Error(message = "Only ADMIN or MANAGER may cancel document requests.")
        }
        val request = dataSource.getRequestById(projectId, requestId)
            ?: return DomainResult.Error(message = "Document request '$requestId' not found.")
        val lifecycle = VendorDocumentLifecycleValidator.validateRequestTransition(
            request.status, VendorDocumentRequestStatus.CANCELLED
        )
        if (lifecycle is DomainResult.Error) return lifecycle

        val now = System.currentTimeMillis()
        val updated = request.copy(
            status = VendorDocumentRequestStatus.CANCELLED,
            cancelledAt = now,
            updatedAt = now
        )
        dataSource.saveRequest(updated)
        recordActivity(VendorDocumentActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            vendorId = request.vendorId,
            requestId = requestId,
            eventType = VendorDocumentActivityEventType.DOCUMENT_CANCELLED,
            actorId = actorId,
            actorRole = callerRole.name,
            previousState = request.status.name,
            newState = VendorDocumentRequestStatus.CANCELLED.name,
            details = reason
        ))
        return DomainResult.Success(updated)
    }

    // =========================================================================
    // Document Submissions & Lifecycle
    // =========================================================================

    override suspend fun submitDocument(
        projectId: String,
        vendorId: String,
        documentType: VendorDocumentType,
        title: String,
        description: String,
        fileReferenceId: String,
        fileName: String,
        mimeType: String,
        fileSize: Long,
        issueDate: Long?,
        expiryDate: Long?,
        requestId: String?,
        notes: String,
        idempotencyKey: String?,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<VendorDocument> {
        val authResult = VendorDocumentAuthorizationValidator.validateSubmitDocument(callerRole, vendorId, callerVendorId)
        if (authResult is DomainResult.Error) return authResult

        val fieldResult = VendorDocumentValidator.validateSubmission(
            projectId, vendorId, documentType, title, fileReferenceId, issueDate, expiryDate, actorId
        )
        if (fieldResult is DomainResult.Error) return fieldResult

        // Idempotency check
        if (idempotencyKey != null) {
            val existing = dataSource.getDocumentByIdempotencyKey(projectId, idempotencyKey)
            if (existing != null) return DomainResult.Success(existing)
        }

        val documentNo = dataSource.generateDocumentNumber(projectId)
        val now = System.currentTimeMillis()
        val documentId = UUID.randomUUID().toString()

        val document = VendorDocument(
            documentId = documentId,
            documentNo = documentNo,
            projectId = projectId,
            vendorId = vendorId,
            documentType = documentType,
            title = title,
            description = description,
            status = VendorDocumentStatus.SUBMITTED,
            verificationStatus = VendorDocumentVerificationStatus.PENDING_REVIEW,
            fileReferenceId = fileReferenceId,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            documentVersion = 1,
            issueDate = issueDate,
            expiryDate = expiryDate,
            requestedAt = null,
            submittedAt = now,
            createdBy = actorId,
            submittedBy = actorId,
            requestId = requestId,
            idempotencyKey = idempotencyKey,
            createdAt = now,
            updatedAt = now
        )

        dataSource.saveDocument(document)

        // Create initial version record
        val version = VendorDocumentVersion(
            versionId = UUID.randomUUID().toString(),
            projectId = projectId,
            documentId = documentId,
            vendorId = vendorId,
            versionNumber = 1,
            fileReferenceId = fileReferenceId,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            issueDate = issueDate,
            expiryDate = expiryDate,
            status = VendorDocumentStatus.SUBMITTED,
            verificationStatus = VendorDocumentVerificationStatus.PENDING_REVIEW,
            submittedBy = actorId,
            submittedAt = now,
            notes = notes,
            createdAt = now
        )
        dataSource.saveVersion(version)

        // Update linked request if provided
        if (requestId != null) {
            val req = dataSource.getRequestById(projectId, requestId)
            if (req != null && !req.status.isTerminal) {
                val updatedReq = req.copy(
                    status = VendorDocumentRequestStatus.SUBMITTED,
                    submittedDocumentId = documentId,
                    updatedAt = now
                )
                dataSource.saveRequest(updatedReq)
            }
        }

        // Notify internal reviewer
        notificationRepository.createNotification(
            projectId = projectId,
            recipientUserId = "internal-review-queue",
            recipientType = "ROLE",
            notificationType = NotificationType.GENERAL,
            priority = NotificationPriority.NORMAL,
            title = "Document Submitted: ${documentType.defaultLabel}",
            message = "Vendor '$vendorId' submitted: $title. Ready for review.",
            referenceType = "VendorDocument",
            referenceId = documentId,
            actorId = actorId,
            callerRole = callerRole
        )

        recordActivity(VendorDocumentActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            vendorId = vendorId,
            documentId = documentId,
            requestId = requestId,
            eventType = VendorDocumentActivityEventType.DOCUMENT_SUBMITTED,
            actorId = actorId,
            actorRole = callerRole.name,
            newState = VendorDocumentStatus.SUBMITTED.name
        ))

        return DomainResult.Success(document)
    }

    override suspend fun createNewVersion(
        projectId: String,
        documentId: String,
        fileReferenceId: String,
        fileName: String,
        mimeType: String,
        fileSize: Long,
        issueDate: Long?,
        expiryDate: Long?,
        notes: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<VendorDocumentVersion> {
        val doc = dataSource.getDocumentById(projectId, documentId)
            ?: return DomainResult.Error(message = "Document '$documentId' not found.")

        val authResult = VendorDocumentAuthorizationValidator.validateSubmitDocument(callerRole, doc.vendorId, callerVendorId)
        if (authResult is DomainResult.Error) return authResult

        if (doc.status == VendorDocumentStatus.CANCELLED) {
            return DomainResult.Error(message = "Cannot create new version of a CANCELLED document.")
        }

        val existingVersions = dataSource.getVersions(projectId, documentId)
        val nextVersionNumber = (existingVersions.maxOfOrNull { it.versionNumber } ?: 0) + 1
        val now = System.currentTimeMillis()

        val version = VendorDocumentVersion(
            versionId = UUID.randomUUID().toString(),
            projectId = projectId,
            documentId = documentId,
            vendorId = doc.vendorId,
            versionNumber = nextVersionNumber,
            fileReferenceId = fileReferenceId,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            issueDate = issueDate,
            expiryDate = expiryDate,
            status = VendorDocumentStatus.SUBMITTED,
            verificationStatus = VendorDocumentVerificationStatus.PENDING_REVIEW,
            submittedBy = actorId,
            submittedAt = now,
            notes = notes,
            createdAt = now
        )
        dataSource.saveVersion(version)

        // Update document head to new version
        val updatedDoc = doc.copy(
            documentVersion = nextVersionNumber,
            fileReferenceId = fileReferenceId,
            fileName = fileName,
            mimeType = mimeType,
            fileSize = fileSize,
            issueDate = issueDate,
            expiryDate = expiryDate,
            status = VendorDocumentStatus.SUBMITTED,
            verificationStatus = VendorDocumentVerificationStatus.PENDING_REVIEW,
            submittedAt = now,
            submittedBy = actorId,
            updatedAt = now
        )
        dataSource.saveDocument(updatedDoc)

        recordActivity(VendorDocumentActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            vendorId = doc.vendorId,
            documentId = documentId,
            eventType = VendorDocumentActivityEventType.DOCUMENT_VERSION_CREATED,
            actorId = actorId,
            actorRole = callerRole.name,
            newState = "Version $nextVersionNumber"
        ))

        return DomainResult.Success(version)
    }

    override suspend fun getDocument(
        projectId: String,
        documentId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<VendorDocument> {
        val doc = dataSource.getDocumentById(projectId, documentId)
            ?: return DomainResult.Error(message = "Document '$documentId' not found in project '$projectId'.")
        val authResult = VendorDocumentAuthorizationValidator.validateReadAccess(callerRole, doc.vendorId, callerVendorId)
        if (authResult is DomainResult.Error) return authResult

        recordActivity(VendorDocumentActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            vendorId = doc.vendorId,
            documentId = documentId,
            eventType = VendorDocumentActivityEventType.DOCUMENT_VIEWED,
            actorId = actorId,
            actorRole = callerRole.name
        ))

        return DomainResult.Success(doc)
    }

    override suspend fun listDocuments(
        projectId: String,
        vendorId: String?,
        documentType: VendorDocumentType?,
        status: VendorDocumentStatus?,
        verificationStatus: VendorDocumentVerificationStatus?,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<List<VendorDocument>> {
        val effectiveVendorId = if (callerRole == UserRole.VENDOR) callerVendorId else vendorId
        if (callerRole == UserRole.VENDOR && effectiveVendorId.isNullOrBlank()) {
            return DomainResult.Error(message = "Vendor ID required for VENDOR role.")
        }
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role cannot access vendor documents.")
        }
        val all = dataSource.listDocuments(projectId, effectiveVendorId)
        val filtered = all.filter { d ->
            (documentType == null || d.documentType == documentType) &&
            (status == null || d.status == status) &&
            (verificationStatus == null || d.verificationStatus == verificationStatus)
        }
        return DomainResult.Success(filtered)
    }

    // =========================================================================
    // Review & Verification
    // =========================================================================

    override suspend fun startReview(
        projectId: String,
        documentId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorDocument> {
        val doc = dataSource.getDocumentById(projectId, documentId)
            ?: return DomainResult.Error(message = "Document '$documentId' not found.")
        val authResult = VendorDocumentAuthorizationValidator.validateReviewAction(callerRole, doc.documentType)
        if (authResult is DomainResult.Error) return authResult

        val lifecycle = VendorDocumentLifecycleValidator.validateDocumentTransition(doc.status, VendorDocumentStatus.UNDER_REVIEW)
        if (lifecycle is DomainResult.Error) return lifecycle

        val now = System.currentTimeMillis()
        val updated = doc.copy(
            status = VendorDocumentStatus.UNDER_REVIEW,
            verificationStatus = VendorDocumentVerificationStatus.PENDING_REVIEW,
            reviewedAt = now,
            updatedAt = now
        )
        dataSource.saveDocument(updated)

        recordActivity(VendorDocumentActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            vendorId = doc.vendorId,
            documentId = documentId,
            eventType = VendorDocumentActivityEventType.DOCUMENT_REVIEW_STARTED,
            actorId = actorId,
            actorRole = callerRole.name,
            previousState = doc.status.name,
            newState = VendorDocumentStatus.UNDER_REVIEW.name
        ))

        return DomainResult.Success(updated)
    }

    override suspend fun approveDocument(
        projectId: String,
        documentId: String,
        remarks: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorDocumentReview> {
        val doc = dataSource.getDocumentById(projectId, documentId)
            ?: return DomainResult.Error(message = "Document '$documentId' not found.")
        val authResult = VendorDocumentAuthorizationValidator.validateReviewAction(callerRole, doc.documentType)
        if (authResult is DomainResult.Error) return authResult

        val reviewResult = VendorDocumentReviewValidator.validateReview(
            projectId, documentId, doc.vendorId, VendorDocumentVerificationStatus.VERIFIED, actorId, remarks
        )
        if (reviewResult is DomainResult.Error) return reviewResult

        val reviewNo = dataSource.generateReviewNumber(projectId)
        val now = System.currentTimeMillis()

        val review = VendorDocumentReview(
            reviewId = UUID.randomUUID().toString(),
            reviewNo = reviewNo,
            projectId = projectId,
            documentId = documentId,
            vendorId = doc.vendorId,
            documentVersion = doc.documentVersion,
            reviewStatus = VendorDocumentVerificationStatus.VERIFIED,
            reviewedBy = actorId,
            reviewedAt = now,
            remarks = remarks,
            createdAt = now
        )
        dataSource.saveReview(review)

        val updatedDoc = doc.copy(
            status = VendorDocumentStatus.APPROVED,
            verificationStatus = VendorDocumentVerificationStatus.VERIFIED,
            reviewedAt = now,
            approvedAt = now,
            approvedBy = actorId,
            updatedAt = now
        )
        dataSource.saveDocument(updatedDoc)

        // Notify vendor of approval
        notificationRepository.createNotification(
            projectId = projectId,
            recipientUserId = doc.vendorId,
            recipientType = "VENDOR",
            notificationType = NotificationType.GENERAL,
            priority = NotificationPriority.NORMAL,
            title = "Document Approved: ${doc.documentType.defaultLabel}",
            message = "Your document '${doc.title}' has been approved.",
            referenceType = "VendorDocument",
            referenceId = documentId,
            actorId = actorId,
            callerRole = callerRole
        )

        recordActivity(VendorDocumentActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            vendorId = doc.vendorId,
            documentId = documentId,
            eventType = VendorDocumentActivityEventType.DOCUMENT_APPROVED,
            actorId = actorId,
            actorRole = callerRole.name,
            previousState = doc.status.name,
            newState = VendorDocumentStatus.APPROVED.name
        ))

        return DomainResult.Success(review)
    }

    override suspend fun rejectDocument(
        projectId: String,
        documentId: String,
        rejectionReason: String,
        remarks: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorDocumentReview> {
        val doc = dataSource.getDocumentById(projectId, documentId)
            ?: return DomainResult.Error(message = "Document '$documentId' not found.")
        val authResult = VendorDocumentAuthorizationValidator.validateReviewAction(callerRole, doc.documentType)
        if (authResult is DomainResult.Error) return authResult

        val reviewResult = VendorDocumentReviewValidator.validateReview(
            projectId, documentId, doc.vendorId, VendorDocumentVerificationStatus.REJECTED, actorId, remarks, rejectionReason
        )
        if (reviewResult is DomainResult.Error) return reviewResult

        val reviewNo = dataSource.generateReviewNumber(projectId)
        val now = System.currentTimeMillis()

        val review = VendorDocumentReview(
            reviewId = UUID.randomUUID().toString(),
            reviewNo = reviewNo,
            projectId = projectId,
            documentId = documentId,
            vendorId = doc.vendorId,
            documentVersion = doc.documentVersion,
            reviewStatus = VendorDocumentVerificationStatus.REJECTED,
            reviewedBy = actorId,
            reviewedAt = now,
            remarks = remarks,
            rejectionReason = rejectionReason,
            createdAt = now
        )
        dataSource.saveReview(review)

        val updatedDoc = doc.copy(
            status = VendorDocumentStatus.REJECTED,
            verificationStatus = VendorDocumentVerificationStatus.REJECTED,
            reviewedAt = now,
            rejectedAt = now,
            rejectedBy = actorId,
            rejectionReason = rejectionReason,
            updatedAt = now
        )
        dataSource.saveDocument(updatedDoc)

        // Notify vendor of rejection
        notificationRepository.createNotification(
            projectId = projectId,
            recipientUserId = doc.vendorId,
            recipientType = "VENDOR",
            notificationType = NotificationType.GENERAL,
            priority = NotificationPriority.HIGH,
            title = "Document Rejected: ${doc.documentType.defaultLabel}",
            message = "Your document '${doc.title}' was rejected. Reason: $rejectionReason",
            referenceType = "VendorDocument",
            referenceId = documentId,
            actorId = actorId,
            callerRole = callerRole
        )

        recordActivity(VendorDocumentActivityEvent(
            eventId = UUID.randomUUID().toString(),
            projectId = projectId,
            vendorId = doc.vendorId,
            documentId = documentId,
            eventType = VendorDocumentActivityEventType.DOCUMENT_REJECTED,
            actorId = actorId,
            actorRole = callerRole.name,
            previousState = doc.status.name,
            newState = VendorDocumentStatus.REJECTED.name,
            details = rejectionReason
        ))

        return DomainResult.Success(review)
    }

    override suspend fun getDocumentReviews(
        projectId: String,
        documentId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<List<VendorDocumentReview>> {
        val doc = dataSource.getDocumentById(projectId, documentId)
            ?: return DomainResult.Error(message = "Document '$documentId' not found.")
        val authResult = VendorDocumentAuthorizationValidator.validateReadAccess(callerRole, doc.vendorId, callerVendorId)
        if (authResult is DomainResult.Error) return authResult
        return DomainResult.Success(dataSource.getReviews(projectId, documentId))
    }

    override suspend fun getDocumentVersions(
        projectId: String,
        documentId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<List<VendorDocumentVersion>> {
        val doc = dataSource.getDocumentById(projectId, documentId)
            ?: return DomainResult.Error(message = "Document '$documentId' not found.")
        val authResult = VendorDocumentAuthorizationValidator.validateReadAccess(callerRole, doc.vendorId, callerVendorId)
        if (authResult is DomainResult.Error) return authResult
        return DomainResult.Success(dataSource.getVersions(projectId, documentId))
    }

    // =========================================================================
    // Compliance & Expiry Analytics
    // =========================================================================

    override suspend fun getComplianceSummary(
        projectId: String,
        vendorId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<VendorComplianceSummary> {
        val authResult = VendorDocumentAuthorizationValidator.validateReadAccess(callerRole, vendorId, callerVendorId)
        if (authResult is DomainResult.Error) return authResult
        val docs = dataSource.listDocuments(projectId, vendorId)
        val summary = VendorComplianceEngine.evaluateVendorCompliance(projectId, vendorId, docs)
        return DomainResult.Success(summary)
    }

    override suspend fun getProjectComplianceSummaries(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<VendorComplianceSummary>> {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER && callerRole != UserRole.ACCOUNTS) {
            return DomainResult.Error(message = "Only ADMIN, MANAGER, or ACCOUNTS may access project-wide compliance summaries.")
        }
        val docs = dataSource.listDocuments(projectId)
        val vendorIds = docs.map { it.vendorId }.distinct()
        val summaries = vendorIds.map { vid ->
            VendorComplianceEngine.evaluateVendorCompliance(projectId, vid, docs)
        }
        return DomainResult.Success(summaries)
    }

    override suspend fun getExpirySummary(
        projectId: String,
        vendorId: String?,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<List<VendorDocumentExpiryInfo>> {
        val effectiveVendorId = if (callerRole == UserRole.VENDOR) callerVendorId else vendorId
        if (callerRole == UserRole.VENDOR && effectiveVendorId.isNullOrBlank()) {
            return DomainResult.Error(message = "Vendor ID required for VENDOR role.")
        }
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role cannot access document expiry data.")
        }
        val docs = dataSource.listDocuments(projectId, effectiveVendorId)
        val expiry = docs.map { VendorDocumentExpiryInfo.calculate(it) }
        return DomainResult.Success(expiry)
    }

    override suspend fun processRenewalReminders(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<Int> {
        if (callerRole != UserRole.ADMIN && callerRole != UserRole.MANAGER) {
            return DomainResult.Error(message = "Only ADMIN or MANAGER may trigger renewal reminders.")
        }
        val policy = VendorDocumentExpiryPolicy()
        val docs = dataSource.listDocuments(projectId)
        var count = 0
        docs.filter { policy.isEligibleForReminder(it) }.forEach { doc ->
            val idempotencyKey = policy.generateReminderIdempotencyKey(projectId, doc.documentId, 30)
            notificationRepository.createNotification(
                projectId = projectId,
                recipientUserId = doc.vendorId,
                recipientType = "VENDOR",
                notificationType = NotificationType.GENERAL,
                priority = NotificationPriority.HIGH,
                title = "Document Renewal Required: ${doc.documentType.defaultLabel}",
                message = "Your document '${doc.title}' is expiring soon. Please renew before it expires.",
                referenceType = "VendorDocument",
                referenceId = doc.documentId,
                idempotencyKey = idempotencyKey,
                actorId = actorId,
                callerRole = callerRole
            )
            recordActivity(VendorDocumentActivityEvent(
                eventId = UUID.randomUUID().toString(),
                projectId = projectId,
                vendorId = doc.vendorId,
                documentId = doc.documentId,
                eventType = VendorDocumentActivityEventType.DOCUMENT_RENEWAL_REMINDER_SENT,
                actorId = actorId,
                actorRole = callerRole.name
            ))
            count++
        }
        return DomainResult.Success(count)
    }

    // =========================================================================
    // Activity & Audit
    // =========================================================================

    override suspend fun recordActivity(event: VendorDocumentActivityEvent): DomainResult<Unit> {
        dataSource.recordActivity(event)
        return DomainResult.Success(Unit)
    }

    override suspend fun getActivityEvents(
        projectId: String,
        vendorId: String?,
        documentId: String?,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String?
    ): DomainResult<List<VendorDocumentActivityEvent>> {
        val effectiveVendorId = if (callerRole == UserRole.VENDOR) callerVendorId else vendorId
        if (callerRole == UserRole.CUSTOMER) {
            return DomainResult.Error(message = "CUSTOMER role cannot access document activity.")
        }
        val events = dataSource.getActivityEvents(projectId, effectiveVendorId, documentId)
        return DomainResult.Success(events)
    }

    override fun observeDocuments(
        projectId: String,
        vendorId: String?,
        callerRole: UserRole,
        callerVendorId: String?
    ): Flow<List<VendorDocument>> {
        val effectiveVendorId = if (callerRole == UserRole.VENDOR) callerVendorId else vendorId
        return dataSource.observeDocuments(projectId, effectiveVendorId)
    }

    override fun observeDocumentRequests(
        projectId: String,
        vendorId: String?,
        callerRole: UserRole,
        callerVendorId: String?
    ): Flow<List<VendorDocumentRequest>> {
        val effectiveVendorId = if (callerRole == UserRole.VENDOR) callerVendorId else vendorId
        return dataSource.observeRequests(projectId, effectiveVendorId)
    }
}
