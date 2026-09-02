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

class VendorEvaluationApprovalTest {

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
    fun testSeparationOfDutiesEvaluatorCannotApproveOwnEvaluation() = runBlocking {
        val now = Instant.now()
        val evaluation = VendorEvaluation(
            evaluationId = "EVAL-002",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            periodType = EvaluationPeriodType.YEARLY,
            periodStart = now.minusSeconds(86400 * 365),
            periodEnd = now,
            evaluatorId = "user_sam",
            evaluatorName = "Sam Smith",
            evaluationScore = 92.0,
            rating = PerformanceRating.EXCELLENT,
            status = EvaluationStatus.DRAFT,
            createdBy = "user_sam"
        )
        service.createEvaluation(evaluation)
        service.submitEvaluation("PRJ-01", "EVAL-002", "user_sam")

        // Evaluator tries to approve own evaluation -> Must FAIL SoD validation
        val selfApproveRes = service.approveEvaluation("PRJ-01", "EVAL-002", "user_sam", EvaluationDecision.APPROVED)
        assertTrue(selfApproveRes is DomainResult.Error)
        val errMsg = (selfApproveRes as DomainResult.Error).message
        assertTrue(errMsg.contains("Separation of duties violation") || errMsg.contains("cannot approve"))

        // Independent manager approves -> Must SUCCEED
        val validApproveRes = service.approveEvaluation("PRJ-01", "EVAL-002", "manager_alice", EvaluationDecision.APPROVED)
        assertTrue(validApproveRes is DomainResult.Success)
    }

    @Test
    fun testSubmitterCannotApproveWhenDifferentFromEvaluator() = runBlocking {
        val now = Instant.now()
        val evaluation = VendorEvaluation(
            evaluationId = "EVAL-003",
            projectId = "PRJ-01",
            tenantId = "PRJ-01",
            vendorId = "VND-01",
            periodType = EvaluationPeriodType.MONTHLY,
            periodStart = now.minusSeconds(86400 * 30),
            periodEnd = now,
            evaluatorId = "user_bob",
            evaluatorName = "Bob Smith",
            evaluationScore = 80.0,
            rating = PerformanceRating.GOOD,
            status = EvaluationStatus.DRAFT,
            createdBy = "user_bob"
        )
        service.createEvaluation(evaluation)
        service.submitEvaluation("PRJ-01", "EVAL-003", "submitter_charlie")

        // Submitter Charlie tries to approve -> Must FAIL SoD validation
        val selfApproveRes = service.approveEvaluation("PRJ-01", "EVAL-003", "submitter_charlie", EvaluationDecision.APPROVED)
        assertTrue(selfApproveRes is DomainResult.Error)
    }
}
