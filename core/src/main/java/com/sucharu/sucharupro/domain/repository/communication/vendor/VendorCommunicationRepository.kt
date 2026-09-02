package com.sucharu.sucharupro.domain.repository.communication.vendor

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationPriority
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Vendor & Supplier Communication Management (Module 10 Step 05).
 *
 * All operations enforce:
 * - RBAC via [VendorCommunicationAuthorizationValidator]
 * - Project isolation (projectId must match)
 * - Vendor isolation (vendorId must match caller's access scope)
 * - Lifecycle validation via [VendorCommunicationLifecycleValidator]
 * - Idempotency via idempotencyKey
 * - Concurrency safety (Mutex-protected data source)
 *
 * ZERO mutation of VendorPayable, SupplierPayment, Inventory, Production, Orders,
 * Delivery, or Finance records. This is a communication/observation layer only.
 */
interface VendorCommunicationRepository {

    // =========================================================================
    // Core CRUD
    // =========================================================================

    suspend fun createCommunication(
        projectId: String,
        vendorId: String,
        supplierReferenceId: String? = null,
        communicationType: VendorCommunicationType,
        channel: NotificationChannel = NotificationChannel.IN_APP,
        priority: NotificationPriority = NotificationPriority.NORMAL,
        subject: String,
        message: String,
        referenceType: String? = null,
        referenceId: String? = null,
        requiresAcknowledgement: Boolean = false,
        scheduledAt: Long? = null,
        idempotencyKey: String? = null,
        metadata: Map<String, String> = emptyMap(),
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorCommunication>

    suspend fun getCommunication(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorCommunication>

    suspend fun listCommunications(
        projectId: String,
        vendorId: String? = null,
        communicationType: VendorCommunicationType? = null,
        status: VendorCommunicationStatus? = null,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<List<VendorCommunication>>

    suspend fun updateDraft(
        projectId: String,
        communicationId: String,
        subject: String? = null,
        message: String? = null,
        priority: NotificationPriority? = null,
        scheduledAt: Long? = null,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorCommunication>

    // =========================================================================
    // Lifecycle transitions
    // =========================================================================

    suspend fun submit(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorCommunication>

    suspend fun schedule(
        projectId: String,
        communicationId: String,
        scheduledAt: Long,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorCommunication>

    suspend fun queue(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorCommunication>

    suspend fun send(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorCommunication>

    suspend fun markDelivered(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorCommunication>

    suspend fun markRead(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorCommunication>

    suspend fun acknowledge(
        projectId: String,
        communicationId: String,
        acknowledgeMessage: String? = null,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorCommunicationAcknowledgement>

    suspend fun decline(
        projectId: String,
        communicationId: String,
        declineMessage: String? = null,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorCommunicationAcknowledgement>

    suspend fun cancel(
        projectId: String,
        communicationId: String,
        reason: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorCommunication>

    suspend fun retry(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorCommunication>

    // =========================================================================
    // Read receipts
    // =========================================================================

    suspend fun markReadReceipt(
        projectId: String,
        communicationId: String,
        vendorId: String,
        readByActorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorCommunicationReadReceipt>

    suspend fun getReadReceipt(
        projectId: String,
        communicationId: String,
        vendorId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorCommunicationReadReceipt?>

    // =========================================================================
    // Acknowledgement
    // =========================================================================

    suspend fun getAcknowledgement(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorCommunicationAcknowledgement?>

    // =========================================================================
    // History & Audit
    // =========================================================================

    suspend fun getHistory(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<List<VendorCommunicationHistory>>

    suspend fun getActivityEvents(
        projectId: String,
        communicationId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<VendorCommunicationActivityEvent>>

    // =========================================================================
    // Engagement
    // =========================================================================

    suspend fun recordEngagement(
        projectId: String,
        vendorId: String,
        communicationId: String,
        eventType: VendorEngagementEventType,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorEngagementEvent>

    suspend fun getEngagementEvents(
        projectId: String,
        vendorId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<List<VendorEngagementEvent>>

    // =========================================================================
    // Summaries & Analytics
    // =========================================================================

    suspend fun getVendorSummary(
        projectId: String,
        vendorId: String,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorCommunicationSummary>

    suspend fun getProjectSummary(
        projectId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<VendorCommunicationSummary>

    suspend fun getEngagementSummary(
        projectId: String,
        vendorId: String?,
        actorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): DomainResult<VendorEngagementSummary>

    // =========================================================================
    // Reference lookup
    // =========================================================================

    suspend fun getByReference(
        projectId: String,
        referenceType: String,
        referenceId: String,
        actorId: String,
        callerRole: UserRole
    ): DomainResult<List<VendorCommunication>>

    // =========================================================================
    // Reactive observation
    // =========================================================================

    fun observeVendorCommunications(
        projectId: String,
        vendorId: String,
        callerRole: UserRole,
        callerVendorId: String? = null
    ): Flow<List<VendorCommunication>>

    fun observeProjectCommunications(
        projectId: String,
        callerRole: UserRole
    ): Flow<List<VendorCommunication>>
}
