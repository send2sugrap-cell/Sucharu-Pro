package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.QcCostStatus
import com.sucharu.sucharupro.domain.model.qc.QcCostTimeReconciliation
import org.junit.Assert.assertTrue
import org.junit.Test

class QcCostTimeReconciliationValidationTest {

    @Test
    fun `validateCalculationParams succeeds with non-negative benchmarks`() {
        val result = QcCostTimeReconciliationValidator.validateCalculationParams(
            plannedCost = 500.0,
            plannedMinutes = 120L
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `validateCalculationParams fails on negative benchmarks`() {
        val resNegCost = QcCostTimeReconciliationValidator.validateCalculationParams(
            plannedCost = -100.0,
            plannedMinutes = 60L
        )
        assertTrue(resNegCost is DomainResult.Error)

        val resNegTime = QcCostTimeReconciliationValidator.validateCalculationParams(
            plannedCost = 100.0,
            plannedMinutes = -30L
        )
        assertTrue(resNegTime is DomainResult.Error)
    }

    @Test
    fun `validateLockPrerequisites succeeds for RECONCILED or ADJUSTED states`() {
        val recon = QcCostTimeReconciliation(
            id = "recon-01",
            productionJobId = "JOB-01",
            projectId = "PRJ-01",
            plannedCost = 500.0,
            actualCost = 550.0,
            plannedMinutes = 60L,
            actualMinutes = 70L,
            status = QcCostStatus.RECONCILED,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        assertTrue(QcCostTimeReconciliationValidator.validateLockPrerequisites(recon) is DomainResult.Success)
    }

    @Test
    fun `validateLockPrerequisites fails when already locked or in draft`() {
        val lockedRecon = QcCostTimeReconciliation(
            id = "recon-01",
            productionJobId = "JOB-01",
            projectId = "PRJ-01",
            plannedCost = 500.0,
            actualCost = 550.0,
            plannedMinutes = 60L,
            actualMinutes = 70L,
            status = QcCostStatus.LOCKED,
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        assertTrue(QcCostTimeReconciliationValidator.validateLockPrerequisites(lockedRecon) is DomainResult.Error)

        val draftRecon = lockedRecon.copy(status = QcCostStatus.DRAFT)
        assertTrue(QcCostTimeReconciliationValidator.validateLockPrerequisites(draftRecon) is DomainResult.Error)
    }
}
