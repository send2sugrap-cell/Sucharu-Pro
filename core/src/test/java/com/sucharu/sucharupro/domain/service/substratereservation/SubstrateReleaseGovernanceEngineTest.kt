package com.sucharu.sucharupro.domain.service.substratereservation

import com.sucharu.sucharupro.domain.model.substratereservation.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Pure mathematical and rule verification test suite for SubstrateReleaseGovernanceEngine.
 * Module 19 Step 05.
 */
class SubstrateReleaseGovernanceEngineTest {

    private fun createInput(
        triggerType: GovernanceTriggerType = GovernanceTriggerType.JOB_CANCELLATION,
        allocatedSheets: Long = 10000L,
        consumedSheets: Long = 0L,
        committedSheets: Long = 0L,
        previousRequiredSheets: Long = 10000L,
        newRequiredSheets: Long = 0L,
        productionStatus: String? = null,
        isSkuChanged: Boolean = false
    ): SubstrateReleaseGovernanceEngine.EvaluationInput {
        return SubstrateReleaseGovernanceEngine.EvaluationInput(
            tenantId = "TENANT-001",
            reservationId = "RES-ART300-01",
            orderId = "ORD-2026-9041",
            orderItemId = "ITEM-01",
            executionJobId = "JOB-1122",
            triggerType = triggerType,
            upstreamEventId = "EVT-UPSTREAM-01",
            sku = "ART-300-25X36",
            materialName = "Art Card 300 GSM (25x36)",
            warehouseId = "WH-CENTRAL-01",
            previousRequiredSheets = previousRequiredSheets,
            newRequiredSheets = newRequiredSheets,
            allocatedSheets = allocatedSheets,
            consumedSheets = consumedSheets,
            committedSheets = committedSheets,
            productionStatus = productionStatus,
            isSkuChanged = isSkuChanged,
            evaluator = "planner_tester"
        )
    }

    @Test
    fun testJobCancellation_zeroConsumption_eligibleForFullRelease() {
        val input = createInput(
            allocatedSheets = 10000L,
            consumedSheets = 0L,
            committedSheets = 0L,
            productionStatus = "READY"
        )

        val result = SubstrateReleaseGovernanceEngine.evaluate(input)

        assertEquals(ReleaseGovernanceDecision.RELEASE_ELIGIBLE, result.decision)
        assertEquals(GovernanceExecutionStatus.EVALUATED, result.executionStatus)
        assertEquals(ReleaseBlockingReason.NONE, result.blockingReason)
        assertEquals(10000L, result.releasableSheets)
        assertEquals(0L, result.retainedSheets)
        assertEquals(0L, result.additionalRequiredSheets)
        assertTrue(result.masterIntegrityHash.isNotBlank())
        assertTrue(result.deduplicationFingerprint.isNotBlank())
    }

    @Test
    fun testJobCancellation_partialConsumptionAndCommitment_eligibleForPartialRelease() {
        val input = createInput(
            allocatedSheets = 10000L,
            consumedSheets = 2000L,
            committedSheets = 1000L,
            productionStatus = "READY"
        )

        val result = SubstrateReleaseGovernanceEngine.evaluate(input)

        assertEquals(ReleaseGovernanceDecision.PARTIAL_RELEASE_ELIGIBLE, result.decision)
        assertEquals(ReleaseBlockingReason.NONE, result.blockingReason)
        assertEquals(7000L, result.releasableSheets) // 10000 - 2000 - 1000 = 7000
        assertEquals(3000L, result.retainedSheets)   // 2000 consumed + 1000 committed
        assertEquals(0L, result.additionalRequiredSheets)
    }

    @Test
    fun testJobCancellation_fullyConsumed_noReleaseRequired() {
        val input = createInput(
            allocatedSheets = 10000L,
            consumedSheets = 10000L,
            committedSheets = 0L,
            productionStatus = "READY"
        )

        val result = SubstrateReleaseGovernanceEngine.evaluate(input)

        assertEquals(ReleaseGovernanceDecision.NO_RELEASE_REQUIRED, result.decision)
        assertEquals(0L, result.releasableSheets)
        assertEquals(10000L, result.retainedSheets)
    }

    @Test
    fun testActiveProductionLockout_blocksRelease() {
        val blockedStatuses = listOf("IN_PROGRESS", "ON_HOLD", "QC_PENDING", "COMPLETING", "REWORK_REQUIRED", "BLOCKED")

        for (status in blockedStatuses) {
            val input = createInput(
                allocatedSheets = 10000L,
                consumedSheets = 1000L,
                committedSheets = 0L,
                productionStatus = status
            )

            val result = SubstrateReleaseGovernanceEngine.evaluate(input)

            assertEquals("Status $status must block release", ReleaseGovernanceDecision.RELEASE_BLOCKED, result.decision)
            assertEquals("Status $status must set PRODUCTION_IN_PROGRESS", ReleaseBlockingReason.PRODUCTION_IN_PROGRESS, result.blockingReason)
            assertEquals(0L, result.releasableSheets)
        }
    }

    @Test
    fun testCompletedProduction_noReleaseRequired() {
        val completedStatuses = listOf("COMPLETED", "DELIVERED")

        for (status in completedStatuses) {
            val input = createInput(
                allocatedSheets = 10000L,
                consumedSheets = 10000L,
                committedSheets = 0L,
                productionStatus = status
            )

            val result = SubstrateReleaseGovernanceEngine.evaluate(input)

            assertEquals(ReleaseGovernanceDecision.NO_RELEASE_REQUIRED, result.decision)
            assertEquals(ReleaseBlockingReason.JOB_COMPLETED, result.blockingReason)
            assertEquals(0L, result.releasableSheets)
        }
    }

    @Test
    fun testRevisionQuantityReduction_releasesExcessUncommittedSheets() {
        val input = createInput(
            triggerType = GovernanceTriggerType.QUANTITY_REDUCTION,
            allocatedSheets = 10000L,
            consumedSheets = 1000L,
            committedSheets = 0L,
            previousRequiredSheets = 10000L,
            newRequiredSheets = 6000L,
            productionStatus = "READY"
        )

        val result = SubstrateReleaseGovernanceEngine.evaluate(input)

        assertEquals(ReleaseGovernanceDecision.PARTIAL_RELEASE_ELIGIBLE, result.decision)
        assertEquals(4000L, result.releasableSheets) // 10000 - 6000 = 4000
        assertEquals(6000L, result.retainedSheets)
        assertEquals(0L, result.additionalRequiredSheets)
    }

    @Test
    fun testRevisionQuantityIncrease_requiresAdditionalReservation() {
        val input = createInput(
            triggerType = GovernanceTriggerType.QUANTITY_INCREASE,
            allocatedSheets = 10000L,
            consumedSheets = 0L,
            committedSheets = 0L,
            previousRequiredSheets = 10000L,
            newRequiredSheets = 15000L,
            productionStatus = "READY"
        )

        val result = SubstrateReleaseGovernanceEngine.evaluate(input)

        assertEquals(ReleaseGovernanceDecision.NO_RELEASE_REQUIRED, result.decision)
        assertEquals(0L, result.releasableSheets)
        assertEquals(10000L, result.retainedSheets)
        assertEquals(5000L, result.additionalRequiredSheets) // 15000 - 10000 = 5000
    }

    @Test
    fun testSpecificationRevision_skuChange_releasesOldMaterial() {
        val input = createInput(
            triggerType = GovernanceTriggerType.SPECIFICATION_REVISION,
            allocatedSheets = 10000L,
            consumedSheets = 0L,
            committedSheets = 0L,
            previousRequiredSheets = 10000L,
            newRequiredSheets = 10000L,
            productionStatus = "READY",
            isSkuChanged = true
        )

        val result = SubstrateReleaseGovernanceEngine.evaluate(input)

        assertEquals(ReleaseGovernanceDecision.RELEASE_ELIGIBLE, result.decision)
        assertEquals(10000L, result.releasableSheets)
        assertEquals(10000L, result.additionalRequiredSheets) // New SKU needs allocation
    }

    @Test
    fun testAmbiguousProductionState_requiresManualReview() {
        val input = createInput(
            allocatedSheets = 10000L,
            consumedSheets = 0L,
            committedSheets = 0L,
            productionStatus = "UNKNOWN_CUSTOM_STAGE"
        )

        val result = SubstrateReleaseGovernanceEngine.evaluate(input)

        assertEquals(ReleaseGovernanceDecision.REQUIRES_REVIEW, result.decision)
        assertEquals(ReleaseBlockingReason.AMBIGUOUS_PRODUCTION_STATE, result.blockingReason)
        assertEquals(0L, result.releasableSheets)
    }
}
