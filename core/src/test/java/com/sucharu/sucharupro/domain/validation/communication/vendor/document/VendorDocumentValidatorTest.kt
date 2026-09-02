package com.sucharu.sucharupro.domain.validation.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentType
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorDocumentValidatorTest {

    private val validProjectId = "proj-001"
    private val validVendorId = "ven-001"
    private val validTitle = "ISO 9001 Quality Certificate"
    private val validFileRef = "file-ref-abc123"
    private val validActorId = "user-staff-01"

    // ─────────────────────────────────────────────
    // Happy path
    // ─────────────────────────────────────────────

    @Test
    fun validateSubmission_allValidFields_noExpiryRequired_succeeds() {
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.OTHER,
            title = validTitle,
            fileReferenceId = validFileRef,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateSubmission_typeRequiresExpiry_expiryProvided_succeeds() {
        val now = System.currentTimeMillis()
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.BUSINESS_LICENSE,
            title = "Business License 2026",
            fileReferenceId = validFileRef,
            issueDate = now - 10_000,
            expiryDate = now + 365L * 86_400_000L,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateSubmission_noExpiryDateForNonRequiringType_succeeds() {
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = "TIN Certificate",
            fileReferenceId = validFileRef,
            issueDate = null,
            expiryDate = null,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Success)
    }

    // ─────────────────────────────────────────────
    // Blank field validation
    // ─────────────────────────────────────────────

    @Test
    fun validateSubmission_blankProjectId_fails() {
        val result = VendorDocumentValidator.validateSubmission(
            projectId = "   ",
            vendorId = validVendorId,
            documentType = VendorDocumentType.OTHER,
            title = validTitle,
            fileReferenceId = validFileRef,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateSubmission_blankVendorId_fails() {
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = "",
            documentType = VendorDocumentType.OTHER,
            title = validTitle,
            fileReferenceId = validFileRef,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateSubmission_blankTitle_fails() {
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.OTHER,
            title = "",
            fileReferenceId = validFileRef,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateSubmission_titleExceeds200Chars_fails() {
        val longTitle = "A".repeat(201)
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.OTHER,
            title = longTitle,
            fileReferenceId = validFileRef,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateSubmission_titleExactly200Chars_succeeds() {
        val exactTitle = "B".repeat(200)
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.OTHER,
            title = exactTitle,
            fileReferenceId = validFileRef,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateSubmission_blankFileReferenceId_fails() {
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.OTHER,
            title = validTitle,
            fileReferenceId = "  ",
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateSubmission_blankActorId_fails() {
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.OTHER,
            title = validTitle,
            fileReferenceId = validFileRef,
            actorId = ""
        )
        assertTrue(result is DomainResult.Error)
    }

    // ─────────────────────────────────────────────
    // Expiry date validation
    // ─────────────────────────────────────────────

    @Test
    fun validateSubmission_typeRequiresExpiry_expiryMissing_fails() {
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.BUSINESS_LICENSE,
            title = validTitle,
            fileReferenceId = validFileRef,
            expiryDate = null,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateSubmission_expiryBeforeIssueDate_fails() {
        val now = System.currentTimeMillis()
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.TRADE_LICENSE,
            title = validTitle,
            fileReferenceId = validFileRef,
            issueDate = now,
            expiryDate = now - 1000L,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateSubmission_expiryEqualsIssueDate_succeeds() {
        val now = System.currentTimeMillis()
        val result = VendorDocumentValidator.validateSubmission(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.TRADE_LICENSE,
            title = validTitle,
            fileReferenceId = validFileRef,
            issueDate = now,
            expiryDate = now,
            actorId = validActorId
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateSubmission_multipleErrors_errorMessageContainsAllFields() {
        val result = VendorDocumentValidator.validateSubmission(
            projectId = "",
            vendorId = "",
            documentType = VendorDocumentType.OTHER,
            title = "",
            fileReferenceId = "",
            actorId = ""
        )
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("projectId"))
        assertTrue(msg.contains("vendorId"))
        assertTrue(msg.contains("title"))
        assertTrue(msg.contains("fileReferenceId"))
        assertTrue(msg.contains("actorId"))
    }
}
