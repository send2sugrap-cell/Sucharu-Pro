package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.communication.vendor.*
import kotlinx.coroutines.flow.Flow

/**
 * Data source contract for Vendor Communications, History, Read Receipts,
 * Acknowledgements, Engagement Events, and Audit Activity (Module 10 Step 05).
 */
interface VendorCommunicationDataSource {

    // Core communications
    suspend fun saveCommunication(communication: VendorCommunication)
    suspend fun getCommunicationById(projectId: String, communicationId: String): VendorCommunication?
    suspend fun getCommunicationByNo(projectId: String, communicationNo: String): VendorCommunication?
    suspend fun getByIdempotencyKey(projectId: String, idempotencyKey: String): VendorCommunication?
    suspend fun getByDuplicateCriteria(
        projectId: String,
        vendorId: String,
        communicationType: VendorCommunicationType,
        referenceType: String?,
        referenceId: String?
    ): VendorCommunication?

    fun observeCommunicationsByVendor(projectId: String, vendorId: String): Flow<List<VendorCommunication>>
    fun observeCommunicationsByProject(projectId: String): Flow<List<VendorCommunication>>

    suspend fun getCommunicationsByReference(
        projectId: String,
        referenceType: String,
        referenceId: String
    ): List<VendorCommunication>

    suspend fun generateCommunicationNumber(projectId: String): String

    // History (append-only)
    suspend fun recordHistory(history: VendorCommunicationHistory)
    suspend fun getHistory(projectId: String, communicationId: String): List<VendorCommunicationHistory>

    // Read receipts
    suspend fun saveReadReceipt(receipt: VendorCommunicationReadReceipt)
    suspend fun getReadReceipt(projectId: String, communicationId: String, vendorId: String): VendorCommunicationReadReceipt?

    // Acknowledgements (immutable after creation)
    suspend fun saveAcknowledgement(acknowledgement: VendorCommunicationAcknowledgement)
    suspend fun getAcknowledgement(projectId: String, communicationId: String): VendorCommunicationAcknowledgement?

    // Engagement events
    suspend fun recordEngagementEvent(event: VendorEngagementEvent)
    suspend fun getEngagementEventsByVendor(projectId: String, vendorId: String): List<VendorEngagementEvent>

    // Audit activity events (append-only, immutable)
    suspend fun recordActivityEvent(event: VendorCommunicationActivityEvent)
    suspend fun getActivityEvents(projectId: String, communicationId: String): List<VendorCommunicationActivityEvent>
}
