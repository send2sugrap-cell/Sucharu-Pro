package com.sucharu.sucharupro.domain.model.imposition

import com.sucharu.sucharupro.domain.model.printingcalculator.ColorMode
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingSideOption
import java.math.BigDecimal

/**
 * Lifecycle status of a Gang-Run Specification.
 * Module 18 Step 02.
 */
enum class GangRunStatus {
    DRAFT,
    OPTIMIZED,
    COMMITTED_TO_PRESS,
    SUPERSEDED,
    CANCELLED
}

/**
 * Clustering policy strategy defining criteria strictness.
 */
enum class GangRunClusteringPolicy(val displayName: String) {
    STRICT_IDENTICAL_SUBSTRATE("Strict Match: Identical Paper Stock, GSM, and Colors"),
    RELAXED_GSM_TOLERANCE("Relaxed: Paper Stock Match, GSM within +/- 10 GSM"),
    MAXIMIZE_SHEET_YIELD("Yield First: Co-locate all compatible jobs up to max slots")
}

/**
 * Candidate job submitted for potential gang-run batching.
 */
data class GangRunCandidateItem(
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val finishedDimension: PrintingDimension,
    val requiredQuantity: Long,
    val paperStockType: PaperStockType,
    val gsm: BigDecimal,
    val colorMode: ColorMode = ColorMode.CMYK_FOUR_COLOR,
    val printingSideOption: PrintingSideOption = PrintingSideOption.SINGLE_SIDED,
    val targetDueDateEpochMs: Long? = null,
    val allowRotation: Boolean = true
) {
    init {
        require(jobId.isNotBlank()) { "Job ID must not be blank." }
        require(orderId.isNotBlank()) { "Order ID must not be blank." }
        require(orderItemId.isNotBlank()) { "Order Item ID must not be blank." }
        require(requiredQuantity > 0L) { "Required quantity must be positive: $requiredQuantity" }
        require(gsm > BigDecimal.ZERO) { "GSM must be strictly positive: $gsm" }
    }
}

/**
 * Homogeneous cluster of compatible candidate jobs.
 */
data class GangRunCluster(
    val clusterId: String,
    val paperStockType: PaperStockType,
    val representativeGsm: BigDecimal,
    val colorMode: ColorMode,
    val printingSideOption: PrintingSideOption,
    val candidateItems: List<GangRunCandidateItem>
)

/**
 * UP-slot allocation of a single job within a shared gang-run form.
 */
data class GangRunItemAllocation(
    val jobId: String,
    val orderId: String,
    val orderItemId: String,
    val productName: String,
    val assignedSlots: Int,
    val orientation: ImpositionLayoutOrientation,
    val slotItemWidthMm: BigDecimal,
    val slotItemHeightMm: BigDecimal,
    val requiredQuantity: Long,
    val producedQuantity: Long,
    val overageQuantity: Long,
    val itemOccupiedAreaMm2: BigDecimal,
    val relativeYieldPercentage: BigDecimal
) {
    init {
        require(assignedSlots > 0) { "Assigned slots must be positive: $assignedSlots" }
        require(producedQuantity >= requiredQuantity) {
            "Produced quantity ($producedQuantity) cannot be less than required quantity ($requiredQuantity)"
        }
    }
}

/**
 * Authoritative Gang-Run Batch Specification Aggregate Root.
 * Module 18 Step 02.
 */
data class GangRunSpecification(
    val gangRunId: String,
    val tenantId: String,
    val batchName: String,
    val paperStockType: PaperStockType,
    val gsm: BigDecimal,
    val colorMode: ColorMode,
    val printingSideOption: PrintingSideOption,
    val parentSheetDimension: PrintingDimension,
    val marginSpec: ImpositionMarginSpec,
    val spacingSpec: ImpositionSpacingSpec,
    val totalAvailableSlots: Int,
    val allocatedSlotsCount: Int,
    val commonRequiredSheets: Long,
    val totalProducedItems: Long,
    val totalOverageItems: Long,
    val usableAreaMm2: BigDecimal,
    val occupiedAreaMm2: BigDecimal,
    val wasteAreaMm2: BigDecimal,
    val sheetYieldPercentage: BigDecimal,
    val allocations: List<GangRunItemAllocation>,
    val version: Int = 1,
    val status: GangRunStatus = GangRunStatus.OPTIMIZED,
    val integrityHash: String,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String
)

/**
 * Cryptographically sealed handoff contract emitted to Module 19 (Substrate Stock Auto-Reservation).
 * Module 18 Step 02.
 */
data class Module18Step02GangRunHandoffContract(
    val contractVersion: String = "1.0.0",
    val gangRunId: String,
    val tenantId: String,
    val paperStockType: String,
    val gsm: BigDecimal,
    val parentSheetWidthMm: BigDecimal,
    val parentSheetHeightMm: BigDecimal,
    val totalParentSheetsRequired: Long,
    val totalAllocatedJobs: Int,
    val jobIds: List<String>,
    val orderItemIds: List<String>,
    val totalProducedItems: Long,
    val totalOverageItems: Long,
    val sheetYieldPercentage: BigDecimal,
    val integrityHash: String,
    val generatedAt: Long = System.currentTimeMillis()
)
