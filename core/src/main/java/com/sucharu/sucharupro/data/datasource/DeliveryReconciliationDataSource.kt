package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem
import kotlinx.coroutines.flow.Flow

/**
 * Data Source contract for Delivery Reconciliation storage (Module 08 Step 09).
 */
interface DeliveryReconciliationDataSource {
    fun observeReconciliations(projectId: String): Flow<List<DeliveryReconciliation>>
    fun observeReconciliation(reconciliationId: String): Flow<DeliveryReconciliation?>
    fun observeItems(reconciliationId: String): Flow<List<DeliveryReconciliationItem>>
    fun observeDiscrepancies(reconciliationId: String): Flow<List<DeliveryReconciliationDiscrepancy>>
    fun observeActivityEvents(reconciliationId: String): Flow<List<DeliveryReconciliationActivityEvent>>

    suspend fun getReconciliation(reconciliationId: String): DeliveryReconciliation?
    suspend fun getReconciliationByDeliveryOrder(deliveryOrderId: String): DeliveryReconciliation?
    suspend fun getItems(reconciliationId: String): List<DeliveryReconciliationItem>
    suspend fun getDiscrepancies(reconciliationId: String): List<DeliveryReconciliationDiscrepancy>
    suspend fun getDiscrepancy(discrepancyId: String): DeliveryReconciliationDiscrepancy?

    suspend fun insertReconciliation(
        reconciliation: DeliveryReconciliation,
        items: List<DeliveryReconciliationItem>,
        discrepancies: List<DeliveryReconciliationDiscrepancy> = emptyList()
    )

    suspend fun updateReconciliation(
        reconciliation: DeliveryReconciliation,
        items: List<DeliveryReconciliationItem>? = null,
        discrepancies: List<DeliveryReconciliationDiscrepancy>? = null
    )

    suspend fun updateDiscrepancy(discrepancy: DeliveryReconciliationDiscrepancy)
    suspend fun insertActivityEvent(event: DeliveryReconciliationActivityEvent)
}
