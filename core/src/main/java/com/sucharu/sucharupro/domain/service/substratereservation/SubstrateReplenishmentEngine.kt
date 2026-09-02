package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.printingcalculator.MeasurementUnit
import com.sucharu.sucharupro.domain.model.printingcalculator.PaperStockType
import com.sucharu.sucharupro.domain.model.printingcalculator.PrintingDimension
import com.sucharu.sucharupro.domain.model.substratereservation.*
import com.sucharu.sucharupro.domain.model.vendor.Vendor
import com.sucharu.sucharupro.domain.model.vendor.VendorStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest

/**
 * Mathematical Engine for Deterministic Substrate Auto-Replenishment & Supplier Ranking.
 * Module 19 Step 04.
 */
object SubstrateReplenishmentEngine {

    private const val DEFAULT_PACK_SIZE = 500

    data class EvaluationInput(
        val tenantId: String,
        val productId: String,
        val sku: String,
        val materialName: String,
        val stockType: PaperStockType,
        val gsm: BigDecimal,
        val sheetDimension: PrintingDimension,
        val warehouseId: String,
        val warehouseName: String,
        val onHandPhysicalSheets: Long,
        val activeReservedSheets: Long,
        val pendingInboundSheets: Long = 0L,
        val plannedDemandSheets: Long = 0L,
        val policy: SubstrateReplenishmentPolicy,
        val candidateVendors: List<Vendor> = emptyList(),
        val evaluator: String = "system"
    )

    /**
     * Executes deterministic replenishment evaluation.
     */
    fun evaluate(input: EvaluationInput): SubstrateReplenishmentEvaluation {
        val onHand = maxOf(0L, input.onHandPhysicalSheets)
        val reserved = maxOf(0L, input.activeReservedSheets)
        val inbound = maxOf(0L, input.pendingInboundSheets)
        val demand = maxOf(0L, input.plannedDemandSheets)
        val policy = input.policy

        // Available Stock = On Hand - Active Reservations
        val available = maxOf(0L, onHand - reserved)

        // Net Projected Availability = Available + Inbound - Planned Forward Demand
        val netProjected = available + inbound - demand

        // Evaluate Trigger State, Reason, and Urgency Priority
        val (triggerState, priority, reason, isReorderRequired) = determineTrigger(
            onHand = onHand,
            available = available,
            netProjected = netProjected,
            policy = policy
        )

        // Calculate Projected Shortfall and Recommended Reorder Quantity
        val (shortfall, recommendedSheets, recommendedReams) = if (isReorderRequired) {
            val target = policy.targetStockSheets
            val rawDeficit = maxOf(0L, target - netProjected)
            val withMoq = maxOf(rawDeficit, policy.minimumOrderQuantitySheets)
            val packSize = if (policy.standardPackReamSize > 0) policy.standardPackReamSize else DEFAULT_PACK_SIZE
            val roundedSheets = roundUpToPackSize(withMoq, packSize)
            val reams = BigDecimal(roundedSheets).divide(BigDecimal(packSize), 4, RoundingMode.HALF_UP)
            Triple(rawDeficit, roundedSheets, reams)
        } else {
            Triple(0L, 0L, BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
        }

        // Deterministically rank available Module 12 vendors
        val rankedSuppliers = rankSuppliers(input.candidateVendors, policy)
        val primaryVendor = rankedSuppliers.firstOrNull()

        // Generate SHA-256 Deduplication Fingerprint & Master Integrity Hash
        val fingerprint = computeFingerprint(
            tenantId = input.tenantId,
            sku = input.sku,
            warehouseId = input.warehouseId,
            policyVersion = policy.policyVersion,
            onHand = onHand,
            reserved = reserved,
            inbound = inbound,
            demand = demand,
            triggerState = triggerState,
            recommendedSheets = recommendedSheets
        )

        val masterHash = computeMasterIntegrityHash(
            fingerprint = fingerprint,
            evaluationId = "EVAL-${input.tenantId}-${input.sku}",
            primaryVendorId = primaryVendor?.vendorId ?: "NONE",
            timestamp = System.currentTimeMillis()
        )

        return SubstrateReplenishmentEvaluation(
            evaluationId = "EVAL-${java.util.UUID.randomUUID().toString().take(12).uppercase()}",
            tenantId = input.tenantId,
            productId = input.productId,
            sku = input.sku,
            materialName = input.materialName,
            stockType = input.stockType,
            gsm = input.gsm,
            sheetDimension = input.sheetDimension,
            warehouseId = input.warehouseId,
            warehouseName = input.warehouseName,
            onHandPhysicalSheets = onHand,
            activeReservedSheets = reserved,
            availableSheets = available,
            pendingInboundSheets = inbound,
            plannedDemandSheets = demand,
            netProjectedAvailabilitySheets = netProjected,
            safetyStockSheets = policy.safetyStockSheets,
            reorderPointSheets = policy.reorderPointSheets,
            targetStockSheets = policy.targetStockSheets,
            isReorderRequired = isReorderRequired,
            projectedShortfallSheets = shortfall,
            recommendedReorderSheets = recommendedSheets,
            recommendedReorderReams = recommendedReams,
            triggerState = triggerState,
            priority = priority,
            primaryReason = reason,
            policyId = policy.policyId,
            policyVersion = policy.policyVersion,
            recommendedSuppliers = rankedSuppliers,
            primaryVendorId = primaryVendor?.vendorId,
            primaryVendorName = primaryVendor?.vendorName,
            deduplicationFingerprint = fingerprint,
            masterIntegrityHash = masterHash,
            evaluatedBy = input.evaluator,
            evaluatedAt = System.currentTimeMillis()
        )
    }

    private fun determineTrigger(
        onHand: Long,
        available: Long,
        netProjected: Long,
        policy: SubstrateReplenishmentPolicy
    ): Quadruple<ReplenishmentTriggerState, ReplenishmentPriority, ReplenishmentReason, Boolean> {
        return when {
            // Case 1: Physical on-hand dropped below minimum plant emergency floor
            onHand < policy.minimumStockSheets -> {
                Quadruple(
                    ReplenishmentTriggerState.REORDER_TRIGGERED,
                    ReplenishmentPriority.CRITICAL,
                    ReplenishmentReason.MIN_STOCK_VIOLATION,
                    true
                )
            }
            // Case 2: Available/projected stock breached safety buffer
            netProjected < policy.safetyStockSheets -> {
                Quadruple(
                    ReplenishmentTriggerState.REORDER_TRIGGERED,
                    ReplenishmentPriority.HIGH,
                    ReplenishmentReason.SAFETY_STOCK_BREACH,
                    true
                )
            }
            // Case 3: Net projected stock fell below reorder threshold
            netProjected <= policy.reorderPointSheets -> {
                Quadruple(
                    ReplenishmentTriggerState.REORDER_TRIGGERED,
                    ReplenishmentPriority.NORMAL,
                    ReplenishmentReason.REORDER_POINT_REACHED,
                    true
                )
            }
            // Case 4: Stock approaching reorder point within a 15% buffer
            netProjected <= policy.reorderPointSheets + (policy.reorderPointSheets * 0.15).toLong() -> {
                Quadruple(
                    ReplenishmentTriggerState.WATCH,
                    ReplenishmentPriority.LOW,
                    ReplenishmentReason.REORDER_POINT_REACHED,
                    false
                )
            }
            // Case 5: Stock healthy
            else -> {
                Quadruple(
                    ReplenishmentTriggerState.NORMAL,
                    ReplenishmentPriority.LOW,
                    ReplenishmentReason.REORDER_POINT_REACHED,
                    false
                )
            }
        }
    }

    /**
     * Deterministically ranks candidate vendors from Module 12 authority.
     */
    fun rankSuppliers(
        vendors: List<Vendor>,
        policy: SubstrateReplenishmentPolicy
    ): List<SupplierReorderCandidate> {
        if (vendors.isEmpty()) return emptyList()

        return vendors
            .filter { it.status == VendorStatus.ACTIVE }
            .sortedWith(
                compareByDescending<Vendor> { it.status == VendorStatus.ACTIVE }
                    .thenBy { it.vendorName }
                    .thenBy { it.vendorCode }
            )
            .mapIndexed { index, vendor ->
                val rank = index + 1
                val baseScore = BigDecimal(100 - (index * 10)).max(BigDecimal("50.00"))
                SupplierReorderCandidate(
                    candidateId = "SRC-${vendor.vendorId}-${policy.policyId}",
                    vendorId = vendor.vendorId,
                    vendorCode = vendor.vendorCode,
                    vendorName = vendor.vendorName,
                    rank = rank,
                    suitabilityScore = baseScore,
                    estimatedLeadTimeDays = policy.leadTimeDays,
                    quotedCostPerSheet = BigDecimal("0.4500"),
                    minimumOrderQuantitySheets = policy.minimumOrderQuantitySheets,
                    standardPackSize = policy.standardPackReamSize,
                    primaryContactEmail = vendor.primaryEmail,
                    primaryContactPhone = vendor.primaryPhone,
                    isApprovedSupplier = true,
                    selectionRationale = "Rank #$rank active canonical supplier for tenant ${vendor.projectId} matching ${policy.sku}"
                )
            }
    }

    fun roundUpToPackSize(sheets: Long, packSize: Int): Long {
        if (packSize <= 0) return sheets
        val remainder = sheets % packSize
        return if (remainder == 0L) sheets else sheets + (packSize - remainder)
    }

    fun computeFingerprint(
        tenantId: String,
        sku: String,
        warehouseId: String,
        policyVersion: String,
        onHand: Long,
        reserved: Long,
        inbound: Long,
        demand: Long,
        triggerState: ReplenishmentTriggerState,
        recommendedSheets: Long
    ): String {
        val payload = "$tenantId|$sku|$warehouseId|$policyVersion|$onHand|$reserved|$inbound|$demand|${triggerState.name}|$recommendedSheets"
        return sha256(payload)
    }

    fun computeMasterIntegrityHash(
        fingerprint: String,
        evaluationId: String,
        primaryVendorId: String,
        timestamp: Long
    ): String {
        val payload = "$fingerprint|$evaluationId|$primaryVendorId|$timestamp"
        return sha256(payload)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
