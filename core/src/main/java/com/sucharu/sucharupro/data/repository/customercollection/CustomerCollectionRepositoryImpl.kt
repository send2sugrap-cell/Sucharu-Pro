package com.sucharu.sucharupro.data.repository.customercollection

import com.sucharu.sucharupro.data.datasource.customercollection.CustomerCollectionDataSource
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercollection.*
import com.sucharu.sucharupro.domain.repository.customercollection.CustomerCollectionRepository

class CustomerCollectionRepositoryImpl(
    private val dataSource: CustomerCollectionDataSource
) : CustomerCollectionRepository {

    override suspend fun saveAction(action: CustomerCollectionAction): DomainResult<CustomerCollectionAction> {
        return dataSource.saveAction(action)
    }

    override suspend fun getActionById(
        tenantId: String,
        projectId: String,
        actionId: String
    ): DomainResult<CustomerCollectionAction?> {
        return dataSource.getActionById(tenantId, projectId, actionId)
    }

    override suspend fun getActionByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerCollectionAction?> {
        return dataSource.getActionByIdempotencyKey(tenantId, projectId, idempotencyKey)
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
        return dataSource.listActions(tenantId, projectId, customerId, status, assignedUserId, limit, offset)
    }

    override suspend fun savePaymentPromise(promise: CustomerPaymentPromise): DomainResult<CustomerPaymentPromise> {
        return dataSource.savePaymentPromise(promise)
    }

    override suspend fun getPaymentPromiseById(
        tenantId: String,
        projectId: String,
        promiseId: String
    ): DomainResult<CustomerPaymentPromise?> {
        return dataSource.getPaymentPromiseById(tenantId, projectId, promiseId)
    }

    override suspend fun listPaymentPromises(
        tenantId: String,
        projectId: String,
        customerId: String?,
        status: PaymentPromiseStatus?,
        limit: Int,
        offset: Int
    ): DomainResult<List<CustomerPaymentPromise>> {
        return dataSource.listPaymentPromises(tenantId, projectId, customerId, status, limit, offset)
    }

    override suspend fun recordAuditEvent(event: CustomerCollectionAuditEvent): DomainResult<Unit> {
        return dataSource.recordAuditEvent(event)
    }

    override suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        customerId: String?,
        actionId: String?,
        limit: Int
    ): DomainResult<List<CustomerCollectionAuditEvent>> {
        return dataSource.getAuditEvents(tenantId, projectId, customerId, actionId, limit)
    }
}
