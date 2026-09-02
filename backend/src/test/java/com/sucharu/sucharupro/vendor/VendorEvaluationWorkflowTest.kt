package com.sucharu.sucharupro.vendor

import com.sucharu.sucharupro.data.datasource.FakeVendorDataSource
import com.sucharu.sucharupro.data.datasource.FakeVendorPerformanceDataSource
import com.sucharu.sucharupro.data.repository.VendorPerformanceRepositoryImpl
import com.sucharu.sucharupro.data.repository.VendorRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.vendor.*
import com.sucharu.sucharupro.domain.service.vendor.VendorPerformanceServiceImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant

class VendorEvaluationWorkflowTest {

    private lateinit var vendorRepo: VendorRepositoryImpl
    private lateinit var repo: VendorPerformanceRepositoryImpl
    private lateinit var service: VendorPerformanceServiceImpl

    @Before
    fun setUp() {
        runBlocking {
            vendorRepo = VendorRepositoryImpl(FakeVendorDataSource())
            val ds = FakeVendorPerformanceDataSource()
            repo = VendorPerformanceRepositoryImpl(ds)
            service = VendorPerformanceServiceImpl(
                performanceRepository = repo,
                vendorRepository = vendorRepo
            )

            vendorRepo.createVendor(
                Vendor(
                    vendorId = "VND-01",
                    projectId = "PRJ-01",
                    vendorCode = "V001",
                    vendorName = "Vendor 1",
                    status = VendorStatus.ACTIVE
                )
            )
        }
    }

    @Test
    fun testEvaluationFullLifecycle() = runBlocking {
        val now = Instant.now()
        val evaluation = VendorEvaluation(
            evaluationId = "EVAL-001",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            periodType = EvaluationPeriodType.QUARTERLY,
            periodStart = now.minusSeconds(86400 * 90),
            periodEnd = now,
            evaluatorId = "evaluator_user",
            evaluatorName = "John Doe",
            evaluationScore = 88.0,
            rating = PerformanceRating.GOOD,
            status = EvaluationStatus.DRAFT,
            criteria = listOf(
                VendorEvaluationCriterion(
                    criterionId = "CRIT-01",
                    evaluationId = "EVAL-001",
                    name = "Technical Competence",
                    category = "CAPABILITY",
                    weight = 1.0,
                    score = 90.0
                ),
                VendorEvaluationCriterion(
                    criterionId = "CRIT-02",
                    evaluationId = "EVAL-001",
                    name = "Communication & Support",
                    category = "SERVICE",
                    weight = 1.0,
                    score = 86.0
                )
            ),
            createdBy = "evaluator_user"
        )

        // Create
        val createRes = service.createEvaluation(evaluation)
        assertTrue(createRes is DomainResult.Success)

        // Submit
        val submitRes = service.submitEvaluation("PRJ-01", "EVAL-001", "evaluator_user", "Ready for review")
        assertTrue(submitRes is DomainResult.Success)
        val submitted = (submitRes as DomainResult.Success).data
        assertEquals(EvaluationStatus.SUBMITTED, submitted.status)

        // Review
        val reviewRes = service.reviewEvaluation("PRJ-01", "EVAL-001", "reviewer_user", "Looks solid")
        assertTrue(reviewRes is DomainResult.Success)
        val reviewed = (reviewRes as DomainResult.Success).data
        assertEquals(EvaluationStatus.UNDER_REVIEW, reviewed.status)

        // Approve (Independent manager)
        val approveRes = service.approveEvaluation("PRJ-01", "EVAL-001", "approver_manager", EvaluationDecision.APPROVED, "Approved for continuation")
        assertTrue(approveRes is DomainResult.Success)
        val approved = (approveRes as DomainResult.Success).data
        assertEquals(EvaluationStatus.APPROVED, approved.status)
        assertEquals(EvaluationDecision.APPROVED, approved.decision)

        // Finalize
        val finalizeRes = service.finalizeEvaluation("PRJ-01", "EVAL-001", "approver_manager")
        assertTrue(finalizeRes is DomainResult.Success)
        val finalized = (finalizeRes as DomainResult.Success).data
        assertEquals(EvaluationStatus.FINALIZED, finalized.status)
        assertNotNull(finalized.finalizedAt)
    }
}
