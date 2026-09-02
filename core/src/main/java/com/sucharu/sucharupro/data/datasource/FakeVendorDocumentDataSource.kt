package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.vendor.document.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar

/**
 * Thread-safe in-memory implementation of [VendorDocumentDataSource] (Module 10 Step 06).
 * Numbering: VDOC-YYYY-XXXXX, VREQ-YYYY-XXXXX, VREV-YYYY-XXXXX
 */
class FakeVendorDocumentDataSource : VendorDocumentDataSource {

    private val mutex = Mutex()
    private val documentsState = MutableStateFlow<Map<String, VendorDocument>>(emptyMap())
    private val requestsState = MutableStateFlow<Map<String, VendorDocumentRequest>>(emptyMap())
    private val versionsState = MutableStateFlow<List<VendorDocumentVersion>>(emptyList())
    private val reviewsState = MutableStateFlow<List<VendorDocumentReview>>(emptyList())
    private val activityState = MutableStateFlow<List<VendorDocumentActivityEvent>>(emptyList())

    private var docCounter = 0
    private var reqCounter = 0
    private var revCounter = 0

    private fun yearTag() = Calendar.getInstance().get(Calendar.YEAR)

    // =========================================================================
    // Documents
    // =========================================================================

    override suspend fun saveDocument(document: VendorDocument) = mutex.withLock {
        documentsState.update { it + (document.documentId to document) }
    }

    override suspend fun getDocumentById(projectId: String, documentId: String): VendorDocument? =
        mutex.withLock { documentsState.value[documentId]?.takeIf { it.projectId == projectId } }

    override suspend fun getDocumentByNo(projectId: String, documentNo: String): VendorDocument? =
        mutex.withLock {
            documentsState.value.values.firstOrNull { it.projectId == projectId && it.documentNo == documentNo }
        }

    override suspend fun getDocumentByIdempotencyKey(projectId: String, idempotencyKey: String): VendorDocument? =
        mutex.withLock {
            documentsState.value.values.firstOrNull {
                it.projectId == projectId && it.idempotencyKey == idempotencyKey
            }
        }

    override suspend fun listDocuments(projectId: String, vendorId: String?): List<VendorDocument> =
        mutex.withLock {
            documentsState.value.values.filter {
                it.projectId == projectId && (vendorId == null || it.vendorId == vendorId)
            }.sortedByDescending { it.createdAt }
        }

    override fun observeDocuments(projectId: String, vendorId: String?): Flow<List<VendorDocument>> =
        documentsState.map { map ->
            map.values.filter {
                it.projectId == projectId && (vendorId == null || it.vendorId == vendorId)
            }.sortedByDescending { it.createdAt }
        }

    override suspend fun generateDocumentNumber(projectId: String): String = mutex.withLock {
        docCounter++
        "VDOC-${yearTag()}-%05d".format(docCounter)
    }

    // =========================================================================
    // Requests
    // =========================================================================

    override suspend fun saveRequest(request: VendorDocumentRequest) = mutex.withLock {
        requestsState.update { it + (request.requestId to request) }
    }

    override suspend fun getRequestById(projectId: String, requestId: String): VendorDocumentRequest? =
        mutex.withLock { requestsState.value[requestId]?.takeIf { it.projectId == projectId } }

    override suspend fun getRequestByNo(projectId: String, requestNo: String): VendorDocumentRequest? =
        mutex.withLock {
            requestsState.value.values.firstOrNull { it.projectId == projectId && it.requestNo == requestNo }
        }

    override suspend fun getRequestByIdempotencyKey(projectId: String, idempotencyKey: String): VendorDocumentRequest? =
        mutex.withLock {
            requestsState.value.values.firstOrNull {
                it.projectId == projectId && it.idempotencyKey == idempotencyKey
            }
        }

    override suspend fun listRequests(projectId: String, vendorId: String?): List<VendorDocumentRequest> =
        mutex.withLock {
            requestsState.value.values.filter {
                it.projectId == projectId && (vendorId == null || it.vendorId == vendorId)
            }.sortedByDescending { it.createdAt }
        }

    override suspend fun getActiveRequest(
        projectId: String,
        vendorId: String,
        documentType: VendorDocumentType
    ): VendorDocumentRequest? = mutex.withLock {
        requestsState.value.values.firstOrNull { r ->
            r.projectId == projectId &&
            r.vendorId == vendorId &&
            r.documentType == documentType &&
            !r.status.isTerminal
        }
    }

    override fun observeRequests(projectId: String, vendorId: String?): Flow<List<VendorDocumentRequest>> =
        requestsState.map { map ->
            map.values.filter {
                it.projectId == projectId && (vendorId == null || it.vendorId == vendorId)
            }.sortedByDescending { it.createdAt }
        }

    override suspend fun generateRequestNumber(projectId: String): String = mutex.withLock {
        reqCounter++
        "VREQ-${yearTag()}-%05d".format(reqCounter)
    }

    // =========================================================================
    // Versions
    // =========================================================================

    override suspend fun saveVersion(version: VendorDocumentVersion) = mutex.withLock {
        versionsState.update { it + version }
    }

    override suspend fun getVersions(projectId: String, documentId: String): List<VendorDocumentVersion> =
        mutex.withLock {
            versionsState.value.filter {
                it.projectId == projectId && it.documentId == documentId
            }.sortedByDescending { it.versionNumber }
        }

    // =========================================================================
    // Reviews
    // =========================================================================

    override suspend fun saveReview(review: VendorDocumentReview) = mutex.withLock {
        reviewsState.update { it + review }
    }

    override suspend fun getReviews(projectId: String, documentId: String): List<VendorDocumentReview> =
        mutex.withLock {
            reviewsState.value.filter {
                it.projectId == projectId && it.documentId == documentId
            }.sortedByDescending { it.createdAt }
        }

    override suspend fun generateReviewNumber(projectId: String): String = mutex.withLock {
        revCounter++
        "VREV-${yearTag()}-%05d".format(revCounter)
    }

    // =========================================================================
    // Activity Events
    // =========================================================================

    override suspend fun recordActivity(event: VendorDocumentActivityEvent) = mutex.withLock {
        activityState.update { it + event }
    }

    override suspend fun getActivityEvents(
        projectId: String,
        vendorId: String?,
        documentId: String?
    ): List<VendorDocumentActivityEvent> = mutex.withLock {
        activityState.value.filter { e ->
            e.projectId == projectId &&
            (vendorId == null || e.vendorId == vendorId) &&
            (documentId == null || e.documentId == documentId)
        }.sortedByDescending { it.timestamp }
    }
}
