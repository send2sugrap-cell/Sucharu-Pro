package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory implementation of [DeliveryGovernanceDataSource].
 */
class FakeDeliveryGovernanceDataSource : DeliveryGovernanceDataSource {

    private val mutex = Mutex()
    private val alertsFlow = MutableStateFlow<Map<String, DeliveryGovernanceAlert>>(emptyMap())
    private val events = mutableListOf<DeliveryGovernanceActivityEvent>()

    override suspend fun getAlerts(projectId: String): List<DeliveryGovernanceAlert> = mutex.withLock {
        alertsFlow.value.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
    }

    override suspend fun getAlertById(alertId: String): DeliveryGovernanceAlert? = mutex.withLock {
        alertsFlow.value[alertId]
    }

    override suspend fun insertAlert(alert: DeliveryGovernanceAlert) = mutex.withLock {
        val current = alertsFlow.value.toMutableMap()
        current[alert.alertId] = alert
        alertsFlow.value = current
        Unit
    }

    override suspend fun insertAlerts(alerts: List<DeliveryGovernanceAlert>) = mutex.withLock {
        val current = alertsFlow.value.toMutableMap()
        alerts.forEach { current[it.alertId] = it }
        alertsFlow.value = current
        Unit
    }

    override suspend fun updateAlert(alert: DeliveryGovernanceAlert) = mutex.withLock {
        val current = alertsFlow.value.toMutableMap()
        current[alert.alertId] = alert
        alertsFlow.value = current
        Unit
    }

    override suspend fun getActivityEvents(alertId: String): List<DeliveryGovernanceActivityEvent> = mutex.withLock {
        events.filter { it.alertId == alertId }.sortedBy { it.timestamp }
    }

    override suspend fun insertActivityEvent(event: DeliveryGovernanceActivityEvent) = mutex.withLock {
        events.add(event)
        Unit
    }

    override fun observeAlerts(projectId: String): Flow<List<DeliveryGovernanceAlert>> {
        return alertsFlow.map { map ->
            map.values.filter { it.projectId == projectId }.sortedByDescending { it.createdAt }
        }
    }
}
