package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.common.FileReference
import com.sucharu.sucharupro.domain.model.qc.ProductionRework
import com.sucharu.sucharupro.domain.model.qc.ReworkEvidence
import com.sucharu.sucharupro.domain.model.qc.ReworkReason
import com.sucharu.sucharupro.domain.model.qc.ReworkStatus
import com.sucharu.sucharupro.domain.model.qc.ReworkType
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Domain validation unit tests for [ProductionRework] parameters and field integrity (Module 06 Step 05).
 */
class ProductionReworkValidationTest {

    @Test
    fun validateCreationParams_validInputs_returnsSuccess() {
        val result = ProductionReworkValidator.validateCreationParams(
            projectId = "proj-101",
            productionJobId = "job-202",
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Cyan and Magenta registration realignment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateCreationParams_blankProjectId_fails() {
        val result = ProductionReworkValidator.validateCreationParams(
            projectId = "   ",
            productionJobId = "job-202",
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Realignment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Project ID cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun validateCreationParams_blankJobId_fails() {
        val result = ProductionReworkValidator.validateCreationParams(
            projectId = "proj-101",
            productionJobId = "",
            affectedQuantity = 100,
            quantityUnit = "sheets",
            description = "Realignment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Production Job ID cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun validateCreationParams_zeroOrNegativeAffectedQuantity_fails() {
        val zeroResult = ProductionReworkValidator.validateCreationParams(
            projectId = "proj-101",
            productionJobId = "job-202",
            affectedQuantity = 0,
            quantityUnit = "sheets",
            description = "Realignment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(zeroResult is DomainResult.Error)
        assertTrue((zeroResult as DomainResult.Error).message.contains("Affected quantity must be greater than 0"))

        val negResult = ProductionReworkValidator.validateCreationParams(
            projectId = "proj-101",
            productionJobId = "job-202",
            affectedQuantity = -15,
            quantityUnit = "sheets",
            description = "Realignment",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(negResult is DomainResult.Error)
        assertTrue((negResult as DomainResult.Error).message.contains("Affected quantity must be greater than 0"))
    }

    @Test
    fun validateCreationParams_blankDescription_fails() {
        val result = ProductionReworkValidator.validateCreationParams(
            projectId = "proj-101",
            productionJobId = "job-202",
            affectedQuantity = 50,
            quantityUnit = "pcs",
            description = "   ",
            requestedBy = "insp-01",
            timestamp = "2026-08-17T10:00:00Z",
            callerRole = UserRole.QC_INSPECTOR
        )
        assertTrue(result is DomainResult.Error)
        assertEquals("Rework description cannot be blank.", (result as DomainResult.Error).message)
    }

    @Test
    fun validateRework_validModel_returnsSuccess() {
        val rework = ProductionRework(
            reworkId = "rew-001",
            projectId = "proj-101",
            productionJobId = "job-202",
            reworkType = ReworkType.COLOR_CORRECTION,
            reason = ReworkReason.DEFECT_CORRECTION,
            status = ReworkStatus.REQUESTED,
            affectedQuantity = 250,
            quantityUnit = "sheets",
            description = "কালার ডেল্টা কারেকশন",
            requestedBy = "user-10",
            requestedAt = "2026-08-17T10:00:00Z",
            createdAt = "2026-08-17T10:00:00Z",
            updatedAt = "2026-08-17T10:00:00Z"
        )
        val result = ProductionReworkValidator.validateRework(rework)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateEvidence_validEvidence_returnsSuccess() {
        val evidence = ReworkEvidence(
            evidenceId = "evi-01",
            reworkId = "rew-001",
            description = "Before correction photo",
            createdBy = "user-10",
            createdAt = "2026-08-17T10:00:00Z"
        )
        val result = ProductionReworkValidator.validateEvidence(evidence, "rew-001")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateEvidence_mismatchedReworkId_fails() {
        val evidence = ReworkEvidence(
            evidenceId = "evi-01",
            reworkId = "rew-999",
            description = "Before correction photo",
            createdBy = "user-10",
            createdAt = "2026-08-17T10:00:00Z"
        )
        val result = ProductionReworkValidator.validateEvidence(evidence, "rew-001")
        assertTrue(result is DomainResult.Error)
        assertTrue((result as DomainResult.Error).message.contains("does not match target rework"))
    }

    @Test
    fun validateFileReference_valid_returnsSuccess() {
        val fileRef = FileReference(
            fileId = "file-01",
            fileName = "sample.jpg",
            mimeType = "image/jpeg",
            storagePath = "/uploads/sample.jpg",
            fileSize = 1024L,
            uploadedAt = "2026-08-17T10:00:00Z"
        )
        val result = ProductionReworkValidator.validateFileReference(fileRef)
        assertTrue(result is DomainResult.Success)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fileReference_zeroSize_throwsException() {
        FileReference(
            fileId = "file-01",
            fileName = "sample.jpg",
            mimeType = "image/jpeg",
            storagePath = "/uploads/sample.jpg",
            fileSize = 0L,
            uploadedAt = "2026-08-17T10:00:00Z"
        )
    }
}
