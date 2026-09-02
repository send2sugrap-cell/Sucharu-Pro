package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliation
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationDiscrepancy
import com.sucharu.sucharupro.domain.model.delivery.reconciliation.DeliveryReconciliationItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe, in-memory implementation of DeliveryReconciliationDataSource for testing & production wiring (Module 08 Step 09).
 */
class FakeDeliveryReconciliationDataSource : DeliveryReconciliationDataSource {

    private val mutex = Mutex()
    private val reconciliations = MutableStateFlow<Map<String, DeliveryReconciliation>>(emptyMap())
    private val items = MutableStateFlow<Map<String, List<DeliveryReconciliationItem>>>(emptyMap())
    private val discrepancies = MutableStateFlow<Map<String, List<DeliveryReconciliationDiscrepancy>>>(emptyMap())
    private val events = MutableStateFlow<Map<String, List<DeliveryReconciliationActivityEvent>>>(emptyMap())

    override fun observeReconciliations(projectId: String): Flow<List<DeliveryReconciliation>> {
        return reconciliations.asStateFlow().map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }

    override fun observeReconciliation(reconciliationId: String): Flow<DeliveryReconciliation?> {
        return reconciliations.asStateFlow().map { it[reconciliationId] }
    }

    override fun observeItems(reconciliationId: String): Flow<List<DeliveryReconciliationItem>> {
        return items.asStateFlow().map { it[reconciliationId] ?: emptyList() }
    }

    override fun observeDiscrepancies(reconciliationId: String): Flow<List<DeliveryReconciliationDiscrepancy>> {
        return discrepancies.asStateFlow().map { it[reconciliationId] ?: emptyList() }
    }

    override fun observeActivityEvents(reconciliationId: String): Flow<List<DeliveryReconciliationActivityEvent>> {
        return events.asStateFlow().map { it[reconciliationId] ?: emptyList() }
    }

    override suspend fun getReconciliation(reconciliationId: String): DeliveryReconciliation? = mutex.withLock {
        reconciliations.value[reconciliationId]
    }

    override suspend fun getReconciliationByDeliveryOrder(deliveryOrderId: String): DeliveryReconciliation? = mutex.withLock {
        reconciliations.value.values.find { it.deliveryOrderId == deliveryOrderId }
    }

    override suspend fun getItems(reconciliationId: String): List<DeliveryReconciliationItem> = mutex.withLock {
        items.value[reconciliationId] ?: emptyList()
    }

    override suspend fun getDiscrepancies(reconciliationId: String): List<DeliveryReconciliationDiscrepancy> = mutex.withLock {
        discrepancies.value[reconciliationId] ?: emptyList()
    }

    override suspend fun getDiscrepancy(discrepancyId: String): DeliveryReconciliationDiscrepancy? = mutex.withLock {
        discrepancies.value.values.flatten().find { it.discrepancyId == discrepancyId }
    }

    override suspend fun insertReconciliation(
        reconciliation: DeliveryReconciliation,
        items: List<DeliveryReconciliationItem>,
        discrepancies: List<DeliveryReconciliationDiscrepancy>
    ) = mutex.withLock {
        reconciliations.value = reconciliations.value + (reconciliation.reconciliationId to reconciliation)
        this.items.value = this.items.value + (reconciliation.reconciliationId to items)
        this.discrepancies.value = this.discrepancies.value + (reconciliation.reconciliationId to discrepancies)
    }

    override suspend fun updateReconciliation(
        reconciliation: DeliveryReconciliation,
        items: List<DeliveryReconciliationItem>?,
        discrepancies: List<DeliveryReconciliationDiscrepancy>?
    ) = mutex.withLock {
        reconciliations.value = reconciliations.value + (reconciliation.reconciliationId to reconciliation)
        if (items != null) {
            this.items.value = this.items.value + (reconciliation.reconciliationId to items)
        }
        if (discrepancies != null) {
            this.discrepancies.value = this.discrepancies.value + (reconciliation.reconciliationId to discrepancies)
        }
    }

    override suspend fun updateDiscrepancy(discrepancy: DeliveryReconciliationDiscrepancy) = mutex.withLock {
        val list = discrepancies.value[discrepancy.reconciliationId] ?: emptyList()
        val updated = list.map { if (it.discrepancyId == discrepancy.discrepancyId) discrepancy else it }
        discrepancies.value = discrepancies.value + (discrepancy.reconciliationId to updated)
    }

    override suspend fun insertActivityEvent(event: DeliveryReconciliationActivityEvent) = mutex.withLock {
        val current = events.value[event.reconciliationId] ?: emptyList()
        events.value = events.value + (event.reconciliationId to (current + event))
    }
}
