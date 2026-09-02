package com.sucharu.sucharupro.domain.validation

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.qc.DefectCategory
import com.sucharu.sucharupro.domain.model.qc.DefectEvidence
import com.sucharu.sucharupro.domain.model.qc.DefectSeverity
import com.sucharu.sucharupro.domain.model.qc.DefectSource
import com.sucharu.sucharupro.domain.model.qc.DefectStatus
import com.sucharu.sucharupro.domain.model.qc.ProductionDefect
import com.sucharu.sucharupro.domain.model.user.UserRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ProductionDefectValidator] rules and boundary constraints (Module 06 Step 04).
 */
class ProductionDefectValidationTest {

    private val validDefect = ProductionDefect(
        defectId = "def-01",
        productionJobId = "job-01",
        category = DefectCategory.BINDING_ERROR,
        severity = DefectSeverity.MAJOR,
        source = DefectSource.CHECKLIST_INSPECTION,
        status = DefectStatus.OPEN,
        title = "Defective perfect binding",
        description = "Glue did not set properly on book spines.",
        affectedQuantity = 200,
        affectedUnit = "books",
        detectedAt = "2026-08-17T10:00:00Z",
        detectedBy = "insp-01",
        createdAt = "2026-08-17T10:00:00Z",
        updatedAt = "2026-08-17T10:00:00Z"
    )

    @Test
    fun validateDefect_validModel_succeeds() {
        val result = ProductionDefectValidator.validateDefect(validDefect)
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateDefect_emptyFields_fails() {
        assertFalse(ProductionDefectValidator.validateDefect(validDefect.copy(title = "   ")).isSuccess)
        assertFalse(ProductionDefectValidator.validateDefect(validDefect.copy(description = "")).isSuccess)
        assertFalse(ProductionDefectValidator.validateDefect(validDefect.copy(affectedUnit = " ")).isSuccess)
    }

    @Test
    fun validateEvidence_matchingDefectId_succeeds() {
        val evidence = DefectEvidence(
            evidenceId = "evi-01",
            defectId = "def-01",
            description = "Photo showing cracked spine",
            createdBy = "insp-01",
            createdAt = "2026-08-17T10:05:00Z"
        )
        val result = ProductionDefectValidator.validateEvidence(evidence, "def-01")
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateEvidence_mismatchedDefectId_fails() {
        val evidence = DefectEvidence(
            evidenceId = "evi-01",
            defectId = "def-OTHER",
            description = "Photo showing cracked spine",
            createdBy = "insp-01",
            createdAt = "2026-08-17T10:05:00Z"
        )
        val result = ProductionDefectValidator.validateEvidence(evidence, "def-01")
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateResolution_withoutNotesOrResolvedBy_fails() {
        val res1 = ProductionDefectValidator.validateResolution(validDefect, resolutionNotes = "", resolvedBy = "insp-01")
        assertTrue(res1 is DomainResult.Error)

        val res2 = ProductionDefectValidator.validateResolution(validDefect, resolutionNotes = "Re-glued", resolvedBy = "")
        assertTrue(res2 is DomainResult.Error)

        val res3 = ProductionDefectValidator.validateResolution(validDefect, resolutionNotes = "Re-glued with EVA hotmelt", resolvedBy = "insp-01")
        assertTrue(res3 is DomainResult.Success)
    }

    @Test
    fun validateClosure_unresolvedDefect_fails() {
        val res = ProductionDefectValidator.validateClosure(validDefect, closedBy = "admin-01", callerRole = UserRole.ADMIN)
        assertTrue(res is DomainResult.Error)
        val err = res as DomainResult.Error
        assertTrue(err.message.contains("not in RESOLVED status"))
    }
}
