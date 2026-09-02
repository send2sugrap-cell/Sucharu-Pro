package com.sucharu.sucharupro.domain.model.communication.automation

/**
 * Incoming domain/business event trigger payload (Module 10 Step 08).
 *
 * Security: Sanitized payload container preventing credential and sensitive token leakage.
 */
data class CommunicationTriggerEvent(
    val triggerId: String,
    val projectId: String,
    val eventType: CommunicationAutomationEventType,
    val sourceEntityType: String, // "ORDER", "PAYMENT", "DELIVERY", "QC", "CUSTOMER", "VENDOR"
    val sourceEntityId: String,
    val referenceId: String? = null,
    val referenceType: String? = null,
    val actorUserId: String,
    val payloadMetadata: Map<String, String> = emptyMap(),
    val occurredAt: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(triggerId.isNotBlank()) { "Trigger ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(sourceEntityType.isNotBlank()) { "Source Entity Type cannot be blank." }
        require(sourceEntityId.isNotBlank()) { "Source Entity ID cannot be blank." }
        require(actorUserId.isNotBlank()) { "Actor User ID cannot be blank." }

        val forbiddenKeys = listOf("password", "token", "secret", "cvv", "card_number", "pin", "api_key", "bearer")
        for (key in payloadMetadata.keys) {
            val lower = key.lowercase()
            require(forbiddenKeys.none { lower.contains(it) }) {
                "Sensitive key '$key' is prohibited in trigger event payload."
            }
        }
    }
}
