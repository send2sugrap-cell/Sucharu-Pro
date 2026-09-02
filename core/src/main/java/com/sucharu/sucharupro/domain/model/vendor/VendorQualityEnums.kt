package com.sucharu.sucharupro.domain.model.vendor

/**
 * Inspection type classifications for quality control.
 */
enum class VendorInspectionType {
    RECEIVING_INSPECTION,
    RANDOM_INSPECTION,
    PRE_PRODUCTION,
    IN_PROCESS,
    FINAL_INSPECTION,
    RETURN_INSPECTION,
    OTHER
}

/**
 * Lifecycle status of a quality inspection.
 */
enum class VendorInspectionStatus {
    DRAFT,
    IN_PROGRESS,
    PASSED,
    PARTIALLY_PASSED,
    FAILED,
    CANCELLED;

    fun canTransitionTo(target: VendorInspectionStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(IN_PROGRESS, CANCELLED)
            IN_PROGRESS -> target in setOf(PASSED, PARTIALLY_PASSED, FAILED, CANCELLED)
            PASSED -> false
            PARTIALLY_PASSED -> false
            FAILED -> false
            CANCELLED -> false
        }
    }
}

/**
 * Item-level and overall inspection result.
 */
enum class InspectionResult {
    ACCEPTED,
    CONDITIONAL,
    REJECTED
}

/**
 * Categorization of quality defects.
 */
enum class VendorDefectType {
    QUANTITY_MISMATCH,
    QUALITY_DEFECT,
    DIMENSION_ERROR,
    COLOR_VARIANCE,
    MATERIAL_VARIANCE,
    PRINT_QUALITY,
    FINISHING_DEFECT,
    PACKAGING_DAMAGE,
    DELIVERY_DAMAGE,
    SPECIFICATION_MISMATCH,
    MISSING_COMPONENT,
    CONTAMINATION,
    OTHER
}

/**
 * Severity level of a defect.
 */
enum class VendorDefectSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Lifecycle status of a formal vendor rejection.
 */
enum class VendorRejectionStatus {
    DRAFT,
    PENDING_VENDOR_RESPONSE,
    ACCEPTED,
    DISPUTED,
    RETURN_PENDING,
    REPLACEMENT_PENDING,
    CREDIT_PENDING,
    RESOLVED,
    CANCELLED,
    CLOSED;

    fun canTransitionTo(target: VendorRejectionStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(PENDING_VENDOR_RESPONSE, CANCELLED)
            PENDING_VENDOR_RESPONSE -> target in setOf(
                ACCEPTED, DISPUTED, RETURN_PENDING, REPLACEMENT_PENDING, CREDIT_PENDING, RESOLVED, CANCELLED
            )
            ACCEPTED -> target in setOf(RESOLVED, CLOSED)
            DISPUTED -> target in setOf(RESOLVED, CLOSED)
            RETURN_PENDING -> target in setOf(RESOLVED, CLOSED)
            REPLACEMENT_PENDING -> target in setOf(RESOLVED, CLOSED)
            CREDIT_PENDING -> target in setOf(RESOLVED, CLOSED)
            RESOLVED -> target == CLOSED
            CANCELLED -> false
            CLOSED -> false
        }
    }
}

/**
 * Formal disposition assigned to rejected materials/services.
 */
enum class VendorRejectionDisposition {
    RETURN_TO_VENDOR,
    REPLACE,
    REWORK,
    ACCEPT_WITH_DISCOUNT,
    SCRAP,
    HOLD,
    OTHER
}

/**
 * Categorization of formal vendor disputes.
 */
enum class VendorDisputeType {
    QUALITY,
    QUANTITY,
    SPECIFICATION,
    DELIVERY,
    PRICE,
    INVOICE,
    REJECTION,
    REPLACEMENT,
    OTHER
}

/**
 * Urgency/priority ranking of a vendor dispute.
 */
enum class VendorDisputePriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Lifecycle status of a vendor dispute.
 */
enum class VendorDisputeStatus {
    OPEN,
    UNDER_REVIEW,
    AWAITING_VENDOR,
    AWAITING_INTERNAL_ACTION,
    ESCALATED,
    RESOLUTION_PROPOSED,
    RESOLVED,
    CLOSED,
    REJECTED,
    CANCELLED;

    fun canTransitionTo(target: VendorDisputeStatus): Boolean {
        if (this == target) return true
        return when (this) {
            OPEN -> target in setOf(UNDER_REVIEW, AWAITING_VENDOR, ESCALATED, REJECTED, CANCELLED)
            UNDER_REVIEW -> target in setOf(AWAITING_VENDOR, AWAITING_INTERNAL_ACTION, ESCALATED, RESOLUTION_PROPOSED, RESOLVED, REJECTED, CANCELLED)
            AWAITING_VENDOR -> target in setOf(UNDER_REVIEW, AWAITING_INTERNAL_ACTION, ESCALATED, RESOLUTION_PROPOSED, RESOLVED, REJECTED, CANCELLED)
            AWAITING_INTERNAL_ACTION -> target in setOf(UNDER_REVIEW, AWAITING_VENDOR, ESCALATED, RESOLUTION_PROPOSED, RESOLVED, REJECTED, CANCELLED)
            ESCALATED -> target in setOf(UNDER_REVIEW, AWAITING_VENDOR, RESOLUTION_PROPOSED, RESOLVED, REJECTED, CANCELLED)
            RESOLUTION_PROPOSED -> target in setOf(RESOLVED, UNDER_REVIEW, ESCALATED, REJECTED, CANCELLED)
            RESOLVED -> target == CLOSED
            CLOSED -> false
            REJECTED -> false
            CANCELLED -> false
        }
    }
}

/**
 * Event types for dispute event stream.
 */
enum class VendorDisputeEventType {
    CREATED,
    ASSIGNED,
    VENDOR_NOTIFIED,
    VENDOR_RESPONDED,
    EVIDENCE_ADDED,
    INVESTIGATION_STARTED,
    INVESTIGATION_UPDATED,
    RESOLUTION_PROPOSED,
    RESOLUTION_ACCEPTED,
    RESOLUTION_REJECTED,
    ESCALATED,
    RESOLVED,
    CLOSED,
    CANCELLED
}
