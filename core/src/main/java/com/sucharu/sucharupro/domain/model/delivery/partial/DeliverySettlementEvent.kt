package com.sucharu.sucharupro.domain.model.delivery.partial

/**
 * Event types for audit trail of Delivery Partial Settlement (Module 08 Step 06).
 */
enum class DeliverySettlementEventType(val defaultLabel: String) {
    CREATED("Settlement Created"),
    UPDATED("Settlement Updated"),
    SPLIT_CREATED("Split Dispatch Created"),
    QUANTITY_ALLOCATED("Quantity Allocated"),
    PARTIAL_DELIVERY_RECORDED("Partial Delivery Recorded"),
    DELIVERY_COMPLETED("Delivery Completed"),
    SHORTAGE_RECORDED("Shortage Recorded"),
    EXCESS_RECORDED("Excess Recorded"),
    RETURN_RECORDED("Return Recorded"),
    REPLACEMENT_RECORDED("Replacement Recorded"),
    SETTLEMENT_RECALCULATED("Settlement Recalculated"),
    SETTLED("Settlement Finalized"),
    DISPUTED("Settlement Disputed"),
    CANCELLED("Settlement Cancelled")
}

/**
 * Immutable audit/timeline event model for settlement activities (Module 08 Step 06).
 */
data class DeliverySettlementEvent(
    val eventId: String,
    val projectId: String,
    val settlementId: String,
    val eventType: DeliverySettlementEventType,
    val referenceId: String? = null,
    val actorId: String,
    val timestamp: Long,
    val details: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(eventId.isNotBlank()) { "Event ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(settlementId.isNotBlank()) { "Settlement ID cannot be blank." }
        require(actorId.isNotBlank()) { "Actor ID cannot be blank." }
        require(timestamp > 0) { "Timestamp must be positive." }
    }
}
