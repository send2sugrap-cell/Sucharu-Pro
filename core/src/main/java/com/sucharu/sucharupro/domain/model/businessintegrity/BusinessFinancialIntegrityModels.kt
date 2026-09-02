package com.sucharu.sucharupro.domain.model.businessintegrity

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Status for multi-module financial integrity runs.
 */
enum class FinancialIntegrityStatus {
    PASSED,
    WARNING,
    FAILED,
    BLOCKED;

    val isSuccess: Boolean
        get() = this in setOf(PASSED, WARNING)

    val isBlockingForPeriodClose: Boolean
        get() = this in setOf(FAILED, BLOCKED)
}

/**
 * Severity level for control assertion results.
 */
enum class AssertionSeverity {
    INFO,
    WARNING,
    CRITICAL;

    val isBlocking: Boolean
        get() = this == CRITICAL
}

/**
 * Period close readiness status.
 */
enum class PeriodClosureStatus {
    READY,
    BLOCKED,
    FINALIZED
}

/**
 * Canonical 18 Financial Control Assertions.
 */
enum class FinancialAssertionType(val code: String, val displayName: String, val defaultSeverity: AssertionSeverity) {
    ASSERTION_01_LEDGER_BALANCE(
        "ASSERT-01",
        "Ledger Debit/Credit Balance Integrity",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_02_EXPENSE_POSTING(
        "ASSERT-02",
        "Expense Approval and Ledger Posting Consistency",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_03_PAYABLE_BALANCE(
        "ASSERT-03",
        "Vendor Payable Outstanding Balance Consistency",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_04_PAYMENT_SETTLEMENT(
        "ASSERT-04",
        "Vendor Payment Settlement Consistency",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_05_COMMITMENT_CONSUMPTION(
        "ASSERT-05",
        "Cost Commitment Consumption vs Limit Integrity",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_06_ACCRUAL_REVERSAL(
        "ASSERT-06",
        "Cost Accrual vs Reversal Limit Integrity",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_07_ADJUSTMENT_POSTING(
        "ASSERT-07",
        "Financial Adjustment Compensating Posting Integrity",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_08_HARD_CLOSE_LOCK(
        "ASSERT-08",
        "Hard-Closed Period Lock & Backdated Mutation Protection",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_09_REPORTING_CONSISTENCY(
        "ASSERT-09",
        "Management Reporting Projections vs Canonical Sources",
        AssertionSeverity.WARNING
    ),
    ASSERTION_10_BUDGET_ACTUALS(
        "ASSERT-10",
        "Budget Actuals Origin from Canonical Spend Sources",
        AssertionSeverity.WARNING
    ),
    ASSERTION_11_FORECAST_NON_MUTATION(
        "ASSERT-11",
        "Deterministic Forecast Non-Mutation Invariance",
        AssertionSeverity.INFO
    ),
    ASSERTION_12_REFUND_WRITEOFF_AUDIT(
        "ASSERT-12",
        "Refunds & Write-Offs Authorized Audit Linkage",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_13_TENANT_ISOLATION(
        "ASSERT-13",
        "Multi-Tenant Boundary Isolation & Non-Contamination",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_14_PROJECT_ISOLATION(
        "ASSERT-14",
        "Project Boundary & Dimension Isolation",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_15_AUDIT_TRAIL_COMPLETENESS(
        "ASSERT-15",
        "Immutable Audit Trail Completeness",
        AssertionSeverity.WARNING
    ),
    ASSERTION_16_SEPARATION_OF_DUTIES(
        "ASSERT-16",
        "Separation of Duties (Creator != Approver != Finalizer)",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_17_IDEMPOTENCY_SAFETY(
        "ASSERT-17",
        "Idempotent Operations Duplicate Mutation Protection",
        AssertionSeverity.CRITICAL
    ),
    ASSERTION_18_CONCURRENCY_SAFETY(
        "ASSERT-18",
        "Concurrent Financial Operation Transaction Safety",
        AssertionSeverity.CRITICAL
    )
}

/**
 * Result entity for an individual control assertion evaluation.
 */
data class FinancialControlAssertion(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val runId: String,
    val periodId: String,
    val assertionType: FinancialAssertionType,
    val assertionName: String = assertionType.displayName,
    val status: FinancialIntegrityStatus = FinancialIntegrityStatus.PASSED,
    val severity: AssertionSeverity = assertionType.defaultSeverity,
    val expectedValue: String,
    val actualValue: String,
    val varianceValue: String? = null,
    val explanation: String,
    val recommendedAction: String? = null,
    val sourceEntitiesCount: Int = 0,
    val evaluatedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Entity representing a multi-module financial integrity run.
 */
data class FinancialIntegrityRun(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val runNumber: String,
    val status: FinancialIntegrityStatus = FinancialIntegrityStatus.PASSED,
    val executedBy: String,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val totalAssertionsCount: Int = 18,
    val passedAssertionsCount: Int = 0,
    val warningAssertionsCount: Int = 0,
    val failedAssertionsCount: Int = 0,
    val blockedAssertionsCount: Int = 0,
    val integrityChecksum: String,
    val notes: String? = null,
    val idempotencyKey: String? = null,
    val assertions: List<FinancialControlAssertion> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Tamper-evident certificate generated upon final period hard-closure.
 */
data class PeriodCloseCertificate(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val periodCode: String,
    val finalRunId: String,
    val closedBy: String,
    val closedAt: Long = System.currentTimeMillis(),
    val approvedBy: String,
    val approvedAt: Long = System.currentTimeMillis(),
    val status: String = "FINALIZED",
    val totalRecognizedExpenses: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalSettledPayables: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalLedgerDebit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalLedgerCredit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val netRecognizedAdjustments: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val certificateChecksum: String,
    val snapshotPayloadJson: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Period finalization readiness assessment.
 */
data class PeriodFinalizationReadiness(
    val periodId: String,
    val periodCode: String,
    val status: PeriodClosureStatus,
    val isReadyForClose: Boolean,
    val blockingReasons: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val latestRunId: String? = null,
    val latestRunStatus: FinancialIntegrityStatus? = null,
    val totalAssertionsCount: Int = 18,
    val failedAssertionsCount: Int = 0,
    val warningAssertionsCount: Int = 0,
    val evaluatedAt: Long = System.currentTimeMillis()
)

/**
 * Module 16 Handoff Contract: Verified Canonical Financial Dataset.
 * Ready for consumption by Module 16 (Profit & Cost Analysis).
 */
data class Module16FinancialHandoffContract(
    val tenantId: String,
    val projectId: String,
    val periodId: String,
    val periodCode: String,
    val currency: String = "BDT",
    val isPeriodClosed: Boolean,
    val closureCertificateChecksum: String?,
    val totalRecognizedRevenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalDirectExpenses: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalVendorPayablesSettled: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalRecognizedCostAllocations: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalActiveCommitmentExposure: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalOutstandingAccruals: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val netFinancialAdjustments: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val ledgerTotalDebit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val ledgerTotalCredit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val isLedgerBalanced: Boolean,
    val allocatedJobCostsCount: Int = 0,
    val verifiedAt: Long = System.currentTimeMillis()
)
