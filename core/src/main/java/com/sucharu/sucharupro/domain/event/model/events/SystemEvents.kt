package com.sucharu.sucharupro.domain.event.model.events

import com.sucharu.sucharupro.domain.event.model.DomainEvent
import com.sucharu.sucharupro.domain.event.model.DomainEventType

/**
 * Emitted when system maintenance is scheduled or announced.
 */
data class SystemMaintenanceScheduledEvent(
    val maintenanceId: String,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val description: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.SYSTEM_MAINTENANCE_SCHEDULED
    override val aggregateId: String get() = maintenanceId
    override val aggregateType: String get() = "SYSTEM"

    init {
        require(maintenanceId.isNotBlank()) { "maintenanceId cannot be blank" }
        require(description.isNotBlank()) { "description cannot be blank" }
        require(endTimestamp >= startTimestamp) { "endTimestamp must be after or equal to startTimestamp" }
    }
}

/**
 * Emitted when a system-wide or tenant-scoped operational alert is triggered.
 */
data class SystemAlertEvent(
    val alertId: String,
    val severity: String,
    val alertMessage: String,
    val affectedSubsystem: String,
    override val aggregateVersion: Long = 1L
) : DomainEvent {
    override val eventType: DomainEventType get() = DomainEventType.SYSTEM_ALERT
    override val aggregateId: String get() = alertId
    override val aggregateType: String get() = "SYSTEM"

    init {
        require(alertId.isNotBlank()) { "alertId cannot be blank" }
        require(severity.isNotBlank()) { "severity cannot be blank" }
        require(alertMessage.isNotBlank()) { "alertMessage cannot be blank" }
    }
}
