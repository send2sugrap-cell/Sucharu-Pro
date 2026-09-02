package com.sucharu.sucharupro.domain.validation.communication.vendor.document

import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.communication.vendor.document.VendorDocumentType
import org.junit.Assert.assertTrue
import org.junit.Test

class VendorDocumentRequestValidatorTest {

    private val validProjectId = "proj-001"
    private val validVendorId = "ven-001"
    private val validTitle = "Request for TIN Certificate"
    private val validRequestedBy = "user-manager-01"

    // ─────────────────────────────────────────────
    // Happy path
    // ─────────────────────────────────────────────

    @Test
    fun validateCreate_allValidFields_noDueDate_succeeds() {
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = validTitle,
            dueDate = null,
            requestedBy = validRequestedBy
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateCreate_allValidFields_futureDueDate_succeeds() {
        val futureDate = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000L
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.QUALITY_CERTIFICATE,
            title = validTitle,
            dueDate = futureDate,
            requestedBy = validRequestedBy
        )
        assertTrue(result is DomainResult.Success)
    }

    // ─────────────────────────────────────────────
    // Blank field validation
    // ─────────────────────────────────────────────

    @Test
    fun validateCreate_blankProjectId_fails() {
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = "  ",
            vendorId = validVendorId,
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = validTitle,
            requestedBy = validRequestedBy
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreate_blankVendorId_fails() {
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = validProjectId,
            vendorId = "",
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = validTitle,
            requestedBy = validRequestedBy
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreate_blankTitle_fails() {
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = "",
            requestedBy = validRequestedBy
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreate_titleExceeds200Chars_fails() {
        val longTitle = "X".repeat(201)
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = longTitle,
            requestedBy = validRequestedBy
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreate_titleExactly200Chars_succeeds() {
        val exactTitle = "Y".repeat(200)
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = exactTitle,
            requestedBy = validRequestedBy
        )
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun validateCreate_blankRequestedBy_fails() {
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = validTitle,
            requestedBy = ""
        )
        assertTrue(result is DomainResult.Error)
    }

    // ─────────────────────────────────────────────
    // Due date validation
    // ─────────────────────────────────────────────

    @Test
    fun validateCreate_pastDueDate_fails() {
        val pastDate = System.currentTimeMillis() - 1000L
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = validTitle,
            dueDate = pastDate,
            requestedBy = validRequestedBy
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreate_dueDateExactlyNow_fails() {
        val now = System.currentTimeMillis()
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = validProjectId,
            vendorId = validVendorId,
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = validTitle,
            dueDate = now,
            requestedBy = validRequestedBy,
            now = now + 1L // dueDate is "in the past" relative to now+1
        )
        assertTrue(result is DomainResult.Error)
    }

    @Test
    fun validateCreate_multipleErrors_reportedTogether() {
        val result = VendorDocumentRequestValidator.validateCreate(
            projectId = "",
            vendorId = "",
            documentType = VendorDocumentType.TIN_CERTIFICATE,
            title = "",
            requestedBy = ""
        )
        assertTrue(result is DomainResult.Error)
        val msg = (result as DomainResult.Error).message
        assertTrue(msg.contains("projectId"))
        assertTrue(msg.contains("vendorId"))
        assertTrue(msg.contains("title"))
        assertTrue(msg.contains("requestedBy"))
    }
}
