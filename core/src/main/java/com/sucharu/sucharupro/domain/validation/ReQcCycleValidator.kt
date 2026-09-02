package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus

/**
 * Domain validator for Re-QC cycle numbering, lineage progression, duplicate prevention,
 * and source rework prerequisites (Module 06 Step 06).
 */
object ReQcCycleValidator {

    /**
     * Validates sequential cycle numbering against existing cycles for a job.
     */
    fun validateCycleNumber(
        existingCycles: List<ReQcInspection>,
        newCycleNumber: Int
    ): DomainResult<Unit> {
        val maxExisting = existingCycles.maxOfOrNull { it.cycleNumber } ?: 0
        val expectedNext = maxExisting + 1

        if (newCycleNumber != expectedNext) {
            return DomainResult.Error(
                message = "Invalid cycle number $newCycleNumber. Expected sequential cycle number $expectedNext (Max existing: $maxExisting)."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Ensures only one active Re-QC cycle exists at a time for a given rework record.
     */
    fun validateDuplicateActiveCycle(
        existingCycles: List<ReQcInspection>,
        productionReworkId: String
    ): DomainResult<Unit> {
        val activeCycle = existingCycles.find {
            it.productionReworkId == productionReworkId &&
                    !it.isTerminal &&
                    it.status != ReQcStatus.RETURNED_TO_REWORK
        }

        if (activeCycle != null) {
            return DomainResult.Error(
                message = "An active Re-QC cycle already exists for rework '$productionReworkId' (Cycle: ${activeCycle.cycleNumber}, ID: ${activeCycle.reQcId}, Status: ${activeCycle.status.defaultLabel})."
            )
        }
        return DomainResult.Success(Unit)
    }

    /**
     * Validates previous cycle relationship and status for multi-cycle failure loops.
     */
    fun validatePreviousCycle(
        cycleNumber: Int,
        previousReQcId: String?,
        existingCycles: List<ReQcInspection>
    ): DomainResult<Unit> {
        if (cycleNumber == 1) {
            // Cycle 1 may have null previousReQcId
            return DomainResult.Success(Unit)
        }

        if (previousReQcId.isNullOrBlank()) {
            return DomainResult.Error(
                message = "Previous Re-QC ID is mandatory for cycle $cycleNumber (Multi-cycle failure loop)."
            )
        }

        val prevCycle = existingCycles.find { it.reQcId == previousReQcId }
            ?: return DomainResult.Error(
                message = "Previous Re-QC cycle '$previousReQcId' not found in existing cycles."
            )

        if (prevCycle.status != ReQcStatus.FAILED && prevCycle.status != ReQcStatus.RETURNED_TO_REWORK) {
            return DomainResult.Error(
                message = "Cannot create next Re-QC cycle from previous cycle '${prevCycle.reQcId}' in status '${prevCycle.status.defaultLabel}' (Must be FAILED or RETURNED_TO_REWORK)."
            )
        }

        if (prevCycle.cycleNumber >= cycleNumber) {
            return DomainResult.Error(
                message = "Previous cycle number (${prevCycle.cycleNumber}) must be less than new cycle number ($cycleNumber)."
            )
        }

        return DomainResult.Success(Unit)
    }

    /**
     * Validates that source rework is valid and in RETURNED_TO_QC state.
     */
    fun validateSourceReworkState(
        rework: ProductionRework
    ): DomainResult<Unit> {
        if (rework.status != ReworkStatus.RETURNED_TO_QC) {
            return DomainResult.Error(
                message = "Cannot create Re-QC from rework '${rework.reworkId}' because it is in '${rework.status.defaultLabel}' status (Must be 'RETURNED_TO_QC')."
            )
        }
        return DomainResult.Success(Unit)
    }
}
