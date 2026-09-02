package com.sucharu.sucharupro.domain.model.vendorportal

/**
 * Lifecycle Status of a Vendor's Evaluation Response.
 */
enum class VendorPortalEvaluationResponseStatus {
    SUBMITTED,
    ACKNOWLEDGED,
    UNDER_REVIEW,
    ADDITIONAL_INFO_REQUESTED,
    CLOSED
}

/**
 * Type of response submitted by the vendor for an evaluation.
 */
enum class VendorPortalEvaluationResponseType {
    ACKNOWLEDGEMENT,
    FORMAL_RESPONSE,
    CLARIFICATION_REQUEST,
    DISPUTE_APPEAL
}

/**
 * Expiry alert severity levels for vendor certifications & compliance documents.
 */
enum class VendorPortalExpiryAlertLevel {
    NORMAL,
    UPCOMING_30_DAYS,
    CRITICAL_7_DAYS,
    EXPIRED
}

/**
 * Status of vendor remediation on an assigned corrective action.
 */
enum class VendorPortalRemediationStatus {
    PLAN_SUBMITTED,
    IN_PROGRESS,
    COMPLETED_PENDING_VERIFICATION,
    VERIFIED,
    REJECTED
}

/**
 * Types of compliance evidence uploaded by vendor.
 */
enum class VendorPortalComplianceEvidenceType {
    DOCUMENT,
    CERTIFICATE,
    AUDIT_REPORT,
    LAB_TEST_REPORT,
    INSURANCE_POLICY,
    TAX_CLEARANCE,
    ISO_DOCUMENT,
    POLICY_ACKNOWLEDGEMENT,
    OTHER
}

/**
 * Audit event classifications for performance and compliance portal operations.
 */
enum class VendorPortalPerformanceComplianceAuditEventType {
    EVALUATION_VIEWED,
    EVALUATION_ACKNOWLEDGED,
    EVALUATION_RESPONSE_SUBMITTED,
    SCORECARD_VIEWED,
    COMPLIANCE_EVIDENCE_UPLOADED,
    CORRECTIVE_ACTION_VIEWED,
    CORRECTIVE_ACTION_RESPONSE_SUBMITTED,
    CORRECTIVE_ACTION_COMPLETION_REQUESTED,
    EXPIRY_ALERT_ACKNOWLEDGED
}
