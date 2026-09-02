package com.sucharu.sucharupro.domain.service.customercollection

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercollection.*
import com.sucharu.sucharupro.domain.model.customercreditcontrol.ReceivableAgingBucket
import java.math.BigDecimal

interface CustomerCollectionService {

    suspend fun createCollectionAction(
        tenantId: String,
        projectId: String,
        customerId: String,
        invoiceId: String? = null,
        actionType: CollectionActionType = CollectionActionType.REMINDER,
        priority: CollectionPriority? = null,
        scheduledAt: Long,
        nextFollowUpAt: Long? = null,
        assignedUserId: String? = null,
        notes: String? = null,
        idempotencyKey: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCollectionAction>

    suspend fun rescheduleAction(
        tenantId: String,
        projectId: String,
        actionId: String,
        newScheduledAt: Long,
        newNextFollowUpAt: Long? = null,
        notes: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCollectionAction>

    suspend fun assignAction(
        tenantId: String,
        projectId: String,
        actionId: String,
        assignedUserId: String?,
        notes: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCollectionAction>

    suspend fun completeAction(
        tenantId: String,
        projectId: String,
        actionId: String,
        outcome: CollectionOutcomeType,
        outcomeNotes: String? = null,
        nextFollowUpAt: Long? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCollectionAction>

    suspend fun cancelAction(
        tenantId: String,
        projectId: String,
        actionId: String,
        reason: String,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerCollectionAction>

    suspend fun createPaymentPromise(
        tenantId: String,
        projectId: String,
        customerId: String,
        invoiceId: String? = null,
        actionId: String? = null,
        promisedAmount: BigDecimal,
        promisedDate: Long,
        notes: String? = null,
        actorId: String,
        actorRole: String
    ): DomainResult<CustomerPaymentPromise>

    suspend fun getReceivableDueSchedule(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<List<ReceivableDueScheduleItem>>

    suspend fun getCollectionQueue(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        priority: CollectionPriority? = null,
        agingBucket: ReceivableAgingBucket? = null,
        status: CollectionActionStatus? = null,
        assignedUserId: String? = null,
        asOfDate: Long = System.currentTimeMillis(),
        limit: Int = 50,
        offset: Int = 0
    ): DomainResult<List<CollectionQueueItem>>

    suspend fun getCustomerCollectionSummary(
        tenantId: String,
        projectId: String,
        customerId: String,
        asOfDate: Long = System.currentTimeMillis()
    ): DomainResult<CustomerReceivableCollectionSummary>

    suspend fun getAuditHistory(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        actionId: String? = null
    ): DomainResult<List<CustomerCollectionAuditEvent>>
}
