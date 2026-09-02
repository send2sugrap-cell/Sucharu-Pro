package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Profitability Analysis Dimension Scope.
 */
enum class ProfitabilityScope {
    JOB,
    PRODUCT,
    CUSTOMER,
    VENDOR,
    PERIOD,
    BUSINESS,
    PROJECT
}

/**
 * Analytical Cost Component Classification.
 * Non-mutating categorical classification over canonical costs.
 */
enum class CostComponentType {
    DIRECT_PRODUCTION,
    MATERIAL,
    LABOUR,
    MACHINE,
    VENDOR,
    OUTSOURCE,
    TRANSPORT,
    PACKAGING,
    REWORK,
    QC,
    FINANCIAL,
    OVERHEAD,
    OTHER
}

/**
 * Canonical Revenue Source Type.
 */
enum class RevenueSourceType {
    CUSTOMER_INVOICE,
    ORDER,
    JOB,
    OTHER_CANONICAL_REVENUE
}

/**
 * Canonical Cost Attribution Source Type.
 */
enum class CostAttributionSourceType {
    EXPENSE,
    VENDOR_BILL,
    PAYABLE,
    JOB_COST,
    PRODUCTION_RECORD,
    INVENTORY_MOVEMENT,
    REWORK,
    TRANSPORT,
    OTHER_CANONICAL_SOURCE
}

/**
 * Integrity State for Analytical Snapshots.
 */
enum class SourceIntegrityStatus {
    VERIFIED,
    PARTIALLY_VERIFIED,
    SOURCE_MISSING,
    SOURCE_CONFLICT,
    PERIOD_LOCKED,
    CALCULATION_BLOCKED
}

/**
 * Analytical Boundary and Context for Calculations.
 */
data class ProfitabilityAnalysisContext(
    val tenantId: String,
    val projectId: String,
    val analysisId: String,
    val analysisScope: ProfitabilityScope,
    val targetEntityId: String? = null,
    val periodId: String? = null,
    val currency: String = "BDT",
    val sourceSnapshotReference: String? = null,
    val calculationVersion: String = "1.0.0",
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Cost Attribution Reference linking canonical source transactions to analytical dimensions.
 */
data class CostAttributionReference(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val sourceType: CostAttributionSourceType,
    val sourceId: String,
    val componentType: CostComponentType,
    val jobId: String? = null,
    val orderId: String? = null,
    val productId: String? = null,
    val customerId: String? = null,
    val vendorId: String? = null,
    val periodId: String? = null,
    val attributionBasis: String = "DIRECT",
    val sourceAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val attributableAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val recordedAt: Long = System.currentTimeMillis()
)

/**
 * Revenue Provenance Reference linking analytical revenue back to canonical sources.
 */
data class RevenueProvenance(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val canonicalSourceType: RevenueSourceType,
    val canonicalSourceId: String,
    val customerId: String? = null,
    val orderId: String? = null,
    val jobId: String? = null,
    val periodId: String? = null,
    val recognizedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val recognitionState: String = "RECOGNIZED",
    val sourceTimestamp: Long = System.currentTimeMillis()
)

/**
 * Standard Profitability Metrics container.
 */
data class ProfitabilityMetric(
    val revenue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val directCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val indirectCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val grossProfit: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val grossMarginPercentage: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val baselineCost: BigDecimal? = null,
    val costVariance: BigDecimal? = null,
    val revenueVariance: BigDecimal? = null,
    val marginVariance: BigDecimal? = null
)

/**
 * Cost Component Breakdown Summary.
 */
data class CostComponentBreakdown(
    val componentType: CostComponentType,
    val totalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val percentageOfTotalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val itemCount: Int = 0
)

/**
 * Read-Oriented Profitability Analysis Snapshot.
 * Analytical projection over canonical financial data.
 */
data class ProfitabilitySnapshot(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val scope: ProfitabilityScope,
    val targetEntityId: String? = null,
    val periodId: String? = null,
    val currency: String = "BDT",
    val metrics: ProfitabilityMetric,
    val costBreakdowns: List<CostComponentBreakdown> = emptyList(),
    val revenueProvenances: List<RevenueProvenance> = emptyList(),
    val costAttributions: List<CostAttributionReference> = emptyList(),
    val calculationVersion: String = "1.0.0",
    val sourceIntegrityStatus: SourceIntegrityStatus = SourceIntegrityStatus.VERIFIED,
    val financialHandoffVerified: Boolean = true,
    val handoffChecksum: String? = null,
    val integrityNotes: List<String> = emptyList(),
    val generatedBy: String = "SYSTEM",
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Canonical Source Readiness and Availability Summary.
 */
data class ProfitabilitySourceReadiness(
    val tenantId: String,
    val projectId: String,
    val periodId: String? = null,
    val module15HandoffStatus: SourceIntegrityStatus = SourceIntegrityStatus.VERIFIED,
    val isLedgerBalanced: Boolean = true,
    val directExpensesAvailable: Boolean = true,
    val vendorPayablesAvailable: Boolean = true,
    val recognizedRevenueAvailable: Boolean = true,
    val costAllocationsAvailable: Boolean = true,
    val activeCommitmentsCount: Int = 0,
    val outstandingAccrualsCount: Int = 0,
    val periodClosed: Boolean = false,
    val warnings: List<String> = emptyList(),
    val evaluatedAt: Long = System.currentTimeMillis()
)

/**
 * Profitability Reconciliation Event.
 */
data class ProfitabilityReconciliationEvent(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val snapshotId: String,
    val scope: ProfitabilityScope,
    val targetEntityId: String? = null,
    val periodId: String? = null,
    val isReconciled: Boolean,
    val canonicalRevenueTotal: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val snapshotRevenueTotal: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val revenueDifference: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val canonicalCostTotal: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val snapshotCostTotal: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val costDifference: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val discrepancies: List<String> = emptyList(),
    val checkedBy: String = "SYSTEM",
    val checkedAt: Long = System.currentTimeMillis()
)

/**
 * Profitability Analytical Audit Event.
 */
data class ProfitabilityAuditEvent(
    val id: String,
    val tenantId: String,
    val projectId: String,
    val snapshotId: String?,
    val action: String,
    val scope: ProfitabilityScope?,
    val targetEntityId: String?,
    val outcome: String,
    val details: String?,
    val actor: String,
    val timestamp: Long = System.currentTimeMillis(),
    val correlationId: String? = null
)
