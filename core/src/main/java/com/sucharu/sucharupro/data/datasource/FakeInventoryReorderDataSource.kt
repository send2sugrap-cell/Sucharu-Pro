package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderActivityEvent
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryReorderAlert
import com.sucharu.sucharupro.domain.model.inventory.reorder.InventoryStockLevelPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Thread-safe in-memory implementation of [InventoryReorderDataSource] using [MutableStateFlow].
 */
class FakeInventoryReorderDataSource : InventoryReorderDataSource {

    private val _policies = MutableStateFlow<List<InventoryStockLevelPolicy>>(emptyList())
    private val _alerts = MutableStateFlow<List<InventoryReorderAlert>>(emptyList())
    private val _auditEvents = MutableStateFlow<List<InventoryReorderActivityEvent>>(emptyList())

    override fun observePolicies(): Flow<List<InventoryStockLevelPolicy>> = _policies.asStateFlow()

    override suspend fun insertPolicy(policy: InventoryStockLevelPolicy): DomainResult<InventoryStockLevelPolicy> {
        _policies.update { it + policy }
        return DomainResult.Success(policy)
    }

    override suspend fun updatePolicy(policy: InventoryStockLevelPolicy): DomainResult<InventoryStockLevelPolicy> {
        _policies.update { list -> list.map { if (it.policyId == policy.policyId) policy else it } }
        return DomainResult.Success(policy)
    }

    override suspend fun deletePolicy(policyId: String): DomainResult<Unit> {
        _policies.update { it.filterNot { policy -> policy.policyId == policyId } }
        return DomainResult.Success(Unit)
    }

    override fun observeAlerts(): Flow<List<InventoryReorderAlert>> = _alerts.asStateFlow()

    override suspend fun insertAlert(alert: InventoryReorderAlert): DomainResult<InventoryReorderAlert> {
        _alerts.update { it + alert }
        return DomainResult.Success(alert)
    }

    override suspend fun updateAlert(alert: InventoryReorderAlert): DomainResult<InventoryReorderAlert> {
        _alerts.update { list -> list.map { if (it.alertId == alert.alertId) alert else it } }
        return DomainResult.Success(alert)
    }

    override suspend fun deleteAlert(alertId: String): DomainResult<Unit> {
        _alerts.update { it.filterNot { alert -> alert.alertId == alertId } }
        return DomainResult.Success(Unit)
    }

    override fun observeAuditEvents(): Flow<List<InventoryReorderActivityEvent>> = _auditEvents.asStateFlow()

    override suspend fun recordAuditEvent(event: InventoryReorderActivityEvent): DomainResult<InventoryReorderActivityEvent> {
        _auditEvents.update { it + event }
        return DomainResult.Success(event)
    }
}
