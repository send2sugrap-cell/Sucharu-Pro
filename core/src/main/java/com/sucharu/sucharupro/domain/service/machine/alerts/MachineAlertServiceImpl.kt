package com.sucharu.sucharupro.domain.service.machine.alerts

import com.sucharu.sucharupro.domain.machine.MachineStatus
import com.sucharu.sucharupro.domain.machine.alerts.*
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import com.sucharu.sucharupro.domain.model.notification.NotificationType
import com.sucharu.sucharupro.domain.model.user.UserRole
import com.sucharu.sucharupro.domain.repository.machine.MachineRegistryRepository
import com.sucharu.sucharupro.domain.repository.machine.alerts.MachineAlertRepository
import com.sucharu.sucharupro.domain.repository.notification.NotificationRepository

/**
 * Domain Service implementation for Machine Operational Alerts.
 * Integrates directly with Module 10 NotificationRepository for communication dispatch.
 */
class MachineAlertServiceImpl(
    private val alertRepository: MachineAlertRepository,
    private val machineRegistryRepository: MachineRegistryRepository,
    private val notificationRepository: NotificationRepository? = null
) : MachineAlertService {

    override suspend fun raiseAlert(alert: MachineOperationalAlert): DomainResult<MachineOperationalAlert> {
        val machineCheck = machineRegistryRepository.getMachineById(alert.tenantId, alert.machineId)
        if (machineCheck is DomainResult.Error) return machineCheck
        val machine = (machineCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Machine '${alert.machineId}' not found in registry.")

        if (machine.status == MachineStatus.DECOMMISSIONED) {
            return DomainResult.Error(message = "Cannot raise alert for decommissioned machine '${alert.machineId}'.")
        }

        // Deduplication via correlationKey
        if (!alert.correlationKey.isNullOrBlank()) {
            val existingCheck = alertRepository.getAlertByCorrelationKey(alert.tenantId, alert.correlationKey)
            if (existingCheck is DomainResult.Success && existingCheck.data != null) {
                // Return existing active alert to avoid notification spam
                return DomainResult.Success(existingCheck.data)
            }
        }

        // Dispatch canonical Module 10 Notification
        var dispatchedNotificationId: String? = alert.notificationId
        if (notificationRepository != null) {
            val notifType = when (alert.alertType) {
                MachineAlertType.TELEMETRY_ABNORMAL -> NotificationType.MACHINE_TELEMETRY_ALERT
                MachineAlertType.HEALTH_CRITICAL -> NotificationType.MACHINE_HEALTH_CRITICAL
                MachineAlertType.FAULT_EVENT -> NotificationType.MACHINE_FAULT_ALERT
                MachineAlertType.MAINTENANCE_DUE -> NotificationType.MAINTENANCE_DUE_ALERT
                MachineAlertType.MAINTENANCE_OVERDUE -> NotificationType.MAINTENANCE_OVERDUE_ALERT
                MachineAlertType.WARNING -> NotificationType.MACHINE_TELEMETRY_ALERT
            }

            val notifRes = notificationRepository.createNotification(
                projectId = alert.tenantId,
                recipientUserId = alert.createdBy ?: "SYSTEM_STAFF",
                title = "[${machine.name}] ${alert.title}",
                message = alert.description,
                notificationType = notifType,
                channel = NotificationChannel.IN_APP,
                referenceType = "MACHINE_ALERT",
                referenceId = alert.alertId,
                actorId = alert.createdBy ?: "SYSTEM",
                callerRole = UserRole.STAFF
            )
            if (notifRes is DomainResult.Success) {
                dispatchedNotificationId = notifRes.data.notificationId
            }
        }

        val finalAlert = alert.copy(notificationId = dispatchedNotificationId)
        return alertRepository.saveAlert(finalAlert)
    }

    override suspend fun acknowledgeAlert(
        tenantId: String,
        machineId: String,
        alertId: String,
        actorId: String
    ): DomainResult<MachineOperationalAlert> {
        val alertCheck = alertRepository.getAlertById(tenantId, alertId)
        if (alertCheck is DomainResult.Error) return alertCheck
        val existing = (alertCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Alert '$alertId' not found.")

        if (existing.machineId != machineId) {
            return DomainResult.Error(message = "Machine ID mismatch for alert '$alertId'.")
        }

        val transitionValidation = MachineAlertValidator.validateStatusTransition(existing.status, MachineAlertStatus.ACKNOWLEDGED)
        if (transitionValidation is DomainResult.Error) return transitionValidation

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = MachineAlertStatus.ACKNOWLEDGED,
            acknowledgedAt = now,
            acknowledgedBy = actorId,
            updatedAt = now,
            updatedBy = actorId
        )
        return alertRepository.saveAlert(updated)
    }

    override suspend fun resolveAlert(
        tenantId: String,
        machineId: String,
        alertId: String,
        resolutionNotes: String?,
        actorId: String
    ): DomainResult<MachineOperationalAlert> {
        val alertCheck = alertRepository.getAlertById(tenantId, alertId)
        if (alertCheck is DomainResult.Error) return alertCheck
        val existing = (alertCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Alert '$alertId' not found.")

        if (existing.machineId != machineId) {
            return DomainResult.Error(message = "Machine ID mismatch for alert '$alertId'.")
        }

        val transitionValidation = MachineAlertValidator.validateStatusTransition(existing.status, MachineAlertStatus.RESOLVED)
        if (transitionValidation is DomainResult.Error) return transitionValidation

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = MachineAlertStatus.RESOLVED,
            resolvedAt = now,
            resolvedBy = actorId,
            resolutionNotes = resolutionNotes ?: existing.resolutionNotes,
            updatedAt = now,
            updatedBy = actorId
        )
        return alertRepository.saveAlert(updated)
    }

    override suspend fun dismissAlert(
        tenantId: String,
        machineId: String,
        alertId: String,
        reason: String?,
        actorId: String
    ): DomainResult<MachineOperationalAlert> {
        val alertCheck = alertRepository.getAlertById(tenantId, alertId)
        if (alertCheck is DomainResult.Error) return alertCheck
        val existing = (alertCheck as? DomainResult.Success)?.data
            ?: return DomainResult.Error(message = "Alert '$alertId' not found.")

        val transitionValidation = MachineAlertValidator.validateStatusTransition(existing.status, MachineAlertStatus.DISMISSED)
        if (transitionValidation is DomainResult.Error) return transitionValidation

        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = MachineAlertStatus.DISMISSED,
            resolutionNotes = if (!reason.isNullOrBlank()) "Dismissed: $reason" else existing.resolutionNotes,
            updatedAt = now,
            updatedBy = actorId
        )
        return alertRepository.saveAlert(updated)
    }

    override suspend fun getAlertDetails(
        tenantId: String,
        alertId: String
    ): DomainResult<MachineOperationalAlert?> {
        return alertRepository.getAlertById(tenantId, alertId)
    }

    override suspend fun listAlerts(
        tenantId: String,
        machineId: String,
        status: MachineAlertStatus?,
        limit: Int
    ): DomainResult<List<MachineOperationalAlert>> {
        return alertRepository.listAlertsByMachine(tenantId, machineId, status, limit)
    }
}
