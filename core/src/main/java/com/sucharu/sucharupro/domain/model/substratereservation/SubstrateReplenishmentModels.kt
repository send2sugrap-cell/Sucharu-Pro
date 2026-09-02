package com.sucharu.sucharupro.domain.model.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import java.math.BigDecimal

/**
 * Replenishment Policy Category.
 * Module 19 Step 04.
 */
enum class ReplenishmentPolicyType(val label: String) {
    MIN_STOCK("Strict Minimum Stock Buffer"),
    REORDER_POINT("Statistical Reorder Point"),
    SAFETY_STOCK("Safety Stock Breach Trigger"),
    TARGET_STOCK("Target Periodic Stock Level"),
    FIXED_REORDER_QTY("Fixed Lot Size Reorder"),
    DEMAND_AWARE("Demand-Aware Forward Reservation Projection"),
    LEAD_TIME_AWARE("Lead-Time & Supplier Reliability Aware")
}

/**
 * Replenishment Lifecycle & Evaluation Trigger State.
 */
enum class ReplenishmentTriggerState(val label: String) {
    NORMAL("Stock Level Normal & Healthy"),
    WATCH("Stock Approaching Reorder Boundary"),
    REORDER_TRIGGERED("Reorder Threshold Breached • Trigger Active"),
    SUPPLIER_ALERT_PENDING("Supplier Reorder Alert Drafted"),
    SUPPLIER_ALERT_SENT("Supplier Reorder Alert Dispatched"),
    PROCUREMENT_PENDING("Purchase Requisition / PO in Progress"),
    COVERED("Shortfall Covered by Inbound Influx"),
    BLOCKED("Replenishment Blocked by Policy / Quality Hold"),
    CANCELLED("Replenishment Action Dismissed / Cancelled");

    val isAlertable: Boolean get() = this == REORDER_TRIGGERED || this == SUPPLIER_ALERT_PENDING
    val isPendingAction: Boolean get() = this == REORDER_TRIGGERED || this == SUPPLIER_ALERT_PENDING || this == SUPPLIER_ALERT_SENT || this == PROCUREMENT_PENDING
}

/**
 * Replenishment Urgency Priority.
 */
enum class ReplenishmentPriority(val label: String, val weight: Int) {
    LOW("Routine Replenishment Buffer", 10),
    NORMAL("Standard Reorder Cycle", 30),
    HIGH("Safety Stock Violated • Expedite", 60),
    CRITICAL("Stockout Imminent • Active Order Blocked", 100)
}

/**
 * Explicit Root Cause / Justification for Replenishment Trigger.
 */
enum class ReplenishmentReason(val label: String) {
    SAFETY_STOCK_BREACH("On-Hand Balance Breached Safety Stock Level"),
    REORDER_POINT_REACHED("Net Projected Stock Dropped Below Reorder Point"),
    UNCOMMITTED_DEMAND_SURGE("Surge in Forward Imposition / Order Reservations"),
    MIN_STOCK_VIOLATION("Physical Usable Reams Violate Minimum Plant Policy"),
    LEAD_TIME_BUFFER_EXHAUSTED("Lead Time Threshold Requires Immediate Supplier Order"),
    MANUAL_OVERRIDE("Manual Planner Inventory Reorder Override")
}

/**
 * Authoritative Policy Specification for Substrate Auto-Replenishment.
 */
data class SubstrateReplenishmentPolicy(
    val policyId: String,
    val tenantId: String,
    val sku: String,
    val policyType: ReplenishmentPolicyType = ReplenishmentPolicyType.DEMAND_AWARE,
    val minimumStockSheets: Long = 2000L,
    val safetyStockSheets: Long = 4000L,
    val reorderPointSheets: Long = 10000L,
    val targetStockSheets: Long = 30000L,
    val fixedReorderQuantitySheets: Long = 10000L,
    val minimumOrderQuantitySheets: Long = 5000L,
    val standardPackReamSize: Int = 500,
    val leadTimeDays: Int = 5,
    val bufferDays: Int = 2,
    val policyVersion: String = "1.0.0",
    val isActive: Boolean = true
)

/**
 * Recommended Supplier Candidate ranked deterministically from Module 12 Vendor Master.
 */
data class SupplierReorderCandidate(
    val candidateId: String,
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val rank: Int,
    val suitabilityScore: BigDecimal,
    val estimatedLeadTimeDays: Int,
    val quotedCostPerSheet: BigDecimal,
    val minimumOrderQuantitySheets: Long,
    val standardPackSize: Int = 500,
    val primaryContactEmail: String? = null,
    val primaryContactPhone: String? = null,
    val isApprovedSupplier: Boolean = true,
    val selectionRationale: String
)

/**
 * Primary Aggregate Record for a Substrate Replenishment Evaluation.
 */
data class SubstrateReplenishmentEvaluation(
    val evaluationId: String,
    val tenantId: String,
    val productId: String,
    val sku: String,
    val materialName: String,
    val stockType: PaperStockType,
    val gsm: BigDecimal,
    val sheetDimension: PrintingDimension,
    val warehouseId: String,
    val warehouseName: String,
    // Real-time canonical inventory balance snapshots
    val onHandPhysicalSheets: Long,
    val activeReservedSheets: Long,
    val availableSheets: Long,
    val pendingInboundSheets: Long = 0L,
    val plannedDemandSheets: Long = 0L,
    val netProjectedAvailabilitySheets: Long,
    // Thresholds from policy
    val safetyStockSheets: Long,
    val reorderPointSheets: Long,
    val targetStockSheets: Long,
    val isReorderRequired: Boolean,
    val projectedShortfallSheets: Long,
    val recommendedReorderSheets: Long,
    val recommendedReorderReams: BigDecimal,
    val triggerState: ReplenishmentTriggerState,
    val priority: ReplenishmentPriority,
    val primaryReason: ReplenishmentReason,
    val policyId: String,
    val policyVersion: String,
    // Deterministically ranked suppliers from Module 12
    val recommendedSuppliers: List<SupplierReorderCandidate> = emptyList(),
    val primaryVendorId: String? = null,
    val primaryVendorName: String? = null,
    // Cryptographic audit & deduplication
    val deduplicationFingerprint: String,
    val masterIntegrityHash: String,
    val evaluatedBy: String,
    val evaluatedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)

/**
 * Supplier Reorder Alert dispatched to Module 12/13 Procurement Workflow.
 */
data class SupplierReorderAlert(
    val alertId: String,
    val evaluationId: String,
    val tenantId: String,
    val vendorId: String,
    val vendorCode: String,
    val vendorName: String,
    val sku: String,
    val materialName: String,
    val requestedSheets: Long,
    val requestedReams: BigDecimal,
    val targetDeliveryTimestamp: Long? = null,
    val priority: ReplenishmentPriority,
    val status: ReplenishmentTriggerState = ReplenishmentTriggerState.SUPPLIER_ALERT_SENT,
    val alertPayloadJson: String? = null,
    val dispatchedBy: String,
    val dispatchedAt: Long = System.currentTimeMillis(),
    val acknowledgedAt: Long? = null,
    val purchaseRequisitionId: String? = null
)

/**
 * Immutable Audit Event for Replenishment Lifecycle.
 */
data class SubstrateReplenishmentAuditEvent(
    val auditId: String,
    val evaluationId: String,
    val tenantId: String,
    val previousState: ReplenishmentTriggerState,
    val newState: ReplenishmentTriggerState,
    val triggerAction: String,
    val actor: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String? = null
)

/**
 * Downstream Machine-Readable AI Handoff Contract (Version 4.0.0).
 */
data class Module19Step04ReplenishmentHandoffContract(
    val contractVersion: String = "4.0.0",
    val evaluationId: String,
    val tenantId: String,
    val sku: String,
    val materialName: String,
    val warehouseId: String,
    val onHandPhysicalSheets: Long,
    val activeReservedSheets: Long,
    val availableSheets: Long,
    val netProjectedAvailabilitySheets: Long,
    val safetyStockSheets: Long,
    val reorderPointSheets: Long,
    val isReorderRequired: Boolean,
    val projectedShortfallSheets: Long,
    val recommendedReorderSheets: Long,
    val recommendedReorderReams: BigDecimal,
    val triggerState: ReplenishmentTriggerState,
    val priority: ReplenishmentPriority,
    val primaryReason: ReplenishmentReason,
    val preferredVendorId: String?,
    val preferredVendorName: String?,
    val estimatedLeadTimeDays: Int?,
    val deduplicationFingerprint: String,
    val masterIntegrityHash: String,
    val generatedAt: Long = System.currentTimeMillis(),
    val auditSummary: String
)
