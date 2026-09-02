package com.sucharu.sucharupro.domain.model.businesscost

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Standard Cost Center classification in a commercial printing enterprise (Module 15 Step 04).
 */
enum class StandardCostCenterType(val displayName: String, val defaultCode: String) {
    PRINTING("Printing Production", "CC-PRINT"),
    DESIGN("Design & Pre-Press", "CC-DESIGN"),
    PRE_PRESS("Pre-Press & CTP", "CC-PREPRESS"),
    FINISHING("Post-Press & Finishing", "CC-FINISH"),
    PACKAGING("Packaging & Bundling", "CC-PACK"),
    DELIVERY("Logistics & Delivery", "CC-DELIV"),
    WAREHOUSE("Warehouse & Material Storage", "CC-WH"),
    ADMINISTRATION("General Administration", "CC-ADMIN"),
    SALES("Sales & Account Management", "CC-SALES"),
    CUSTOMER_SERVICE("Customer Service & Proofing", "CC-CS"),
    MANAGEMENT("Executive Management", "CC-MGMT"),
    OTHER("Other Operational Cost Center", "CC-OTHER")
}

/**
 * Cost Center Aggregate Entity.
 * Supports hierarchical parent-child relationships (e.g. Production -> Printing, Finishing).
 */
data class BusinessCostCenter(
    val id: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val code: String,
    val name: String,
    val description: String? = null,
    val parentCostCenterId: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Standard Cost Categories for commercial printing operations.
 */
enum class StandardCostCategoryType(val displayName: String, val defaultCode: String, val isDirectProduction: Boolean) {
    PAPER("Paper & Substrates", "CAT-PAPER", true),
    PRINTING("Printing Ink & Plates", "CAT-PRINT", true),
    CTP("CTP Chemical & Output", "CAT-CTP", true),
    LAMINATION("Lamination & Film", "CAT-LAM", true),
    FOILING("Foil Stamping & Die", "CAT-FOIL", true),
    DIE_CUTTING("Die Cutting & Embossing", "CAT-DIECUT", true),
    BINDING("Binding & Stitching", "CAT-BIND", true),
    LABOUR("Direct Production Labour", "CAT-LABOUR", true),
    TRANSPORT("Logistics & Transport", "CAT-TRANSPORT", false),
    PACKAGING("Boxes & Packing Materials", "CAT-PACK", true),
    ELECTRICITY("Electricity & Power", "CAT-ELEC", false),
    MAINTENANCE("Machine Maintenance & Repairs", "CAT-MAINT", false),
    RENT("Facility Rent & Lease", "CAT-RENT", false),
    OFFICE("Office Supplies & Admin", "CAT-OFFICE", false),
    MARKETING("Marketing & Promotion", "CAT-MKT", false),
    COMMUNICATION("Internet & Telecom", "CAT-COMM", false),
    OUTSOURCE("Outsourced Job Work", "CAT-OUTSOURCE", true),
    OTHER("Other Operational Expense", "CAT-OTHER", false)
}

/**
 * Cost Category Aggregate Entity.
 * Configurable, hierarchical, with system-defined flags for canonical protection.
 */
data class BusinessCostCategory(
    val id: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val code: String,
    val name: String,
    val description: String? = null,
    val parentCategoryId: String? = null,
    val isActive: Boolean = true,
    val isSystemDefined: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Operational Cost Allocation Status.
 */
enum class BusinessCostAllocationStatus {
    UNALLOCATED,
    PARTIALLY_ALLOCATED,
    FULLY_ALLOCATED,
    CLASSIFIED,
    RECLASSIFICATION_PENDING,
    RECLASSIFIED,
    CLOSED;

    fun canTransitionTo(target: BusinessCostAllocationStatus): Boolean {
        if (this == target) return true
        return when (this) {
            UNALLOCATED -> target in setOf(PARTIALLY_ALLOCATED, FULLY_ALLOCATED, CLASSIFIED, RECLASSIFIED, CLOSED)
            PARTIALLY_ALLOCATED -> target in setOf(FULLY_ALLOCATED, RECLASSIFICATION_PENDING, RECLASSIFIED, CLOSED)
            FULLY_ALLOCATED -> target in setOf(RECLASSIFICATION_PENDING, RECLASSIFIED, CLOSED)
            CLASSIFIED -> target in setOf(PARTIALLY_ALLOCATED, FULLY_ALLOCATED, RECLASSIFICATION_PENDING, RECLASSIFIED, CLOSED)
            RECLASSIFICATION_PENDING -> target in setOf(RECLASSIFIED, CLASSIFIED, FULLY_ALLOCATED, PARTIALLY_ALLOCATED)
            RECLASSIFIED -> target in setOf(PARTIALLY_ALLOCATED, FULLY_ALLOCATED, RECLASSIFICATION_PENDING, CLOSED)
            CLOSED -> false
        }
    }
}

/**
 * Operational Classification Status.
 */
enum class BusinessCostClassificationStatus {
    UNCLASSIFIED,
    CLASSIFIED,
    RECLASSIFIED,
    DISPUTED,
    VERIFIED
}

/**
 * Canonical Source reference type for operational cost tracking.
 */
enum class BusinessCostTrackingSourceType {
    BUSINESS_EXPENSE,
    VENDOR_PAYABLE,
    BUSINESS_LEDGER_POSTING,
    MANUAL_OPERATIONAL_REFERENCE
}

/**
 * Operational Cost Tracking Record.
 * References canonical financial sources without duplicating accounting balances.
 */
data class BusinessCostTracking(
    val id: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val sourceType: BusinessCostTrackingSourceType,
    val sourceId: String,
    val ledgerPostingId: String? = null,
    val costCenterId: String,
    val costCategoryId: String,
    val jobId: String? = null,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val allocationStatus: BusinessCostAllocationStatus = BusinessCostAllocationStatus.UNALLOCATED,
    val classificationStatus: BusinessCostClassificationStatus = BusinessCostClassificationStatus.CLASSIFIED,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "system",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "system",
    val version: Long = 1L
)

/**
 * Append-Only Audit Event for Cost Classification and Operational Reclassification.
 */
data class BusinessCostClassificationAuditEvent(
    val eventId: String,
    val tenantId: String = "TENANT-001",
    val projectId: String = "PRJ-001",
    val trackingId: String,
    val action: String,
    val actorId: String,
    val actorRole: String,
    val previousStateJson: String? = null,
    val newStateJson: String? = null,
    val reason: String,
    val correlationId: String? = null,
    val idempotencyKey: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Read-only Cost Center rollup projection.
 */
data class BusinessCostCenterSummary(
    val costCenterId: String,
    val code: String,
    val name: String,
    val parentCostCenterId: String?,
    val isActive: Boolean,
    val totalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val allocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unallocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val trackedItemCount: Int = 0,
    val currency: String = "BDT"
)

/**
 * Read-only Cost Category rollup projection.
 */
data class BusinessCostCategorySummary(
    val categoryId: String,
    val code: String,
    val name: String,
    val parentCategoryId: String?,
    val isActive: Boolean,
    val isSystemDefined: Boolean,
    val totalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val jobCount: Int = 0,
    val trackedItemCount: Int = 0,
    val currency: String = "BDT"
)

/**
 * Read-only Detailed Job Cost breakdown projection.
 */
data class BusinessJobCostDetailSummary(
    val jobId: String,
    val totalCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val productionCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val vendorCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val expenseCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val transportCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val labourCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val otherCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val allocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val unallocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val itemCount: Int = 0,
    val currency: String = "BDT",
    val items: List<BusinessCostTracking> = emptyList()
)

/**
 * Overall Operational Cost Tracking Summary.
 */
data class BusinessCostTrackingSummary(
    val totalTrackedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalAllocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalUnallocatedCost: BigDecimal = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
    val totalCostCenters: Int = 0,
    val totalActiveCategories: Int = 0,
    val jobsWithCostCount: Int = 0,
    val reclassificationPendingCount: Int = 0,
    val currency: String = "BDT"
)
