package com.sucharu.sucharupro.domain.model.delivery

/**
 * Represents a request to dispatch a specific Delivery Order.
 *
 * @param dispatchRequestId Unique identifier for the request.
 * @param projectId The project context.
 * @param deliveryOrderId Reference to the Delivery Order to be dispatched.
 * @param requestedBy ID of the user requesting dispatch.
 * @param requestedAt Timestamp of the request.
 * @param priority Dispatch urgency.
 * @param status Current status of the dispatch request.
 * @param notes Optional dispatch instructions.
 */
data class DeliveryDispatchRequest(
    val dispatchRequestId: String,
    val projectId: String,
    val deliveryOrderId: String,
    val requestedBy: String,
    val requestedAt: Long,
    val priority: DeliveryPriority,
    val status: DispatchRequestStatus,
    val notes: String?
) {
    init {
        require(dispatchRequestId.isNotBlank()) { "Dispatch Request ID cannot be blank." }
        require(projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(deliveryOrderId.isNotBlank()) { "Delivery Order ID cannot be blank." }
        require(requestedBy.isNotBlank()) { "Requested By cannot be blank." }
        require(requestedAt > 0) { "Requested At timestamp must be positive." }
    }
}
