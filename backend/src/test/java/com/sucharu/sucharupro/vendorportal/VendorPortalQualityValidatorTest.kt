package com.sucharu.sucharupro.vendorportal

import com.sucharu.sucharupro.domain.model.common.Money
import com.sucharu.sucharupro.domain.model.vendor.VendorDefectSeverity
import com.sucharu.sucharupro.domain.model.vendor.VendorDisputeType
import com.sucharu.sucharupro.domain.model.vendorportal.*
import com.sucharu.sucharupro.domain.validation.vendorportal.VendorPortalQualityValidator
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class VendorPortalQualityValidatorTest {

    @Test
    fun testValidateQualityCaseSuccess() {
        val case = VendorPortalQualityCase(
            caseId = "QC-01",
            tenantId = "T1",
            projectId = "P1",
            vendorId = "V1",
            caseNumber = "QC-100",
            title = "Dimensional deviation",
            description = "Observed out-of-spec dimensions on outer diameter."
        )
        VendorPortalQualityValidator.validateQualityCase(case)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testValidateQualityCaseBlankTitleFails() {
        val case = VendorPortalQualityCase(
            caseId = "QC-01",
            tenantId = "T1",
            projectId = "P1",
            vendorId = "V1",
            caseNumber = "QC-100",
            title = "",
            description = "Some description"
        )
        VendorPortalQualityValidator.validateQualityCase(case)
    }

    @Test
    fun testQualityCaseStatusTransitions() {
        // Valid transitions
        VendorPortalQualityValidator.validateQualityCaseStatusTransition(
            VendorPortalQualityCaseStatus.OPEN,
            VendorPortalQualityCaseStatus.ACKNOWLEDGED
        )
        VendorPortalQualityValidator.validateQualityCaseStatusTransition(
            VendorPortalQualityCaseStatus.ACKNOWLEDGED,
            VendorPortalQualityCaseStatus.RESPONSE_SUBMITTED
        )

        // Invalid transition
        assertThrows(IllegalArgumentException::class.java) {
            VendorPortalQualityValidator.validateQualityCaseStatusTransition(
                VendorPortalQualityCaseStatus.CLOSED,
                VendorPortalQualityCaseStatus.OPEN
            )
        }
    }

    @Test
    fun testValidateCapaPlanSuccess() {
        val capa = VendorPortalCapaPlan(
            capaId = "CAPA-01",
            tenantId = "T1",
            projectId = "P1",
            vendorId = "V1",
            capaNumber = "CAPA-100",
            title = "Die wear CAPA",
            rootCause = "Stamping die was worn beyond tolerance limits.",
            correctiveAction = "Replaced stamping die with hardened carbide tool.",
            preventiveAction = "Added ultrasonic wear sensors and die life counter.",
            responsiblePerson = "Lead Tooling Eng",
            targetCompletionDate = System.currentTimeMillis() + 86400000L
        )
        VendorPortalQualityValidator.validateCapaPlan(capa)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testValidateCapaPlanShortRootCauseFails() {
        val capa = VendorPortalCapaPlan(
            capaId = "CAPA-01",
            tenantId = "T1",
            projectId = "P1",
            vendorId = "V1",
            capaNumber = "CAPA-100",
            title = "Die wear CAPA",
            rootCause = "Bad",
            correctiveAction = "Replaced stamping die with hardened carbide tool.",
            preventiveAction = "Added ultrasonic wear sensors and die life counter.",
            responsiblePerson = "Lead Tooling Eng",
            targetCompletionDate = System.currentTimeMillis() + 86400000L
        )
        VendorPortalQualityValidator.validateCapaPlan(capa)
    }

    @Test
    fun testValidateDisputeSubmissionSuccess() {
        val dispute = VendorPortalDisputeSummary(
            disputeId = "DISP-01",
            tenantId = "T1",
            projectId = "P1",
            vendorId = "V1",
            disputeReference = "DISP-100",
            sourceType = "REJECTION",
            sourceId = "REJ-100",
            disputeType = VendorDisputeType.QUALITY,
            priority = VendorPortalQualityPriority.HIGH,
            status = VendorPortalDisputeStatus.OPEN,
            subject = "Rejection dispute",
            description = "Material cert EN 10204 3.1 was provided showing yield strength 450 MPa.",
            requestedResolution = VendorPortalResolutionType.CREDIT,
            disputedQuantity = BigDecimal("10"),
            disputedAmount = Money(BigDecimal("500")),
            raisedBy = "VENDOR_01"
        )
        VendorPortalQualityValidator.validateDisputeSubmission(dispute)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testValidateDisputeNegativeAmountFails() {
        val dispute = VendorPortalDisputeSummary(
            disputeId = "DISP-01",
            tenantId = "T1",
            projectId = "P1",
            vendorId = "V1",
            disputeReference = "DISP-100",
            sourceType = "REJECTION",
            sourceId = "REJ-100",
            disputeType = VendorDisputeType.QUALITY,
            priority = VendorPortalQualityPriority.HIGH,
            status = VendorPortalDisputeStatus.OPEN,
            subject = "Rejection dispute",
            description = "Material cert EN 10204 3.1 was provided showing yield strength 450 MPa.",
            requestedResolution = VendorPortalResolutionType.CREDIT,
            disputedQuantity = BigDecimal("10"),
            disputedAmount = Money(BigDecimal("-500")),
            raisedBy = "VENDOR_01"
        )
        VendorPortalQualityValidator.validateDisputeSubmission(dispute)
    }
}
