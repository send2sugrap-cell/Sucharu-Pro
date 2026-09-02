package com.sucharu.sucharupro.data.datasource.customercollection

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercollection.*
import java.util.concurrent.ConcurrentHashMap

class FakeCustomerCollectionDataSource : CustomerCollectionDataSource {

    private val actions = ConcurrentHashMap<String, CustomerCollectionAction>()
    private val promises = ConcurrentHashMap<String, CustomerPaymentPromise>()
    private val auditEvents = ConcurrentHashMap<String, MutableList<CustomerCollectionAuditEvent>>()

    override suspend fun saveAction(action: CustomerCollectionAction): DomainResult<CustomerCollectionAction> {
        val key = "${action.tenantId}_${action.projectId}_${action.actionId}"
        actions[key] = action
        return DomainResult.Success(action)
    }

    override suspend fun getActionById(
        tenantId: String,
        projectId: String,
        actionId: String
    ): DomainResult<CustomerCollectionAction?> {
        val key = "${tenantId}_${projectId}_${actionId}"
        return DomainResult.Success(actions[key])
    }

    override suspend fun getActionByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerCollectionAction?> {
        val found = actions.values.firstOrNull {
            it.tenantId == tenantId && it.projectId == projectId && it.idempotencyKey == idempotencyKey
        }
        return DomainResult.Success(found)
    }

    override suspend fun listActions(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: CollectionActionStatus?,
        assignedUserId: String?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerCollectionAction>> {
        val filtered = actions.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { status == null || it.status == status }
            .filter { assignedUserId == null || it.assignedUserId == assignedUserId }
            .sortedByDescending { it.scheduledAt }
            .drop(offset)
            .take(limit)
        return DomainResult.Success(filtered)
    }

    override suspend fun savePaymentPromise(promise: CustomerPaymentPromise): DomainResult<CustomerPaymentPromise> {
        val key = "${promise.tenantId}_${promise.projectId}_${promise.promiseId}"
        promises[key] = promise
        return DomainResult.Success(promise)
    }

    override suspend fun getPaymentPromiseById(
        tenantId: String,
        projectId: String,
        promiseId: String
    ): DomainResult<CustomerPaymentPromise?> {
        val key = "${tenantId}_${projectId}_${promiseId}"
        return DomainResult.Success(promises[key])
    }

    override suspend fun listPaymentPromises(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: PaymentPromiseStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerPaymentPromise>> {
        val filtered = promises.values
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { status == null || it.status == status }
            .sortedByDescending { it.promisedDate }
            .drop(offset)
            .take(limit)
        return DomainResult.Success(filtered)
    }

    override suspend fun recordAuditEvent(event: CustomerCollectionAuditEvent): DomainResult<Unit> {
        val key = "${event.tenantId}_${event.projectId}_${event.customerId}"
        auditEvents.computeIfAbsent(key) { mutableListOf() }.add(event)
        return DomainResult.Success(Unit)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        customerId: String?,
        actionId: String?,
        limit: Int
    ): DomainResult<List<CustomerCollectionAuditEvent>> {
        val allEvents = auditEvents.values.flatten()
            .filter { it.tenantId == tenantId && it.projectId == projectId }
            .filter { customerId == null || it.customerId == customerId }
            .filter { actionId == null || it.actionId == actionId }
            .sortedByDescending { it.occurredAt }
            .take(limit)
        return DomainResult.Success(allEvents)
    }
}
