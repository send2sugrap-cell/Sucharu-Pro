package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.job.ProductionJob
import com.sucharu.sucharupro.domain.model.job.ProductionJobStatus
import com.sucharu.sucharupro.domain.model.order.OrderPriority
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-Project Isolation unit tests ensuring rework cannot reference jobs from differing projects (Module 06 Step 05).
 */
class ProductionReworkCrossProjectIsolationTest {

    private fun createJob(jobId: String, orderId: String): ProductionJob {
        return ProductionJob(
            jobId = jobId,
            jobNumber = "JOB-$jobId",
            orderId = orderId,
            orderNumber = "ORD-$orderId",
            customerId = "cust-01",
            handoffId = "handoff-01",
            title = "Offset Print Catalog",
            priority = OrderPriority.NORMAL,
            status = ProductionJobStatus.IN_PROGRESS,
            quantity = 1000,
            unit = "Pcs",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
    }

    @Test
    fun validateJobCrossProjectIsolation_matchingProject_succeeds() {
        val job = createJob("job-100", "proj-alpha")
        val result = ProductionReworkValidator.validateJobCrossProjectIsolation("proj-alpha", job)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateJobCrossProjectIsolation_mismatchedProject_fails() {
        val job = createJob("job-100", "proj-beta")
        val result = ProductionReworkValidator.validateJobCrossProjectIsolation("proj-alpha", job)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cross-project reference violation"))
    }
}
