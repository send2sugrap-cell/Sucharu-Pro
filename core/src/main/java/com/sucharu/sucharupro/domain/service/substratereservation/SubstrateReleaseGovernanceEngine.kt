package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Mathematical & State Evaluation Engine for Substrate Release & Revision Governance.
 * Module 19 Step 05.
 *
 * Deterministic with ZERO side effects.
 */
object SubstrateReleaseGovernanceEngine {

    data class EvaluationInput(
        val tenantId: String,
        val reservationId: String,
        val orderId: String,
        val orderItemId: String,
        val executionJobId: String? = null,
        val triggerType: GovernanceTriggerType,
        val upstreamEventId: String? = null,
        val sku: String,
        val materialName: String,
        val warehouseId: String,
        val previousRequiredSheets: Long,
        val newRequiredSheets: Long,
        val allocatedSheets: Long,
        val consumedSheets: Long = 0L,
        val committedSheets: Long = 0L,
        val productionStatus: String? = null,
        val isSkuChanged: Boolean = false,
        val evaluator: String = "SYSTEM_GOVERNANCE"
    )

    fun evaluate(input: EvaluationInput): SubstrateReleaseGovernanceRecord {
        require(input.tenantId.isNotBlank()) { "Tenant ID cannot be blank." }
        require(input.reservationId.isNotBlank()) { "Reservation ID cannot be blank." }
        require(input.orderId.isNotBlank()) { "Order ID cannot be blank." }
        require(input.allocatedSheets >= 0L) { "Allocated sheets cannot be negative." }
        require(input.consumedSheets >= 0L) { "Consumed sheets cannot be negative." }
        require(input.committedSheets >= 0L) { "Committed sheets cannot be negative." }
        require(input.previousRequiredSheets >= 0L) { "Previous required sheets cannot be negative." }
        require(input.newRequiredSheets >= 0L) { "New required sheets cannot be negative." }
        require(input.consumedSheets <= input.allocatedSheets) {
            "Consumed sheets (${input.consumedSheets}) cannot exceed allocated sheets (${input.allocatedSheets})."
        }

        val prodStatusNormalized = input.productionStatus?.trim()?.uppercase()

        // 1. Check for Active Production Lockout (Module 17 Authority)
        val isProductionActive = prodStatusNormalized in setOf(
            "IN_PROGRESS", "ON_HOLD", "QC_PENDING", "COMPLETING", "REWORK_REQUIRED", "BLOCKED"
        )
        val isProductionCompleted = prodStatusNormalized in setOf("COMPLETED", "DELIVERED")
        val isProductionAmbiguous = prodStatusNormalized != null && (
            prodStatusNormalized in setOf("AMBIGUOUS", "UNKNOWN") ||
            prodStatusNormalized !in setOf(
                "DRAFT", "READY", "PLANNED", "SCHEDULED", "PENDING", "CANCELLED", "RELEASED",
                "IN_PROGRESS", "ON_HOLD", "QC_PENDING", "COMPLETING", "REWORK_REQUIRED", "BLOCKED",
                "COMPLETED", "DELIVERED"
            )
        )

        var decision: ReleaseGovernanceDecision
        var blockingReason: ReleaseBlockingReason
        var releasableSheets: Long
        var retainedSheets: Long
        var additionalRequiredSheets: Long
        var explanation: String

        if (isProductionActive) {
            decision = ReleaseGovernanceDecision.RELEASE_BLOCKED
            blockingReason = ReleaseBlockingReason.PRODUCTION_IN_PROGRESS
            releasableSheets = 0L
            retainedSheets = input.allocatedSheets
            additionalRequiredSheets = max(0L, input.newRequiredSheets - input.allocatedSheets)
            explanation = "Material release strictly blocked because production job is currently in status $prodStatusNormalized."
        } else if (isProductionCompleted) {
            decision = ReleaseGovernanceDecision.NO_RELEASE_REQUIRED
            blockingReason = ReleaseBlockingReason.JOB_COMPLETED
            releasableSheets = 0L
            retainedSheets = input.allocatedSheets
            additionalRequiredSheets = 0L
            explanation = "Job execution complete. All reserved substrate is accounted for or physically consumed."
        } else if (isProductionAmbiguous) {
            decision = ReleaseGovernanceDecision.REQUIRES_REVIEW
            blockingReason = ReleaseBlockingReason.AMBIGUOUS_PRODUCTION_STATE
            releasableSheets = 0L
            retainedSheets = input.allocatedSheets
            additionalRequiredSheets = 0L
            explanation = "Production lifecycle state is ambiguous ($prodStatusNormalized). Material release held for manual supervisor review."
        } else {
            // Production is DRAFT, READY, SCHEDULED, CANCELLED, or unassigned
            if (input.consumedSheets >= input.allocatedSheets && input.allocatedSheets > 0L) {
                decision = ReleaseGovernanceDecision.NO_RELEASE_REQUIRED
                blockingReason = ReleaseBlockingReason.MATERIAL_ALREADY_CONSUMED
                releasableSheets = 0L
                retainedSheets = input.allocatedSheets
                additionalRequiredSheets = max(0L, input.newRequiredSheets - input.allocatedSheets)
                explanation = "All allocated substrate (${input.allocatedSheets} sheets) has already been consumed."
            } else {
                val remainingReserved = max(0L, input.allocatedSheets - input.consumedSheets)
                val uncommittedRemainder = max(0L, remainingReserved - input.committedSheets)

                when (input.triggerType) {
                    GovernanceTriggerType.JOB_CANCELLATION,
                    GovernanceTriggerType.ORDER_CANCELLATION -> {
                        releasableSheets = uncommittedRemainder
                        retainedSheets = input.allocatedSheets - releasableSheets
                        additionalRequiredSheets = 0L

                        if (releasableSheets == input.allocatedSheets) {
                            decision = ReleaseGovernanceDecision.RELEASE_ELIGIBLE
                            blockingReason = ReleaseBlockingReason.NONE
                            explanation = "Full reservation (${input.allocatedSheets} sheets) eligible for release and inventory restoration."
                        } else if (releasableSheets > 0L) {
                            decision = ReleaseGovernanceDecision.PARTIAL_RELEASE_ELIGIBLE
                            blockingReason = ReleaseBlockingReason.NONE
                            explanation = "Partial release eligible ($releasableSheets sheets). Retained ${input.consumedSheets} consumed and ${input.committedSheets} committed sheets."
                        } else {
                            decision = ReleaseGovernanceDecision.NO_RELEASE_REQUIRED
                            blockingReason = if (input.committedSheets > 0L) {
                                ReleaseBlockingReason.MATERIAL_COMMITTED_TO_FLOOR
                            } else {
                                ReleaseBlockingReason.MATERIAL_ALREADY_CONSUMED
                            }
                            explanation = "No substrate releasable. All allocated material is consumed or committed."
                        }
                    }

                    GovernanceTriggerType.QUANTITY_REDUCTION -> {
                        val potentialReduction = max(0L, input.allocatedSheets - input.newRequiredSheets)
                        releasableSheets = min(uncommittedRemainder, potentialReduction)
                        retainedSheets = input.allocatedSheets - releasableSheets
                        additionalRequiredSheets = 0L

                        if (releasableSheets > 0L) {
                            decision = ReleaseGovernanceDecision.PARTIAL_RELEASE_ELIGIBLE
                            blockingReason = if (releasableSheets < potentialReduction) {
                                ReleaseBlockingReason.MATERIAL_COMMITTED_TO_FLOOR
                            } else {
                                ReleaseBlockingReason.NONE
                            }
                            explanation = "Quantity reduced from ${input.previousRequiredSheets} to ${input.newRequiredSheets}. $releasableSheets sheets eligible for release."
                        } else {
                            decision = ReleaseGovernanceDecision.NO_RELEASE_REQUIRED
                            blockingReason = if (potentialReduction > 0L) ReleaseBlockingReason.MATERIAL_COMMITTED_TO_FLOOR else ReleaseBlockingReason.NONE
                            explanation = "Quantity reduced, but existing allocation is locked by floor commitments. 0 sheets releasable."
                        }
                    }

                    GovernanceTriggerType.QUANTITY_INCREASE -> {
                        releasableSheets = 0L
                        retainedSheets = input.allocatedSheets
                        additionalRequiredSheets = max(0L, input.newRequiredSheets - input.allocatedSheets)
                        decision = ReleaseGovernanceDecision.NO_RELEASE_REQUIRED
                        blockingReason = ReleaseBlockingReason.NONE
                        explanation = "Quantity increased from ${input.previousRequiredSheets} to ${input.newRequiredSheets}. Additional $additionalRequiredSheets sheets required."
                    }

                    GovernanceTriggerType.SPECIFICATION_REVISION -> {
                        if (input.isSkuChanged) {
                            releasableSheets = uncommittedRemainder
                            retainedSheets = input.allocatedSheets - releasableSheets
                            additionalRequiredSheets = input.newRequiredSheets
                            decision = if (releasableSheets > 0L) ReleaseGovernanceDecision.RELEASE_ELIGIBLE else ReleaseGovernanceDecision.NO_RELEASE_REQUIRED
                            blockingReason = if (input.consumedSheets > 0L) ReleaseBlockingReason.MATERIAL_ALREADY_CONSUMED else ReleaseBlockingReason.NONE
                            explanation = "Substrate SKU changed. Existing allocation ($releasableSheets sheets) released; new requirement ($additionalRequiredSheets sheets) queued for resolution."
                        } else {
                            // Dimensions or specs changed within same substrate
                            val potentialDelta = input.newRequiredSheets - input.allocatedSheets
                            if (potentialDelta > 0L) {
                                releasableSheets = 0L
                                retainedSheets = input.allocatedSheets
                                additionalRequiredSheets = potentialDelta
                                decision = ReleaseGovernanceDecision.NO_RELEASE_REQUIRED
                                blockingReason = ReleaseBlockingReason.NONE
                                explanation = "Spec revision increased sheet count. $additionalRequiredSheets additional sheets required."
                            } else {
                                val potentialRelease = max(0L, -potentialDelta)
                                releasableSheets = min(uncommittedRemainder, potentialRelease)
                                retainedSheets = input.allocatedSheets - releasableSheets
                                additionalRequiredSheets = 0L
                                decision = if (releasableSheets > 0L) ReleaseGovernanceDecision.PARTIAL_RELEASE_ELIGIBLE else ReleaseGovernanceDecision.NO_RELEASE_REQUIRED
                                blockingReason = ReleaseBlockingReason.NONE
                                explanation = "Spec revision reduced sheet count. $releasableSheets sheets eligible for release."
                            }
                        }
                    }

                    GovernanceTriggerType.SCHEDULE_CHANGE,
                    GovernanceTriggerType.MANUAL_REVIEW -> {
                        releasableSheets = 0L
                        retainedSheets = input.allocatedSheets
                        additionalRequiredSheets = 0L
                        decision = ReleaseGovernanceDecision.REQUIRES_REVIEW
                        blockingReason = ReleaseBlockingReason.AMBIGUOUS_PRODUCTION_STATE
                        explanation = "Schedule change or manual review triggered. Allocation retained pending supervisor confirmation."
                    }
                }
            }
        }

        val fingerprint = computeFingerprint(
            tenantId = input.tenantId,
            reservationId = input.reservationId,
            orderId = input.orderId,
            triggerType = input.triggerType,
            prevSheets = input.previousRequiredSheets,
            newSheets = input.newRequiredSheets,
            allocatedSheets = input.allocatedSheets,
            consumedSheets = input.consumedSheets,
            committedSheets = input.committedSheets,
            releasableSheets = releasableSheets,
            decision = decision
        )

        val masterHash = computeMasterIntegrityHash(
            tenantId = input.tenantId,
            reservationId = input.reservationId,
            fingerprint = fingerprint,
            releasableSheets = releasableSheets,
            retainedSheets = retainedSheets,
            additionalSheets = additionalRequiredSheets,
            decision = decision,
            blockingReason = blockingReason
        )

        return SubstrateReleaseGovernanceRecord(
            governanceId = "GOV-${UUID.randomUUID().toString().take(12).uppercase()}",
            tenantId = input.tenantId,
            reservationId = input.reservationId,
            orderId = input.orderId,
            orderItemId = input.orderItemId,
            executionJobId = input.executionJobId,
            triggerType = input.triggerType,
            upstreamEventId = input.upstreamEventId,
            sku = input.sku,
            materialName = input.materialName,
            warehouseId = input.warehouseId,
            previousRequiredSheets = input.previousRequiredSheets,
            newRequiredSheets = input.newRequiredSheets,
            allocatedSheets = input.allocatedSheets,
            consumedSheets = input.consumedSheets,
            committedSheets = input.committedSheets,
            releasableSheets = releasableSheets,
            retainedSheets = retainedSheets,
            additionalRequiredSheets = additionalRequiredSheets,
            decision = decision,
            executionStatus = GovernanceExecutionStatus.EVALUATED,
            blockingReason = blockingReason,
            explanation = explanation,
            deduplicationFingerprint = fingerprint,
            masterIntegrityHash = masterHash,
            evaluatedBy = input.evaluator,
            evaluatedAt = System.currentTimeMillis()
        )
    }

    fun computeFingerprint(
        tenantId: String,
        reservationId: String,
        orderId: String,
        triggerType: GovernanceTriggerType,
        prevSheets: Long,
        newSheets: Long,
        allocatedSheets: Long,
        consumedSheets: Long,
        committedSheets: Long,
        releasableSheets: Long,
        decision: ReleaseGovernanceDecision
    ): String {
        val raw = "$tenantId|$reservationId|$orderId|${triggerType.name}|$prevSheets|$newSheets|$allocatedSheets|$consumedSheets|$committedSheets|$releasableSheets|${decision.name}"
        return sha256(raw)
    }

    fun computeMasterIntegrityHash(
        tenantId: String,
        reservationId: String,
        fingerprint: String,
        releasableSheets: Long,
        retainedSheets: Long,
        additionalSheets: Long,
        decision: ReleaseGovernanceDecision,
        blockingReason: ReleaseBlockingReason
    ): String {
        val raw = "$tenantId|$reservationId|$fingerprint|$releasableSheets|$retainedSheets|$additionalSheets|${decision.name}|${blockingReason.name}"
        return sha256(raw)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
