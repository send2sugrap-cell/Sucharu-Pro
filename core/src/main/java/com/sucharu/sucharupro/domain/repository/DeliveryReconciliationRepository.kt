package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationSummary
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Delivery Reconciliation & Settlement (Module 08 Step 09).
 */
interface DeliveryReconciliationRepository {

    // Reactive Queries
    fun observeReconciliations(projectId: String): Flow<List<DeliveryReconciliation>>
    fun observeReconciliation(reconciliationId: String): Flow<DeliveryReconciliation?>
    fun observeItems(reconciliationId: String): Flow<List<DeliveryReconciliationItem>>
    fun observeDiscrepancies(reconciliationId: String): Flow<List<DeliveryReconciliationDiscrepancy>>
    fun observeActivityEvents(reconciliationId: String): Flow<List<DeliveryReconciliationActivityEvent>>
    fun observeSummary(projectId: String): Flow<DeliveryReconciliationSummary>

    // Synchronous Queries
    suspend fun getReconciliation(
        reconciliationId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReconciliation>

    suspend fun getReconciliationByDeliveryOrder(
        deliveryOrderId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReconciliation>

    suspend fun getItems(
        reconciliationId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryReconciliationItem>>

    suspend fun getDiscrepancies(
        reconciliationId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryReconciliationDiscrepancy>>

    suspend fun getActivityEvents(
        reconciliationId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<List<DeliveryReconciliationActivityEvent>>

    // Mutations & Calculations
    suspend fun createReconciliation(
        deliveryOrderId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReconciliation>

    suspend fun refreshCalculation(
        reconciliationId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReconciliation>

    suspend fun startReconciliation(
        reconciliationId: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReconciliation>

    suspend fun resolveDiscrepancy(
        reconciliationId: String,
        discrepancyId: String,
        resolutionNotes: String,
        actorId: String,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReconciliationDiscrepancy>

    suspend fun markReconciled(
        reconciliationId: String,
        actorId: String,
        notes: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReconciliation>

    suspend fun closeReconciliation(
        reconciliationId: String,
        actorId: String,
        notes: String? = null,
        callerRole: UserRole? = null,
        callerProjectId: String? = null
    ): DomainResult<DeliveryReconciliation>
}
