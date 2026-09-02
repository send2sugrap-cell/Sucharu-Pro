package com.sucharu.sucharupro.domain.model.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal

/**
 * Substrate Reservation Lifecycle Status.
 * Module 19 Step 01 & Step 02.
 */
enum class SubstrateReservationStatus(val defaultLabel: String) {
    REQUESTED("Requirement Resolved / Requested"),
    RESERVED_SOFT("Soft Held for Quotation / Pre-Production"),
    ALLOCATED_HARD("Hard Allocated for Scheduled Job"),
    ISSUED_TO_FLOOR("Issued & Dispatched to Shop Floor"),
    CANCELLED("Reservation Cancelled & Stock Restored"),
    EXPIRED("Reservation Expired");

    val isTerminal: Boolean get() = this == ISSUED_TO_FLOOR || this == CANCELLED || this == EXPIRED
    val isActiveHold: Boolean get() = this == RESERVED_SOFT || this == ALLOCATED_HARD
}

/**
 * Explicit Reservation Commitment Mode.
 * Module 19 Step 02.
 */
enum class SubstrateReservationMode {
    SOFT,
    HARD
}

/**
 * SKU Matching Confidence / Resolution Classification.
 */
enum class SubstrateSkuMatchConfidence {
    EXACT_SKU_MATCH,
    SPECIFICATION_MATCH,
    COMPATIBLE_SUBSTITUTE,
    UNMATCHED_NO_SKU
}

/**
 * Resolved Substrate Material Requirement derived deterministically from upstream Module 17/18.
 */
data class SubstrateRequirement(
    val requirementId: String,
    val tenantId: String,
    val orderId: String,
    val orderItemId: String,
    val calculationId: String? = null,
    val stockType: PaperStockType,
    val requestedMaterialCode: String? = null,
    val requestedMaterialName: String,
    val gsm: BigDecimal,
    val sheetDimension: PrintingDimension,
    val productiveSheetsRequired: Long,
    val wasteSheetsRequired: Long,
    val totalSheetsRequired: Long,
    val totalReamsRequired: BigDecimal,
    val totalWeightKg: BigDecimal,
    val grainDirection: String = "LONG_GRAIN",
    val requiredByTimestamp: Long? = null,
    val resolvedAt: Long = System.currentTimeMillis()
)

/**
 * Outcome of matching a requirement against canonical Module 06 Inventory Product master.
 */
data class SubstrateSkuResolutionResult(
    val resolutionId: String,
    val tenantId: String,
    val requirement: SubstrateRequirement,
    val matchedProductId: String?,
    val matchedSku: String?,
    val matchedProductName: String?,
    val warehouseId: String?,
    val warehouseName: String?,
    val confidence: SubstrateSkuMatchConfidence,
    val onHandPhysicalSheets: Long,
    val currentlyReservedSheets: Long,
    val availableReservableSheets: Long,
    val isSufficientStockAvailable: Boolean,
    val missingDeficitSheets: Long = 0L,
    val diagnosticReason: String? = null
)

/**
 * Physical warehouse/batch source allocation for hard substrate reservations.
 * Module 19 Step 02.
 */
data class SubstrateAllocationSource(
    val allocationId: String,
    val reservationId: String,
    val tenantId: String,
    val warehouseId: String,
    val locationId: String? = null,
    val batchNumber: String? = null,
    val allocatedSheets: Long,
    val allocatedReams: BigDecimal,
    val allocatedWeightKg: BigDecimal,
    val allocatedAt: Long = System.currentTimeMillis(),
    val allocatedBy: String
)

/**
 * Authoritative Substrate Stock Reservation record.
 */
data class SubstrateReservation(
    val reservationId: String,
    val tenantId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String? = null,
    val workOrderId: String? = null,
    val productId: String,
    val sku: String,
    val productName: String,
    val warehouseId: String,
    val locationId: String? = null,
    val stockType: PaperStockType,
    val gsm: BigDecimal,
    val sheetDimension: PrintingDimension,
    val reservedSheets: Long,
    val reservedReams: BigDecimal,
    val reservedWeightKg: BigDecimal,
    val status: SubstrateReservationStatus = SubstrateReservationStatus.RESERVED_SOFT,
    val mode: SubstrateReservationMode = SubstrateReservationMode.SOFT,
    val idempotencyKey: String,
    val expiryTimestamp: Long? = null,
    val softHoldExpiresAt: Long? = null,
    val promotedAt: Long? = null,
    val promotedBy: String? = null,
    val reservedBy: String,
    val reservedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = reservedAt,
    val notes: String? = null,
    val allocationSources: List<SubstrateAllocationSource> = emptyList()
)

/**
 * Immutable Audit Event for Substrate Reservation Mutations.
 */
data class SubstrateReservationAuditEvent(
    val eventId: String,
    val reservationId: String,
    val tenantId: String,
    val previousStatus: SubstrateReservationStatus?,
    val newStatus: SubstrateReservationStatus,
    val quantityChangeSheets: Long,
    val actor: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AI & Cross-Module Governance Handoff Contract for Module 19 Step 01.
 */
data class Module19Step01SubstrateReservationHandoffContract(
    val contractVersion: String = "1.0.0",
    val tenantId: String,
    val reservationId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String?,
    val sku: String,
    val productName: String,
    val gsm: BigDecimal,
    val sheetWidthMm: BigDecimal,
    val sheetHeightMm: BigDecimal,
    val reservedSheets: Long,
    val reservedReams: BigDecimal,
    val reservedWeightKg: BigDecimal,
    val status: String,
    val isHardAllocated: Boolean,
    val isStockInterlocked: Boolean,
    val reservedBy: String,
    val timestamp: Long
)

/**
 * AI & Cross-Module Governance Handoff Contract for Module 19 Step 02.
 */
data class Module19Step02SubstrateReservationHandoffContract(
    val contractVersion: String = "2.0.0",
    val tenantId: String,
    val reservationId: String,
    val orderId: String,
    val orderItemId: String,
    val executionJobId: String?,
    val sku: String,
    val productName: String,
    val gsm: BigDecimal,
    val sheetWidthMm: BigDecimal,
    val sheetHeightMm: BigDecimal,
    val reservedSheets: Long,
    val reservedReams: BigDecimal,
    val reservedWeightKg: BigDecimal,
    val mode: String,
    val status: String,
    val isHardAllocated: Boolean,
    val softHoldExpiresAt: Long?,
    val promotedAt: Long?,
    val promotedBy: String?,
    val allocationSourcesCount: Int,
    val reservedBy: String,
    val timestamp: Long
)
