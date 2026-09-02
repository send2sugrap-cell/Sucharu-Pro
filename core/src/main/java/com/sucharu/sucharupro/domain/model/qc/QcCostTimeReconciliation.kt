package com.sucharu.sucharupro.domain.model.qc

/**
 * Aggregate entity representing Planned vs. Actual operational QC cost and time reconciliation (Module 06 Step 08).
 *
 * Compares planned benchmarks against aggregated actual expenditures, captures failure counts,
 * and maintains reconciliation status and lock state.
 */
data class QcCostTimeReconciliation(
    val id: String,
    val productionJobId: String,
    val projectId: String,
    val plannedCost: Double,
    val actualCost: Double,
    val costVariance: Double = actualCost - plannedCost,
    val plannedMinutes: Long,
    val actualMinutes: Long,
    val timeVarianceMinutes: Long = actualMinutes - plannedMinutes,
    val qcEntryCount: Int = 0,
    val timeEntryCount: Int = 0,
    val defectCount: Int = 0,
    val reworkCount: Int = 0,
    val reQcCycleCount: Int = 0,
    val finalQcCount: Int = 0,
    val status: QcCostStatus = QcCostStatus.RECONCILED,
    val reconciledBy: String? = null,
    val reconciledByName: String? = null,
    val reconciledAt: String? = null,
    val lockedBy: String? = null,
    val lockedByName: String? = null,
    val lockedAt: String? = null,
    val snapshotId: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    /** Whether this reconciliation is locked and immutable. */
    val isLocked: Boolean get() = status == QcCostStatus.LOCKED

    /** Whether actual cost exceeded planned benchmark. */
    val hasCostOverrun: Boolean get() = costVariance > 0.0

    /** Whether actual time exceeded planned duration. */
    val hasTimeOverrun: Boolean get() = timeVarianceMinutes > 0L

    companion object {
        fun calculate(
            id: String,
            productionJobId: String,
            projectId: String,
            plannedCost: Double,
            plannedMinutes: Long,
            costEntries: List<QcCostEntry>,
            timeEntries: List<QcTimeEntry>,
            defectCount: Int = 0,
            reworkCount: Int = 0,
            reQcCycleCount: Int = 0,
            finalQcCount: Int = 0,
            reconciledBy: String? = null,
            reconciledByName: String? = null,
            timestamp: String,
            notes: String? = null
        ): QcCostTimeReconciliation {
            val activeCosts = costEntries.filter { it.isActive }
            val activeTimes = timeEntries.filter { it.isActive }

            val totalActualCost = activeCosts.sumOf { it.totalCost }
            val totalActualMinutes = activeTimes.sumOf { it.durationMinutes }

            return QcCostTimeReconciliation(
                id = id,
                productionJobId = productionJobId,
                projectId = projectId,
                plannedCost = plannedCost,
                actualCost = totalActualCost,
                costVariance = totalActualCost - plannedCost,
                plannedMinutes = plannedMinutes,
                actualMinutes = totalActualMinutes,
                timeVarianceMinutes = totalActualMinutes - plannedMinutes,
                qcEntryCount = activeCosts.size,
                timeEntryCount = activeTimes.size,
                defectCount = defectCount,
                reworkCount = reworkCount,
                reQcCycleCount = reQcCycleCount,
                finalQcCount = finalQcCount,
                status = QcCostStatus.RECONCILED,
                reconciledBy = reconciledBy,
                reconciledByName = reconciledByName,
                reconciledAt = timestamp,
                notes = notes,
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
    }
}
