package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.FinalQcInspection
import com.sucharu.sucharupro.domain.model.qc.FinalQcStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [FinalQcValidator] (Module 06 Step 07).
 */
class FinalQcValidationTest {

    @Test
    fun validCreationParams_succeeds() {
        val result = FinalQcValidator.validateCreationParams(
            projectId = "proj-01",
            productionJobId = "job-01",
            totalQuantity = 500,
            quantityUnit = "sheets",
            timestamp = "2026-08-17T10:00:00Z"
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun blankProjectId_fails() {
        val result = FinalQcValidator.validateCreationParams(
            projectId = "  ",
            productionJobId = "job-01",
            totalQuantity = 500,
            quantityUnit = "sheets",
            timestamp = "2026-08-17T10:00:00Z"
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Project ID cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun blankJobId_fails() {
        val result = FinalQcValidator.validateCreationParams(
            projectId = "proj-01",
            productionJobId = "",
            totalQuantity = 500,
            quantityUnit = "sheets",
            timestamp = "2026-08-17T10:00:00Z"
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Production Job ID cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun zeroOrNegativeTotalQuantity_fails() {
        val zeroRes = FinalQcValidator.validateCreationParams("proj-01", "job-01", 0, "sheets", "2026-08-17T10:00:00Z")
        assertTrue(zeroRes is DomainResult.Error)

        val negRes = FinalQcValidator.validateCreationParams("proj-01", "job-01", -5, "sheets", "2026-08-17T10:00:00Z")
        assertTrue(negRes is DomainResult.Error)
    }

    @Test
    fun inspectionModelValidation_detectsQuantityInconsistency() {
        val invalidModel = FinalQcInspection(
            finalQcId = "fqc-01",
            projectId = "proj-01",
            productionJobId = "job-01",
            status = FinalQcStatus.PASSED,
            totalQuantity = 100,
            inspectedQuantity = 100,
            acceptedQuantity = 80,
            rejectedQuantity = 10, // 80 + 10 != 100
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = FinalQcValidator.validateInspectionModel(invalidModel)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("must equal the sum"))
    }

    @Test
    fun passPrerequisites_withRejectedQuantity_fails() {
        val result = FinalQcValidator.validatePassPrerequisites(
            acceptedQuantity = 90,
            rejectedQuantity = 10,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T10:00:00Z"
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cannot mark Final QC as PASS when rejected quantity is greater than zero"))
    }

    @Test
    fun failPrerequisites_withBlankReason_fails() {
        val result = FinalQcValidator.validateFailPrerequisites(
            failureReason = "   ",
            rejectedQuantity = 10,
            inspectorId = "insp-01",
            timestamp = "2026-08-17T10:00:00Z"
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Failure reason/notes cannot be blank for a FAIL decision.", (result as DomainResult.Error).message)
    }
}
