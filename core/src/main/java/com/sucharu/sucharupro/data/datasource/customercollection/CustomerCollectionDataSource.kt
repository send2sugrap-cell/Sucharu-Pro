package com.sucharu.sucharupro.data.datasource.customercollection

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.customercollection.*

interface CustomerCollectionDataSource {

    suspend fun saveAction(action: CustomerCollectionAction): DomainResult<CustomerCollectionAction>

    suspend fun getActionById(
        tenantId: String,
        projectId: String,
        actionId: String
    ): DomainResult<CustomerCollectionAction?>

    suspend fun getActionByIdempotencyKey(
        tenantId: String,
        projectId: String,
        idempotencyKey: String
    ): DomainResult<CustomerCollectionAction?>

    suspend fun listActions(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: CollectionActionStatus? = null,
        assignedUserId: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<CustomerCollectionAction>>

    suspend fun savePaymentPromise(promise: CustomerPaymentPromise): DomainResult<CustomerPaymentPromise>

    suspend fun getPaymentPromiseById(
        tenantId: String,
        projectId: String,
        promiseId: String
    ): DomainResult<CustomerPaymentPromise?>

    suspend fun listPaymentPromises(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        status: PaymentPromiseStatus? = null,
        limit: Int = 100,
        offset: Int = 0
    ): DomainResult<List<CustomerPaymentPromise>>

    suspend fun recordAuditEvent(event: CustomerCollectionAuditEvent): DomainResult<Unit>

    suspend fun getAuditEvents(
        tenantId: String,
        projectId: String,
        customerId: String? = null,
        actionId: String? = null,
        limit: Int = 100
    ): DomainResult<List<CustomerCollectionAuditEvent>>
}
