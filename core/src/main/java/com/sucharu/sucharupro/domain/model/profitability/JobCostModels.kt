package com.sucharu.sucharupro.domain.model.profitability

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 12 Canonical Job Cost Component Types (Module 16 Step 02).
 */
enum class JobCostComponentType(val displayName: String) {
    MATERIAL_COST("Material Cost"),
    LABOUR_COST("Labour Cost"),
    MACHINE_COST("Machine Cost"),
    PRODUCTION_OPERATION_COST("Production Operation Cost"),
    VENDOR_OUTSOURCE_COST("Vendor / Outsource Cost"),
    REWORK_COST("Rework Cost"),
    WASTAGE_COST("Wastage Cost"),
    FINISHING_COST("Finishing Cost"),
    PACKAGING_COST("Packaging Cost"),
    TRANSPORT_COST("Transport Cost"),
    OTHER_DIRECT_COST("Other Direct Cost"),
    ALLOCATED_INDIRECT_COST("Allocated Indirect Cost");

    val isDirect: Boolean get() = this != ALLOCATED_INDIRECT_COST
    val isIndirect: Boolean get() = this == ALLOCATED_INDIRECT_COST
}

/**
 * Directness of a cost component relative to an individual Job.
 */
enum class CostDirectness {
    DIRECT,
    INDIRECT,
    UNALLOCATED
}

/**
 * Approved basis used when allocating indirect/overhead costs to a Job.
 */
enum class AllocationBasisType(val displayName: String) {
    MACHINE_HOURS("Machine Hours"),
    LABOUR_HOURS("Labour Hours"),
    PRODUCTION_QUANTITY("Production Quantity"),
    DIRECT_COST_RATIO("Direct Cost Ratio"),
    CUSTOM_APPROVED_RATIO("Custom Approved Ratio"),
    UNALLOCATED("Unallocated / No Basis");
}

/**
 * Deterministic cost variance classification against planned/estimated baseline.
 */
enum class CostVarianceClassification(val displayName: String) {
    UNDER_BUDGET("Under Budget"),
    ON_TARGET("On Target (±2%)"),
    OVER_BUDGET("Over Budget"),
    BASELINE_UNAVAILABLE("Baseline Unavailable"),
    SOURCE_CONFLICT("Source Conflict"),
    SOURCE_INCOMPLETE("Source Incomplete");
}

/**
 * Analytical readiness status for a Job actual cost calculation.
 */
enum class JobCostReadinessStatus(val displayName: String) {
    COMPLETE("Complete & Verified"),
    PARTIAL("Partial (Some Sources Pending)"),
    PENDING("Pending (Core Sources Missing)"),
    CONFLICTED("Conflicted (Source Discrepancy)"),
    UNALLOCATED("Unallocated (Overhead Pending)"),
    INVALID("Invalid / Verification Failed");

    val isReadyForReporting: Boolean get() = this == COMPLETE || this == PARTIAL
}

/**
 * Canonical Source Provenance Record for an individual Job Cost attribution.
 */
data class JobCostProvenance(
    val provenanceId: String,
    val tenantId: String,
    val projectId: String,
    val jobId: String,
    val sourceModule: String, // e.g. "MODULE_04", "MODULE_06", "MODULE_08", "MODULE_12", "MODULE_15"
    val sourceEntityType: String, // e.g. "PRODUCTION_STAGE", "QC_COST", "STOCK_OUT", "VENDOR_PAYABLE", "WORK_ORDER", "EXPENSE"
    val sourceEntityId: String,
    val sourceTransactionId: String? = null,
    val sourceReference: String? = null,
    val vendorId: String? = null,
    val operationId: String? = null,
    val inventoryMovementId: String? = null,
    val expenseId: String? = null,
    val payableId: String? = null,
    val qcCostId: String? = null,
    val reworkId: String? = null,
    val costComponentType: JobCostComponentType,
    val directness: CostDirectness = CostDirectness.DIRECT,
    val originalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val attributedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val attributionBasis: String = "CANONICAL_ACTUAL",
    val calculationExplanation: String = "Direct attribution from source record",
    val fingerprintHash: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Individual Cost Component Breakdown for a Job.
 */
data class JobCostComponent(
    val componentId: String,
    val tenantId: String,
    val projectId: String,
    val jobId: String,
    val componentType: JobCostComponentType,
    val directness: CostDirectness,
    val quantity: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unitRate: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val originalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val attributedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val percentageOfTotalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val currency: String = "BDT",
    val attributionBasis: String = "CANONICAL",
    val sourceItemCount: Int = 1,
    val calculationExplanation: String = "",
    val provenances: List<JobCostProvenance> = emptyList()
)

/**
 * Cost Variance against an estimated / baseline printing cost.
 */
data class JobCostVariance(
    val actualCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val estimatedCost: BigDecimal? = null,
    val costVariance: BigDecimal? = null, // actualCost - estimatedCost
    val costVariancePercentage: BigDecimal? = null, // ((actualCost - estimatedCost) / estimatedCost) * 100
    val classification: CostVarianceClassification = CostVarianceClassification.BASELINE_UNAVAILABLE,
    val explanation: String = ""
)

/**
 * Detailed overhead cost allocation trace.
 */
data class JobCostAllocationDetail(
    val allocationId: String,
    val tenantId: String,
    val projectId: String,
    val jobId: String,
    val costPoolName: String,
    val allocationBasis: AllocationBasisType,
    val poolTotalAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val numeratorValue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val denominatorValue: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val allocatedRatio: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val allocatedAmount: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val sourcePeriodId: String? = null,
    val approvedBy: String? = null,
    val allocatedAt: Long = System.currentTimeMillis()
)

/**
 * Immutable Analytical Job Actual Cost Snapshot (Module 16 Step 02).
 */
data class JobCostSnapshot(
    val snapshotId: String,
    val tenantId: String,
    val projectId: String,
    val jobId: String,
    val jobNumber: String? = null,
    val customerId: String? = null,
    val productId: String? = null,
    val jobQuantity: Int = 0,
    val calculationVersion: String = "JOB_COST_ENGINE_V1",
    val calculationTimestamp: Long = System.currentTimeMillis(),
    val currency: String = "BDT",
    val totalActualCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalDirectCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalIndirectCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val estimatedCost: BigDecimal? = null,
    val costVariance: BigDecimal? = null,
    val costVariancePercentage: BigDecimal? = null,
    val varianceClassification: CostVarianceClassification = CostVarianceClassification.BASELINE_UNAVAILABLE,
    val readinessStatus: JobCostReadinessStatus = JobCostReadinessStatus.COMPLETE,
    val isReconciled: Boolean = true,
    val sourceCount: Int = 0,
    val duplicateSourceCount: Int = 0,
    val unresolvedSourceCount: Int = 0,
    val costComponents: List<JobCostComponent> = emptyList(),
    val provenances: List<JobCostProvenance> = emptyList(),
    val allocations: List<JobCostAllocationDetail> = emptyList(),
    val warnings: List<String> = emptyList(),
    val integrityHash: String = "",
    val generatedBy: String = "SYSTEM"
)

/**
 * Result of a non-mutating Job Cost Reconciliation run.
 */
data class JobCostReconciliationEvent(
    val reconciliationId: String,
    val tenantId: String,
    val projectId: String,
    val jobId: String,
    val snapshotId: String,
    val isReconciled: Boolean,
    val componentTotalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val snapshotTotalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val provenanceTotalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val componentDifference: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val provenanceDifference: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val duplicateCount: Int = 0,
    val missingSourceCount: Int = 0,
    val discrepancies: List<String> = emptyList(),
    val checkedBy: String = "SYSTEM",
    val checkedAt: Long = System.currentTimeMillis()
)

/**
 * Immutable Job Cost Audit Event.
 */
data class JobCostAuditEvent(
    val eventId: String,
    val tenantId: String,
    val projectId: String,
    val jobId: String,
    val snapshotId: String? = null,
    val action: String, // e.g. "CALCULATED", "RECONCILED", "EXPORTED"
    val actor: String,
    val actorRole: String = "STAFF",
    val outcome: String = "SUCCESS",
    val details: String = "",
    val correlationId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
