package com.sucharu.sucharupro.domain.service.profitability

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.profitability.JobCostMathUtils
import com.sucharu.sucharupro.domain.model.profitability.JobCostReconciliationEvent
import com.sucharu.sucharupro.domain.model.profitability.JobCostSnapshot
import com.sucharu.sucharupro.domain.validation.profitability.JobCostValidator
import java.math.BigDecimal
import java.util.UUID

/**
 * Production implementation of JobCostReconciliationService.
 */
class JobCostReconciliationServiceImpl : JobCostReconciliationService {

    override suspend fun reconcileJobCostSnapshot(
        snapshot: JobCostSnapshot,
        actor: String
    ): DomainResult<JobCostReconciliationEvent> {
        val discrepancies = mutableListOf<String>()

        // 1. Sum of components
        val componentSum = snapshot.costComponents.fold(BigDecimal.ZERO) { acc, c ->
            acc.add(c.attributedAmount)
        }.let { JobCostMathUtils.scaleMoney(it) }

        val snapshotTotal = snapshot.totalActualCost
        val componentDiff = snapshotTotal.subtract(componentSum).abs()

        if (componentDiff.compareTo(BigDecimal("0.0001")) > 0) {
            discrepancies.add("Snapshot total cost ($snapshotTotal) differs from component sum ($componentSum) by $componentDiff")
        }

        // 2. Sum of provenances
        val provenanceSum = snapshot.provenances.fold(BigDecimal.ZERO) { acc, p ->
            acc.add(p.attributedAmount)
        }.let { JobCostMathUtils.scaleMoney(it) }

        val provenanceDiff = snapshotTotal.subtract(provenanceSum).abs()

        if (provenanceDiff.compareTo(BigDecimal("0.0001")) > 0) {
            discrepancies.add("Snapshot total cost ($snapshotTotal) differs from provenance sum ($provenanceSum) by $provenanceDiff")
        }

        // 3. Duplicate checks
        val duplicates = JobCostValidator.detectDuplicateProvenances(snapshot.provenances)
        discrepancies.addAll(duplicates)

        val isReconciled = discrepancies.isEmpty()

        val event = JobCostReconciliationEvent(
            reconciliationId = "REC-JOB-${UUID.randomUUID()}",
            tenantId = snapshot.tenantId,
            projectId = snapshot.projectId,
            jobId = snapshot.jobId,
            snapshotId = snapshot.snapshotId,
            isReconciled = isReconciled,
            componentTotalCost = componentSum,
            snapshotTotalCost = snapshotTotal,
            provenanceTotalCost = provenanceSum,
            componentDifference = componentDiff,
            provenanceDifference = provenanceDiff,
            duplicateCount = duplicates.size,
            missingSourceCount = snapshot.unresolvedSourceCount,
            discrepancies = discrepancies,
            checkedBy = actor,
            checkedAt = System.currentTimeMillis()
        )

        return DomainResult.Success(event)
    }
}
