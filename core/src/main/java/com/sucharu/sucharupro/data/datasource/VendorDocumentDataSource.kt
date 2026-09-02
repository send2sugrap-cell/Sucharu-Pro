package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.vendor.document.*
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for Vendor Documents, Requests, Versions, Reviews and Activity Events (Module 10 Step 06).
 */
interface VendorDocumentDataSource {

    // Document CRUD & Query
    suspend fun saveDocument(document: VendorDocument)
    suspend fun getDocumentById(projectId: String, documentId: String): VendorDocument?
    suspend fun getDocumentByNo(projectId: String, documentNo: String): VendorDocument?
    suspend fun getDocumentByIdempotencyKey(projectId: String, idempotencyKey: String): VendorDocument?
    suspend fun listDocuments(projectId: String, vendorId: String? = null): List<VendorDocument>
    fun observeDocuments(projectId: String, vendorId: String? = null): Flow<List<VendorDocument>>
    suspend fun generateDocumentNumber(projectId: String): String

    // Document Request CRUD & Query
    suspend fun saveRequest(request: VendorDocumentRequest)
    suspend fun getRequestById(projectId: String, requestId: String): VendorDocumentRequest?
    suspend fun getRequestByNo(projectId: String, requestNo: String): VendorDocumentRequest?
    suspend fun getRequestByIdempotencyKey(projectId: String, idempotencyKey: String): VendorDocumentRequest?
    suspend fun listRequests(projectId: String, vendorId: String? = null): List<VendorDocumentRequest>
    suspend fun getActiveRequest(projectId: String, vendorId: String, documentType: VendorDocumentType): VendorDocumentRequest?
    fun observeRequests(projectId: String, vendorId: String? = null): Flow<List<VendorDocumentRequest>>
    suspend fun generateRequestNumber(projectId: String): String

    // Versions
    suspend fun saveVersion(version: VendorDocumentVersion)
    suspend fun getVersions(projectId: String, documentId: String): List<VendorDocumentVersion>

    // Reviews
    suspend fun saveReview(review: VendorDocumentReview)
    suspend fun getReviews(projectId: String, documentId: String): List<VendorDocumentReview>
    suspend fun generateReviewNumber(projectId: String): String

    // Activity Events
    suspend fun recordActivity(event: VendorDocumentActivityEvent)
    suspend fun getActivityEvents(projectId: String, vendorId: String? = null, documentId: String? = null): List<VendorDocumentActivityEvent>
}
