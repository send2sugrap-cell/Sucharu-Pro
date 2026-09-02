package com.sucharu.sucharupro.domain.validation.vendorportal

import com.sucharu.sucharupro.domain.model.vendorportal.*
import java.math.BigDecimal

/**
 * Domain validator enforcing delivery notice quantity integrity, lifecycle transitions,
 * quality response constraints, and exception invariants (Module 13 Step 05).
 */
object VendorPortalDeliveryValidator {

    fun validateDeliveryNotice(notice: VendorPortalDeliveryNotice) {
        require(notice.noticeId.isNotBlank()) { "Notice ID cannot be blank." }
        require(notice.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(notice.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(notice.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(notice.purchaseOrderId.isNotBlank()) { "Purchase Order ID cannot be blank." }
        require(notice.orderNumber.isNotBlank()) { "Order Number cannot be blank." }
        require(notice.noticeNumber.isNotBlank()) { "Notice Number cannot be blank." }
        require(notice.plannedDeliveryDate > 0) { "Planned delivery date must be a valid future or current timestamp." }
        require(notice.createdBy.isNotBlank()) { "CreatedBy cannot be blank." }
        require(notice.items.isNotEmpty()) { "Delivery notice must contain at least one item." }

        for (item in notice.items) {
            validateDeliveryNoticeItem(item)
        }
    }

    fun validateDeliveryNoticeItem(item: VendorPortalDeliveryNoticeItem) {
        require(item.itemId.isNotBlank()) { "Item ID cannot be blank." }
        require(item.noticeId.isNotBlank()) { "Notice ID cannot be blank." }
        require(item.purchaseOrderItemId.isNotBlank()) { "Purchase order item ID cannot be blank." }
        require(item.itemName.isNotBlank()) { "Item name cannot be blank." }
        require(item.deliveryQuantity > BigDecimal.ZERO) { "Delivery quantity must be strictly positive." }
        require(item.orderedQuantity > BigDecimal.ZERO) { "Ordered quantity must be strictly positive." }
        require(item.previouslyDeliveredQuantity >= BigDecimal.ZERO) { "Previously delivered quantity cannot be negative." }

        val remainingAllowed = item.orderedQuantity.subtract(item.previouslyDeliveredQuantity)
        require(item.deliveryQuantity <= remainingAllowed) {
            "Delivery quantity (${item.deliveryQuantity}) exceeds remaining eligible ordered quantity ($remainingAllowed) for item '${item.itemName}'."
        }
    }

    fun validateNoticeStatusTransition(
        currentStatus: VendorPortalDeliveryNoticeStatus,
        targetStatus: VendorPortalDeliveryNoticeStatus
    ) {
        val validTransitions = mapOf(
            VendorPortalDeliveryNoticeStatus.DRAFT to setOf(
                VendorPortalDeliveryNoticeStatus.SUBMITTED,
                VendorPortalDeliveryNoticeStatus.CANCELLED
            ),
            VendorPortalDeliveryNoticeStatus.SUBMITTED to setOf(
                VendorPortalDeliveryNoticeStatus.ACKNOWLEDGED,
                VendorPortalDeliveryNoticeStatus.IN_TRANSIT,
                VendorPortalDeliveryNoticeStatus.CANCELLED
            ),
            VendorPortalDeliveryNoticeStatus.ACKNOWLEDGED to setOf(
                VendorPortalDeliveryNoticeStatus.IN_TRANSIT,
                VendorPortalDeliveryNoticeStatus.DELIVERED,
                VendorPortalDeliveryNoticeStatus.CANCELLED
            ),
            VendorPortalDeliveryNoticeStatus.IN_TRANSIT to setOf(
                VendorPortalDeliveryNoticeStatus.DELIVERED
            ),
            VendorPortalDeliveryNoticeStatus.DELIVERED to emptySet(),
            VendorPortalDeliveryNoticeStatus.CANCELLED to emptySet()
        )

        val allowed = validTransitions[currentStatus] ?: emptySet()
        require(allowed.contains(targetStatus)) {
            "Illegal delivery notice status transition from $currentStatus to $targetStatus."
        }
    }

    fun validateQualityResponse(response: VendorPortalQualityResponse) {
        require(response.responseId.isNotBlank()) { "Response ID cannot be blank." }
        require(response.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(response.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(response.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(response.inspectionId.isNotBlank()) { "Inspection ID cannot be blank." }
        require(response.comment.isNotBlank()) { "Comment / response text cannot be blank." }
        require(response.respondedBy.isNotBlank()) { "RespondedBy actor ID cannot be blank." }

        when (response.responseType) {
            VendorPortalQualityResponseType.PROPOSE_CORRECTIVE_ACTION -> {
                require(!response.correctiveActionPlan.isNullOrBlank()) {
                    "Corrective action plan description is required when proposing corrective action."
                }
            }
            VendorPortalQualityResponseType.COMMIT_REPLACEMENT -> {
                require(response.promisedReplacementDate != null && response.promisedReplacementDate > 0) {
                    "Promised replacement date is required when committing to replacement."
                }
            }
            VendorPortalQualityResponseType.REQUEST_DISPUTE -> {
                require(response.comment.length >= 10) {
                    "Dispute request requires detailed explanation (minimum 10 characters)."
                }
            }
            VendorPortalQualityResponseType.ACKNOWLEDGE -> {
                // Comment is sufficient
            }
        }
    }

    fun validateDeliveryException(exception: VendorPortalDeliveryException) {
        require(exception.exceptionId.isNotBlank()) { "Exception ID cannot be blank." }
        require(exception.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(exception.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(exception.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(exception.sourceType.isNotBlank()) { "Source type cannot be blank." }
        require(exception.sourceId.isNotBlank()) { "Source ID cannot be blank." }
        require(exception.title.isNotBlank()) { "Title cannot be blank." }
        require(exception.description.isNotBlank()) { "Description cannot be blank." }
        require(exception.createdBy.isNotBlank()) { "CreatedBy cannot be blank." }
    }

    fun validateEvidence(evidence: VendorPortalDeliveryEvidence) {
        require(evidence.evidenceId.isNotBlank()) { "Evidence ID cannot be blank." }
        require(evidence.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(evidence.projectId.isNotBlank()) { "Project ID cannot be blank." }
        require(evidence.vendorId.isNotBlank()) { "Vendor ID cannot be blank." }
        require(evidence.entityType.isNotBlank()) { "Entity type cannot be blank." }
        require(evidence.entityId.isNotBlank()) { "Entity ID cannot be blank." }
        require(evidence.filename.isNotBlank()) { "Filename cannot be blank." }
        require(evidence.fileReference.isNotBlank()) { "File reference URI cannot be blank." }
        require(evidence.sizeBytes > 0) { "Size in bytes must be positive." }
        require(evidence.uploadedBy.isNotBlank()) { "UploadedBy cannot be blank." }
    }
}
