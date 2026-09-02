package com.sucharu.sucharupro.domain.model.qc

/**
 * Structured enumeration of release eligibility conditions and blocking reasons (Module 06 Step 07).
 */
enum class FinalQcReleaseEligibility(
    val defaultLabel: String,
    val isBlocking: Boolean
) {
    /** All 14 quality gates satisfied. Production release is eligible. */
    ELIGIBLE(
        defaultLabel = "Eligible for Production Release",
        isBlocking = false
    ),

    /** Pre-Production QC is missing or has not passed. */
    BLOCKED_PRE_PRODUCTION_QC(
        defaultLabel = "Pre-Production QC is not completed or passed",
        isBlocking = true
    ),

    /** One or more active/unresolved defects remain open on the job. */
    BLOCKED_OPEN_DEFECT(
        defaultLabel = "Active unresolved defect(s) block release",
        isBlocking = true
    ),

    /** One or more rework orders remain active/incomplete. */
    BLOCKED_ACTIVE_REWORK(
        defaultLabel = "Active rework process is in progress",
        isBlocking = true
    ),

    /** One or more Re-QC cycles remain unpassed or failed. */
    BLOCKED_FAILED_RE_QC(
        defaultLabel = "Unresolved or failed Re-QC cycle remains",
        isBlocking = true
    ),

    /** Final QC inspection is still pending or in progress. */
    BLOCKED_PENDING_INSPECTION(
        defaultLabel = "Final QC inspection is pending or in inspection",
        isBlocking = true
    ),

    /** Required inspection checklist was not completed. */
    BLOCKED_MISSING_CHECKLIST(
        defaultLabel = "Mandatory checklist is incomplete",
        isBlocking = true
    ),

    /** The job is archived, non-existent, or in an invalid state. */
    BLOCKED_INVALID_JOB(
        defaultLabel = "Production job is invalid or archived",
        isBlocking = true
    ),

    /** Actor lacks the required RBAC role to authorize release. */
    BLOCKED_RBAC(
        defaultLabel = "Actor is not authorized to release production",
        isBlocking = true
    ),

    /** Cross-project or cross-job contamination detected. */
    BLOCKED_CROSS_PROJECT_REFERENCE(
        defaultLabel = "Cross-project or cross-job reference mismatch",
        isBlocking = true
    ),

    /** The job has already been released (idempotency guard). */
    BLOCKED_ALREADY_RELEASED(
        defaultLabel = "Production release has already been authorized",
        isBlocking = true
    ),

    /** Rejected quantity is greater than 0 or exceeds acceptable tolerance. */
    BLOCKED_QUANTITY_REJECTED(
        defaultLabel = "Inspection has rejected quantity",
        isBlocking = true
    ),

    /** Final QC inspection decision is explicitly FAIL. */
    BLOCKED_INSPECTION_FAILED(
        defaultLabel = "Final QC inspection decision is FAIL",
        isBlocking = true
    );

    companion object {
        fun fromString(value: String?): FinalQcReleaseEligibility? {
            if (value.isNullOrBlank()) return null
            return entries.find { it.name.equals(value.trim(), ignoreCase = true) }
        }
    }
}

/**
 * Structured evaluation result for Final QC Production Release eligibility.
 */
data class FinalQcEligibilityResult(
    val isEligible: Boolean,
    val reasons: List<FinalQcReleaseEligibility>,
    val message: String
)
