package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.FinalQcDecision
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcReleaseEligibility
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.qc.ProductionQc
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.QcDecision
import com.sucharu.sucharupro.domain.model.qc.QcStatus
import com.sucharu.sucharupro.domain.model.qc.QcType
import com.sucharu.sucharupro.domain.model.qc.ReQcDecision
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Comprehensive evaluation tests for [FinalQcEligibilityValidator] (Module 06 Step 07).
 */
class FinalQcReleaseEligibilityTest {

    private fun createValidJob(): ProductionJob {
        return ProductionJob(
            jobId = "job-01",
            jobNumber = "JOB-1001",
            orderId = "ord-01",
            orderNumber = "ORD-1001",
            customerId = "cust-01",
            handoffId = "ho-01",
            title = "Annual Report 2026",
            quantity = 500,
            status = ProductionJobStatus.IN_PROGRESS,
            createdAt = "2026-08-17T08:00:00Z",
            updatedAt = "2026-08-17T08:00:00Z"
        )
    }

    private fun createValidPreProdQc(): ProductionQc {
        return ProductionQc(
            qcId = "qc-pre-01",
            productionJobId = "job-01",
            qcType = QcType.PRE_PRODUCTION,
            status = QcStatus.PASSED,
            decision = QcDecision.PASS,
            createdAt = "2026-08-17T08:30:00Z",
            updatedAt = "2026-08-17T08:30:00Z"
        )
    }

    private fun createPassedFinalQc(): FinalQcInspection {
        return FinalQcInspection(
            finalQcId = "fqc-01",
            projectId = "ord-01", // In this test, job projectId is matched to orderId
            productionJobId = "job-01",
            status = FinalQcStatus.PASSED,
            decision = FinalQcDecision.PASS,
            totalQuantity = 500,
            inspectedQuantity = 500,
            acceptedQuantity = 500,
            rejectedQuantity = 0,
            createdAt = "2026-08-17T11:00:00Z",
            updatedAt = "2026-08-17T11:00:00Z"
        )
    }

    @Test
    fun allQualityGatesSatisfied_returnsEligible() {
        val job = createValidJob()
        val preQc = createValidPreProdQc()
        val finalQc = createPassedFinalQc().copy(projectId = job.orderId)

        val result = FinalQcEligibilityValidator.evaluateEligibility(
            inspection = finalQc,
            job = job,
            preProductionQcList = listOf(preQc)
        )

        assertTrue("Expected eligible", result.isEligible)
        assertTrue(result.reasons.contains(FinalQcReleaseEligibility.ELIGIBLE))
    }

    @Test
    fun missingPreProductionQc_blocked() {
        val job = createValidJob()
        val finalQc = createPassedFinalQc().copy(projectId = job.orderId)

        val result = FinalQcEligibilityValidator.evaluateEligibility(
            inspection = finalQc,
            job = job,
            preProductionQcList = emptyList() // No Pre-Prod QC
        )

        assertFalse(result.isEligible)
        assertTrue(result.reasons.contains(FinalQcReleaseEligibility.BLOCKED_PRE_PRODUCTION_QC))
    }

    @Test
    fun activeOpenDefect_blocked() {
        val job = createValidJob()
        val preQc = createValidPreProdQc()
        val finalQc = createPassedFinalQc().copy(projectId = job.orderId)

        val openDefect = ProductionDefect(
            defectId = "def-01",
            productionJobId = "job-01",
            category = DefectCategory.PRINT_QUALITY,
            severity = DefectSeverity.CRITICAL,
            source = DefectSource.PRODUCTION_STAGE,
            status = DefectStatus.OPEN,
            title = "Missing Page 4",
            description = "Page 4 omitted from binding",
            affectedQuantity = 50,
            detectedAt = "2026-08-17T09:00:00Z",
            detectedBy = "insp-01",
            createdAt = "2026-08-17T09:00:00Z",
            updatedAt = "2026-08-17T09:00:00Z"
        )

        val result = FinalQcEligibilityValidator.evaluateEligibility(
            inspection = finalQc,
            job = job,
            preProductionQcList = listOf(preQc),
            defectList = listOf(openDefect)
        )

        assertFalse(result.isEligible)
        assertTrue(result.reasons.contains(FinalQcReleaseEligibility.BLOCKED_OPEN_DEFECT))
    }

    @Test
    fun activeRework_blocked() {
        val job = createValidJob()
        val preQc = createValidPreProdQc()
        val finalQc = createPassedFinalQc().copy(projectId = job.orderId)

        val activeRework = ProductionRework(
            reworkId = "rew-01",
            projectId = job.orderId,
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            status = ReworkStatus.IN_PROGRESS,
            affectedQuantity = 50,
            description = "Plate realignment",
            requestedBy = "user-01",
            requestedAt = "2026-08-17T09:30:00Z",
            createdAt = "2026-08-17T09:30:00Z",
            updatedAt = "2026-08-17T09:30:00Z"
        )

        val result = FinalQcEligibilityValidator.evaluateEligibility(
            inspection = finalQc,
            job = job,
            preProductionQcList = listOf(preQc),
            reworkList = listOf(activeRework)
        )

        assertFalse(result.isEligible)
        assertTrue(result.reasons.contains(FinalQcReleaseEligibility.BLOCKED_ACTIVE_REWORK))
    }

    @Test
    fun failedReQc_blocked() {
        val job = createValidJob()
        val preQc = createValidPreProdQc()
        val finalQc = createPassedFinalQc().copy(projectId = job.orderId)

        val rework = ProductionRework(
            reworkId = "rew-01",
            projectId = job.orderId,
            productionJobId = "job-01",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            status = ReworkStatus.RETURNED_TO_QC,
            affectedQuantity = 50,
            description = "Completed rework pass 1",
            requestedBy = "user-01",
            requestedAt = "2026-08-17T09:30:00Z",
            createdAt = "2026-08-17T09:30:00Z",
            updatedAt = "2026-08-17T09:30:00Z"
        )

        val failedReQc = ReQcInspection(
            reQcId = "reqc-01",
            projectId = job.orderId,
            productionJobId = "job-01",
            productionReworkId = "rew-01",
            cycleNumber = 1,
            status = ReQcStatus.FAILED,
            decision = ReQcDecision.FAIL,
            createdBy = "insp-01",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )

        val result = FinalQcEligibilityValidator.evaluateEligibility(
            inspection = finalQc,
            job = job,
            preProductionQcList = listOf(preQc),
            reworkList = listOf(rework),
            reQcList = listOf(failedReQc)
        )

        assertFalse(result.isEligible)
        assertTrue(result.reasons.contains(FinalQcReleaseEligibility.BLOCKED_FAILED_RE_QC))
    }
}
