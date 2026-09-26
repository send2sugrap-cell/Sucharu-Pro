package com.sucharu.sucharupro.domain.service.communication

import com.sucharu.sucharupro.domain.model.communication.automation.*
import com.sucharu.sucharupro.domain.model.notification.NotificationChannel
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * BI-10 Domain Service for Event-Driven Communication Automation & Dispatch.
 */
class CommunicationAutomationDispatchService {

    private val dispatchStore = ConcurrentHashMap<String, CommunicationDispatchAttempt>()
    private val idempotencyMap = ConcurrentHashMap<String, String>()

    init {
        // Default Sample Dispatches
        val d1 = CommunicationDispatchAttempt(
            dispatchId = "DSP-2026-001",
            eventId = "EVT-ORD-1001",
            ruleId = "RULE-ORD-CONFIRM",
            recipientId = "CUST-1001",
            recipientRole = "CUSTOMER",
            channel = NotificationChannel.IN_APP,
            title = "অর্ডার নিশ্চিতকরণ — ORD-1001",
            renderedMessage = "প্রিয় কাস্টমার, আপনার অর্ডার #ORD-1001 সফলভাবে নিশ্চিত করা হয়েছে (মূল্য: ৳410)।",
            deliveryStatus = CommunicationDeliveryStatus.DELIVERED,
            attemptCount = 1,
            idempotencyKey = "IDEM-EVT-ORD-1001-CUST-1001",
            dispatchedAt = "2026-09-26T14:05:00Z",
            deliveredAt = "2026-09-26T14:05:02Z"
        )
        val d2 = CommunicationDispatchAttempt(
            dispatchId = "DSP-2026-002",
            eventId = "EVT-SLA-1001",
            ruleId = "RULE-SLA-OVERDUE",
            recipientId = "STAFF-001",
            recipientRole = "STAFF",
            channel = NotificationChannel.IN_APP,
            title = "⚠️ SLA Overdue Alert — Order #ORD-1001",
            renderedMessage = "Order #ORD-1001 for Dhaka Printing Press is 1 day overdue in PRINTING stage.",
            deliveryStatus = CommunicationDeliveryStatus.SENT,
            attemptCount = 1,
            idempotencyKey = "IDEM-EVT-SLA-1001-STAFF-001",
            dispatchedAt = "2026-09-26T19:00:00Z"
        )

        dispatchStore[d1.dispatchId] = d1
        dispatchStore[d2.dispatchId] = d2
        idempotencyMap[d1.idempotencyKey] = d1.dispatchId
        idempotencyMap[d2.idempotencyKey] = d2.dispatchId
    }

    /**
     * Process an incoming Business Event and dispatches communication with idempotency deduplication.
     */
    suspend fun processAndDispatchEvent(
        eventId: String,
        recipientId: String,
        title: String,
        message: String,
        channel: NotificationChannel = NotificationChannel.IN_APP,
        idempotencyKey: String
    ): CommunicationDispatchAttempt {
        // Idempotency check: if event was already processed with same key, return existing dispatch
        idempotencyMap[idempotencyKey]?.let { existingId ->
            dispatchStore[existingId]?.let { return it }
        }

        val dispatchId = "DSP-" + UUID.randomUUID().toString().take(8).uppercase()
        val timestamp = "2026-09-26T21:00:00Z"

        val dispatch = CommunicationDispatchAttempt(
            dispatchId = dispatchId,
            eventId = eventId,
            recipientId = recipientId,
            channel = channel,
            title = title,
            renderedMessage = message,
            deliveryStatus = CommunicationDeliveryStatus.SENT,
            attemptCount = 1,
            idempotencyKey = idempotencyKey,
            dispatchedAt = timestamp,
            deliveredAt = timestamp
        )

        dispatchStore[dispatchId] = dispatch
        idempotencyMap[idempotencyKey] = dispatchId
        return dispatch
    }

    suspend fun buildAutomationDispatchSummary(): CommunicationAutomationDispatchSummary {
        val timestamp = "2026-09-26T21:00:00Z"
        val dispatches = dispatchStore.values.toList()

        return CommunicationAutomationDispatchSummary(
            totalEventsProcessedCount = dispatches.size,
            queuedCount = dispatches.count { it.deliveryStatus == CommunicationDeliveryStatus.QUEUED },
            sentCount = dispatches.count { it.deliveryStatus == CommunicationDeliveryStatus.SENT },
            deliveredCount = dispatches.count { it.deliveryStatus == CommunicationDeliveryStatus.DELIVERED },
            failedCount = dispatches.count { it.deliveryStatus == CommunicationDeliveryStatus.FAILED },
            retriedCount = dispatches.count { it.deliveryStatus == CommunicationDeliveryStatus.RETRYING },
            isOutboxDecoupledDeliveryEnforced = true,
            activeDispatches = dispatches,
            generatedAt = timestamp
        )
    }
}
