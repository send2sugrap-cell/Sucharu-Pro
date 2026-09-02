package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.ReQcInspection
import com.sucharu.sucharupro.domain.model.qc.ReQcStatus
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ReQcValidator] core validation and mandatory field constraints (Module 06 Step 06).
 */
class ReQcValidationTest {

    @Test
    fun validateCreationParams_validInputs_returnsSuccess() {
        val result = ReQcValidator.validateCreationParams(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            cycleNumber = 1,
            createdBy = "user-001",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateCreationParams_blankProjectId_returnsError() {
        val result = ReQcValidator.validateCreationParams(
            projectId = "",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            cycleNumber = 1,
            createdBy = "user-001",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Project ID cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun validateCreationParams_blankJobId_returnsError() {
        val result = ReQcValidator.validateCreationParams(
            projectId = "proj-001",
            productionJobId = "",
            productionReworkId = "rew-001",
            cycleNumber = 1,
            createdBy = "user-001",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Production Job ID cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun validateCreationParams_blankReworkId_returnsError() {
        val result = ReQcValidator.validateCreationParams(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "  ",
            cycleNumber = 1,
            createdBy = "user-001",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Production Rework ID cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun validateCreationParams_zeroOrNegativeCycleNumber_returnsError() {
        val result = ReQcValidator.validateCreationParams(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            cycleNumber = 0,
            createdBy = "user-001",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Cycle number must be >= 1"))
    }

    @Test
    fun validateCreationParams_blankCreatedBy_returnsError() {
        val result = ReQcValidator.validateCreationParams(
            projectId = "proj-001",
            productionJobId = "job-001",
            productionReworkId = "rew-001",
            cycleNumber = 1,
            createdBy = "",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("CreatedBy actor ID cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun validateReQc_validModel_returnsSuccess() {
        val reQc = ReQcInspection(
            reQcId = "reqc-001",
            productionJobId = "job-001",
            projectId = "proj-001",
            productionReworkId = "rew-001",
            cycleNumber = 1,
            createdBy = "user-001",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = ReQcValidator.validateReQc(reQc)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateReQc_negativeQuantity_returnsError() {
        val reQc = ReQcInspection(
            reQcId = "reqc-001",
            productionJobId = "job-001",
            projectId = "proj-001",
            productionReworkId = "rew-001",
            cycleNumber = 1,
            affectedQuantity = -10,
            createdBy = "user-001",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = ReQcValidator.validateReQc(reQc)
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("Affected quantity cannot be negative"))
    }
}
