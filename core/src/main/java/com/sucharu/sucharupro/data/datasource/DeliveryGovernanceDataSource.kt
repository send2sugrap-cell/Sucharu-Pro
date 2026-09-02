package com.sucharu.sucharupro.data.datasource

import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceActivityEvent
import com.sucharu.sucharupro.domain.model.delivery.governance.DeliveryGovernanceAlert
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for Delivery Governance Alerts and Audit Activity Events (Module 08 Step 10).
 */
interface DeliveryGovernanceDataSource {
    suspend fun getAlerts(projectId: String): List<DeliveryGovernanceAlert>
    suspend fun getAlertById(alertId: String): DeliveryGovernanceAlert?
    suspend fun insertAlert(alert: DeliveryGovernanceAlert)
    suspend fun insertAlerts(alerts: List<DeliveryGovernanceAlert>)
    suspend fun updateAlert(alert: DeliveryGovernanceAlert)
    suspend fun getActivityEvents(alertId: String): List<DeliveryGovernanceActivityEvent>
    suspend fun insertActivityEvent(event: DeliveryGovernanceActivityEvent)
    fun observeAlerts(projectId: String): Flow<List<DeliveryGovernanceAlert>>
}
