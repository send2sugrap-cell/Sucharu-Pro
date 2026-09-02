package com.sucharu.sucharupro.domain.model.vendor

/**
 * Functional category/domain of the vendor performance KPI.
 */
enum class KpiType {
    DELIVERY,
    QUALITY,
    COST,
    COMPLIANCE,
    SERVICE,
    OPERATIONAL,
    OTHER
}

/**
 * How the KPI values are collected and computed.
 */
enum class KpiMeasurementMethod {
    AUTOMATED,
    MANUAL,
    HYBRID
}

/**
 * Directional optimization intent for scoring.
 */
enum class KpiDirection {
    HIGHER_IS_BETTER,
    LOWER_IS_BETTER,
    TARGET_IS_BEST
}

/**
 * Lifecycle state of a KPI definition.
 */
enum class KpiStatus {
    ACTIVE,
    INACTIVE,
    DEPRECATED
}

/**
 * Confidence/availability state of data measured for a KPI.
 */
enum class MeasurementConfidenceState {
    SUFFICIENT_DATA,
    LOW_SAMPLE_SIZE,
    NO_DATA
}

/**
 * Lifecycle status of a vendor performance scorecard.
 */
enum class ScorecardStatus {
    DRAFT,
    GENERATED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    FINALIZED,
    CANCELLED;

    fun canTransitionTo(target: ScorecardStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(GENERATED, CANCELLED)
            GENERATED -> target in setOf(UNDER_REVIEW, APPROVED, REJECTED, CANCELLED)
            UNDER_REVIEW -> target in setOf(APPROVED, REJECTED, CANCELLED)
            APPROVED -> target in setOf(FINALIZED, CANCELLED)
            REJECTED -> target in setOf(DRAFT, CANCELLED)
            FINALIZED -> false // Immutable historical snapshot
            CANCELLED -> false
        }
    }
}

/**
 * Evaluation periods for grouping performance measurements.
 */
enum class EvaluationPeriodType {
    MONTHLY,
    QUARTERLY,
    HALF_YEARLY,
    YEARLY,
    CUSTOM
}

/**
 * Formal evaluation lifecycle status.
 */
enum class EvaluationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    FINALIZED,
    CANCELLED;

    fun canTransitionTo(target: EvaluationStatus): Boolean {
        if (this == target) return true
        return when (this) {
            DRAFT -> target in setOf(SUBMITTED, CANCELLED)
            SUBMITTED -> target in setOf(UNDER_REVIEW, APPROVED, REJECTED, CANCELLED)
            UNDER_REVIEW -> target in setOf(APPROVED, REJECTED, CANCELLED)
            APPROVED -> target in setOf(FINALIZED, CANCELLED)
            REJECTED -> target in setOf(DRAFT, CANCELLED)
            FINALIZED -> false // Immutable historical snapshot
            CANCELLED -> false
        }
    }
}

/**
 * Formal evaluation decision outcome.
 */
enum class EvaluationDecision {
    APPROVED,
    CONDITIONALLY_APPROVED,
    REJECTED,
    ACTION_REQUIRED
}

/**
 * Overall performance rating bands.
 */
enum class PerformanceRating {
    EXCELLENT,
    GOOD,
    ACCEPTABLE,
    NEEDS_IMPROVEMENT,
    CRITICAL
}

/**
 * Types of compliance requirements imposed on vendors.
 */
enum class ComplianceRequirementType {
    TRADE_LICENSE,
    TAX_VAT,
    BUSINESS_REGISTRATION,
    SAFETY_CERTIFICATION,
    QUALITY_CERTIFICATION,
    ENVIRONMENTAL_CERTIFICATION,
    INSURANCE,
    CONTRACTUAL,
    INTERNAL_QUALIFICATION,
    OTHER
}

/**
 * Verification and lifecycle status of a vendor compliance record.
 */
enum class ComplianceStatus {
    PENDING,
    SUBMITTED,
    UNDER_REVIEW,
    VERIFIED,
    EXPIRING_SOON,
    EXPIRED,
    REJECTED,
    WAIVED;

    fun canTransitionTo(target: ComplianceStatus): Boolean {
        if (this == target) return true
        return when (this) {
            PENDING -> target in setOf(SUBMITTED, WAIVED)
            SUBMITTED -> target in setOf(UNDER_REVIEW, VERIFIED, REJECTED, WAIVED)
            UNDER_REVIEW -> target in setOf(VERIFIED, REJECTED, WAIVED)
            VERIFIED -> target in setOf(EXPIRING_SOON, EXPIRED, UNDER_REVIEW, WAIVED)
            EXPIRING_SOON -> target in setOf(EXPIRED, SUBMITTED, VERIFIED, WAIVED)
            EXPIRED -> target in setOf(SUBMITTED, WAIVED)
            REJECTED -> target in setOf(SUBMITTED, WAIVED)
            WAIVED -> target in setOf(PENDING, SUBMITTED)
        }
    }
}

/**
 * Risk level associated with compliance or operational performance.
 */
enum class ComplianceRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Verification state of compliance record.
 */
enum class ComplianceVerificationStatus {
    PENDING,
    UNDER_REVIEW,
    VERIFIED,
    REJECTED
}

/**
 * Types of evidence attached to compliance records.
 */
enum class ComplianceEvidenceType {
    DOCUMENT,
    CERTIFICATE,
    REPORT,
    IMAGE,
    LINK,
    OTHER
}

/**
 * Priority of corrective action.
 */
enum class CorrectiveActionPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Lifecycle status of a vendor corrective action.
 */
enum class CorrectiveActionStatus {
    OPEN,
    IN_PROGRESS,
    PENDING_VERIFICATION,
    VERIFIED,
    CLOSED,
    CANCELLED;

    fun canTransitionTo(target: CorrectiveActionStatus): Boolean {
        if (this == target) return true
        return when (this) {
            OPEN -> target in setOf(IN_PROGRESS, CANCELLED)
            IN_PROGRESS -> target in setOf(PENDING_VERIFICATION, CANCELLED)
            PENDING_VERIFICATION -> target in setOf(VERIFIED, IN_PROGRESS, CANCELLED)
            VERIFIED -> target in setOf(CLOSED)
            CLOSED -> false
            CANCELLED -> false
        }
    }
}

/**
 * Types of vendor risk indicators.
 */
enum class RiskIndicatorType {
    QUALITY_FAILURE,
    HIGH_REJECTION_RATE,
    HIGH_DISPUTE_RATE,
    LATE_DELIVERY,
    EXPIRED_COMPLIANCE,
    COMPLIANCE_EXPIRING_SOON,
    PRICE_VARIANCE,
    INVOICE_MISMATCH,
    UNRESOLVED_CORRECTIVE_ACTIONS,
    LOW_PERFORMANCE_SCORE,
    OTHER
}

/**
 * Severity ranking for risk indicators.
 */
enum class RiskSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Lifecycle status of a detected risk.
 */
enum class RiskStatus {
    ACTIVE,
    MITIGATED,
    RESOLVED
}

/**
 * Append-only audit event types for vendor performance subsystem.
 */
enum class VendorPerformanceAuditEventType {
    KPI_CREATED,
    KPI_UPDATED,
    KPI_DEACTIVATED,
    MEASUREMENT_GENERATED,
    SCORECARD_GENERATED,
    SCORECARD_SUBMITTED,
    SCORECARD_APPROVED,
    SCORECARD_REJECTED,
    SCORECARD_FINALIZED,
    EVALUATION_CREATED,
    EVALUATION_SUBMITTED,
    EVALUATION_APPROVED,
    EVALUATION_REJECTED,
    EVALUATION_FINALIZED,
    COMPLIANCE_REQUIREMENT_CREATED,
    COMPLIANCE_RECORD_SUBMITTED,
    COMPLIANCE_RECORD_VERIFIED,
    COMPLIANCE_RECORD_REJECTED,
    COMPLIANCE_RECORD_EXPIRED,
    CORRECTIVE_ACTION_CREATED,
    CORRECTIVE_ACTION_STARTED,
    CORRECTIVE_ACTION_VERIFIED,
    CORRECTIVE_ACTION_CLOSED,
    RISK_DETECTED,
    RISK_RESOLVED
}
