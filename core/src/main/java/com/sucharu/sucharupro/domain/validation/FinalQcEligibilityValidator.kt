package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.FinalQcDecision
import com.sucharu.sucharupro.domain.model.qc.FinalQcEligibilityResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseEligibility
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcChecklistStatus
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcInspectionChecklist
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.qc.ReQcDecision
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus

/**
 * Domain validator evaluating the complete 14-point quality gate for Final QC Production Release (Module 06 Step 07).
 */
object FinalQcEligibilityValidator {

    /**
     * Evaluates whether a [FinalQcInspection] is eligible for production release.
     */
    fun evaluateEligibility(
        inspection: FinalQcInspection,
        job: ProductionJob?,
        preProductionQcList: List<ProductionQc> = emptyList(),
        checklistList: List<QcInspectionChecklist> = emptyList(),
        defectList: List<ProductionDefect> = emptyList(),
        reworkList: List<ProductionRework> = emptyList(),
        reQcList: List<ReQcInspection> = emptyList()
    ): FinalQcEligibilityResult {
        val blockers = mutableListOf<FinalQcReleaseEligibility>()

        // 1. Validate Job Existence & State (when job is provided)
        if (job != null) {
            if (job.status.isTerminal) {
                blockers.add(FinalQcReleaseEligibility.BLOCKED_INVALID_JOB)
            }
            // Check project consistency
            if (job.orderId != inspection.projectId) {
                blockers.add(FinalQcReleaseEligibility.BLOCKED_CROSS_PROJECT_REFERENCE)
            }
        }

        // 2. Already Released Check
        if (inspection.status == FinalQcStatus.RELEASED) {
            blockers.add(FinalQcReleaseEligibility.BLOCKED_ALREADY_RELEASED)
        }

        // 3. Final QC Inspection State Checks
        when (inspection.status) {
            FinalQcStatus.DRAFT, FinalQcStatus.PENDING, FinalQcStatus.ASSIGNED, FinalQcStatus.IN_INSPECTION -> {
                blockers.add(FinalQcReleaseEligibility.BLOCKED_PENDING_INSPECTION)
            }
            FinalQcStatus.FAILED -> {
                blockers.add(FinalQcReleaseEligibility.BLOCKED_INSPECTION_FAILED)
            }
            FinalQcStatus.BLOCKED -> {
                blockers.add(FinalQcReleaseEligibility.BLOCKED_PENDING_INSPECTION)
            }
            FinalQcStatus.CANCELLED -> {
                blockers.add(FinalQcReleaseEligibility.BLOCKED_INVALID_JOB)
            }
            FinalQcStatus.PASSED -> {
                if (inspection.decision != FinalQcDecision.PASS) {
                    blockers.add(FinalQcReleaseEligibility.BLOCKED_INSPECTION_FAILED)
                }
                if (inspection.rejectedQuantity > 0) {
                    blockers.add(FinalQcReleaseEligibility.BLOCKED_QUANTITY_REJECTED)
                }
            }
            FinalQcStatus.RELEASED -> {
                // Handled in already released check
            }
        }

        // 4. Pre-Production QC Gate Check
        val preProdQcs = preProductionQcList.filter {
            it.productionJobId == inspection.productionJobId && it.qcType == QcType.PRE_PRODUCTION
        }
        val hasPassedPreProd = preProdQcs.isNotEmpty() && preProdQcs.all {
            it.status == QcStatus.PASSED && it.decision == QcDecision.PASS
        }
        if (!hasPassedPreProd) {
            blockers.add(FinalQcReleaseEligibility.BLOCKED_PRE_PRODUCTION_QC)
        }

        // 5. Checklist Gate Check (if any checklist is attached/required)
        val relevantChecklists = checklistList.filter { it.productionJobId == inspection.productionJobId }
        val hasIncompleteChecklist = relevantChecklists.any {
            it.status in setOf(QcChecklistStatus.DRAFT, QcChecklistStatus.IN_PROGRESS)
        }
        if (hasIncompleteChecklist) {
            blockers.add(FinalQcReleaseEligibility.BLOCKED_MISSING_CHECKLIST)
        }

        // 6. Defect Gate Check: No active unresolved defects
        val relevantDefects = defectList.filter { it.productionJobId == inspection.productionJobId }
        val activeDefects = relevantDefects.filter {
            it.status in setOf(
                DefectStatus.OPEN,
                DefectStatus.ACKNOWLEDGED,
                DefectStatus.UNDER_INVESTIGATION,
                DefectStatus.CONTAINED,
                DefectStatus.RESOLUTION_PENDING
            )
        }
        if (activeDefects.isNotEmpty()) {
            blockers.add(FinalQcReleaseEligibility.BLOCKED_OPEN_DEFECT)
        }

        // 7. Rework Gate Check: No active rework in progress
        val relevantReworks = reworkList.filter { it.productionJobId == inspection.productionJobId }
        val activeReworks = relevantReworks.filter {
            it.status in setOf(
                ReworkStatus.REQUESTED,
                ReworkStatus.UNDER_REVIEW,
                ReworkStatus.APPROVED,
                ReworkStatus.ASSIGNED,
                ReworkStatus.IN_PROGRESS
            )
        }
        if (activeReworks.isNotEmpty()) {
            blockers.add(FinalQcReleaseEligibility.BLOCKED_ACTIVE_REWORK)
        }

        // 8. Re-QC Gate Check: All Re-QC chains on this job must terminate in a PASSED cycle
        val relevantReQcs = reQcList.filter { it.productionJobId == inspection.productionJobId }
        if (relevantReQcs.isNotEmpty()) {
            // Leaf cycles are cycles that have not been superseded by a subsequent cycle
            val previousCycleIds = relevantReQcs.mapNotNull { it.previousReQcId }.toSet()
            val leafCycles = relevantReQcs.filter { it.reQcId !in previousCycleIds }
            val hasUnresolvedLeaf = leafCycles.any { it.status != ReQcStatus.PASSED || it.decision != ReQcDecision.PASS }
            if (hasUnresolvedLeaf) {
                blockers.add(FinalQcReleaseEligibility.BLOCKED_FAILED_RE_QC)
            }
        }

        val isEligible = blockers.isEmpty()
        val message = if (isEligible) {
            "All 14 quality gates verified. Production release is eligible."
        } else {
            "Production release blocked by: ${blockers.joinToString { it.defaultLabel }}"
        }

        return FinalQcEligibilityResult(
            isEligible = isEligible,
            reasons = if (isEligible) listOf(FinalQcReleaseEligibility.ELIGIBLE) else blockers.distinct(),
            message = message
        )
    }
}
